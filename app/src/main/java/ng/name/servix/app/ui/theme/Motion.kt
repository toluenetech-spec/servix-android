package ng.name.servix.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Motion tokens from `m0_motion_system.html` (CORRECTIONS.md §4): a press is a 0.96 scale on a stiff
 * spring, content moves on one easing at 280 ms, sheets arrive on a softer spring, and success pops
 * once. Nothing overshoots except the success pop, and that overshoot is the design's own damping 0.55.
 *
 * Android has no `prefers-reduced-motion`; the system switch is Settings.Global.ANIMATOR_DURATION_SCALE.
 * When it is 0 the app fades only — see [reducedMotion] and [motionTween].
 */
object ServixMotion {
    const val PRESS_SCALE = 0.96f
    const val CONTENT_MS = 280
    const val SUCCESS_MS = 620

    /** The one content easing: cubic-bezier(0.2, 0, 0, 1). */
    val ContentEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> content() = tween<T>(durationMillis = CONTENT_MS, easing = ContentEasing)

    fun <T> press() = spring<T>(stiffness = 400f, dampingRatio = 0.75f)

    fun <T> sheet() = spring<T>(stiffness = 300f)

    fun <T> successPop() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
}

/**
 * True when the device asks for animations to be off. Read once per composition; the setting is a
 * global the user can change while the app is backgrounded, so screens that care re-read on resume.
 */
@Composable
fun reducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/** A fade when animations are off, the content tween when they are on. */
@Composable
fun <T> motionTween(durationMillis: Int = ServixMotion.CONTENT_MS) =
    if (reducedMotion()) {
        tween<T>(durationMillis = 120)
    } else {
        tween<T>(durationMillis = durationMillis, easing = ServixMotion.ContentEasing)
    }
