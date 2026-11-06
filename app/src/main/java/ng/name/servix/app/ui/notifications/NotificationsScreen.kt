package ng.name.servix.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ng.name.servix.app.ui.a11y.heading
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ng.name.servix.app.data.model.NotificationDto
import ng.name.servix.app.ui.catalogue.EmptyStateCard
import ng.name.servix.app.ui.components.ErrorStateCard
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixTextButton
import ng.name.servix.app.ui.components.ServixTopBar
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.WarmIvory

/** Account › Notifications: list, unread dot, mark all read, manual refresh (polling model). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onItemClick: (NotificationDto) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(WarmIvory)) {
        ServixTopBar(
            title = "Notifications",
            back = onBackClick,
        )
        when {
            uiState.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepForest)
            }
            uiState.error != null && uiState.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(20.dp)) {
                ErrorStateCard(message = uiState.error, onRetry = onRefresh)
            }
            uiState.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(20.dp)) {
                EmptyStateCard(
                    title = "No notifications yet",
                    message = "Booking updates and account messages will appear here.",
                )
            }
            else -> PullToRefreshBox(
                isRefreshing = uiState.refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("notifications_list"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (uiState.unread > 0) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            ServixTextButton(
                                text = if (uiState.markingAll) "Marking…" else "Mark all read",
                                onClick = onMarkAllRead,
                            )
                        }
                    }
                }
                items(uiState.items, key = { it.id }) { item ->
                    NotificationRow(item = item, onClick = { onItemClick(item) })
                }
            }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationDto, onClick: () -> Unit) {
    val unread = item.readAt == null
    ServixCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .background(if (unread) ForestGreen else WarmIvory, CircleShape),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Medium,
                    color = Charcoal,
                )
                Text(text = item.body, style = MaterialTheme.typography.bodySmall, color = MutedGray)
                Text(
                    text = item.createdAt.take(16).replace('T', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
        }
    }
}
