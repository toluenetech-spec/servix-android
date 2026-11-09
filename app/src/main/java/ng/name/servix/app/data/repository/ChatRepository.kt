package ng.name.servix.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ng.name.servix.app.data.model.ChatMessagesResponse
import ng.name.servix.app.data.model.ChatThreadDto
import ng.name.servix.app.data.model.FeaturesDto
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.ServixApiService

/**
 * `GET /features` — the only source of truth for which optional areas the app may show
 * (`api/src/lib/features.ts`). The app never hardcodes a flag: a failed read leaves every flag as
 * `false`, so a gated area disappears rather than opening onto a 503.
 */
data class FeaturesState(
    val flags: FeaturesDto = FeaturesDto(),
    /** True once at least one successful read happened, so the UI can distinguish "off" from "unknown". */
    val loaded: Boolean = false,
    val failed: Boolean = false,
)

class FeaturesRepository(private val api: ServixApiService) {
    private val _state = MutableStateFlow(FeaturesState())
    val state: StateFlow<FeaturesState> = _state.asStateFlow()

    suspend fun refresh(): FeaturesState = withContext(Dispatchers.IO) {
        val next = runCatching { api.getFeatures() }
            .fold(
                onSuccess = { FeaturesState(flags = it, loaded = true) },
                onFailure = { _state.value.copy(loaded = _state.value.loaded, failed = true) },
            )
        _state.value = next
        next
    }
}

/**
 * Chat data access — `api/docs/CHAT.md` is authoritative; every call below maps to a route in
 * `api/src/routes/community.ts`.
 */
class ChatRepository(private val api: ServixApiService) {

    suspend fun threads(): Result<List<ChatThreadDto>> = safe { api.getChatThreads() }

    suspend fun messages(threadId: String, before: String? = null): Result<ChatMessagesResponse> =
        safe { api.getChatMessages(threadId, before) }

    suspend fun send(
        threadId: String,
        clientId: String,
        body: String,
        replyToId: String? = null,
        attachment: ng.name.servix.app.data.model.ChatAttachmentInput? = null,
    ) = safe {
        api.sendChatMessage(
            threadId,
            ng.name.servix.app.data.model.SendChatMessageRequest(
                clientId = clientId,
                body = body,
                replyToId = replyToId,
                attachment = attachment,
            ),
        )
    }

    /** `POST …/messages/:mid/react { emoji }` → the full message (also arrives as `message_update`). */
    suspend fun react(threadId: String, messageId: String, emoji: String) =
        safe { api.reactToChatMessage(threadId, messageId, ng.name.servix.app.data.model.ChatReactionRequest(emoji)) }

    /** `DELETE …/messages/:mid` — sender only, tombstone row, object removed from storage. */
    suspend fun delete(threadId: String, messageId: String) =
        safe { api.deleteChatMessage(threadId, messageId) }

    suspend fun markRead(threadId: String): Result<Unit> = safe { api.markChatThreadRead(threadId); Unit }

    suspend fun markDelivered(threadId: String): Result<Unit> = safe { api.markChatThreadDelivered(threadId); Unit }

    suspend fun typing(threadId: String, typing: Boolean): Result<Unit> =
        safe { api.sendChatTyping(threadId, ng.name.servix.app.data.model.ChatTypingRequest(typing)); Unit }

    suspend fun block(threadId: String): Result<Unit> = safe { api.blockChatThread(threadId); Unit }

    /** `POST /community/threads` — one of the two ids, exactly as the API requires. */
    suspend fun startFromBooking(bookingId: String): Result<String> =
        safe { api.createChatThread(ng.name.servix.app.data.model.CreateChatThreadRequest(bookingId = bookingId)).id }

    suspend fun startFromConnection(connectionId: String): Result<String> =
        safe { api.createChatThread(ng.name.servix.app.data.model.CreateChatThreadRequest(connectionId = connectionId)).id }

    private suspend fun <T> safe(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }
}
