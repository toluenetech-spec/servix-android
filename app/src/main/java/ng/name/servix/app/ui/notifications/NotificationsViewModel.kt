package ng.name.servix.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.data.model.NotificationDto
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.data.repository.SessionState
import ng.name.servix.app.data.repository.retrySessionRead

data class NotificationsUiState(
    val ownerId: String? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val unread: Int = 0,
    val items: List<NotificationDto> = emptyList(),
    val error: String? = null,
    val markingAll: Boolean = false,
)

/** Backs the Account › Notifications screen (GET/POST /account/notifications*, account.ts:44-63). */
class NotificationsViewModel(
    private val accountRepository: AccountRepository,
    private val session: StateFlow<SessionState>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var ownerId: String? = null
    private var generation = 0L
    private var loadJob: Job? = null
    init {
        viewModelScope.launch {
            session.map { (it as? SessionState.Authenticated)?.user?.id }.distinctUntilChanged().collect { id ->
                generation++
                loadJob?.cancel()
                ownerId = id
                _uiState.value = NotificationsUiState(ownerId = id, loading = id != null)
                if (id != null) load()
            }
        }
    }

    fun load(refresh: Boolean = false) {
        val owner = ownerId ?: return
        val requestGeneration = generation
        loadJob?.cancel()
        _uiState.update { it.copy(loading = if (refresh) it.loading else it.items.isEmpty(), refreshing = refresh, error = null) }
        loadJob = viewModelScope.launch {
            val result = retrySessionRead({ generation == requestGeneration && ownerId == owner &&
                (session.value as? SessionState.Authenticated)?.user?.id == owner }) { accountRepository.getNotifications() }
            currentCoroutineContext().ensureActive()
            if (generation != requestGeneration || ownerId != owner) return@launch
            result.fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            unread = page.unread,
                            items = page.items,
                            error = null,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(loading = false, refreshing = false, error = if ((err as? ng.name.servix.app.data.model.ServixApiException)?.status == 401)
                            "We could not load notifications. Please try again." else err.message ?: "We could not load notifications.")
                    }
                },
            )
        }
    }

    fun markAllRead() {
        val owner = ownerId ?: return
        val requestGeneration = generation
        if (_uiState.value.markingAll) return
        loadJob?.cancel()
        _uiState.update { it.copy(markingAll = true) }
        viewModelScope.launch {
            val result = accountRepository.markNotificationsRead(all = true)
            currentCoroutineContext().ensureActive()
            if (ownerId != owner || generation != requestGeneration) return@launch
            result.fold(
                onSuccess = { result ->
                    _uiState.update { state ->
                        state.copy(
                            markingAll = false,
                            unread = result.unread,
                            items = state.items.map { if (it.readAt == null) it.copy(readAt = "now") else it },
                        )
                    }
                    load(refresh = true)
                },
                onFailure = { _uiState.update { state -> state.copy(markingAll = false) } },
            )
        }
    }

    fun markRead(id: String) {
        val owner = ownerId ?: return
        val requestGeneration = generation
        loadJob?.cancel()
        viewModelScope.launch {
            val response = accountRepository.markNotificationsRead(ids = listOf(id))
            currentCoroutineContext().ensureActive()
            if (ownerId != owner || generation != requestGeneration) return@launch
            response.onSuccess { result ->
                _uiState.update { state ->
                    state.copy(
                        unread = result.unread,
                        items = state.items.map { if (it.id == id) it.copy(readAt = "now") else it },
                    )
                }
                load(refresh = true)
            }
        }
    }
    companion object {
        fun factory(repository: AccountRepository, session: StateFlow<SessionState>): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = NotificationsViewModel(repository, session) as T
            }
    }

}
