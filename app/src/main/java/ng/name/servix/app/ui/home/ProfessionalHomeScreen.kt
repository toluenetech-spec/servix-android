package ng.name.servix.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.ui.components.AvatarImage
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
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.ServixFormatters
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Professional S8c: all jobs, totals and publishing problems come from the professional API. */
@Composable
fun ProfessionalHomeScreen(
    user: UserDto,
    state: ProfessionalHomeState,
    unreadNotifications: Int,
    chatsEnabled: Boolean,
    onNotifications: () -> Unit,
    onAccount: () -> Unit,
    onSeeAllJobs: () -> Unit,
    onViewJob: (String) -> Unit,
    onJobAction: (String, String) -> Unit,
    onMessage: (String) -> Unit,
    onEditService: (String) -> Unit,
    onAi: () -> Unit,
    onRetry: () -> Unit,
) {
    var declineId by remember(user.id) { mutableStateOf<String?>(null) }
    declineId?.let { id ->
        AlertDialog(onDismissRequest = { declineId = null }, title = { Text("Decline this request?") },
            text = { Text("The customer will be notified that you declined.") },
            confirmButton = { TextButton(onClick = { declineId = null; onJobAction(id, "decline") }) { Text("Decline request") } },
            dismissButton = { TextButton(onClick = { declineId = null }) { Text("Keep request") } })
    }
    val zone = ZoneId.of("Africa/Lagos")
    val greeting = when (LocalTime.now(zone).hour) { in 0..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    val today = LocalDate.now(zone).format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.UK))
    Column(Modifier.fillMaxSize().background(WarmIvory)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("$greeting, ${user.firstName}", style = ServixType.titleLg, color = Charcoal)
                Text(today, style = ServixType.bodySm, color = MutedGray)
            }
            IconButton(onClick = onNotifications, modifier = Modifier.size(48.dp).testTag("home_notifications_button")) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Charcoal)
                    if (unreadNotifications > 0) Box(Modifier.size(8.dp).background(ForestGreen, CircleShape))
                }
            }
            IconButton(onClick = onAccount, modifier = Modifier.size(48.dp).testTag("home_account_avatar")) {
                AvatarImage(size = 36.dp, initials = user.fullName, image = user.avatarUrl)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
        ProSectionHeader("Your jobs", "See all", onSeeAllJobs)
        state.actionError?.let { ProError(it, onRetry) }
        state.jobsError?.let { ProError(it, onRetry) }
        val jobs = state.jobs.filter { it.status in setOf("requested", "accepted", "in_progress", "delivered") }.take(3)
        if (state.loading && jobs.isEmpty()) Text("Loading your jobs…", style = ServixType.bodyMd, color = MutedGray, modifier = Modifier.padding(vertical = 16.dp))
        else if (jobs.isEmpty() && state.jobsError == null) Text("No jobs yet.", style = ServixType.bodyMd, color = MutedGray, modifier = Modifier.padding(vertical = 16.dp))
        jobs.forEach { job ->
            ServixCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("professional_home_job"), contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(status = job.status)
                        Spacer(Modifier.weight(1f))
                        Text(listOf(ServixFormatters.formatShortDayMonth(job.scheduledAt), ServixFormatters.formatSlotTime(job.scheduledAt)).filter { it.isNotBlank() }.joinToString(" "), style = ServixType.bodySm, color = MutedGray)
                    }
                    Text(job.serviceTitle, style = ServixType.titleMd, color = Charcoal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    job.customerName?.takeIf { it.isNotBlank() }?.let { Text(it, style = ServixType.bodySm, color = MutedGray) }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val action = professionalActionFor(job.status)
                        ProActionButton(if (action == null) "View" else if (state.busyBookingId == job.id) "Please wait…" else action.second,
                            onClick = { if (action == null) onViewJob(job.id) else onJobAction(job.id, action.first) },
                            modifier = Modifier.weight(1f).testTag("professional_job_action_${job.id}"), enabled = state.busyBookingId == null)
                        if (chatsEnabled) ProActionButton("Message", onClick = { onMessage(job.id) }, modifier = Modifier.weight(1f), secondary = true)
                    }
                    if (job.status == "requested") TextButton(onClick = { declineId = job.id }, enabled = state.busyBookingId == null) {
                        Text("Decline", style = ServixType.labelLg, color = MutedGray)
                    }
                }
            }
        }
        state.analyticsError?.let { ProError(it, onRetry) }
        state.totals?.let { totals ->
            ProSectionHeader("This week")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                totals.earnings?.let { ProStat("Earned", "₦" + NumberFormat.getNumberInstance(Locale.UK).format(it), Modifier.weight(1f)) }
                totals.completed?.let { ProStat("Jobs done", it.toString(), Modifier.weight(1f)) }
                totals.ratingAvg?.let { ProStat("Rating", if ((totals.reviewCount ?: 0) > 0) String.format(Locale.UK, "%.1f★", it) else "No reviews", Modifier.weight(1f)) }
            }
            val rate = totals.responseRate?.let { "Response rate: $it% · " }.orEmpty()
            Text("${rate}Last 7 days · Rating overall", style = ServixType.bodySm, color = MutedGray, modifier = Modifier.padding(top = 8.dp))
        }
        state.servicesError?.let { ProError(it, onRetry) }
        state.services.filter { it.publishProblems.isNotEmpty() }.forEach { service ->
            ProSectionHeader("Finish your service", "Edit service") { onEditService(service.id) }
            ServixCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (service.title.isNotBlank()) Text(service.title, style = ServixType.titleMd, color = Charcoal)
                    service.publishProblems.values.forEach { problem -> Text("• $problem", style = ServixType.bodySm, color = MutedGray) }
                }
            }
        }
        ServixCard(modifier = Modifier.fillMaxWidth().padding(top = 28.dp), containerColor = SoftSand.copy(alpha = 0.4f), contentPadding = PaddingValues(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(40.dp).background(DeepForest, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Servix AI", style = ServixType.titleMd, color = Charcoal)
                    Text("Describe the job and get a fair price range before you book.", style = ServixType.bodySm, color = MutedGray)
                    ProActionButton("Ask Servix AI", onClick = onAi)
                }
            }
        }
        }
    }
}

@Composable
private fun ProSectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = ServixType.titleMd, color = Charcoal, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(action, style = ServixType.labelLg, color = DeepForest)
        }
    }
}

@Composable
private fun ProStat(label: String, value: String, modifier: Modifier = Modifier) {
    ServixCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, style = ServixType.titleLg, color = DeepForest, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = ServixType.labelMd, color = MutedGray)
        }
    }
}

@Composable
private fun ProActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false, enabled: Boolean = true) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (secondary) PureWhite else DeepForest, contentColor = if (secondary) DeepForest else PureWhite),
        border = if (secondary) BorderStroke(1.dp, BorderSand) else null, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
        if (secondary) Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 4.dp))
        Text(text, style = ServixType.labelLg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProError(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(message, style = ServixType.bodySm, color = DangerRed)
        TextButton(onClick = onRetry) { Text("Try again", style = ServixType.labelLg, color = DeepForest) }
    }
}
