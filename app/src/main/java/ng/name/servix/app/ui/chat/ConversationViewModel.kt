package ng.name.servix.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.data.model.ChatMessageDto
import ng.name.servix.app.data.realtime.ChatEvent
import ng.name.servix.app.data.realtime.ChatEventStream
import ng.name.servix.app.data.model.ChatAttachmentDto
import ng.name.servix.app.data.model.ChatAttachmentInput
import ng.name.servix.app.data.model.ChatUploadKind
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.repository.ChatRepository
import ng.name.servix.app.data.repository.UploadRepository
import java.time.Instant
import java.util.UUID

/**
 * Why a send did not land. The API separates "you may not" from "not right now", and the bubble says
 * which one it is instead of a generic failure (community.ts: `429 MESSAGE_LIMIT` = 30/min).
 */
enum class SendFailure {
    /** `429 MESSAGE_LIMIT` — retrying immediately will just fail again. */
    RATE_LIMITED,

    /** Blocked, unaccepted, offline — retrying may work. */
    OTHER,
}

data class ConversationUiState(
    val loading: Boolean = true,
    val loadingOlder: Boolean = false,
    val messages: List<ChatMessageDto> = emptyList(),
    val rows: List<ChatRow> = emptyList(),
    val input: String = "",
    val canSend: Boolean = true,
    val sending: Boolean = false,
    val hasMore: Boolean = false,
    val olderCursor: String? = null,
    val online: Boolean = false,
    val lastSeenAt: String? = null,
    val otherReadAt: String? = null,
    val otherDeliveredAt: String? = null,
    val otherTyping: Boolean = false,
    val myUserId: String = "",
    val pendingClients: Set<String> = emptySet(),
    /** clientId → why it failed; the message (and its clientId) is kept so retry stays idempotent. */
    val failures: Map<String, SendFailure> = emptyMap(),
    val error: String? = null,
    val reactions: List<String> = DEFAULT_REACTIONS,
    /** The message being quoted in the composer (`replyToId` on send). */
    val replyTo: ChatMessageDto? = null,
    /** clientId → 0..1 while the file streams to `POST /uploads`. */
    val uploadProgress: Map<String, Float> = emptyMap(),
    /** Long-press target: the action sheet (react / reply / copy / delete). */
    val actionTarget: ChatMessageDto? = null,
    /** Full-screen image viewer. */
    val viewerUrl: String? = null,
) {
    fun tickFor(message: ChatMessageDto): TickState? =
        ChatTicks.state(message, myUserId, otherReadAt, otherDeliveredAt, message.clientId in pendingClients)

    fun failureOf(message: ChatMessageDto): SendFailure? = message.clientId?.let { failures[it] }
}

/** The reaction set the API accepts (`community.ts:9 CHAT_REACTIONS`) — Stage 2 uses the picker. */
val DEFAULT_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "✅")

/**
 * One open conversation: history with `before` pagination, optimistic sends keyed by `clientId`,
 * read/delivered watermarks, typing with a 2.5 s throttle, and live upserts from the event stream.
 */
