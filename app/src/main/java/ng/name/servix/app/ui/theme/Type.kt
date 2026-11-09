package ng.name.servix.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ng.name.servix.app.R

/**
 * Inter 4.1 static instances (OFL, see res/font/OFL.txt), one Font() per real weight file.
 * Before this, a single file was registered for every weight, so bold headings rendered regular.
 */
val ServixFont: FontFamily = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
)

/**
 * The Stitch type scale, exactly as the design's `tailwind.config` declares it (CORRECTIONS.md §4
 * "Type scale"). Material slots are the carriers; the Stitch names are kept in [ServixType] so a
 * screen can ask for `headlineLgMobile` instead of remembering which slot aliases it.
 *
 * Inter, one real weight file per weight (see res/font/OFL.txt) — a single file per weight is what
 * makes 600 render as semibold rather than faux-bold.
 */
private fun stitch(
    size: Int,
    line: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = ServixFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

object ServixType {
    val displayLg = stitch(40, 48, FontWeight.Bold, -0.02)
    val headlineLg = stitch(32, 40, FontWeight.SemiBold, -0.02)
    val headlineLgMobile = stitch(26, 34, FontWeight.SemiBold, -0.01)
    val headlineMd = stitch(22, 28, FontWeight.SemiBold, -0.01)
    val titleLg = stitch(18, 24, FontWeight.SemiBold)
    val titleMd = stitch(16, 22, FontWeight.SemiBold)
    val bodyLg = stitch(16, 24, FontWeight.Normal)
    val bodyMd = stitch(14, 20, FontWeight.Normal)
    val bodySm = stitch(12, 16, FontWeight.Normal)
    val labelLg = stitch(14, 20, FontWeight.SemiBold, 0.01)
    val labelMd = stitch(12, 16, FontWeight.Medium, 0.02)
    val currencyDisplay = stitch(28, 36, FontWeight.Bold, -0.01).copy(fontFeatureSettings = "tnum")
}

val ServixTypography = Typography(
    displayLarge = ServixType.displayLg,
    displayMedium = ServixType.headlineLg,
    displaySmall = ServixType.headlineLgMobile,
    headlineLarge = ServixType.headlineLgMobile,
    headlineMedium = ServixType.headlineMd,
    headlineSmall = ServixType.titleLg,
    titleLarge = ServixType.titleLg,
    titleMedium = ServixType.titleMd,
    titleSmall = ServixType.labelLg,
    bodyLarge = ServixType.bodyLg,
    bodyMedium = ServixType.bodyMd,
    bodySmall = ServixType.bodySm,
    labelLarge = ServixType.labelLg,
    labelMedium = ServixType.labelMd,
    labelSmall = ServixType.labelMd,
)

/** Prices and counters: tabular figures so columns of naira amounts line up. */
val ServixNumericStyle = ServixType.currencyDisplay
