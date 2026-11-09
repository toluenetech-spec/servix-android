package ng.name.servix.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Radii from the Stitch theme (CORRECTIONS.md §1 G2): `rounded` 12 · `lg` 16 · `xl` 20 · `2xl` 24.
 * `rounded-full` maps to CircleShape at call sites.
 */
val RadiusSmall = 12.dp     // buttons in dense rows, chips' parent blocks
val RadiusMedium = 16.dp    // list rows, cards inside cards
val RadiusLarge = 20.dp     // primary buttons, text fields (HTML `rounded-xl`)
val RadiusXLarge = 24.dp    // hero cards, sheets (HTML `rounded-2xl`)

/** Legacy names kept so the 1.1 screens compile untouched while they migrate stage by stage. */
val ServixCardRadius = RadiusMedium
val ServixButtonRadius = RadiusLarge
val ServixSheetRadius = RadiusXLarge

fun shapeSmall() = RoundedCornerShape(RadiusSmall)
fun shapeMedium() = RoundedCornerShape(RadiusMedium)
fun shapeLarge() = RoundedCornerShape(RadiusLarge)
fun shapeXLarge() = RoundedCornerShape(RadiusXLarge)

val ServixShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(RadiusSmall),
    medium = RoundedCornerShape(RadiusMedium),
    large = RoundedCornerShape(RadiusLarge),
    extraLarge = RoundedCornerShape(RadiusXLarge),
)
