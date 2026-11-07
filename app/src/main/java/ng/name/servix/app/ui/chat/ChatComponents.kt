package ng.name.servix.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ng.name.servix.app.data.model.ChatMessageDto
import ng.name.servix.app.data.model.ChatThreadDto
import ng.name.servix.app.ui.catalogue.ServixAvatarImage
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory

/** Bubble radii: 16 dp body with a 4 dp corner on the sender's side (brand radii 16/24/12). */
private val MineShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
private val TheirsShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
private val GroupedMine = RoundedCornerShape(16.dp)
private val GroupedTheirs = RoundedCornerShape(16.dp)

/** One row of the thread list (`GET /community/threads`). */
@Composable
fun ChatThreadRow(
    thread: ChatThreadDto,
    myUserId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = thread.person?.fullName.orEmpty().ifBlank { "Servix user" }
    val title = thread.person?.professionalProfile?.title
        ?: thread.booking?.serviceTitle
        ?: "Connection"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_thread_${thread.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            ServixAvatarImage(
                rawImageUrl = thread.person?.professionalProfile?.imageUrl,
                name = name,
                size = 48.dp,
            )
            if (thread.online) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(ForestGreen)
                        .border(2.dp, WarmIvory, CircleShape)
                        .testTag("chat_online_${thread.id}"),
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Charcoal,
                    fontWeight = if (thread.unreadCount > 0) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = ChatTime.inboxLabel(thread.lastMessageAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
            Text(title, style = MaterialTheme.typography.labelSmall, color = MutedGray, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (thread.lastMessageSenderId == myUserId && thread.lastMessage.isNotBlank()) {
                    TickGlyph(
                        state = ChatTicks.state(
                            ChatMessageDto(
                                id = thread.lastMessageId.orEmpty(),
                                senderId = myUserId,
                                createdAt = thread.lastMessageAt.orEmpty(),
                            ),
                            myUserId,
                            thread.otherReadAt,
                            thread.otherDeliveredAt,
                        ),
                    )
                }
                Text(
                    text = thread.lastMessage.ifBlank { "No messages yet" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (thread.unreadCount > 0) Charcoal else MutedGray,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (thread.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Coral)
                            .testTag("chat_unread_${thread.id}"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (thread.unreadCount > 9) "9+" else thread.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmIvory,
                        )
                    }
                }
            }
        }
    }
}

/**
 * ✓ sent, ✓✓ delivered, ✓✓ seen (accent), clock while pending.
 *
 * [onDark] is for the Deep-Forest bubble: Forest Green on forest is invisible, so on my own bubbles
 * the states step from translucent ivory to full ivory to Coral instead of relying on hue alone.
 */
@Composable
fun TickGlyph(state: TickState?, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val base = if (onDark) WarmIvory else MutedGray
    when (state) {
        null -> Unit
        TickState.PENDING -> Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = "Sending",
            tint = base.copy(alpha = 0.6f),
            modifier = modifier.size(12.dp),
        )
        TickState.SENT -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Sent",
            tint = base.copy(alpha = 0.7f),
            modifier = modifier.size(14.dp),
        )
        TickState.DELIVERED -> Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = "Delivered",
            tint = base.copy(alpha = 0.7f),
            modifier = modifier.size(14.dp),
        )
        TickState.READ -> Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = "Seen",
            // Coral on the forest bubble, Forest Green on ivory: both are palette colours with
            // real contrast against what is behind them.
            tint = if (onDark) Coral else ForestGreen,
            modifier = modifier.size(14.dp),
        )
    }
}

@Composable
fun ChatDaySeparator(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedGray,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SoftSand)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** A message bubble. Text only in Stage 1; media bubbles arrive in Stage 2. */
@Composable
fun ChatBubble(
    row: ChatRow.Message,
    tick: TickState?,
    failed: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mine = row.mine
    val message = row.message
    val shape = when {
        mine && row.grouped -> GroupedMine
        mine -> MineShape
        row.grouped -> GroupedTheirs
        else -> TheirsShape
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (mine) DeepForest else PureWhite)
                    .border(1.dp, if (mine) DeepForest else BorderSand, shape)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag(if (mine) "bubble_mine" else "bubble_theirs"),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (message.kind == "deleted") {
                        Text(
                            text = "This message was deleted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mine) WarmIvory.copy(alpha = 0.75f) else MutedGray,
                        )
                    } else if (ChatText.isLocationShare(message.body)) {
                        val url = ChatText.locationUrl(message.body)
                        Text(
                            text = "📍 Shared location",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mine) WarmIvory else Charcoal,
                        )
                        if (url != null) {
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (mine) WarmIvory.copy(alpha = 0.85f) else ForestGreen,
                            )
                        }
                    } else {
                        Text(
                            text = message.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (mine) WarmIvory else Charcoal,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = ChatTime.clockLabel(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mine) WarmIvory.copy(alpha = 0.7f) else MutedGray,
                        )
                        if (mine) TickGlyph(tick, onDark = true)
                    }
                }
            }
            if (failed) {
                Text(
                    text = "Tap to retry",
                    style = MaterialTheme.typography.labelSmall,
                    color = Coral,
                    modifier = Modifier
                        .clickable(onClick = onRetry)
                        .padding(top = 2.dp)
                        .testTag("bubble_retry"),
                )
            }
        }
    }
}
