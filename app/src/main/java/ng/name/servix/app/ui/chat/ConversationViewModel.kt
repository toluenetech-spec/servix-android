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
import ng.name.servix.app.data.repository.ChatRepository
import java.time.Instant
import java.util.UUID

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
    val failedClients: Set<String> = emptySet(),
    val error: String? = null,
    val reactions: List<String> = DEFAULT_REACTIONS,
) {
    fun tickFor(message: ChatMessageDto): TickState? =
        ChatTicks.state(message, myUserId, otherReadAt, otherDeliveredAt, message.clientId in pendingClients)

    fun failed(message: ChatMessageDto): Boolean = message.clientId?.let { it in failedClients } == true
}

/** The reaction set the API accepts (`community.ts:9 CHAT_REACTIONS`) — Stage 2 uses the picker. */
val DEFAULT_REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "✅")

/**
 * One open conversation: history with `before` pagination, optimistic sends keyed by `clientId`,
 * read/delivered watermarks, typing with a 2.5 s throttle, and live upserts from the event stream.
 */
class ConversationViewModel(
    private val chatRepository: ChatRepository,
    private val stream: ChatEventStream,
    private val threadId: String,
    private val myUserId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState(myUserId = myUserId))
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private var typingJob: Job? = null
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
                    _uiState.update { state ->
                        val merged = ChatMerge.upsertAll(page.messages, state.messages.filter { it.id.isBlank() })
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
                    markRead()
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
                failedClients = it.failedClients - (client.clientId ?: ""),
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
                failedClients = it.failedClients - clientId,
                error = null,
            )
        }
        post(message)
    }

    private fun post(message: ChatMessageDto) {
        val clientId = message.clientId ?: return
        _uiState.update { it.copy(sending = true) }
        viewModelScope.launch {
            chatRepository.send(threadId, clientId, message.body).fold(
                onSuccess = { saved ->
                    _uiState.update { state ->
                        val merged = ChatMerge.upsert(state.messages, saved)
                        state.copy(
                            sending = false,
                            messages = merged,
                            rows = ChatTimeline.build(merged, myUserId),
                            pendingClients = state.pendingClients - clientId,
                            failedClients = state.failedClients - clientId,
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update { state ->
                        state.copy(
                            sending = false,
                            pendingClients = state.pendingClients - clientId,
                            failedClients = state.failedClients + clientId,
                            // The API's own message, shown next to "Tap to retry" (plan Stage 2 error rule).
                            error = failure.message ?: "Message not sent.",
                        )
                    }
                },
            )
        }
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

    private fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.Message -> if (event.threadId == threadId) {
                upsert(event.message)
                if (event.message.senderId != myUserId) markRead()
            }
            is ChatEvent.MessageUpdate -> if (event.threadId == threadId) upsert(event.message)
            is ChatEvent.Typing -> if (event.threadId == threadId && event.userId != myUserId) {
                _uiState.update { it.copy(otherTyping = event.typing) }
                if (event.typing) {
                    typingJob?.cancel()
                    viewModelScope.launch {
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
            is ChatEvent.Presence -> _uiState.update { it.copy(online = event.online, lastSeenAt = event.lastSeen ?: it.lastSeenAt) }
            is ChatEvent.Hello -> Unit
            is ChatEvent.ThreadEvent, is ChatEvent.ConnectionEvent, is ChatEvent.Unknown -> Unit
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
                failedClients = message.clientId?.let { state.failedClients - it } ?: state.failedClients,
            )
        }
    }

    companion object {
        const val TYPING_THROTTLE_MS = 2_500L
        const val TYPING_IDLE_MS = 3_000L

        fun factory(
            chatRepository: ChatRepository,
            stream: ChatEventStream,
            threadId: String,
            myUserId: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ConversationViewModel(chatRepository, stream, threadId, myUserId) as T
        }
    }
}
