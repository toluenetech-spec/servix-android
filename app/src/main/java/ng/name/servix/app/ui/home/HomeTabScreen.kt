package ng.name.servix.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale
import ng.name.servix.app.R
import ng.name.servix.app.data.model.AiUsage
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.ui.components.AttentionCard
import ng.name.servix.app.ui.components.AvatarImage
import ng.name.servix.app.ui.components.CategoryChip
import ng.name.servix.app.ui.components.EmptyState
import ng.name.servix.app.ui.components.ErrorCard
import ng.name.servix.app.ui.components.ListSkeleton
import ng.name.servix.app.ui.components.ProfessionalRow
import ng.name.servix.app.ui.components.SearchField
import ng.name.servix.app.ui.components.SectionHeader
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.ServixTextButton
import ng.name.servix.app.ui.components.StatusChip
import ng.name.servix.app.ui.theme.BorderSand
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.ServixButtonRadius
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.ServixFormatters

/**
 * Pass 2 §2 of the design plan: the whole app opens straight onto the work, never a landing page.
 * Home is the dashboard — greeting, search, what needs attention, live bookings — and below the
 * fold it is also the catalogue: categories and recommended professionals. One scroll, one screen.
 */
@Composable
fun HomeTabScreen(
    user: UserDto?,
    city: String?,
    uiState: HomeUiState,
    aiUsage: AiUsage? = null,
    onRefresh: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenCategory: (String?) -> Unit = {},
    onOpenProfessional: (String) -> Unit = {},
    onOpenBookingDetail: (String) -> Unit = {},
    onOpenBookingsTab: () -> Unit = {},
    onOpenAi: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onCreateAccountClick: () -> Unit = {},
    unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {},
    /** Release 1.1: the chat store owns this number (Σ unreadCount) — same value as the tab badge. */
    chatsEnabled: Boolean = false,
    chatsUnread: Int = 0,
    onOpenChats: () -> Unit = {},
    /** `features.requests` from GET /features; never hardcoded. */
    requestsEnabled: Boolean = false,
    onPostRequest: () -> Unit = {},
    /** Professional-only, read-only strip above the jobs list. */
    thisWeekJobs: Int = 0,
    payoutsPendingLabel: String? = null,
    /** Professional-only profile completion nudge (0-100). */
    profileStrength: Int = 0,
    /** Null until the app can resolve the professional's own slug (Stage 3 profile work). */
    onOpenPublicProfile: (() -> Unit)? = null,
) {
    val signedIn = user != null
    val isProfessional = user?.role == "professional"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        HomeTopBar(
            user = user,
            city = city,
            onOpenAccount = onOpenAccount,
            onSignIn = onSignInClick,
            unreadNotifications = unreadNotifications,
            onOpenNotifications = onOpenNotifications,
        )

        SearchField(
            placeholder = stringResource(R.string.home_search_placeholder),
            onClick = onOpenSearch,
            testTag = "home_search_field",
        )

        if (!signedIn) {
            SignInCard(
                onSignInClick = onSignInClick,
                onCreateAccountClick = onCreateAccountClick,
            )
        }

        if (signedIn && uiState.attention.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = stringResource(R.string.home_needs_attention))
                uiState.attention.forEach { item ->
                    AttentionCard(
                        text = item.message,
                        actionLabel = item.action,
                        onClick = { onOpenBookingDetail(item.bookingId) },
                        modifier = Modifier.testTag("home_attention_card"),
                    )
                }
            }
        }

        if (signedIn && chatsEnabled) {
            HomeChatsCard(unread = chatsUnread, onClick = onOpenChats)
        }

        if (signedIn && !(uiState.loading && uiState.bookings.isEmpty())) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    title = if (isProfessional) "Your jobs" else stringResource(R.string.home_your_bookings),
                    action = stringResource(R.string.home_see_all),
                    onAction = onOpenBookingsTab,
                )
                if (uiState.upcomingBookings.isEmpty()) {
                    if (!uiState.loading) {
                        EmptyState(
                            title = stringResource(R.string.home_no_bookings_title),
                            body = stringResource(R.string.home_no_bookings_body),
                            actionLabel = stringResource(R.string.home_explore_services),
                            onAction = { onOpenCategory(null) },
                            icon = Icons.Rounded.CalendarMonth,
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
                        modifier = Modifier.fillMaxWidth().offset(x = (-16).dp),
                    ) {
                        items(uiState.upcomingBookings, key = { it.id }) { booking ->
                            UpcomingBookingCard(
                                counterparty = if (isProfessional) booking.customerName else booking.professionalName,
                                booking = booking,
                                onClick = { onOpenBookingDetail(booking.id) },
                            )
                        }
                        item { Spacer(Modifier.width(8.dp)) }
                    }
                }
            }
        }

        if (signedIn && isProfessional) {
            ThisWeekStrip(jobsThisWeek = thisWeekJobs, payoutsPending = payoutsPendingLabel)
            ProfileStrengthCard(strength = profileStrength, onOpenPublicProfile = onOpenPublicProfile)
        }

        if (signedIn && requestsEnabled && !isProfessional) {
            PostRequestCard(onClick = onPostRequest)
        }

        if (!(signedIn && isProfessional) && uiState.loading && uiState.categories.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(title = stringResource(R.string.home_recommended))
                ListSkeleton(rows = 4)
            }
        } else if (!(signedIn && isProfessional)) {
            if (uiState.error != null) {
                ErrorCard(message = uiState.error, onRetry = onRefresh)
            }

            if (uiState.categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.home_categories),
                        action = stringResource(R.string.home_see_all),
                        onAction = { onOpenCategory(null) },
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
                        modifier = Modifier.fillMaxWidth().offset(x = (-16).dp),
                    ) {
                        items(uiState.categories.take(8), key = { it.id }) { category ->
                            CategoryChip(
                                label = category.name,
                                selected = false,
                                onClick = { onOpenCategory(category.id) },
                            )
                        }
                        item { Spacer(Modifier.width(8.dp)) }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionHeader(
                    title = stringResource(R.string.home_recommended),
                    action = stringResource(R.string.home_see_all),
                    onAction = { onOpenCategory(null) },
                )
                uiState.recommended.forEach { professional ->
                    ProfessionalRow(
                        name = professional.name,
                        title = professional.title.ifBlank { professional.location },
                        rating = professional.rating,
                        reviewCount = professional.reviewCount,
                        startingPrice = professional.startingPrice,
                        verified = professional.verified,
                        initials = professional.name,
                        subtitle = professional.location.takeIf { it.isNotBlank() },
                        avatarUrl = professional.image,
                        onClick = { onOpenProfessional(professional.slug) },
                        modifier = Modifier.testTag("home_professional_row"),
                    )
                }
                if (!uiState.loading && uiState.recommended.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.home_no_professionals_title),
                        body = stringResource(R.string.home_no_professionals_body),
                        actionLabel = stringResource(R.string.home_explore_services),
                        onAction = { onOpenCategory(null) },
                    )
                }
            }
        }

        // The assistant lives inside Chats now; Home only keeps it when chats are not available.
        if (!(signedIn && chatsEnabled)) {
            AiCard(onOpenAi = onOpenAi)
        }

        if (signedIn) {
            PlanLine(
                planName = uiState.planName,
                usage = aiUsage,
                onManage = onOpenAccount,
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    user: UserDto?,
    city: String?,
    onOpenAccount: () -> Unit,
    onSignIn: () -> Unit,
    unreadNotifications: Int = 0,
    onOpenNotifications: () -> Unit = {},
) {
    val greeting = when (LocalTime.now(ZoneId.of("Africa/Lagos")).hour) {
        in 0..11 -> stringResource(R.string.home_good_morning)
        in 12..16 -> stringResource(R.string.home_good_afternoon)
        else -> stringResource(R.string.home_good_evening)
    }
    val today = LocalDate.now(ZoneId.of("Africa/Lagos"))
        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (user != null) "$greeting, ${user.firstName}" else greeting,
                style = MaterialTheme.typography.titleLarge,
                color = DeepForest,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (user == null) (city?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_city_fallback)) else today,
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
            )
        }
        if (user != null) {
            IconButton(
                onClick = onOpenNotifications,
                modifier = Modifier.size(48.dp).testTag("home_notifications_button"),
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = stringResource(R.string.notifications_open),
                        tint = DeepForest,
                        modifier = Modifier.size(24.dp).padding(top = 2.dp),
                    )
                    if (unreadNotifications > 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(ForestGreen, CircleShape)
                                .testTag("home_notifications_dot"),
                        )
                    }
                }
            }
            IconButton(
                onClick = onOpenAccount,
                modifier = Modifier.size(48.dp).testTag("home_account_avatar"),
            ) {
                AvatarImage(size = 36.dp, initials = user.fullName, image = user.avatarUrl)
            }
        } else {
            ServixTextButton(text = stringResource(R.string.sign_in), onClick = onSignIn)
        }
    }
}

