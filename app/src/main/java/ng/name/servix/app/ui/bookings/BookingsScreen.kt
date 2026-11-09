package ng.name.servix.app.ui.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.layout.size
import ng.name.servix.app.ui.components.ServixSegmentedControl
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
import ng.name.servix.app.ui.a11y.heading
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import ng.name.servix.app.R
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.repository.BookingRepository
import ng.name.servix.app.data.repository.SessionState
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
    private val session: StateFlow<SessionState>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingsUiState())
    val uiState: StateFlow<BookingsUiState> = _uiState.asStateFlow()

    private var ownerId: String? = null
    private var generation = 0L
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            session.map { (it as? SessionState.Authenticated)?.user?.id }.distinctUntilChanged().collect { id ->
                generation++
                loadJob?.cancel()
                ownerId = id
                _uiState.value = BookingsUiState(loading = id != null)
                if (id != null) loadBookings()
            }
        }
    }

    fun selectTab(tab: BookingsListTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadBookings(forceRefresh: Boolean = false) {
        val owner = ownerId ?: return
        val requestGeneration = generation
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                if (forceRefresh && it.bookings.isNotEmpty()) {
                    it.copy(refreshing = true, error = null)
                } else {
                    it.copy(loading = true, error = null)
                }
            }
            val res = bookingRepository.getMyBookings()
            currentCoroutineContext().ensureActive()
            if (generation != requestGeneration || ownerId != owner) return@launch
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
        fun factory(bookingRepository: BookingRepository, session: StateFlow<SessionState>): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookingsViewModel(bookingRepository, session) as T
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
                        text = stringResource(R.string.bookings_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepForest,
                        modifier = Modifier.heading(),
                    )
                }
            }

            // Active / Past segmented control
            item(key = "tabs") {
                ServixSegmentedControl(
                    options = listOf(
                        BookingsListTab.ACTIVE.name to "${stringResource(R.string.bookings_tab_active)} (${activeList.size})",
                        BookingsListTab.PAST.name to "${stringResource(R.string.bookings_tab_past)} (${pastList.size})",
                    ),
                    selectedId = uiState.selectedTab.name,
                    onSelect = { id -> onTabSelect(BookingsListTab.valueOf(id)) },
                )
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

            groupedBookings(displayedBookings).forEach { (group, bookingsInGroup) ->
                item(key = "group_$group") {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedGray,
                        modifier = Modifier.testTag("bookings_group_${group.replace(" ", "_").lowercase()}"),
                    )
                }
                items(bookingsInGroup, key = { it.id }) { booking ->
                    BookingRow(
                        booking = booking,
                        onClick = { onOpenBooking(booking.id) },
                    )
                }
            }
        }
    }
}

/** Today / This week / Later grouping for the Active tab, keyed off the scheduled slot. */
internal fun groupedBookings(bookings: List<BookingDto>): List<Pair<String, List<BookingDto>>> {
    val today = java.time.LocalDate.now(java.time.ZoneId.of("Africa/Lagos"))
    val weekEnd = today.plusDays(7)
    val buckets = linkedMapOf<String, MutableList<BookingDto>>()
    bookings.forEach { booking ->
        val date = runCatching {
            java.time.OffsetDateTime.parse(booking.scheduledAt).atZoneSameInstant(java.time.ZoneId.of("Africa/Lagos")).toLocalDate()
        }.getOrNull() ?: runCatching {
            java.time.LocalDate.parse(booking.scheduledAt.take(10))
        }.getOrNull()
        val group = when {
            date == null -> "Later"
            date.isBefore(today) -> "Today"
            date == today -> "Today"
            date.isBefore(weekEnd) -> "This week"
            else -> "Later"
        }
        buckets.getOrPut(group) { mutableListOf() }.add(booking)
    }
    return listOf("Today", "This week", "Later").mapNotNull { key ->
        buckets[key]?.takeIf { it.isNotEmpty() }?.let { key to it.toList() }
    }
}

/** Compact list row: status chip, service, professional + slot, price, chevron. */
@Composable
fun BookingRow(
    booking: BookingDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ServixCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("booking_row_${booking.reference}"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BookingStatusChip(status = booking.status)
                    Text(
                        text = ServixFormatters.formatNaira(booking.amount),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = DeepForest,
                    )
                }
                Text(
                    text = booking.serviceTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Charcoal,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        if (!booking.professionalName.isNullOrBlank()) {
                            append(booking.professionalName)
                            append(" · ")
                        }
                        append(ServixFormatters.formatDateTimeMedium(booking.scheduledAt))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MutedGray,
                modifier = Modifier.size(20.dp),
            )
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
