package ng.name.servix.app.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.rules.ActivityScenarioRule
import ng.name.servix.app.ui.theme.ServixTheme
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.io.FileOutputStream

const val PIXEL_7_QUALIFIERS = "w411dp-h914dp-xxhdpi"

/**
 * Renders a composable on a Robolectric Pixel-7-class window (411 x 914 dp, xxhdpi)
 * and writes app/build/reports/screenshots/<name>.png.
 * Fake in-memory data only — these tests must never reach the network.
 */
fun AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>.capture(
    name: String,
    fontScale: Float = 1f,
    /**
     * Optional requirement on the drawn pixels, e.g. "the avatar photo is on screen". Coil loads
     * asynchronously, so when this is supplied the harness re-draws and re-checks for up to ten
     * seconds and fails the test if it never passes: a render that silently fell back to initials
     * cannot go green.
     */
    verify: ((Bitmap) -> Boolean)? = null,
    /** Extra context for the failure message when [verify] never passes (e.g. Coil's error). */
    describe: () -> String = { "" },
    afterContent: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    activity.setContent {
        ServixTheme {
            if (fontScale == 1f) {
                content()
            } else {
                val base = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density = base.density, fontScale = fontScale),
                ) { content() }
            }
        }
    }
    waitForIdle()
    afterContent()
    waitForIdle()
    shadowOf(Looper.getMainLooper()).idle()

    val decor: View = activity.window.decorView
    val metrics = activity.resources.displayMetrics
    val width = metrics.widthPixels
    val height = metrics.heightPixels
    if (decor.width != width || decor.height != height) {
        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        decor.layout(0, 0, width, height)
    }
    waitForIdle()
    shadowOf(Looper.getMainLooper()).idle()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    decor.draw(Canvas(bitmap))
    if (verify != null) {
        val deadline = System.currentTimeMillis() + 10_000
        while (!verify(bitmap) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            waitForIdle()
            // idleFor, not idle: Coil's crossfade is a ValueAnimator on the main looper, and a paused
            // Robolectric looper never advances the clock, so the fade would stay at alpha 0 and the
            // photo would never become visible.
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(120))
            bitmap.eraseColor(0)
            decor.draw(Canvas(bitmap))
        }
        check(verify(bitmap)) {
            "Render $name never met the pixel requirement. " + describe().ifBlank { "Images did not load." }
        }
    }
    // Renders are committed at 50% scale to keep the repository small.
    val half = Bitmap.createScaledBitmap(bitmap, width / 2, height / 2, true)

    val dir = File("build/reports/screenshots/screens").apply { mkdirs() }
    val file = File(dir, "$name.png")
    FileOutputStream(file).use { out ->
        check(half.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG encode failed for $name" }
    }
    half.recycle()
    bitmap.recycle()
    check(file.length() > 4_000) { "Screenshot $name looks empty (${file.length()} bytes)" }
    onRoot().fetchSemanticsNode()
}