@Composable
private fun UpcomingBookingCard(
    booking: BookingDto,
    onClick: () -> Unit,
    /** The person on the other side: a customer sees the professional, a professional the customer. */
    counterparty: String? = null,
) {
    ServixCard(
        modifier = Modifier
            .width(280.dp)
            .clickable(onClick = onClick)
            .testTag("home_upcoming_card"),
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(status = booking.status)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MutedGray,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = booking.serviceTitle.ifBlank { stringResource(R.string.home_booking_fallback) },
                style = MaterialTheme.typography.titleMedium,
                color = DeepForest,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = counterparty.orEmpty()
                    .ifBlank { stringResource(R.string.home_professional_fallback) },
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(
                    ServixFormatters.formatShortDayMonth(booking.scheduledAt),
                    ServixFormatters.formatSlotTime(booking.scheduledAt),
                ).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = Charcoal,
            )
        }
    }
}

@Composable
private fun AiCard(onOpenAi: () -> Unit) {
    ServixCard(
        containerColor = DeepForest,
        borderColor = DeepForest,
        contentPadding = PaddingValues(20.dp),
        modifier = Modifier.testTag("home_ai_card"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = WarmIvory,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.nav_ai),
                    style = MaterialTheme.typography.titleMedium,
                    color = WarmIvory,
                )
            }
            Text(
                text = stringResource(R.string.home_ai_body),
                style = MaterialTheme.typography.bodyMedium,
                color = WarmIvory.copy(alpha = 0.82f),
            )
            Surface(
                onClick = onOpenAi,
                shape = RoundedCornerShape(ServixButtonRadius),
                color = WarmIvory,
                border = BorderStroke(1.dp, BorderSand),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_ai_cta),
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepForest,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanLine(
    planName: String,
    usage: AiUsage?,
    onManage: () -> Unit,
) {
    val usageText = usage?.allowed?.takeIf { it > 0 }?.let { allowed ->
        stringResource(
            R.string.home_plan_ai_usage,
            usage.used.grouped(),
            allowed.grouped(),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = listOfNotNull(
                stringResource(R.string.home_plan_name, planName),
                usageText,
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
            modifier = Modifier.weight(1f),
        )
        ServixTextButton(text = stringResource(R.string.home_manage_plan), onClick = onManage)
    }
}

@Composable
private fun SignInCard(
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
) {
    ServixCard(
        containerColor = PureWhite,
        contentPadding = PaddingValues(20.dp),
        modifier = Modifier.testTag("home_sign_in_card"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.home_sign_in_title),
                style = MaterialTheme.typography.titleMedium,
                color = DeepForest,
            )
            Text(
                text = stringResource(R.string.home_sign_in_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ServixPrimaryButton(
                    text = stringResource(R.string.sign_in),
                    onClick = onSignInClick,
                    modifier = Modifier.weight(1f),
                    testTag = "home_sign_in_button",
                )
                ServixSecondaryButton(
                    text = stringResource(R.string.create_account),
                    onClick = onCreateAccountClick,
                    modifier = Modifier.weight(1f),
                    testTag = "home_create_account_button",
                )
            }
        }
    }
}


