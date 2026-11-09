package ng.name.servix.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ng.name.servix.app.data.model.ChatAttachmentDto
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory

/** "1.4 MB", "812 KB" — the bytes the API stored, not a rounded guess. */
internal fun formatBytes(bytes: Long?): String {
    val value = bytes ?: return ""
    if (value <= 0) return ""
    return when {
        value >= 1024 * 1024 -> String.format("%.1f MB", value / 1024.0 / 1024.0)
        value >= 1024 -> "${value / 1024} KB"
        else -> "$value B"
    }
}

/** "0:07", "1:42" — voice-note length from `attachment.durationMs`. */
internal fun formatDuration(ms: Long?): String {
    val total = ((ms ?: 0) / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/** Photo bubble: the thumbnail opens the full-screen viewer; the ring shows the upload. */
@Composable
internal fun ImageBubble(
    attachment: ChatAttachmentDto,
    uploadProgress: Float?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SoftSand)
            .clickable(enabled = attachment.url.isNotBlank()) { onClick() }
            .testTag("bubble_image"),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = attachment.url,
            contentDescription = attachment.name ?: "Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(220.dp).height(165.dp),
        )
        if (uploadProgress != null) {
            Box(
                modifier = Modifier.width(220.dp).height(165.dp).background(Charcoal.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { uploadProgress },
                    color = WarmIvory,
                    trackColor = WarmIvory.copy(alpha = 0.3f),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp).testTag("bubble_image_progress"),
                )
            }
        }
    }
}

/** Voice note: play/pause, the duration pill, and a progress line that only moves while playing. */
@Composable
internal fun AudioBubble(
    durationMs: Long?,
    playing: Boolean,
    positionMs: Long,
    mine: Boolean,
    onToggle: () -> Unit,
) {
    val tint = if (mine) WarmIvory else DeepForest
    val track = if (mine) WarmIvory.copy(alpha = 0.35f) else SoftSand
    val fraction = if ((durationMs ?: 0) > 0) (positionMs.toFloat() / durationMs!!.toFloat()).coerceIn(0f, 1f) else 0f
    Row(
        modifier = Modifier
            .widthIn(min = 150.dp, max = 240.dp)
            .testTag("bubble_audio"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape)
                .background(if (mine) WarmIvory.copy(alpha = 0.18f) else SoftSand)
                .clickable(onClick = onToggle)
                .testTag("bubble_audio_toggle"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "Pause voice note" else "Play voice note",
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(track)) {
                Box(
                    modifier = Modifier.fillMaxWidth(fraction).height(3.dp).clip(CircleShape)
                        .background(if (mine) WarmIvory else ForestGreen),
                )
            }
            Text(
                text = formatDuration(if (positionMs > 0) positionMs else durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = if (mine) WarmIvory.copy(alpha = 0.8f) else MutedGray,
            )
        }
    }
}

/** File bubble: the attachment's own name and size; tapping hands the URL to the system viewer. */
@Composable
internal fun FileBubble(
    attachment: ChatAttachmentDto,
    mine: Boolean,
    onOpen: () -> Unit,
) {
    val tint = if (mine) WarmIvory else DeepForest
    Row(
        modifier = Modifier
            .widthIn(max = 240.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(vertical = 2.dp)
            .testTag("bubble_file"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
                .background(if (mine) WarmIvory.copy(alpha = 0.18f) else SoftSand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = attachment.name ?: "File",
                style = MaterialTheme.typography.bodyMedium,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(formatBytes(attachment.size).takeIf { it.isNotBlank() }, "Tap to open")
                    .joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = if (mine) WarmIvory.copy(alpha = 0.7f) else MutedGray,
            )
        }
    }
}

/** Quoted preview inside a bubble: who and what, from `replyTo` (body is capped at 200 by the API). */
@Composable
internal fun ReplyQuote(
    reply: ng.name.servix.app.data.model.ChatReplyDto,
    mine: Boolean,
    onTap: () -> Unit,
    otherName: String,
) {
    val accent = if (mine) WarmIvory else ForestGreen
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (mine) WarmIvory.copy(alpha = 0.14f) else SoftSand)
            .clickable(onClick = onTap)
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("bubble_reply"),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = if (reply.senderId == null) otherName else otherName,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
        )
        Text(
            text = replyPreviewText(reply),
            style = MaterialTheme.typography.bodySmall,
            color = if (mine) WarmIvory.copy(alpha = 0.85f) else Charcoal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Matches the server's own preview language (`previewOf`, community.ts:46-53). */
internal fun replyPreviewText(reply: ng.name.servix.app.data.model.ChatReplyDto): String = when {
    reply.kind == "deleted" -> "This message was deleted"
    reply.kind == "image" -> "📷 Photo"
    reply.kind == "audio" -> "🎤 Voice note"
    reply.kind == "file" -> "📄 ${reply.attachmentName ?: "File"}"
    else -> reply.body.orEmpty()
}

/** Reaction pills under a bubble; mine is filled, theirs is outlined. One tap toggles. */
@Composable
internal fun ReactionPills(
    reactions: Map<String, List<String>>,
    myUserId: String,
    onToggle: (String) -> Unit,
) {
    if (reactions.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        reactions.forEach { (emoji, users) ->
            val mine = users.contains(myUserId)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (mine) SoftSand else PureWhite)
                    .border(1.dp, if (mine) ForestGreen else BorderSand, CircleShape)
                    .clickable { onToggle(emoji) }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .testTag("reaction_pill"),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(emoji, style = MaterialTheme.typography.labelSmall)
                Text(
                    text = users.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mine) ForestGreen else MutedGray,
                )
            }
        }
    }
}

/** Full-screen photo viewer; tap or the close button returns to the thread. */
@Composable
internal fun ImageViewer(url: String, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp)
            .background(Charcoal)
            .clickable(onClick = onClose)
            .testTag("image_viewer"),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = "Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(WarmIvory.copy(alpha = 0.9f))
                .clickable(onClick = onClose)
                .testTag("image_viewer_close"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = DeepForest, modifier = Modifier.size(20.dp))
        }
    }
}
