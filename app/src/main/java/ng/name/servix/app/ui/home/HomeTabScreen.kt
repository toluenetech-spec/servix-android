package ng.name.servix.app.ui.home

import androidx.compose.runtime.Composable
import ng.name.servix.app.data.model.AiUsage
import ng.name.servix.app.data.model.UserDto

/** Dispatch Home by session without changing its data sources or navigation callbacks. */
@Composable
fun HomeTabScreen(
    user: UserDto?, city: String?, uiState: HomeUiState, aiUsage: AiUsage? = null,
    onRefresh: () -> Unit = {}, onOpenSearch: () -> Unit = {}, onOpenCategory: (String?) -> Unit = {},
    onOpenProfessional: (String) -> Unit = {}, onOpenBookingDetail: (String) -> Unit = {},
    onOpenBookingsTab: () -> Unit = {}, onOpenAi: () -> Unit = {}, onOpenAccount: () -> Unit = {},
    onSignInClick: () -> Unit = {}, onCreateAccountClick: () -> Unit = {}, unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {}, chatsEnabled: Boolean = false, chatsUnread: Int = 0,
    onOpenChats: () -> Unit = {},
) {
    if (user == null) {
        GuestHomeScreen(city, uiState, onRefresh, onOpenSearch, onOpenCategory, onOpenProfessional,
            onSignInClick, onCreateAccountClick, onOpenAi)
    } else {
        CustomerHomeScreen(user, uiState, aiUsage, unreadNotifications, chatsEnabled, chatsUnread,
            onRefresh, onOpenSearch, onOpenCategory, onOpenProfessional, onOpenBookingDetail,
            onOpenBookingsTab, onOpenAi, onOpenAccount, onOpenNotifications, onOpenChats)
    }
}