/** Home shortcut into Chats — same unread number as the tab badge, same source. */
@Composable
private fun HomeChatsCard(unread: Int, onClick: () -> Unit) {
    ServixCard(
        modifier = Modifier.clickable(onClick = onClick).testTag("home_chats_card"),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SoftSand),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Chat, contentDescription = null, tint = DeepForest, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Chats", style = MaterialTheme.typography.titleMedium, color = DeepForest)
                Text(
                    text = if (unread > 0) "$unread unread · Servix AI is here" else "Servix AI is here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
            }
            if (unread > 0) {
                Box(
                    modifier = Modifier.clip(CircleShape).background(Coral).padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (unread > 9) "9+" else unread.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmIvory,
                    )
                }
            }
        }
    }
}

/** Customers only, and only while `features.requests` is true. */
@Composable
private fun PostRequestCard(onClick: () -> Unit) {
    ServixCard(
        modifier = Modifier.clickable(onClick = onClick).testTag("home_post_request"),
        containerColor = SoftSand,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = DeepForest, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Post a request", style = MaterialTheme.typography.titleMedium, color = DeepForest)
                Text(
                    "Describe the job once, get proposals from professionals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
            }
            Text("Post", style = MaterialTheme.typography.labelLarge, color = DeepForest)
        }
    }
}

/** Read-only work strip: what is scheduled this week and what is still waiting to be paid out. */
@Composable
private fun ThisWeekStrip(jobsThisWeek: Int, payoutsPending: String?) {
    ServixCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("This week", style = MaterialTheme.typography.labelMedium, color = ForestGreen)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeekStat(label = "Jobs scheduled", value = jobsThisWeek.toString(), modifier = Modifier.weight(1f))
                WeekStat(label = "Payouts pending", value = payoutsPending ?: "—", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WeekStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = DeepForest)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedGray)
    }
}

/** Completion nudge for professionals: percentage + the one action that raises it. */
@Composable
private fun ProfileStrengthCard(strength: Int, onOpenPublicProfile: (() -> Unit)?) {
    val clamped = strength.coerceIn(0, 100)
    ServixCard(contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Profile strength", style = MaterialTheme.typography.titleSmall, color = DeepForest)
                Text("$clamped%", style = MaterialTheme.typography.titleSmall, color = ForestGreen)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftSand),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clamped / 100f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ForestGreen),
                )
            }
            Text(
                "Professionals with photos, a clear title and three portfolio pieces get more bookings.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
            if (onOpenPublicProfile != null) {
                ServixSecondaryButton(
                    text = "View public profile",
                    onClick = onOpenPublicProfile,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "home_profile_strength_cta",
                )
            }
        }
    }
}
