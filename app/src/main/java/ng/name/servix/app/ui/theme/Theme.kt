package ng.name.servix.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Brand colours wired into Material so every stock component (ripples, dividers, sheet scrims) picks
 * up Servix values instead of a default purple. These are the Stitch roles from CORRECTIONS.md §4.
 */
private val ServixLightColors = lightColorScheme(
    primary = DeepForest,
    onPrimary = PureWhite,
    primaryContainer = ForestGreen,
    onPrimaryContainer = WarmIvory,
    inversePrimary = InversePrimary,
    secondary = ForestGreen,
    onSecondary = PureWhite,
    secondaryContainer = SoftSand,
    onSecondaryContainer = DeepForest,
    tertiary = Coral,
    onTertiary = PureWhite,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = WarmIvory,
    onBackground = Charcoal,
    surface = WarmIvory,
    onSurface = Charcoal,
    surfaceVariant = SoftSand,
    onSurfaceVariant = MutedGray,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SoftSand,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = BorderSand,
    outline = BorderStrongSand,
    outlineVariant = BorderSand,
    error = DangerRed,
    onError = PureWhite,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

/**
 * v1 is light only: the design has no dark palette and the system theme must never invert the brand
 * (PROMPT_STITCH_IMPLEMENTATION §"How to translate": no dynamic colour, no dark theme).
 */
@Composable
fun ServixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ServixLightColors,
        typography = ServixTypography,
        shapes = ServixShapes,
        content = content,
    )
}
