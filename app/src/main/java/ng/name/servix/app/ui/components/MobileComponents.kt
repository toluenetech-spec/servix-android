package ng.name.servix.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DangerRed
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.ServixButtonRadius
import ng.name.servix.app.ui.theme.ServixCardRadius
import ng.name.servix.app.ui.theme.ServixNumericStyle
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.ServixFormatters

/** Slim top bar. Back arrow is a 48 dp target; title is titleLarge, never a tracked all-caps label. */
@Composable
fun ServixTopBar(
    title: String? = null,
    back: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (back != null) {
            IconButton(onClick = back, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = DeepForest)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            color = DeepForest,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** 56 dp Soft Sand field. Tapping it opens Explore with the keyboard up. */
@Composable
fun SearchField(
    value: String = "",
    placeholder: String = "What do you need done?",
    onClick: (() -> Unit)? = null,
    onValueChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(ServixButtonRadius))
            .background(SoftSand)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = MutedGray, modifier = Modifier.size(20.dp))
        if (onValueChange != null) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Charcoal),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = MutedGray)
                    inner()
                },
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(if (value.isEmpty()) placeholder else value, style = MaterialTheme.typography.bodyLarge, color = if (value.isEmpty()) MutedGray else Charcoal)
        }
    }
}

/** Replaces the web's tracked all-caps eyebrow labels (impeccable "template chrome"). */
@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = DeepForest)
        if (action != null) {
            ServixTextButton(text = action, onClick = { onAction?.invoke() })
        }
    }
}

@Composable
fun AvatarImage(
    size: Dp,
    initials: String,
    image: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SoftSand),
        contentAlignment = Alignment.Center,
    ) {
        val label = avatarLabel(initials)
        if (image.isNullOrBlank()) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = DeepForest)
        } else {
            SubcomposeAsyncImage(
                model = image,
                contentDescription = null,
                modifier = Modifier.size(size),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                loading = { Box(Modifier.fillMaxWidth().height(size).background(SoftSand)) },
                error = { Text(label, style = MaterialTheme.typography.titleMedium, color = DeepForest) },
            )
        }
    }
}

/**
 * First letter of the first name + first letter of the last name ("TB"), a single letter when
 * there is only one name, and already-formed initials are passed through untouched.
 */
fun avatarLabel(source: String?): String {
    val clean = source?.trim().orEmpty()
    if (clean.isEmpty()) return "?"
    return if (clean.contains(' ')) {
        ng.name.servix.app.util.ServixFormatters.extractInitials(clean)
    } else {
        clean.take(2).uppercase()
    }
}

/** Vertical list row: 48 dp avatar, name + verified, title, rating, starting price. */
@Composable
fun ProfessionalRow(
    name: String,
    title: String,
    rating: Double,
    reviewCount: Int,
    startingPrice: Long,
    verified: Boolean,
    initials: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    avatarUrl: String? = null,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(ServixCardRadius))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarImage(size = 48.dp, initials = initials, image = avatarUrl)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = DeepForest, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (verified) Icon(Icons.Rounded.Verified, contentDescription = "Verified", tint = ForestGreen, modifier = Modifier.size(16.dp))
            }
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MutedGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = Coral, modifier = Modifier.size(14.dp))
                Text("$rating ($reviewCount)", style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), color = Charcoal)
                subtitle?.let { Text("· $it", style = MaterialTheme.typography.labelMedium, color = MutedGray) }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            PriceText(amount = startingPrice, style = MaterialTheme.typography.titleMedium)
            Text("starting", style = MaterialTheme.typography.labelSmall, color = MutedGray)
        }
    }
}

/** 16:10 image service card used in Explore's services mode. */
@Composable
fun ServiceCard(
    title: String,
    professionalName: String,
    price: Long,
    priceUnit: String,
    rating: Double,
    reviewCount: Int,
    image: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ServixCardRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(ServixCardRadius),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderSand),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(SoftSand),
                contentAlignment = Alignment.Center,
            ) {
                if (image != null) {
                    SubcomposeAsyncImage(model = image, contentDescription = null, modifier = Modifier.fillMaxWidth().height(150.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                } else {
                    Icon(Icons.Rounded.Inbox, contentDescription = null, tint = MutedGray, modifier = Modifier.size(28.dp))
                }
            }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = DeepForest, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(professionalName, style = MaterialTheme.typography.bodySmall, color = MutedGray)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = Coral, modifier = Modifier.size(14.dp))
                    Text("$rating ($reviewCount)", style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), color = Charcoal)
                }
                PriceText(amount = price, unit = priceUnit, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = CircleShape,
        color = if (selected) DeepForest else SoftSand,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = if (selected) WarmIvory else DeepForest, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) WarmIvory else Charcoal)
        }
    }
}


