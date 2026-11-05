package ng.name.servix.app.ui.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.R
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.repository.BookingRepository
import ng.name.servix.app.ui.catalogue.EmptyStateCard
import ng.name.servix.app.ui.components.ErrorStateCard
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.SkeletonBox
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.ServixFormatters

enum class BookingsListTab {
    ACTIVE,
    PAST,
}

data class BookingsUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val bookings: List<BookingDto> = emptyList(),
    val selectedTab: BookingsListTab = BookingsListTab.ACTIVE,
) {
    val activeBookings: List<BookingDto>
        get() = bookings
            .filter { it.status in BookingStatusMeta.ACTIVE_STATUSES }
            .sortedBy { it.scheduledAt }

    val pastBookings: List<BookingDto>
        get() = bookings
            .filter { it.status in BookingStatusMeta.PAST_STATUSES }
            .sortedByDescending { it.scheduledAt }
}

class BookingsViewModel(
    private val bookingRepository: BookingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    fun selectTab(tab: BookingsListTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadBookings(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (forceRefresh && it.bookings.isNotEmpty()) {
                    it.copy(refreshing = true, error = null)
                } else {
                    it.copy(loading = true, error = null)
                }
            }
            val res = bookingRepository.getMyBookings()
            res.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = null,
                            bookings = list,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = err.message ?: "We couldn't load your bookings.",
                        )
                    }
                }
            )
        }
    }

    companion object {
        fun factory(bookingRepository: BookingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookingsViewModel(bookingRepository) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    user: UserDto?,
    uiState: BookingsUiState,
    onTabSelect: (BookingsListTab) -> Unit,
    onRefresh: () -> Unit,
    onExploreServicesClick: () -> Unit,
    onSignInClick: () -> Unit,
    onOpenBooking: (bookingId: String) -> Unit,
) {
    if (user == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.bookings_eyebrow),
                style = MaterialTheme.typography.labelSmall,
                color = ForestGreen,
            )
            Text(
                text = stringResource(R.string.bookings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
            )
            EmptyStateCard(
                title = "My bookings",
                message = stringResource(R.string.bookings_sign_in_message),
                actionLabel = stringResource(R.string.sign_in),
                onActionClick = onSignInClick,
            )
        }
        return
    }

    val activeList = remember(uiState.bookings) { uiState.activeBookings }
    val pastList = remember(uiState.bookings) { uiState.pastBookings }
    val displayedBookings = if (uiState.selectedTab == BookingsListTab.ACTIVE) activeList else pastList

    PullToRefreshBox(
        isRefreshing = uiState.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("bookings_list_column"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.bookings_eyebrow),
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestGreen,
                    )
                    Text(
                        text = stringResource(R.string.bookings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepForest,
                    )
                }
            }

            // Active / Past Tabs
            item(key = "tabs") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BookingsTabPill(
                        label = "${stringResource(R.string.bookings_tab_active)} (${activeList.size})",
                        selected = uiState.selectedTab == BookingsListTab.ACTIVE,
                        onClick = { onTabSelect(BookingsListTab.ACTIVE) },
                        modifier = Modifier.weight(1f),
                        testTag = "bookings_tab_active",
                    )
                    BookingsTabPill(
                        label = "${stringResource(R.string.bookings_tab_past)} (${pastList.size})",
                        selected = uiState.selectedTab == BookingsListTab.PAST,
                        onClick = { onTabSelect(BookingsListTab.PAST) },
                        modifier = Modifier.weight(1f),
                        testTag = "bookings_tab_past",
                    )
                }
            }

            if (uiState.loading && uiState.bookings.isEmpty()) {
                items(4) {
                    ServixCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SkeletonBox(modifier = Modifier.fillMaxWidth(0.45f), height = 18.dp)
                            SkeletonBox(modifier = Modifier.fillMaxWidth(0.8f), height = 22.dp)
                            SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f), height = 16.dp)
                        }
                    }
                }
                return@LazyColumn
            }

            if (uiState.error != null && uiState.bookings.isEmpty()) {
                item(key = "error") {
                    ErrorStateCard(
                        message = uiState.error,
                        onRetry = onRefresh,
                    )
                }
                return@LazyColumn
            }

            if (uiState.bookings.isEmpty()) {
                item(key = "empty_all") {
                    EmptyStateCard(
                        title = stringResource(R.string.bookings_empty_title),
                        message = stringResource(R.string.bookings_empty_message),
                        actionLabel = stringResource(R.string.bookings_explore_services),
                        onActionClick = onExploreServicesClick,
                    )
                }
                return@LazyColumn
            }

            if (displayedBookings.isEmpty()) {
                item(key = "empty_tab") {
                    val emptyTitle = if (uiState.selectedTab == BookingsListTab.ACTIVE) {
                        stringResource(R.string.bookings_empty_active_title)
                    } else {
                        stringResource(R.string.bookings_empty_past_title)
                    }
                    val emptyMsg = if (uiState.selectedTab == BookingsListTab.ACTIVE) {
                        stringResource(R.string.bookings_empty_active_message)
                    } else {
                        stringResource(R.string.bookings_empty_past_message)
                    }
                    EmptyStateCard(
                        title = emptyTitle,
                        message = emptyMsg,
                        actionLabel = stringResource(R.string.bookings_explore_services),
                        onActionClick = onExploreServicesClick,
                    )
                }
                return@LazyColumn
            }

            items(
                items = displayedBookings,
                key = { it.id },
            ) { booking ->
                BookingListItemCard(
                    booking = booking,
                    onClick = { onOpenBooking(booking.id) },
                )
            }
        }
    }
}

@Composable
private fun BookingsTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) DeepForest else PureWhite,
        border = BorderStroke(1.dp, if (selected) DeepForest else BorderSand),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) WarmIvory else Charcoal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp, horizontal = 12.dp),
        )
    }
}

@Composable
fun BookingListItemCard(
    booking: BookingDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ServixCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("booking_card_${booking.reference}"),
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BookingStatusChip(status = booking.status)
                    Text(
                        text = booking.reference,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                        maxLines = 1,
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = ServixFormatters.formatNaira(booking.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepForest,
                    )
                    Text(
                        text = booking.priceUnit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                }
            }
            Text(
                text = booking.serviceTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Charcoal,
            )
            Text(
                text = buildString {
                    if (!booking.professionalName.isNullOrBlank()) {
                        append("with ${booking.professionalName} · ")
                    }
                    append(ServixFormatters.formatDateTimeMedium(booking.scheduledAt))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }
    }
}
