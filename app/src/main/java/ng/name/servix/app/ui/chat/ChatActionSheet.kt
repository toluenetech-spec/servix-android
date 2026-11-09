package ng.name.servix.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ng.name.servix.app.data.model.ChatMessageDto
import ng.name.servix.app.ui.components.ServixBottomSheet
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.SoftSand

/**
 * Long-press actions. Reaction emoji are the server's own list (`GET …/messages` → `reactions`,
 * `community.ts:9`), never a client-side invention; delete only appears for my own messages because
 * the API rejects anything else (`community.ts:222`).
 */
@Composable
fun ChatActionSheet(
    message: ChatMessageDto,
    myUserId: String,
    reactions: List<String>,
    onReact: (String) -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val mine = message.senderId == myUserId
    ServixBottomSheet(title = "Message", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                reactions.forEach { emoji ->
                    val chosen = message.reactions[emoji].orEmpty().contains(myUserId)
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (chosen) SoftSand else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (chosen) ForestGreen else BorderSand, CircleShape)
                            .clickable { onReact(emoji) }
                            .padding(8.dp)
                            .testTag("action_react_$emoji"),
                    )
                }
            }
            HorizontalDivider(color = BorderSand)
            ActionRow(
                icon = Icons.Filled.Reply,
                label = "Reply",
                tint = DeepForest,
                testTag = "action_reply",
                onClick = onReply,
            )
            if (mine && message.kind != "deleted") {
                ActionRow(
                    icon = Icons.Filled.Delete,
                    label = "Delete for everyone",
                    tint = Coral,
                    testTag = "action_delete",
                    onClick = onDelete,
                )
                Text(
                    text = "Deleting removes the message and any file for both of you.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Charcoal)
    }
}