/** Two-or-more option segmented control (plan §5): one row, pill halves, no tab chrome. */
@Composable
fun ServixSegmentedControl(
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ServixButtonRadius),
        color = SoftSand,
    ) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEach { (id, label) ->
                val selected = id == selectedId
                Surface(
                    onClick = { onSelect(id) },
                    shape = RoundedCornerShape(ServixButtonRadius - 4.dp),
                    color = if (selected) DeepForest else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("segmented_$id"),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) WarmIvory else Charcoal,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Filter chip: label plus a value when one is applied. Pill, 48 dp target. */
@Composable
fun FilterChipPill(
    label: String,
    value: String? = null,
    active: Boolean = false,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = CircleShape,
        color = if (active) DeepForest else PureWhite,
        border = BorderStroke(1.dp, if (active) DeepForest else BorderSand),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (active) WarmIvory else Charcoal, modifier = Modifier.size(18.dp))
            }
            Text(
                text = if (value.isNullOrBlank()) label else "$label · $value",
                style = MaterialTheme.typography.labelLarge,
                color = if (active) WarmIvory else Charcoal,
                maxLines = 1,
                modifier = Modifier.widthIn(max = 200.dp),
            )
        }
    }
}

/** Booking status pill, exactly the ten states and tints from plan §3. */
@Composable
fun StatusChip(status: String, modifier: Modifier = Modifier) {
    val (label, background, foreground) = statusStyle(status)
    Surface(modifier = modifier, shape = CircleShape, color = background) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

fun statusStyle(status: String): Triple<String, Color, Color> = when (status) {
    "pending_payment" -> Triple("Awaiting payment", Coral.copy(alpha = 0.12f), Coral)
    "requested" -> Triple("Requested", SoftSand, Charcoal)
    "accepted" -> Triple("Accepted", SoftSand, Charcoal)
    "in_progress" -> Triple("In progress", ForestGreen.copy(alpha = 0.12f), ForestGreen)
    "delivered" -> Triple("Delivered", ForestGreen.copy(alpha = 0.12f), ForestGreen)
    "completed" -> Triple("Completed", DeepForest, WarmIvory)
    "cancelled" -> Triple("Cancelled", MutedGray.copy(alpha = 0.12f), MutedGray)
    "declined" -> Triple("Declined", MutedGray.copy(alpha = 0.12f), MutedGray)
    "disputed" -> Triple("Disputed", DangerRed.copy(alpha = 0.10f), DangerRed)
    "refunded" -> Triple("Refunded", MutedGray.copy(alpha = 0.12f), MutedGray)
    else -> Triple(status, SoftSand, Charcoal)
}

/** ₦350,000 · per project — tabular figures so amounts align. */
@Composable
fun PriceText(
    amount: Long,
    modifier: Modifier = Modifier,
    unit: String? = null,
    style: androidx.compose.ui.text.TextStyle = ServixNumericStyle,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(ServixFormatters.formatNaira(amount), style = style.copy(fontFeatureSettings = "tnum"), color = DeepForest)
        if (!unit.isNullOrBlank()) Text(unit, style = MaterialTheme.typography.bodySmall, color = MutedGray)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedGray)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Charcoal)
    }
}

/** Three quiet skeleton rows instead of a spinner. */
@Composable
fun ListSkeleton(modifier: Modifier = Modifier, rows: Int = 3) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ServixCardRadius))
                    .border(1.dp, BorderSand, RoundedCornerShape(ServixCardRadius))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SkeletonBox(modifier = Modifier.size(48.dp), height = 48.dp, shape = RoundedCornerShape(24.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBox(modifier = Modifier.width(160.dp), height = 14.dp)
                    SkeletonBox(modifier = Modifier.width(100.dp), height = 12.dp)
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    icon: ImageVector = Icons.Rounded.Inbox,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MutedGray, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = DeepForest)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MutedGray)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(4.dp))
            ServixSecondaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ServixCardRadius),
        color = DangerRed.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = Charcoal, modifier = Modifier.weight(1f))
            IconButton(onClick = onRetry, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Retry", tint = DeepForest)
            }
        }
    }
}

@Composable
fun ServixTextButton(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = ForestGreen)
    }
}

/** Coral-accented "needs attention" card for payment / delivery / review. */
@Composable
fun AttentionCard(text: String, actionLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ServixCardRadius),
        color = PureWhite,
        border = BorderStroke(1.dp, BorderSand),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(56.dp).background(Coral))
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text, style = MaterialTheme.typography.bodyMedium, color = Charcoal, modifier = Modifier.weight(1f))
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = DeepForest)
                Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = DeepForest, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServixBottomSheet(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { Box(Modifier.padding(top = 8.dp).width(36.dp).height(4.dp).clip(CircleShape).background(BorderSand)) },
    ) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = DeepForest)
            content()
        }
    }
}

/** Inline spinner used only inside buttons. */
@Composable
fun ButtonSpinner(color: Color = WarmIvory) {
    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = color)
}
