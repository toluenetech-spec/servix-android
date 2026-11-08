package ng.name.servix.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ng.name.servix.app.ui.motion.pressFeedback
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.servix.app.R
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.BorderStrongSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DangerRed
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.ServixButtonRadius
import ng.name.servix.app.ui.theme.ServixCardRadius
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.CustomTabLauncher

/**
 * The real Servix brand artwork (converted from the brand SVGs in public/brand), replacing the old "SX" text tile.
 * [wordmark] = mark + lettering for Welcome/auth; mark-only for the Home top bar.
 */
@Composable
fun ServixLogoMark(
    modifier: Modifier = Modifier,
    darkSurface: Boolean = false,
    wordmark: Boolean = true,
) {
    if (wordmark) {
        Image(
            painter = painterResource(R.drawable.ic_servix_wordmark),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = modifier
                .height(30.dp)
                .then(if (darkSurface) Modifier else Modifier),
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_servix_mark),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = modifier.size(28.dp),
        )
    }
}

@Composable
fun ServixPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    testTag: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interaction,
        shape = RoundedCornerShape(ServixButtonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepForest,
            contentColor = WarmIvory,
            disabledContainerColor = DeepForest.copy(alpha = 0.45f),
            disabledContentColor = WarmIvory.copy(alpha = 0.8f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .pressFeedback(interaction, enabled && !loading)
            .heightIn(min = 52.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = WarmIvory,
            )
            Spacer(modifier = Modifier.size(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun ServixSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(ServixButtonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = SoftSand,
            contentColor = Charcoal,
            disabledContainerColor = SoftSand.copy(alpha = 0.5f),
            disabledContentColor = MutedGray,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .pressFeedback(interaction, enabled)
            .heightIn(min = 52.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun ServixTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    hint: String? = null,
    error: String? = null,
    required: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 5,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    readOnly: Boolean = false,
    testTag: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Floating label lives inside the field (plan §5 auth): 56 dp tall, label in the notch.
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            isError = !error.isNullOrBlank(),
            label = {
                Text(
                    text = if (required) "$label *" else label,
                    style = MaterialTheme.typography.labelLarge,
                )
            },
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                }
            },
            visualTransformation = if (isPassword && !passwordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            trailingIcon = if (isPassword && onTogglePasswordVisibility != null) {
                {
                    IconButton(
                        onClick = onTogglePasswordVisibility,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = stringResource(
                                if (passwordVisible) R.string.hide_password else R.string.show_password
                            ),
                            tint = MutedGray,
                        )
                    }
                }
            } else {
                null
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(ServixButtonRadius),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                errorContainerColor = PureWhite,
                focusedBorderColor = DeepForest,
                unfocusedBorderColor = BorderSand,
                errorBorderColor = DangerRed,
                focusedTextColor = Charcoal,
                unfocusedTextColor = Charcoal,
                cursorColor = DeepForest,
            ),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .let { if (testTag != null) it.testTag(testTag) else it },
        )
        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = DangerRed,
            )
        } else if (!hint.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = if (hint.startsWith("✓")) ForestGreen else MutedGray,
            )
        }
    }
}

@Composable
fun ServixCard(
    modifier: Modifier = Modifier,
    containerColor: Color = PureWhite,
    borderColor: Color = BorderSand,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ServixCardRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun ServixBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = SoftSand,
    contentColor: Color = Charcoal,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun OfflineBanner(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Charcoal)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("offline_banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = stringResource(R.string.offline_banner),
                tint = Coral,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.bodySmall,
                color = WarmIvory,
            )
        }
    }
}

@Composable

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp),
) {
    val transition = rememberInfiniteTransition(label = "skeleton_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton_alpha",
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(SoftSand.copy(alpha = alpha))
            .semantics { contentDescription = "Loading" }
    )
}

@Composable
fun ErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ServixCard(modifier = modifier.testTag("error_state_card")) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = message,
                tint = DangerRed,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Charcoal,
            )
            ServixSecondaryButton(
                text = stringResource(R.string.retry),
                onClick = onRetry,
                testTag = "retry_button",
            )
        }
    }
}
