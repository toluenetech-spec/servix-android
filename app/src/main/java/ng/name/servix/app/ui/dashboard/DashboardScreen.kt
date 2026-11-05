package ng.name.servix.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlin.math.roundToLong
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.R
import ng.name.servix.app.data.model.AccountInsightsDto
import ng.name.servix.app.data.model.AccountOverviewDto
import ng.name.servix.app.data.model.AccountPaymentsResponse
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.SecuritySummaryDto
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.data.repository.BookingRepository
import ng.name.servix.app.ui.bookings.BookingStatusChip
import ng.name.servix.app.ui.catalogue.EmptyStateCard
import ng.name.servix.app.ui.components.ErrorStateCard
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.SkeletonBox
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.ServixFormatters

data class DashboardUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val overview: AccountOverviewDto? = null,
    val bookings: List<BookingDto> = emptyList(),
    val insights: AccountInsightsDto? = null,
    val securitySummary: SecuritySummaryDto? = null,
    val payments: AccountPaymentsResponse? = null,
    val loadedForUserId: String? = null,
) {
    val totalBookingsCount: Int
        get() = bookings.size

    val inProgressCount: Int
        get() = bookings.count {
            it.status == "accepted" || it.status == "in_progress" || it.status == "delivered"
        }

    val completedCount: Int
        get() = bookings.count { it.status == "completed" }

    // Computed from GET /account/insights -> monthly[].spent (reference/api-routes/account.ts:113-126)
    val totalSpentNaira: Long
        get() = insights?.monthly?.sumOf { it.spent }?.roundToLong() ?: 0L

    val recentBookings: List<BookingDto>
        get() = bookings.take(3)
}

class DashboardViewModel(
    private val accountRepository: AccountRepository,
    private val bookingRepository: BookingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(loading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard(userId: String?, forceRefresh: Boolean = false) {
        if (userId == null) return
        val current = _uiState.value
        if (!forceRefresh && current.loadedForUserId == userId && !current.loading && current.error == null) {
            return
        }

        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val overviewDef = async { accountRepository.getAccountOverview() }
            val bookingsDef = async { bookingRepository.getMyBookings() }
            val insightsDef = async { accountRepository.getAccountInsights() }
            val securityDef = async { accountRepository.getSecuritySummary() }
            val paymentsDef = async { accountRepository.getPayments() }

            val overviewRes = overviewDef.await()
            val bookingsRes = bookingsDef.await()
            val insightsRes = insightsDef.await()
            val securityRes = securityDef.await()
            val paymentsRes = paymentsDef.await()

            val primaryError = overviewRes.exceptionOrNull()?.message
                ?: bookingsRes.exceptionOrNull()?.message

            if (overviewRes.isFailure && bookingsRes.isFailure) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = primaryError ?: "We couldn’t load your dashboard. Please try again.",
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    loading = false,
                    error = null,
                    overview = overviewRes.getOrNull() ?: it.overview,
                    bookings = bookingsRes.getOrDefault(it.bookings),
                    insights = insightsRes.getOrNull() ?: it.insights,
                    securitySummary = securityRes.getOrNull() ?: it.securitySummary,
                    payments = paymentsRes.getOrNull() ?: it.payments,
                    loadedForUserId = userId,
                )
            }
        }
    }

    companion object {
        fun factory(
            accountRepository: AccountRepository,
            bookingRepository: BookingRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DashboardViewModel(accountRepository, bookingRepository) as T
                }
            }
    }
}

private enum class DashboardSheet {
    NONE,
    ACCOUNT_SECURITY,
    PAYMENTS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    updateContent: @Composable () -> Unit = {},
    onOpenAi: () -> Unit = {},
    user: UserDto?,
    uiState: DashboardUiState,
    onRefresh: () -> Unit,
    onFindProfessionalClick: () -> Unit,
    onExploreServicesClick: () -> Unit,
    onVerifyEmailClick: () -> Unit,
    onOpenBookingsTab: () -> Unit,
    onOpenBookingDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeSheet by rememberSaveable { mutableStateOf(DashboardSheet.NONE) }

