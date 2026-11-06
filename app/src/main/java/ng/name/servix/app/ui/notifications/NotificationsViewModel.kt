package ng.name.servix.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.data.model.NotificationDto
import ng.name.servix.app.data.repository.AccountRepository

data class NotificationsUiState(
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load(refresh: Boolean = false) {
        _uiState.update { it.copy(loading = it.items.isEmpty(), refreshing = refresh, error = null) }
        viewModelScope.launch {
            accountRepository.getNotifications().fold(
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
                        it.copy(loading = false, refreshing = false, error = err.message ?: "We could not load notifications.")
                    }
                },
            )
        }
    }

    fun markAllRead() {
        if (_uiState.value.markingAll) return
        _uiState.update { it.copy(markingAll = true) }
        viewModelScope.launch {
            accountRepository.markNotificationsRead(all = true).fold(
                onSuccess = { result ->
                    _uiState.update { state ->
                        state.copy(
                            markingAll = false,
                            unread = result.unread,
                            items = state.items.map { if (it.readAt == null) it.copy(readAt = "now") else it },
                        )
                    }
                },
                onFailure = { _uiState.update { state -> state.copy(markingAll = false) } },
            )
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            accountRepository.markNotificationsRead(ids = listOf(id)).onSuccess { result ->
                _uiState.update { state ->
                    state.copy(
                        unread = result.unread,
                        items = state.items.map { if (it.id == id) it.copy(readAt = "now") else it },
                    )
                }
            }
        }
    }
}
