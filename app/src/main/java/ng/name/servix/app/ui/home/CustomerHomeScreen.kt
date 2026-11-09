package ng.name.servix.app.ui.home

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ng.name.servix.app.data.model.AiUsage
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.ProfessionalSummaryDto
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.ui.components.AvatarImage
import ng.name.servix.app.ui.components.EmptyState
import ng.name.servix.app.ui.components.ErrorCard
import ng.name.servix.app.ui.components.ListSkeleton
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.StatusChip
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.DangerRed
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.ServixType
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.SurfaceContainerLow
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.ServixFormatters
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** S8 customer; illustrated facts are shown only when the existing Home data contains them. */
@Composable
fun CustomerHomeScreen(
    user: UserDto, state: HomeUiState, aiUsage: AiUsage?, unreadNotifications: Int,
    chatsEnabled: Boolean, chatsUnread: Int, onRefresh: () -> Unit, onSearch: () -> Unit,
    onCategory: (String?) -> Unit, onProfessional: (String) -> Unit, onBooking: (String) -> Unit,
    onBookings: () -> Unit, onAi: () -> Unit, onAccount: () -> Unit, onNotifications: () -> Unit,
    onChats: () -> Unit,
) {
    val zone = ZoneId.of("Africa/Lagos")
    val greeting = when (LocalTime.now(zone).hour) { in 0..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    val today = LocalDate.now(zone).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK))
    Column(Modifier.fillMaxSize().background(WarmIvory)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SERVIX NIGERIA", style = ServixType.labelMd.copy(fontWeight = FontWeight.SemiBold), color = ForestGreen)
                Text("Home", style = ServixType.titleLg, color = Charcoal)
            }
            IconButton(onClick = onAi, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = "Servix AI", tint = ForestGreen)
            }
            IconButton(onClick = onAccount, modifier = Modifier.size(48.dp).testTag("home_account_avatar")) {
                Box(Modifier.size(32.dp).background(DeepForest, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Person, contentDescription = "Account", tint = PureWhite, modifier = Modifier.size(18.dp))
                }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.background(SurfaceContainerLow, CircleShape).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                    Text(today, style = ServixType.labelMd, color = Charcoal)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onNotifications, modifier = Modifier.size(48.dp).testTag("home_notifications_button")) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Charcoal)
                        if (unreadNotifications > 0) Box(Modifier.size(8.dp).background(DangerRed, CircleShape).testTag("home_notifications_dot"))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("$greeting, ${user.firstName}", style = ServixType.headlineLgMobile, color = Charcoal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Find trusted local professionals", style = ServixType.bodyMd, color = MutedGray)
                    }
                    AvatarImage(size = 48.dp, initials = user.fullName, image = user.avatarUrl,
                        modifier = Modifier.clickable(onClick = onAccount))
                }
                Surface(onClick = onSearch, shape = RoundedCornerShape(20.dp), color = PureWhite,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("home_search_field")) {
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = MutedGray, modifier = Modifier.size(22.dp))
                        Text("What do you need done?", style = ServixType.bodyMd, color = MutedGray)
                    }
                }
            }
            state.attention.forEach { item ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(SoftSand).testTag("home_attention_card")) {
                    Box(Modifier.width(6.dp).heightIn(min = 140.dp).background(DangerRed))
                    Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                            Text("NEEDS YOUR ATTENTION", style = ServixType.labelLg, color = DangerRed)
                        }
                        Text(item.message, style = ServixType.bodyMd, color = Charcoal)
                        CustomerButton(item.action, { onBooking(item.bookingId) }, Modifier.fillMaxWidth())
                    }
                }
            }
            if (chatsEnabled) {
                ServixCard(modifier = Modifier.clickable(onClick = onChats).testTag("home_chats_card"), contentPadding = PaddingValues(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).background(DeepForest, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, tint = PureWhite, modifier = Modifier.size(22.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Chats", style = ServixType.titleMd, color = Charcoal)
                            Text(if (chatsUnread > 0) "$chatsUnread unread" else "Open your conversations", style = ServixType.bodySm, color = MutedGray)
                        }
                        if (chatsUnread > 0) Text(chatsUnread.toString(), style = ServixType.labelMd, color = PureWhite,
                            modifier = Modifier.background(DangerRed, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp))
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MutedGray)
                    }
                }
            }
            if (!(state.loading && state.bookings.isEmpty())) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomerSection("Your bookings", "See all", onBookings)
                    if (state.upcomingBookings.isEmpty()) {
                        if (!state.loading && state.error == null) EmptyState(title = "No bookings yet", body = "When you book a professional, the job shows up here with its status.",
                            actionLabel = "Explore services", onAction = { onCategory(null) })
                    } else state.upcomingBookings.forEach { booking -> CustomerBooking(booking, onBooking) }
                }
            }
            if (state.error != null) ErrorCard(message = state.error, onRetry = onRefresh)
            if (state.loading && state.categories.isEmpty()) ListSkeleton(rows = 4) else {
                if (state.categories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CustomerSection("Browse by category", "See all") { onCategory(null) }
                        state.categories.take(8).chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { category ->
                                    Column(Modifier.weight(1f).heightIn(min = 88.dp).clip(RoundedCornerShape(24.dp))
                                        .background(SurfaceContainerLow).clickable { onCategory(category.id) }.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(44.dp).background(SoftSand, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                                            Icon(guestCategoryIcon(category.icon, category.name), contentDescription = null, tint = ForestGreen, modifier = Modifier.size(24.dp))
                                        }
                                        Text(category.name, style = ServixType.labelMd, color = Charcoal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                ServixCard(containerColor = ForestGreen, borderColor = ForestGreen, contentPadding = PaddingValues(20.dp), modifier = Modifier.testTag("home_ai_card")) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = PureWhite, modifier = Modifier.size(24.dp))
                        Text("Need a project cost estimate?", style = ServixType.titleLg, color = PureWhite)
                        Text("Describe the job and get a fair price range before you book.", style = ServixType.bodyMd, color = WarmIvory)
                        Surface(onClick = onAi, color = SoftSand, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("Ask Servix AI", style = ServixType.labelLg, color = DeepForest) }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomerSection("Recommended for you", "See all") { onCategory(null) }
                    if (state.recommended.isEmpty() && !state.loading && state.error == null) EmptyState(title = "No professionals found",
                        body = "Try another category, or ask Servix AI what a job should cost.", actionLabel = "Explore services", onAction = { onCategory(null) })
                    else LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(state.recommended, key = { it.id }) { pro -> CustomerProfessional(pro) { onProfessional(pro.slug) } }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val amount = aiUsage?.allowed?.takeIf { it > 0 }?.let { " · AI ${NumberFormat.getIntegerInstance(Locale.UK).format(aiUsage.used)} / ${NumberFormat.getIntegerInstance(Locale.UK).format(it)} words this month" }.orEmpty()
                Text("${state.planName} plan$amount", style = ServixType.bodySm, color = MutedGray, modifier = Modifier.weight(1f))
                TextButton(onClick = onAccount) { Text("Manage plan", style = ServixType.labelLg, color = ForestGreen) }
            }
        }
    }
}

