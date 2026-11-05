package ng.name.servix.app.screenshots

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
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
    content: @Composable () -> Unit,
) {
    activity.setContent { ServixTheme { content() } }
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

    val dir = File("build/reports/screenshots/screens").apply { mkdirs() }
    val file = File(dir, "$name.png")
    FileOutputStream(file).use { out ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) { "PNG encode failed for $name" }
    }
    bitmap.recycle()
    check(file.length() > 10_000) { "Screenshot $name looks empty (${file.length()} bytes)" }
    onRoot().fetchSemanticsNode()
}
