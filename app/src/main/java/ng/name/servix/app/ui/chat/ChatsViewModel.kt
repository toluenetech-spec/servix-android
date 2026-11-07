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
import ng.name.servix.app.data.model.ChatThreadDto
import ng.name.servix.app.data.realtime.ChatConnection
import ng.name.servix.app.data.realtime.ChatEvent
import ng.name.servix.app.data.realtime.ChatEventStream
import ng.name.servix.app.data.repository.ChatRepository
import ng.name.servix.app.data.repository.FeaturesRepository

/** Everything the Chats list needs, plus the single unread total the bar and Home both read. */
data class ChatsUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val threads: List<ChatThreadDto> = emptyList(),
    /** Sum of `unreadCount` — the ONE source for the tab badge and the Home card. */
    val unreadTotal: Int = 0,
    val connection: ChatConnection = ChatConnection.IDLE,
    val aiEnabled: Boolean = false,
    val error: String? = null,
    val empty: Boolean = false,
)

/**
 * Backs the Chats tab: `GET /community/threads` + `GET /community/events`
 * (`api/docs/CHAT.md`). While the stream is not live it polls every 15 s, exactly like the web
 * client, so a killed connection degrades instead of freezing.
 */
class ChatsViewModel(
    private val chatRepository: ChatRepository,
    private val featuresRepository: FeaturesRepository,
    private val stream: ChatEventStream,
    private val myUserId: () -> String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var streamStarted = false

    init {
        viewModelScope.launch {
            val features = featuresRepository.refresh()
            _uiState.update { it.copy(aiEnabled = features.flags.ai) }
        }
        viewModelScope.launch {
            stream.connection.collect { connection ->
                _uiState.update { it.copy(connection = connection) }
            }
        }
        viewModelScope.launch {
            stream.events.collect { event -> onEvent(event) }
        }
        refresh()
    }

    /** Called by the shell when the app comes to the foreground; the stream lives only while visible. */
    fun startStream() {
        if (streamStarted) return
        streamStarted = true
        stream.start(viewModelScope)
    }

    fun stopStream() {
        if (!streamStarted) return
        streamStarted = false
        stream.stop()
    }

    fun refresh(quiet: Boolean = false) {
        if (!quiet) _uiState.update { it.copy(loading = it.threads.isEmpty(), refreshing = !it.loading) }
        viewModelScope.launch {
            chatRepository.threads().fold(
                onSuccess = { threads ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            threads = threads,
                            unreadTotal = threads.sumOf { thread -> thread.unreadCount },
                            empty = threads.isEmpty(),
                            error = null,
                        )
                    }
                    ensurePolling()
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = if (it.threads.isEmpty()) failure.message ?: "Chats could not be loaded." else it.error,
                        )
                    }
                },
            )
        }
    }

    /** Opening a thread clears its badge immediately; the API call follows from the conversation. */
    fun markReadLocally(threadId: String) {
        _uiState.update { state ->
            val threads = state.threads.map { thread ->
                if (thread.id == threadId) thread.copy(unread = false, unreadCount = 0) else thread
            }
            state.copy(threads = threads, unreadTotal = threads.sumOf { it.unreadCount })
        }
    }

    fun thread(threadId: String): ChatThreadDto? = _uiState.value.threads.firstOrNull { it.id == threadId }

    private fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.Message -> {
                if (_uiState.value.threads.any { it.id == event.threadId }) {
                    applyIncoming(event.threadId, event.message.body, event.message.createdAt, event.message.senderId, event.message.kind)
                } else {
                    refresh(quiet = true)
                }
            }
            is ChatEvent.MessageUpdate -> {
                if (_uiState.value.threads.any { it.id == event.threadId }) {
                    applyIncoming(event.threadId, event.message.body, event.message.createdAt, event.message.senderId, event.message.kind)
                }
            }
            is ChatEvent.Read -> {
                _uiState.update { state ->
                    state.copy(threads = state.threads.map { if (it.id == event.threadId) it.copy(otherReadAt = event.at) else it })
                }
            }
            is ChatEvent.Delivered -> {
                _uiState.update { state ->
                    state.copy(threads = state.threads.map { if (it.id == event.threadId) it.copy(otherDeliveredAt = event.at) else it })
                }
            }
            is ChatEvent.Presence -> {
                _uiState.update { state ->
                    state.copy(
                        threads = state.threads.map { thread ->
                            if (thread.person?.id == event.userId) {
                                thread.copy(online = event.online, lastSeenAt = event.lastSeen ?: thread.lastSeenAt)
                            } else {
                                thread
                            }
                        },
                    )
                }
            }
            is ChatEvent.Hello -> {
                _uiState.update { state ->
                    state.copy(
                        threads = state.threads.map { thread ->
                            thread.copy(
                                online = thread.person?.id?.let { it in event.online } ?: thread.online,
                                lastSeenAt = thread.person?.id?.let { event.lastSeen[it] } ?: thread.lastSeenAt,
                            )
                        },
                    )
                }
            }
            is ChatEvent.ThreadEvent -> refresh(quiet = true)
            is ChatEvent.ConnectionEvent -> refresh(quiet = true)
            is ChatEvent.Typing, is ChatEvent.Unknown -> Unit
        }
    }

    private fun applyIncoming(threadId: String, body: String, createdAt: String, senderId: String, kind: String) {
        val mine = senderId == myUserId()
        _uiState.update { state ->
            val threads = state.threads.map { thread ->
                if (thread.id != threadId) {
                    thread
                } else {
                    thread.copy(
                        lastMessage = body.ifBlank { previewForKind(kind) },
                        lastMessageKind = kind,
                        lastMessageAt = createdAt,
                        lastMessageSenderId = senderId,
                        unread = if (mine) thread.unread else true,
                        unreadCount = if (mine) thread.unreadCount else thread.unreadCount + 1,
                    )
                }
            }.sortedByDescending { it.lastMessageAt ?: it.updatedAt ?: "" }
            state.copy(threads = threads, unreadTotal = threads.sumOf { it.unreadCount })
        }
    }

    private fun previewForKind(kind: String): String = when (kind) {
        "image" -> "📷 Photo"
        "audio" -> "🎤 Voice note"
        "file" -> "📄 File"
        "deleted" -> "This message was deleted"
        else -> ""
    }

    /** Poll fallback: only while the stream is not live (web parity, `MessagesPage.jsx`). */
    private fun ensurePolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                delay(POLL_MS)
                if (stream.connection.value != ChatConnection.LIVE) refresh(quiet = true)
            }
        }
    }

    companion object {
        const val POLL_MS = 15_000L

        fun factory(
            chatRepository: ChatRepository,
            featuresRepository: FeaturesRepository,
            stream: ChatEventStream,
            myUserId: () -> String?,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatsViewModel(chatRepository, featuresRepository, stream, myUserId) as T
        }
    }
}