    val firstName = user?.firstName?.takeIf { it.isNotBlank() } ?: "there"
    val emailVerified = uiState.overview?.emailVerified ?: (user?.emailVerified == true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvory)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Header mirroring reference/web/src/pages/DashboardPage.jsx:50-55
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    uiState.overview?.plan?.takeIf { it.isNotBlank() }?.let { plan ->
                        ServixBadge(
                            text = plan.replaceFirstChar { it.uppercase() },
                            containerColor = SoftSand,
                            contentColor = DeepForest,
                        )
                    }
                }
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dashboard_refresh_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.retry),
                        tint = DeepForest,
                    )
                }
            }

            Text(
                text = stringResource(R.string.dashboard_welcome_back, firstName),
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
                modifier = Modifier.testTag("dashboard_welcome_heading"),
            )

            Text(
                text = stringResource(R.string.dashboard_subtitle_customer),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )

            Spacer(modifier = Modifier.height(4.dp))

            ServixPrimaryButton(
                text = stringResource(R.string.dashboard_find_professional),
                onClick = onFindProfessionalClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "dashboard_find_pro_button",
            )
        }

        updateContent()

        // Unverified email notice (DashboardPage.jsx:57-62)
        if (user != null && !emailVerified) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Coral.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Coral.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_verify_email_banner"),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_verify_email_notice),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Charcoal,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                    )
                    ServixSecondaryButton(
                        text = stringResource(R.string.dashboard_verify_email_cta),
                        onClick = onVerifyEmailClick,
                        testTag = "dashboard_verify_email_button",
                    )
                }
            }
        }

        if (uiState.loading && uiState.bookings.isEmpty() && uiState.insights == null) {
            DashboardSkeleton()
        } else if (!uiState.error.isNullOrBlank() && uiState.bookings.isEmpty() && uiState.overview == null) {
            ErrorStateCard(
                message = uiState.error,
                onRetry = onRefresh,
            )
        } else {
            // 4-Stat Grid: Bookings, In progress, Completed (GET /bookings) + Spent (GET /account/insights)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.testTag("dashboard_stats_grid"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DashboardStatTile(
                        value = uiState.totalBookingsCount.toString(),
                        label = stringResource(R.string.dashboard_stat_bookings),
                        modifier = Modifier.weight(1f),
                        testTag = "dashboard_stat_bookings",
                    )
                    DashboardStatTile(
                        value = uiState.inProgressCount.toString(),
                        label = stringResource(R.string.dashboard_stat_in_progress),
                        modifier = Modifier.weight(1f),
                        testTag = "dashboard_stat_in_progress",
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DashboardStatTile(
                        value = uiState.completedCount.toString(),
                        label = stringResource(R.string.dashboard_stat_completed),
                        modifier = Modifier.weight(1f),
                        testTag = "dashboard_stat_completed",
                    )
                    DashboardStatTile(
                        value = ServixFormatters.formatNaira(uiState.totalSpentNaira),
                        label = stringResource(R.string.dashboard_stat_spent),
                        modifier = Modifier.weight(1f),
                        testTag = "dashboard_stat_spent",
                    )
                }
            }

            // Action Cards mirroring DashboardPage.jsx:72
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.testTag("dashboard_cards_section"),
            ) {
                DashboardActionCard(
                    title = stringResource(R.string.dashboard_card_bookings_eyebrow),
                    subtitle = stringResource(R.string.dashboard_card_bookings_body),
                    badgeText = if (uiState.totalBookingsCount > 0) "${uiState.totalBookingsCount}" else null,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = DeepForest,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = onOpenBookingsTab,
                    testTag = "dashboard_card_my_bookings",
                )

                DashboardActionCard(
                    title = stringResource(R.string.dashboard_card_ai_eyebrow),
                    subtitle = stringResource(R.string.dashboard_card_ai_body),
                    badgeText = null,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = DeepForest,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = onOpenAi,
                    testTag = "dashboard_card_servix_ai",
                )

                val securityMethodLabel = when (uiState.securitySummary?.method) {
                    "totp" -> "TOTP"
                    "passkey" -> "PASSKEY"
                    else -> if (emailVerified) "VERIFIED" else "VERIFY"
                }
                DashboardActionCard(
                    title = stringResource(R.string.dashboard_card_security_eyebrow),
                    subtitle = stringResource(R.string.dashboard_card_security_title),
                    badgeText = securityMethodLabel,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = DeepForest,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = { activeSheet = DashboardSheet.ACCOUNT_SECURITY },
                    testTag = "dashboard_card_account_security",
                )

                val paymentCount = uiState.payments?.records?.size ?: 0
                DashboardActionCard(
                    title = stringResource(R.string.dashboard_card_payments_eyebrow),
                    subtitle = stringResource(R.string.dashboard_card_payments_body),
                    badgeText = if (paymentCount > 0) "$paymentCount" else uiState.payments?.mode?.uppercase(),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = DeepForest,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = { activeSheet = DashboardSheet.PAYMENTS },
                    testTag = "dashboard_card_payments",
                )
            }

            // Recent bookings section mirroring DashboardPage.jsx:73-91
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.testTag("dashboard_recent_bookings_section"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_recent_bookings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepForest,
                    )
                    if (uiState.bookings.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.dashboard_view_all),
                            style = MaterialTheme.typography.labelLarge,
                            color = ForestGreen,
                            modifier = Modifier
                                .clickable { onOpenBookingsTab() }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("dashboard_all_bookings_link"),
                        )
                    }
                }

                if (uiState.recentBookings.isEmpty()) {
                    EmptyStateCard(
                        title = stringResource(R.string.dashboard_empty_bookings_title),
                        message = stringResource(R.string.dashboard_empty_bookings_body),
                        actionLabel = stringResource(R.string.dashboard_empty_bookings_cta),
                        onActionClick = onExploreServicesClick,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.recentBookings.forEach { booking ->
                            RecentBookingRow(
                                booking = booking,
                                onClick = { onOpenBookingDetail(booking.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheets for Servix AI (Stage 4), Account security, and Payments
    if (activeSheet != DashboardSheet.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSheet = DashboardSheet.NONE },
            sheetState = sheetState,
            containerColor = PureWhite,
            contentColor = Charcoal,
        ) {
            when (activeSheet) {
                DashboardSheet.ACCOUNT_SECURITY -> {
                    AccountSecuritySheetContent(
                        emailVerified = emailVerified,
                        securitySummary = uiState.securitySummary,
                        onVerifyEmailClick = {
                            activeSheet = DashboardSheet.NONE
                            onVerifyEmailClick()
                        },
                        onDismiss = { activeSheet = DashboardSheet.NONE },
                    )
                }
                DashboardSheet.PAYMENTS -> {
                    AccountPaymentsSheetContent(
                        payments = uiState.payments,
                        onOpenBooking = { bookingId ->
                            activeSheet = DashboardSheet.NONE
                            onOpenBookingDetail(bookingId)
                        },
                        onDismiss = { activeSheet = DashboardSheet.NONE },
                    )
                }
                DashboardSheet.NONE -> Unit
            }
        }
    }
}

@Composable
private fun DashboardStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    ServixCard(
        modifier = modifier.testTag(testTag),
        containerColor = PureWhite,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = DeepForest,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }
    }
}

@Composable
private fun DashboardActionCard(
    title: String,
    subtitle: String,
    badgeText: String?,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    testTag: String,
) {
    ServixCard(
        containerColor = PureWhite,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SoftSand,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepForest,
                    )
                    if (!badgeText.isNullOrBlank()) {
                        ServixBadge(
                            text = badgeText,
                            containerColor = SoftSand,
                            contentColor = DeepForest,
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MutedGray,
            )
        }
    }
}