@Composable
private fun CustomerSection(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = ServixType.titleLg, color = Charcoal, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(action, style = ServixType.labelLg, color = ForestGreen) }
    }
}

@Composable
private fun CustomerBooking(booking: BookingDto, onBooking: (String) -> Unit) {
    ServixCard(containerColor = SurfaceContainerLow, borderColor = SurfaceContainerLow,
        modifier = Modifier.clickable { onBooking(booking.id) }.testTag("home_upcoming_card"), contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(status = booking.status)
                Spacer(Modifier.weight(1f))
                Text(listOf(ServixFormatters.formatShortDayMonth(booking.scheduledAt), ServixFormatters.formatSlotTime(booking.scheduledAt))
                    .filter { it.isNotBlank() }.joinToString(" · "), style = ServixType.labelMd, color = MutedGray)
            }
            Text(booking.serviceTitle.ifBlank { "Booking" }, style = ServixType.titleMd, color = Charcoal)
            booking.professionalName?.takeIf { it.isNotBlank() }?.let { name ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarImage(size = 44.dp, initials = name)
                    Text(name, style = ServixType.labelLg, color = Charcoal)
                }
            }
            CustomerButton("View booking", { onBooking(booking.id) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CustomerProfessional(pro: ProfessionalSummaryDto, onClick: () -> Unit) {
    ServixCard(modifier = Modifier.width(270.dp).clickable(onClick = onClick).testTag("home_professional_row"),
        containerColor = SurfaceContainerLow, borderColor = SurfaceContainerLow, contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(128.dp).background(SoftSand, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                AvatarImage(size = 72.dp, initials = pro.name, image = pro.image)
                if (pro.verified) Row(Modifier.align(Alignment.BottomStart).padding(8.dp).background(PureWhite, CircleShape).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Verified, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                    Text("Verified Pro", style = ServixType.labelMd, color = Charcoal)
                }
            }
            Text(pro.name, style = ServixType.titleMd, color = Charcoal, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(pro.title, style = ServixType.bodySm, color = ForestGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("★ ${pro.rating} (${pro.reviewCount})", style = ServixType.bodySm, color = Charcoal)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Starting", style = ServixType.labelMd, color = MutedGray, modifier = Modifier.weight(1f))
                Text(ServixFormatters.formatNaira(pro.startingPrice), style = ServixType.titleMd, color = Charcoal)
            }
            CustomerButton("View profile", onClick, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CustomerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen, contentColor = PureWhite)) {
        Text(text, style = ServixType.labelLg)
    }
}
