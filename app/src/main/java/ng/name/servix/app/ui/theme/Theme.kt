package ng.name.servix.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ServixLightColors = lightColorScheme(
    primary = DeepForest,
    onPrimary = WarmIvory,
    primaryContainer = ForestGreen,
    onPrimaryContainer = WarmIvory,
    secondary = ForestGreen,
    onSecondary = WarmIvory,
    secondaryContainer = SoftSand,
    onSecondaryContainer = Charcoal,
    tertiary = Coral,
    onTertiary = PureWhite,
    background = WarmIvory,
    onBackground = Charcoal,
    surface = PureWhite,
    onSurface = Charcoal,
    surfaceVariant = SoftSand,
    onSurfaceVariant = MutedGray,
    outline = BorderSand,
    outlineVariant = SoftSand,
    error = DangerRed,
    onError = PureWhite,
)

// Mobile radii (plan §3): cards 16, sheets 24 (top), buttons/inputs 12, chips pill. No gradients.
val ServixCardRadius = 16.dp
val ServixButtonRadius = 12.dp
val ServixSheetRadius = 24.dp
val ServixShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(ServixButtonRadius),
    medium = RoundedCornerShape(ServixCardRadius),
    large = RoundedCornerShape(ServixCardRadius),
    extraLarge = RoundedCornerShape(ServixSheetRadius),
)

/** v1 is light only (plan §3): the system theme must never invert the brand. */
@Composable
fun ServixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ServixLightColors,
        typography = ServixTypography,
        shapes = ServixShapes,
        content = content
    )
}
