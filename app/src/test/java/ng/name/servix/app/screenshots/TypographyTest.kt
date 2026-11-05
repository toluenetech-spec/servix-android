package ng.name.servix.app.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ng.name.servix.app.ui.theme.ServixTheme
import ng.name.servix.app.ui.theme.ServixTypography

/**
 * Proves the Inter weights are real files, not one file registered four times:
 * if every weight pointed at the same TTF, semi-bold would measure exactly as wide as regular.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PIXEL_7_QUALIFIERS)
class TypographyTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun titleLargeIsWiderThanBodyLargeAndSemiBoldIsWiderThanRegular() {
        lateinit var measurer: androidx.compose.ui.text.TextMeasurer
        rule.setContent {
            ServixTheme {
                measurer = rememberTextMeasurer()
            }
        }
        rule.waitForIdle()

        val sample = "Welcome back, Adaeze"
        val titleWidth = measurer.measure(sample, ServixTypography.titleLarge).size.width
        val bodyWidth = measurer.measure(sample, ServixTypography.bodyLarge).size.width
        assertTrue("titleLarge ($titleWidth px) should be wider than bodyLarge ($bodyWidth px)", titleWidth > bodyWidth)

        val sameSize = ServixTypography.bodyLarge.copy(fontWeight = FontWeight.Normal, fontSize = 18.sp)
        val regular = measurer.measure(sample, sameSize).size.width
        val semiBold = measurer.measure(sample, sameSize.copy(fontWeight = FontWeight.SemiBold)).size.width
        assertTrue("SemiBold ($semiBold px) should be wider than Regular ($regular px) at the same size", semiBold > regular)

        val bold = measurer.measure(sample, sameSize.copy(fontWeight = FontWeight.Bold)).size.width
        assertTrue("Bold ($bold px) should be wider than SemiBold ($semiBold px)", bold > semiBold)
    }
}