@Composable
private fun RecentBookingRow(
    booking: BookingDto,
    onClick: () -> Unit,
) {
    ServixCard(
        containerColor = PureWhite,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("dashboard_recent_booking_${booking.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = booking.serviceTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepForest,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${booking.professionalName} · ${ServixFormatters.formatDateTimeMedium(booking.scheduledAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = ServixFormatters.formatNaira(booking.amount),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Charcoal,
                )
                BookingStatusChip(status = booking.status)
            }
        }
    }
}

@Composable
private fun AccountSecuritySheetContent(
    emailVerified: Boolean,
    securitySummary: SecuritySummaryDto?,
    onVerifyEmailClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.dashboard_card_security_eyebrow),
            style = MaterialTheme.typography.headlineSmall,
            color = DeepForest,
        )
        Text(
            text = stringResource(R.string.dashboard_card_security_title),
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
        )

        ServixCard(
            containerColor = WarmIvory,
            contentPadding = PaddingValues(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecuritySummaryRow(
                    label = "Email verification",
                    value = if (emailVerified) "Verified" else "Unverified",
                    positive = emailVerified,
                )
                HorizontalDivider(color = BorderSand)
                SecuritySummaryRow(
                    label = "Second factor (method)",
                    value = when (securitySummary?.method) {
                        "totp" -> "Authenticator app (totp)"
                        "passkey" -> "Passkey"
                        null -> "Not enrolled"
                        else -> securitySummary.method
                    },
                    positive = securitySummary?.method != null,
                )
                HorizontalDivider(color = BorderSand)
                SecuritySummaryRow(
                    label = "Recovery codes remaining",
                    value = (securitySummary?.recoveryCodesRemaining ?: 0).toString(),
                    positive = (securitySummary?.recoveryCodesRemaining ?: 0) > 0,
                )
                HorizontalDivider(color = BorderSand)
                SecuritySummaryRow(
                    label = "Passkeys",
                    value = (securitySummary?.passkeys ?: 0).toString(),
                    positive = (securitySummary?.passkeys ?: 0) > 0,
                )
            }
        }

        if (!emailVerified) {
            ServixPrimaryButton(
                text = stringResource(R.string.verify_email_cta),
                onClick = onVerifyEmailClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "security_sheet_verify_email_button",
            )
        }

        ServixSecondaryButton(
            text = stringResource(R.string.close),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            testTag = "security_sheet_close_button",
        )
    }
}

