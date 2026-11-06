package ng.name.servix.app.ui.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Motion budget (plan pass 5). Every duration below is the *base* value; [motionMs] zeroes it when
 * the owner turned animations off in Settings (animatorDurationScale == 0).
 */
object Motion {
    const val SCREEN_MS = 220
    const val SHEET_MS = 250
    const val CROSSFADE_MS = 150
    const val TAB_MS = 120
    const val PRESS_MS = 120
    const val CARET_MS = 600

    const val PRESS_SCALE = 0.98f
    const val PRESS_DARKEN = 0.08f

    /** 0 when "Remove animations" is on, otherwise the base duration unchanged. */
    fun duration(context: Context, baseMs: Int): Int {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        return if (scale <= 0f) 0 else baseMs
    }
}

@Composable
fun motionMs(baseMs: Int): Int {
    val context = LocalContext.current
    return remember(baseMs, context) { Motion.duration(context, baseMs) }
}

/** Shared-axis X for pushes/pops: 30 % slide plus a short fade, 220 ms FastOutSlowIn. */
@Composable
fun screenOffsetDp(fraction: Float = 0.3f): Int = motionMs(Motion.SCREEN_MS)

/** Scale-to-0.98 + 8 % darker while pressed, 120 ms ease-out. No overshoot, no bounce. */
fun Modifier.pressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val ms = motionMs(Motion.PRESS_MS)
    val active = pressed && enabled
    val scale by animateFloatAsState(
        targetValue = if (active) Motion.PRESS_SCALE else 1f,
        animationSpec = tween(ms, easing = FastOutSlowInEasing),
        label = "pressScale",
    )
    val darken by animateFloatAsState(
        targetValue = if (active) Motion.PRESS_DARKEN else 0f,
        animationSpec = tween(ms, easing = FastOutSlowInEasing),
        label = "pressDarken",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .drawWithContent {
            drawContent()
            if (darken > 0f) {
                drawRect(
                    color = Color.Black.copy(alpha = darken),
                    size = Size(this.size.width, this.size.height),
                )
            }
        }
}

/** One light tick. Used only for Book & Pay, Confirm completion, Review submitted, Sign-in success. */
@Composable
fun rememberSuccessTick(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(haptics, view) {
        {
            view.performHapticFeedback(
                android.view.HapticFeedbackConstants.CONFIRM,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
            )
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}
