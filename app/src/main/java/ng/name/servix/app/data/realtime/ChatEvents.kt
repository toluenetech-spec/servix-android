package ng.name.servix.app.data.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import ng.name.servix.app.data.model.ChatMessageDto
import ng.name.servix.app.data.network.ApiErrorParser
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.atomic.AtomicReference

/** Connection state shown in the Chats header (same vocabulary as the web client). */
enum class ChatConnection { IDLE, CONNECTING, LIVE, RECONNECTING, UNAVAILABLE }

/** One frame of `GET /community/events` (`api/src/lib/chatEvents.ts`). */
sealed interface ChatEvent {
    data class Hello(val online: Set<String>, val lastSeen: Map<String, String>) : ChatEvent
    data class Message(val threadId: String, val message: ChatMessageDto) : ChatEvent
    data class MessageUpdate(val threadId: String, val message: ChatMessageDto) : ChatEvent
    data class Typing(val threadId: String, val userId: String, val typing: Boolean) : ChatEvent
    data class Read(val threadId: String, val userId: String, val at: String) : ChatEvent
    data class Delivered(val threadId: String, val userId: String, val at: String) : ChatEvent
    data class Presence(val userId: String, val online: Boolean, val lastSeen: String?) : ChatEvent
    data class ConnectionEvent(val connectionId: String, val status: String) : ChatEvent
    data class ThreadEvent(val threadId: String) : ChatEvent
    data class Unknown(val type: String) : ChatEvent
}

/**
 * Parses one line of the event stream. Pure and unit-tested (`ChatEventParserTest`).
 *
 * The API writes `data: {json}\n\n` frames and `: ping` keep-alives (`chatEvents.ts:127-129`); the
 * event kind is a `type` field inside the JSON, not an SSE `event:` line.
 */
object ChatEventParser {
    private val json: Json = ApiErrorParser.json

    fun parse(line: String): ChatEvent? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith(":")) return null // ": ping" comment
        if (!trimmed.startsWith("data:")) return null
        val payload = trimmed.removePrefix("data:").trim()
        val obj = runCatching { json.parseToJsonElement(payload) as? JsonObject }.getOrNull() ?: return null
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return null
        fun str(key: String): String? = obj[key]?.jsonPrimitive?.contentOrNull
        return when (type) {
            "hello" -> ChatEvent.Hello(
                online = (obj["online"] as? kotlinx.serialization.json.JsonArray)
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet().orEmpty(),
                lastSeen = (obj["lastSeen"] as? JsonObject)
                    ?.mapNotNull { (k, v) -> v.jsonPrimitive.contentOrNull?.let { k to it } }?.toMap().orEmpty(),
            )
            "message" -> ChatEvent.Message(str("threadId").orEmpty(), message(obj) ?: return null)
            "message_update" -> ChatEvent.MessageUpdate(str("threadId").orEmpty(), message(obj) ?: return null)
            "typing" -> ChatEvent.Typing(
                threadId = str("threadId").orEmpty(),
                userId = str("userId").orEmpty(),
                typing = obj["typing"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
            )
            "read" -> ChatEvent.Read(str("threadId").orEmpty(), str("userId").orEmpty(), str("at").orEmpty())
            "delivered" -> ChatEvent.Delivered(str("threadId").orEmpty(), str("userId").orEmpty(), str("at").orEmpty())
            "presence" -> ChatEvent.Presence(
                userId = str("userId").orEmpty(),
                online = obj["online"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                lastSeen = str("lastSeen"),
            )
            "connection" -> ChatEvent.ConnectionEvent(str("connectionId").orEmpty(), str("status").orEmpty())
            "thread" -> ChatEvent.ThreadEvent(str("threadId").orEmpty())
            else -> ChatEvent.Unknown(type)
        }
    }

    private fun message(obj: JsonObject): ChatMessageDto? {
        val node = obj["message"] ?: return null
        return runCatching { json.decodeFromJsonElement(ChatMessageDto.serializer(), node) }.getOrNull()
    }
}

/**
 * Long-lived SSE GET over the app's authenticated OkHttp client.
 *
 * One stream per app session, opened while the app is in the foreground (`start`/`stop` from the
 * shell). Reconnects with exponential backoff (1 s → cap 30 s); while it is not `LIVE` the Chats
 * screen polls every 15 s instead, so a dead stream degrades to the web's own fallback.
 */
class ChatEventStream(
    private val client: OkHttpClient,
    private val eventsUrl: String,
) {
    private val _connection = MutableStateFlow(ChatConnection.IDLE)
    val connection: StateFlow<ChatConnection> = _connection.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private val call = AtomicReference<Call?>(null)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            var attempt = 0
            while (currentCoroutineContext().isActive) {
                _connection.value = if (attempt == 0) ChatConnection.CONNECTING else ChatConnection.RECONNECTING
                val reachedLive = runOnce()
                if (!currentCoroutineContext().isActive) break
                if (reachedLive) attempt = 0 else attempt = (attempt + 1).coerceAtMost(5)
                _connection.value = if (attempt == 0) ChatConnection.CONNECTING else ChatConnection.RECONNECTING
                delay(backoffMs(attempt))
            }
            _connection.value = ChatConnection.IDLE
        }
    }

    fun stop() {
        call.getAndSet(null)?.cancel()
        job?.cancel()
        job = null
        _connection.value = ChatConnection.IDLE
    }

    /** Returns true when the stream was accepted (LIVE) even if it later ended. */
    private suspend fun runOnce(): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(eventsUrl)
            .header("Accept", "text/event-stream")
            .get()
            .build()
        val active = client.newCall(request)
        call.set(active)
        try {
            active.execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 503 || response.code == 403) _connection.value = ChatConnection.UNAVAILABLE
                    return@use false
                }
                val source = response.body?.source() ?: return@use false
                _connection.value = ChatConnection.LIVE
                while (true) {
                    val line = source.readUtf8Line() ?: break
                    ChatEventParser.parse(line)?.let { _events.tryEmit(it) }
                }
                true
            }
        } catch (_: Throwable) {
            false
        } finally {
            call.compareAndSet(active, null)
        }
    }

    private fun backoffMs(attempt: Int): Long {
        if (attempt <= 0) return 1_000
        return (1_000L shl attempt).coerceAtMost(30_000L)
    }
}