@Composable
private fun SecuritySummaryRow(
    label: String,
    value: String,
    positive: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Charcoal,
        )
        ServixBadge(
            text = value,
            containerColor = if (positive) ForestGreen.copy(alpha = 0.14f) else SoftSand,
            contentColor = if (positive) DeepForest else Charcoal,
        )
    }
}

@Composable
private fun AccountPaymentsSheetContent(
    payments: AccountPaymentsResponse?,
    onOpenBooking: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.dashboard_card_payments_eyebrow),
                style = MaterialTheme.typography.headlineSmall,
                color = DeepForest,
            )
            payments?.mode?.takeIf { it.isNotBlank() }?.let { mode ->
                ServixBadge(
                    text = mode.uppercase(),
                    containerColor = SoftSand,
                    contentColor = DeepForest,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = SoftSand.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, BorderSand),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = DeepForest,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.escrow_copy),
                    style = MaterialTheme.typography.bodySmall,
                    color = Charcoal,
                )
            }
        }

        val records = payments?.records.orEmpty()
        if (records.isEmpty()) {
            ServixCard(
                containerColor = WarmIvory,
                contentPadding = PaddingValues(16.dp),
            ) {
                Text(
                    text = "No payments recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                records.forEach { rec ->
                    val bookingId = rec.booking?.id
                    ServixCard(
                        containerColor = WarmIvory,
                        contentPadding = PaddingValues(14.dp),
                        modifier = if (!bookingId.isNullOrBlank()) {
                            Modifier.clickable { onOpenBooking(bookingId) }
                        } else {
                            Modifier
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = rec.booking?.serviceTitle ?: "Booking payment",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepForest,
                                )
                                Text(
                                    text = "${rec.booking?.reference ?: rec.reference} · ${ServixFormatters.formatDateTimeMedium(rec.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MutedGray,
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = ServixFormatters.formatNaira(rec.amount.roundToLong()),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Charcoal,
                                )
                                ServixBadge(
                                    text = rec.status.uppercase(),
                                    containerColor = if (rec.status == "succeeded" || rec.status == "paid") {
                                        ForestGreen.copy(alpha = 0.14f)
                                    } else {
                                        Coral.copy(alpha = 0.14f)
                                    },
                                    contentColor = if (rec.status == "succeeded" || rec.status == "paid") {
                                        DeepForest
                                    } else {
                                        Charcoal
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        ServixSecondaryButton(
            text = stringResource(R.string.close),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            testTag = "payments_sheet_close_button",
        )
    }
}

@Composable
private fun DashboardSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBox(modifier = Modifier.weight(1f), height = 84.dp)
            SkeletonBox(modifier = Modifier.weight(1f), height = 84.dp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBox(modifier = Modifier.weight(1f), height = 84.dp)
            SkeletonBox(modifier = Modifier.weight(1f), height = 84.dp)
        }
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 76.dp)
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 76.dp)
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 76.dp)
    }
}