class ConversationViewModel(
    private val chatRepository: ChatRepository,
    private val uploadRepository: UploadRepository,
    private val stream: ChatEventStream,
    private val threadId: String,
    private val myUserId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState(myUserId = myUserId))
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    /** One entry per clientId that has not landed yet: its quote, its attachment and, while the bytes
     *  are still local, the upload that has to happen first. Retry re-runs exactly this. */
    private data class Outgoing(
        val replyToId: String? = null,
        val attachment: ChatAttachmentInput? = null,
        val pendingUpload: PendingUpload? = null,
    )

    private data class PendingUpload(
        val kind: String,
        val bytes: ByteArray,
        val fileName: String,
        val durationMs: Long? = null,
        val width: Int? = null,
        val height: Int? = null,
    )

    private val outbox = mutableMapOf<String, Outgoing>()

    private var visible = false
    private var otherUserId: String? = null
    private var typingJob: Job? = null
    private var otherTypingJob: Job? = null
    private var typingSentAt = 0L
    private var lastTypingSent = false

    init {
        open()
        viewModelScope.launch {
            stream.events.collect { event -> onEvent(event) }
        }
    }

    fun open() {
        _uiState.update { it.copy(loading = it.messages.isEmpty(), error = null) }
        viewModelScope.launch {
            chatRepository.messages(threadId).fold(
                onSuccess = { page ->
                    otherUserId = page.otherUserId
                    _uiState.update { state ->
                        val merged = ChatMerge.upsertAll(page.messages, state.messages.filter { it.id.isNotBlank() }) +
                            state.messages.filter { pending -> pending.id.isBlank() && page.messages.none { it.clientId != null && it.clientId == pending.clientId } }
                        state.copy(
                            loading = false,
                            messages = merged,
                            rows = ChatTimeline.build(merged, myUserId),
                            canSend = page.canSend,
                            hasMore = page.before != null,
                            olderCursor = page.before,
                            online = page.online,
                            lastSeenAt = page.lastSeenAt,
                            otherReadAt = page.otherReadAt,
                            otherDeliveredAt = page.otherDeliveredAt,
                            reactions = page.reactions.ifEmpty { DEFAULT_REACTIONS },
                        )
                    }
                    if (visible) markRead()
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(loading = false, error = failure.message ?: "This conversation could not be loaded.")
                    }
                },
            )
        }
    }

    /** Pull-to-load-older: `?before=<cursor>` keeps the newest 50 in place. */
    fun loadOlder() {
        val cursor = _uiState.value.olderCursor ?: return
        if (_uiState.value.loadingOlder) return
        _uiState.update { it.copy(loadingOlder = true) }
        viewModelScope.launch {
            chatRepository.messages(threadId, cursor).fold(
                onSuccess = { page ->
                    _uiState.update { state ->
                        val merged = ChatMerge.upsertAll(page.messages, state.messages)
                        state.copy(
                            loadingOlder = false,
                            messages = merged,
                            rows = ChatTimeline.build(merged, myUserId),
                            hasMore = page.before != null,
                            olderCursor = page.before,
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update { it.copy(loadingOlder = false, error = failure.message) }
                },
            )
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
        signalTyping(value.isNotBlank())
    }

    /**
     * Optimistic send: the bubble appears immediately with a `clientId`; the server row replaces it
     * (same `clientId`), so a retry can never duplicate (`senderId + clientId` is the API's key).
     */
    fun send() {
        val state = _uiState.value
        val body = state.input.trim()
        if (body.isEmpty() || state.sending || !state.canSend) return
        val client =
            ChatMessageDto(
                id = "",
                body = body,
                senderId = myUserId,
                createdAt = Instant.now().toString(),
                clientId = UUID.randomUUID().toString(),
                kind = "text",
            )
        _uiState.update {
            val messages = it.messages + client
            it.copy(
                input = "",
                messages = messages,
                rows = ChatTimeline.build(messages, myUserId),
                pendingClients = it.pendingClients + (client.clientId ?: ""),
                failures = it.failures - (client.clientId ?: ""),
                error = null,
            )
        }
        signalTyping(false)
        post(client)
    }

    fun retry(clientId: String) {
        val message = _uiState.value.messages.firstOrNull { it.clientId == clientId && it.id.isBlank() } ?: return
        _uiState.update {
            it.copy(
                pendingClients = it.pendingClients + clientId,
                failures = it.failures - clientId,
                error = null,
            )
        }
        post(message)
    }

    private fun post(message: ChatMessageDto) {
        val clientId = message.clientId ?: return
        val outgoing = outbox[clientId] ?: Outgoing()
        _uiState.update { it.copy(sending = true) }
        viewModelScope.launch {
            // Media is two calls: the raw file to POST /uploads, then the message carrying its key.
            val uploaded = outgoing.attachment ?: outgoing.pendingUpload?.let { pending ->
                uploadRepository.upload(pending.kind, pending.bytes, pending.fileName) { fraction ->
                    _uiState.update { it.copy(uploadProgress = it.uploadProgress + (clientId to fraction)) }
                }.getOrElse { failure ->
                    finishSend(clientId, failure)
                    return@launch
                }.let { file ->
                    val input = uploadRepository.attachmentFor(
                        uploaded = file,
                        name = pending.fileName,
                        bytes = pending.bytes.size.toLong(),
                        durationMs = pending.durationMs,
                        width = pending.width,
                        height = pending.height,
                    )
                    outbox[clientId] = outgoing.copy(attachment = input, pendingUpload = null)
                    input
                }
            }
            chatRepository.send(threadId, clientId, message.body, outgoing.replyToId, uploaded).fold(
                onSuccess = { saved ->
                    _uiState.update { state ->
                        val merged = ChatMerge.upsert(state.messages, saved)
                        state.copy(
                            sending = false,
                            messages = merged,
                            rows = ChatTimeline.build(merged, myUserId),
                            pendingClients = state.pendingClients - clientId,
                            failures = state.failures - clientId,
                            uploadProgress = state.uploadProgress - clientId,
                            replyTo = if (state.replyTo?.clientId == clientId) null else state.replyTo,
                        )
                    }
                    outbox.remove(clientId)
                },
                onFailure = { failure ->
                    val api = failure as? ng.name.servix.app.data.model.ServixApiException
                    val reason = if (api != null && (api.status == 429 || api.code == "MESSAGE_LIMIT")) {
                        SendFailure.RATE_LIMITED
                    } else {
                        SendFailure.OTHER
                    }
                    finishSend(clientId, failure, reason)
                },
            )
        }
    }

    fun setVisible(isVisible: Boolean) {
        if (visible == isVisible) return
        visible = isVisible
        if (isVisible) markRead()
    }

    /** Opening / returning to the thread: `POST …/read` then `POST …/delivered`. */
    fun markRead() {
        viewModelScope.launch {
            chatRepository.markRead(threadId)
            chatRepository.markDelivered(threadId)
        }
    }

    /** Contact card → Block (`POST /community/threads/:id/block`). */
    fun block() {
        viewModelScope.launch {
            chatRepository.block(threadId).fold(
                onSuccess = { _uiState.update { state -> state.copy(canSend = false) } },
                onFailure = { failure ->
                    _uiState.update { state -> state.copy(error = failure.message ?: "This person could not be blocked.") }
                },
            )
        }
    }

    /** One typing call per 2.5 s, and a "stopped typing" after 3 s of silence or on send. */
    private fun signalTyping(typing: Boolean) {
        val now = System.currentTimeMillis()
        typingJob?.cancel()
        if (!typing) {
            if (lastTypingSent) {
                lastTypingSent = false
                viewModelScope.launch { chatRepository.typing(threadId, false) }
            }
            return
        }
        if (now - typingSentAt >= TYPING_THROTTLE_MS) {
            typingSentAt = now
            lastTypingSent = true
            viewModelScope.launch { chatRepository.typing(threadId, true) }
        }
        typingJob = viewModelScope.launch {
            delay(TYPING_IDLE_MS)
            if (lastTypingSent) {
                lastTypingSent = false
                chatRepository.typing(threadId, false)
            }
        }
    }

    // ---------------------------------------------------------------- Stage 2: media, replies, reactions, delete

    /** Image picked from the system picker. `previewUri` renders the local file until the upload lands. */
    fun sendImage(bytes: ByteArray, fileName: String, previewUri: String?) {
        sendMedia(
            kind = ChatUploadKind.IMAGE,
            bytes = bytes,
            fileName = fileName,
            previewUri = previewUri,
            width = null,
            height = null,
        )
    }

    /** Voice note recorded in the composer; the duration pill comes from [durationMs]. */
    fun sendVoice(bytes: ByteArray, fileName: String, durationMs: Long) {
        sendMedia(
            kind = ChatUploadKind.AUDIO,
            bytes = bytes,
            fileName = fileName,
            previewUri = null,
            durationMs = durationMs.coerceIn(0, 3_600_000),
        )
    }

    fun sendFile(bytes: ByteArray, fileName: String) {
        sendMedia(kind = ChatUploadKind.FILE, bytes = bytes, fileName = fileName, previewUri = null)
    }

    private fun sendMedia(
        kind: String,
        bytes: ByteArray,
        fileName: String,
        previewUri: String?,
        durationMs: Long? = null,
        width: Int? = null,
        height: Int? = null,
    ) {
        val state = _uiState.value
        if (!state.canSend) return
        if (bytes.isEmpty()) {
            _uiState.update { it.copy(error = "That file is empty.") }
            return
        }
        val clientId = UUID.randomUUID().toString()
        val optimistic = ChatMessageDto(
            id = "",
            body = state.input.trim(),
            senderId = myUserId,
            createdAt = Instant.now().toString(),
            clientId = clientId,
            kind = when (kind) {
                ChatUploadKind.IMAGE -> "image"
                ChatUploadKind.AUDIO -> "audio"
                else -> "file"
            },
            attachment = ChatAttachmentDto(
                // A local URI is only a preview; the server row replaces it with the bucket URL.
                url = previewUri.orEmpty(),
                name = fileName,
                size = bytes.size.toLong(),
                durationMs = durationMs,
            ),
        )
        val replyToId = state.replyTo?.id?.takeIf { it.isNotBlank() }
        outbox[clientId] = Outgoing(
            replyToId = replyToId,
            pendingUpload = PendingUpload(kind, bytes, fileName, durationMs, width, height),
        )
        _uiState.update {
            val messages = it.messages + optimistic
            it.copy(
                input = "",
                messages = messages,
                rows = ChatTimeline.build(messages, myUserId),
                pendingClients = it.pendingClients + clientId,
                failures = it.failures - clientId,
                replyTo = null,
                error = null,
            )
        }
        signalTyping(false)
        post(optimistic)
    }

    /** A screen-level problem the user must see (an unreadable file, a failed pick). */
    fun showNotice(text: String) {
        _uiState.update { it.copy(error = text) }
    }

    fun reply(message: ChatMessageDto) {
        _uiState.update { it.copy(replyTo = message, actionTarget = null) }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyTo = null) }
    }

    fun openActions(message: ChatMessageDto) {
        _uiState.update { it.copy(actionTarget = message) }
    }

    fun closeActions() {
        _uiState.update { it.copy(actionTarget = null) }
    }

    fun openViewer(url: String) {
        _uiState.update { it.copy(viewerUrl = url) }
    }

    fun closeViewer() {
        _uiState.update { it.copy(viewerUrl = null) }
    }

    /**
     * One reaction per member per message (`community.ts:236-247`): posting the emoji you already
     * chose removes it. The pill updates immediately and is reconciled with the server row.
     */
    fun toggleReaction(message: ChatMessageDto, emoji: String) {
        val id = message.id.takeIf { it.isNotBlank() } ?: return
        val mine = message.reactions[emoji].orEmpty().contains(myUserId)
        patchMessage(message.withReaction(emoji, myUserId, add = !mine))
        viewModelScope.launch {
            chatRepository.react(threadId, id, emoji).fold(
                onSuccess = { updated -> upsert(updated) },
                onFailure = { failure ->
                    patchMessage(message)
                    _uiState.update { it.copy(error = failure.message ?: "That reaction did not go through.") }
                },
            )
        }
    }

    /** Delete for everyone — sender only, tombstone row, and `message_update` does the same for them. */
    fun deleteForEveryone(message: ChatMessageDto) {
        val id = message.id.takeIf { it.isNotBlank() } ?: return
        _uiState.update { it.copy(actionTarget = null) }
        viewModelScope.launch {
            chatRepository.delete(threadId, id).fold(
                onSuccess = { tombstone -> upsert(tombstone) },
                onFailure = { failure ->
                    _uiState.update { it.copy(error = failure.message ?: "That message could not be deleted.") }
                },
            )
        }
    }

    private fun finishSend(clientId: String, failure: Throwable, reason: SendFailure = classify(failure)) {
        _uiState.update { state ->
            state.copy(
                sending = false,
                pendingClients = state.pendingClients - clientId,
                // The clientId stays on the message, so "Tap to retry" is the same idempotency key.
                failures = state.failures + (clientId to reason),
                uploadProgress = state.uploadProgress - clientId,
                error = failure.message ?: "Message not sent.",
            )
        }
    }

    private fun classify(failure: Throwable): SendFailure {
        val api = failure as? ServixApiException
        return if (api != null && (api.status == 429 || api.code == "MESSAGE_LIMIT")) {
            SendFailure.RATE_LIMITED
        } else {
            SendFailure.OTHER
        }
    }

    private fun patchMessage(message: ChatMessageDto) {
        _uiState.update { state ->
            val replaced = state.messages.map { existing ->
                if (existing.id == message.id && message.id.isNotBlank()) message else existing
            }
            state.copy(messages = replaced, rows = ChatTimeline.build(replaced, myUserId))
        }
    }

    private fun ChatMessageDto.withReaction(emoji: String, me: String, add: Boolean): ChatMessageDto {
        val next = reactions.toMutableMap()
        val users = next[emoji].orEmpty().toMutableList()
        if (add) {
            if (me !in users) users += me
        } else {
            users -= me
        }
        if (users.isEmpty()) next.remove(emoji) else next[emoji] = users
        return copy(reactions = next)
    }

    private fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.Message -> if (event.threadId == threadId) {
                upsert(event.message)
                if (visible && event.message.senderId != myUserId) markRead()
            }
            is ChatEvent.MessageUpdate -> if (event.threadId == threadId) upsert(event.message)
            is ChatEvent.Typing -> if (event.threadId == threadId && event.userId != myUserId) {
                _uiState.update { it.copy(otherTyping = event.typing) }
                otherTypingJob?.cancel()
                if (event.typing) {
                    otherTypingJob = viewModelScope.launch {
                        delay(TYPING_IDLE_MS)
                        _uiState.update { it.copy(otherTyping = false) }
                    }
                }
            }
            is ChatEvent.Read -> if (event.threadId == threadId) {
                _uiState.update { it.copy(otherReadAt = event.at) }
            }
            is ChatEvent.Delivered -> if (event.threadId == threadId) {
                _uiState.update { it.copy(otherDeliveredAt = event.at) }
            }
            is ChatEvent.Presence -> if (event.userId == otherUserId) _uiState.update { it.copy(online = event.online, lastSeenAt = event.lastSeen ?: it.lastSeenAt) }
            is ChatEvent.Hello -> otherUserId?.let { other ->
                _uiState.update { it.copy(online = other in event.online, lastSeenAt = event.lastSeen[other] ?: it.lastSeenAt) }
            }
            is ChatEvent.ThreadEvent -> if (event.threadId == threadId && visible) open()
            is ChatEvent.ConnectionEvent -> if (visible) open()
            is ChatEvent.Unknown -> Unit
        }
    }

    private fun upsert(message: ChatMessageDto) {
        _uiState.update { state ->
            val merged = ChatMerge.upsert(state.messages, message)
            state.copy(
                messages = merged,
                rows = ChatTimeline.build(merged, myUserId),
                // A server echo of my own optimistic message clears its clock immediately.
                pendingClients = message.clientId?.let { state.pendingClients - it } ?: state.pendingClients,
                failures = message.clientId?.let { state.failures - it } ?: state.failures,
            )
        }
    }

    companion object {
        const val TYPING_THROTTLE_MS = 2_500L
        const val TYPING_IDLE_MS = 3_000L

        fun factory(
            chatRepository: ChatRepository,
            uploadRepository: UploadRepository,
            stream: ChatEventStream,
            threadId: String,
            myUserId: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ConversationViewModel(chatRepository, uploadRepository, stream, threadId, myUserId) as T
        }
    }
}
