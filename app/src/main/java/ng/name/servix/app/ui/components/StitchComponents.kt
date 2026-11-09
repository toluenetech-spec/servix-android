package ng.name.servix.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DangerRed
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.OnErrorContainer
import ng.name.servix.app.ui.theme.OnTertiaryContainer
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.RadiusLarge
import ng.name.servix.app.ui.theme.ServixMotion
import ng.name.servix.app.ui.theme.ServixType
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.SurfaceContainerHigh
import ng.name.servix.app.ui.theme.SurfaceContainerLow
import ng.name.servix.app.ui.theme.TertiaryContainer
import ng.name.servix.app.ui.theme.ErrorContainer
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.ui.theme.reducedMotion

/**
 * The one button in the app (Stage A).
 *
 * Geometry is the design's: 52 dp minimum height, `rounded-xl` (20 dp), label-lg, no gradient, and a
 * single press response — 0.96 scale on `spring(400, 0.75)` from `m0_motion_system.html`. The HTML's
 * own `active:scale-[0.99]` is not used because the motion sheet is the token source and the two
 * disagree; 0.96 is the sheet's number, so it wins (reported as a deviation).
 *
 * Emil Kowalski: one primary action per surface — [Primary] is filled Forest Green, [Secondary] is a
 * tonal Sand fill, [Text] is flat; never two filled buttons side by side in the same block.
 */
enum class ServixButtonStyle { Primary, Secondary, Text }

@Composable
fun ServixButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ServixButtonStyle = ServixButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    testTag: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val still = reducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !still) ServixMotion.PRESS_SCALE else 1f,
        animationSpec = ServixMotion.press(),
        label = "buttonPress",
    )
    val shape = RoundedCornerShape(RadiusLarge)
    val background = when (style) {
        ServixButtonStyle.Primary -> ForestGreen
        ServixButtonStyle.Secondary -> SurfaceContainerLow
        ServixButtonStyle.Text -> Color.Transparent
    }
    val content = when (style) {
        ServixButtonStyle.Primary -> PureWhite
        ServixButtonStyle.Secondary -> Charcoal
        ServixButtonStyle.Text -> ForestGreen
    }
    val alpha = if (enabled && !loading) 1f else 0.45f
    Row(
        modifier = modifier
            .scale(scale)
            .heightIn(min = 52.dp)
            .then(if (style == ServixButtonStyle.Text) Modifier else Modifier.background(background.copy(alpha = alpha), shape))
            .then(
                if (style == ServixButtonStyle.Secondary) {
                    Modifier.border(1.dp, BorderSand, shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { role = Role.Button }
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) {
            // The spinner replaces the label, so it has to read as a spinner: 20 dp with a 2.5 dp
            // stroke, and a gap so the button's height does not collapse around an 18 dp arc.
            CircularProgressIndicator(
                color = content,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp).testTag("button_spinner"),
            )
        } else {
            leadingIcon?.let { Icon(it, contentDescription = null, tint = content.copy(alpha = alpha), modifier = Modifier.size(18.dp)) }
            Text(
                text = text,
                style = ServixType.labelLg,
                color = content.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailingIcon?.let { Icon(it, contentDescription = null, tint = content.copy(alpha = alpha), modifier = Modifier.size(18.dp)) }
        }
    }
}

/**
 * Field with its label *above* (never a floating placeholder), 52 dp tall, `rounded-xl`, white on the
 * ivory page and a Sand tint while focused — the focus ring the design brief asks for is the 1.5 dp
 * Deep Forest border, which also gives TalkBack a visible focus target.
 */
@Composable
fun ServixField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    error: String? = null,
    supporting: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onImeAction: () -> Unit = {},
    testTag: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(RadiusLarge)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = ServixType.labelMd, color = Charcoal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(if (focused) SurfaceContainerHigh else PureWhite, shape)
                .border(
                    width = if (error != null) 1.5.dp else if (focused) 1.5.dp else 1.dp,
                    color = when {
                        error != null -> DangerRed
                        focused -> DeepForest
                        else -> BorderSand
                    },
                    shape = shape,
                )
                .onFocusChanged { focused = it.isFocused }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            leadingIcon?.let {
                Icon(it, contentDescription = null, tint = MutedGray, modifier = Modifier.size(20.dp))
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, style = ServixType.bodyMd, color = MutedGray.copy(alpha = 0.7f))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = LocalTextStyle.current.merge(ServixType.bodyMd).copy(color = Charcoal),
                    cursorBrush = SolidColor(DeepForest),
                    visualTransformation = visualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                    keyboardActions = KeyboardActions(onDone = { onImeAction() }, onNext = { onImeAction() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                        .let { if (testTag != null) it.testTag(testTag) else it },
                )
            }
            trailing?.invoke()
        }
        val helper = error ?: supporting
        if (!helper.isNullOrBlank()) {
            Text(
                text = helper,
                style = ServixType.bodySm,
                color = if (error != null) DangerRed else MutedGray,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

/**
 * Chip tones exactly as `s13b_booking_status_variants.html` draws them, read from the class names:
 * warn = tertiary-container, info = primary-fixed, ok = secondary-container, bad = error-container,
 * neutral = surface-container. 24 dp tall, uppercase label-md with wide tracking.
 */
enum class StatusTone { Neutral, Info, Warn, Ok, Bad }

@Composable
fun StatusChip(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val (background, foreground) = when (tone) {
        StatusTone.Neutral -> SoftSand to MutedGray
        StatusTone.Info -> SoftSand to DeepForest
        StatusTone.Warn -> TertiaryContainer to OnTertiaryContainer
        StatusTone.Ok -> SoftSand to DeepForest
        StatusTone.Bad -> ErrorContainer to OnErrorContainer
    }
    Box(
        modifier = modifier
            .heightIn(min = 24.dp)
            .background(background, CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = ServixType.labelMd.copy(letterSpacing = 0.6.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = foreground,
            maxLines = 1,
        )
    }
}

/** Booking `status` → tone, straight from the ten variants in `s13b`. */
fun statusTone(status: String): StatusTone = when (status) {
    "pending_payment", "in_progress", "delivered" -> StatusTone.Warn
    "requested", "accepted" -> StatusTone.Info
    "completed", "refunded" -> StatusTone.Ok
    "disputed" -> StatusTone.Bad
    "cancelled", "declined" -> StatusTone.Neutral
    else -> StatusTone.Neutral
}

/** Booking `status` → the label the design prints, so five screens cannot word it five ways. */
fun statusLabel(status: String): String = when (status) {
    "pending_payment" -> "Pending payment"
    "requested" -> "Requested"
    "accepted" -> "Accepted"
    "in_progress" -> "In progress"
    "delivered" -> "Delivered"
    "completed" -> "Completed"
    "cancelled" -> "Cancelled"
    "declined" -> "Declined"
    "disputed" -> "Disputed"
    "refunded" -> "Refunded"
    else -> status.replace('_', ' ')
}
