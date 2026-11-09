package ng.name.servix.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
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
import ng.name.servix.app.data.repository.SessionState
import ng.name.servix.app.data.model.ServixApiException

/** Everything the Chats list needs, plus the single unread total the bar and Home both read. */
data class ChatsUiState(
    val ownerId: String? = null,
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
    private val session: StateFlow<SessionState>,
    private val loadThreads: suspend () -> Result<List<ChatThreadDto>> = { chatRepository.threads() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState: StateFlow<ChatsUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var streamStarted = false
    private var ownerId: String? = null
    private var generation = 0L
    private var loadJob: Job? = null
    private var activeThreadId: String? = null
    private var eventRevision = 0L

    init {
        viewModelScope.launch {
            val features = featuresRepository.refresh()
            _uiState.update { it.copy(aiEnabled = features.flags.ai) }
        }
        viewModelScope.launch {
            stream.connection.collect { connection ->
                val previous = _uiState.value.connection
                _uiState.update { it.copy(connection = connection) }
                if (connection == ChatConnection.LIVE && previous != ChatConnection.LIVE && ownerId != null) refresh(quiet = true)
            }
        }
        viewModelScope.launch {
            stream.events.collect { event -> onEvent(event) }
        }
        viewModelScope.launch {
            session.map { (it as? SessionState.Authenticated)?.user?.id }
                .distinctUntilChanged().collect { userId ->
                    generation++
                    loadJob?.cancel()
                    pollJob?.cancel()
                    pollJob = null
                    ownerId = userId
                    activeThreadId = null
                    _uiState.value = ChatsUiState(ownerId = userId, loading = userId != null, aiEnabled = _uiState.value.aiEnabled)
                    if (userId != null) refresh()
                }
        }
    }

    /** Called by the shell when the app comes to the foreground; the stream lives only while visible. */
    fun startStream() {
        if (streamStarted) return
        streamStarted = true
        stream.start(viewModelScope)
        if (ownerId != null) ensurePolling()
    }

    fun stopStream() {
        if (!streamStarted) return
        streamStarted = false
        stream.stop()
        pollJob?.cancel()
        pollJob = null
    }

    fun refresh(quiet: Boolean = false) {
        val owner = ownerId ?: return
        val currentGeneration = generation
        val requestedRevision = eventRevision
        loadJob?.cancel()
        if (!quiet) _uiState.update { it.copy(loading = it.threads.isEmpty(), refreshing = it.threads.isNotEmpty(), error = null) }
        loadJob = viewModelScope.launch {
            var result = loadThreads()
            currentCoroutineContext().ensureActive()
            // A request started at the sign-in boundary may have carried the previous token. Retry
            // once after the session is published; OkHttp reads the latest token on every request.
            if ((result.exceptionOrNull() as? ServixApiException)?.status == 401 && ownerId == owner && generation == currentGeneration) {
                delay(150)
                result = loadThreads()
                currentCoroutineContext().ensureActive()
            }
            if (generation != currentGeneration || ownerId != owner) return@launch
            if (requestedRevision != eventRevision) {
                refresh(quiet = true)
                return@launch
            }
            result.fold(
                onSuccess = { threads ->
                    _uiState.update { it.copy(loading = false, refreshing = false, threads = threads,
                        unreadTotal = threads.sumOf { thread -> thread.unreadCount }, empty = threads.isEmpty(), error = null) }
                    if (streamStarted) ensurePolling()
                },
                onFailure = { failure ->
                    val message = if ((failure as? ServixApiException)?.status == 401)
                        "Chats could not be loaded. Please try again." else failure.message ?: "Chats could not be loaded."
                    _uiState.update { it.copy(loading = false, refreshing = false,
                        error = if (it.threads.isEmpty()) message else it.error) }
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

    fun setActiveThread(threadId: String?) {
        activeThreadId = threadId
        if (threadId != null) markReadLocally(threadId)
    }

    internal fun onEvent(event: ChatEvent) {
        if (ownerId == null) return
        if (event !is ChatEvent.Typing && event !is ChatEvent.Unknown) eventRevision++
        when (event) {
            is ChatEvent.Message -> {
                if (_uiState.value.threads.any { it.id == event.threadId }) {
                    applyIncoming(event.threadId, event.message, isUpdate = false)
                } else {
                    refresh(quiet = true)
                }
            }
            is ChatEvent.MessageUpdate -> {
                if (_uiState.value.threads.any { it.id == event.threadId }) {
                    applyIncoming(event.threadId, event.message, isUpdate = true)
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

    private fun applyIncoming(threadId: String, message: ng.name.servix.app.data.model.ChatMessageDto, isUpdate: Boolean) {
        val mine = message.senderId == ownerId
        _uiState.update { state ->
            val threads = state.threads.map { thread ->
                if (thread.id != threadId || (isUpdate && thread.lastMessageId != message.id)) thread
                else {
                    val duplicate = message.id.isNotBlank() && thread.lastMessageId == message.id
                    val increaseUnread = !isUpdate && !duplicate && !mine && activeThreadId != threadId
                    thread.copy(
                        lastMessage = message.body.ifBlank { previewForKind(message.kind) },
                        lastMessageKind = message.kind,
                        lastMessageAt = message.createdAt,
                        lastMessageSenderId = message.senderId,
                        lastMessageId = message.id,
                        unread = if (increaseUnread) true else thread.unread,
                        unreadCount = if (increaseUnread) thread.unreadCount + 1 else thread.unreadCount,
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
            session: StateFlow<SessionState>,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ChatsViewModel(chatRepository, featuresRepository, stream, session) as T
        }
    }
}
