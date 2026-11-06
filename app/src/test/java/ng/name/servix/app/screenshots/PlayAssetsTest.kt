package ng.name.servix.app.screenshots

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.PathParser
import androidx.test.core.app.ApplicationProvider
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import ng.name.servix.app.R
import ng.name.servix.app.data.model.*
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.ServixApiService
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.data.repository.AiRepository
import ng.name.servix.app.data.repository.CatalogueRepository
import ng.name.servix.app.ui.account.AccountHubScreen
import ng.name.servix.app.ui.ai.AiScreen
import ng.name.servix.app.ui.ai.AiUiState
import ng.name.servix.app.ui.ai.AiViewModel
import ng.name.servix.app.ui.ai.ChatTurn
import ng.name.servix.app.ui.bookings.BookingDetailScreen
import ng.name.servix.app.ui.bookings.BookingDetailUiState
import ng.name.servix.app.ui.catalogue.ProfessionalProfileScreen
import ng.name.servix.app.ui.catalogue.ProfessionalProfileUiState
import ng.name.servix.app.ui.catalogue.SearchMode
import ng.name.servix.app.ui.catalogue.SearchUiState
import ng.name.servix.app.ui.catalogue.ServiceDetailScreen
import ng.name.servix.app.ui.catalogue.ServiceDetailUiState
import ng.name.servix.app.ui.explore.ExploreScreen
import ng.name.servix.app.ui.home.HomeTabScreen
import ng.name.servix.app.ui.home.HomeUiState
import ng.name.servix.app.ui.notifications.NotificationsScreen
import ng.name.servix.app.ui.notifications.NotificationsUiState
import ng.name.servix.app.ui.theme.ServixTheme
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream

/**
 * 360 x 640 dp at xxhdpi (density 3) = exactly 1080 x 1920 px, Play's 9:16 phone screenshot size.
 * Density matters: the same dp window at xxxhdpi (density 4) would render 1440 x 2560, which is a
 * valid Play size but not the 1080 x 1920 asked for.
 */
const val PLAY_PHONE_QUALIFIERS = "w360dp-h640dp-xxhdpi"
const val PLAY_W = 1080
const val PLAY_H = 1920

private const val DEEP_FOREST = 0xFF12372A.toInt()
private const val WARM_IVORY = 0xFFF7F4EC.toInt()
private const val CORAL = 0xFFE56B5D.toInt()
private const val TAGLINE = "Hire trusted Nigerian professionals."
private const val BRAND_SVG = "servix-logo-dark.svg"

/**
 * Renders the Google Play store assets from the same offline fixture harness as the review
 * renders, but at store sizes and WITHOUT the 50% downscale the review renders use:
 *
 *  * `phone/NN_name.png`          — 1080 x 1920 (360 x 640 dp xxxhdpi)
 *  * `icon-512.png`               — 512 x 512, opaque, the real adaptive launcher icon
 *  * `feature-graphic-1024x500.png` — Deep Forest canvas, the real brand lockup, tagline
 *
 * The wordmark is not a redraw: it is public/brand/servix-logo-dark.svg from
 * toluenetech-spec/servix (ref arena/01a0dc4c-servix), parsed with
 * androidx.core.graphics.PathParser and painted in the SVG's own Warm Ivory fill over the Deep
 * Forest background. The launcher icon is asserted to carry exactly that SVG's monogram path.
 * No network access: the Retrofit binding points at 127.0.0.1:9 and never calls anything.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = PLAY_PHONE_QUALIFIERS,
    instrumentedPackages = ["androidx.loader.content"],
)
class PlayAssetsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val api: ServixApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://127.0.0.1:9/")
            .addConverterFactory(ApiErrorParser.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ServixApiService::class.java)
    }

    private fun aiState() = AiUiState(
        ownerId = Fixtures.USER_ID,
        turns = Fixtures.aiTurns,
        usage = Fixtures.usage,
        categories = Fixtures.categories,
        category = "graphic-design",
        input = "Can you compare the two designers?",
    )

    // ---------------------------------------------------------------- phone screenshots

    @Test
    fun phone01Home() = capturePlayPhone("01_home") {
        HomeTabScreen(
            user = Fixtures.user,
            city = "Lagos",
            uiState = HomeUiState(
                loading = false,
                categories = Fixtures.categories,
                professionals = Fixtures.professionals,
                overview = AccountOverviewDto(emailVerified = true),
                bookings = Fixtures.bookings,
            ),
            aiUsage = Fixtures.usage,
        )
    }

    @Test
    fun phone02Explore() = capturePlayPhone("02_explore") {
        ExploreScreen(
            uiState = SearchUiState(
                mode = SearchMode.PROFESSIONALS,
                query = "logo designer",
                selectedCategory = "graphic-design",
                location = "Lagos",
                categories = Fixtures.categories,
                professionals = Fixtures.professionals,
                total = 12,
                loading = false,
            ),
            categoryNameFor = { slug -> Fixtures.categories.find { it.id == slug }?.name.orEmpty() },
            onQueryChange = {}, onQuerySubmit = {}, onModeChange = {}, onCategorySelect = {},
            onLocationChange = {}, onRatingSelect = {}, onPriceSelect = {}, onAvailabilitySelect = {},
            onSortSelect = {}, onPriceLimitChange = {}, onClearFilters = {}, onRefresh = {}, onLoadNextPage = {},
            onOpenProfessional = {}, onOpenService = {},
        )
    }

    @Test
    fun phone03Professional() = capturePlayPhone("03_professional") {
        ProfessionalProfileScreen(
            user = Fixtures.user,
            uiState = ProfessionalProfileUiState(
                slug = "tunde-bakare",
                loading = false,
                professional = Fixtures.professionalDetail,
                services = Fixtures.services,
                reviews = Fixtures.reviews,
                availability = AvailabilitySummaryDto("2026-11-06T09:00:00.000Z", true, true, true),
                isSaved = false,
            ),
            categoryNameFor = { "Graphic Design" },
            onBackClick = {}, onRetry = {}, onBrowseProfessionalsClick = {}, onToggleSave = {},
            onSignInClick = {}, onOpenService = {},
        )
    }

    @Test
    fun phone04Service() = capturePlayPhone("04_service") {
        ServiceDetailScreen(
            user = Fixtures.user,
            uiState = ServiceDetailUiState(
                slug = "brand-identity-package",
                loading = false,
                service = Fixtures.serviceDetail,
                reviews = Fixtures.reviews,
                availabilitySlots = Fixtures.availabilitySlots,
                selectedSlot = Fixtures.availabilitySlots.first(),
            ),
            categoryNameFor = { "Graphic Design" },
            onSelectSlot = {}, onNotesChange = {}, onSubmitBookAndPay = {},
            onBackClick = {}, onRetry = {}, onBrowseServicesClick = {}, onOpenProfessional = {},
            onSignInToBookClick = {}, onCreateAccountToBookClick = {}, onNavigateToBookingAfterPay = {},
        )
    }

    @Test
    fun phone05Booking() = capturePlayPhone("05_booking") {
        BookingDetailScreen(
            uiState = BookingDetailUiState(
                bookingId = Fixtures.BOOKING_ID,
                loading = false,
                booking = Fixtures.booking("requested"),
            ),
            pollingAfterPay = false,
            onResumeCheck = {}, onBackClick = {}, onRetry = {}, onRefreshPaymentStatus = {},
            onPayClick = {}, onOpenModal = {}, onCloseModal = {}, onSubmitCancel = {},
            onSubmitConfirmCompletion = {}, onSubmitDispute = {}, onSubmitReview = { _, _ -> },
            onStartBookAgain = {}, onContinueRebook = { _, _, _ -> }, onOpenProfessional = {},
            onOpenService = {},
        )
    }

    @Test
    fun phone06Ai() = capturePlayPhone("06_ai") {
        AiScreen(
            user = Fixtures.user,
            state = aiState().copy(
                turns = Fixtures.aiTurns + listOf(
                    ChatTurn(3L, "user", "Does that price include brand guidelines?"),
                    ChatTurn(
                        4L, "assistant",
                        "Yes — the **₦120,000** package includes a logo suite, colour and type system, " +
                            "and a short brand guidelines PDF. Revisions are included; extra deliverables " +
                            "are quoted separately.",
                    ),
                ),
            ),
            viewModel = AiViewModel(AiRepository(api, OkHttpClient()), CatalogueRepository { api }),
            onSignIn = {},
        )
    }

    @Test
    fun phone07Notifications() = capturePlayPhone("07_notifications") {
        NotificationsScreen(
            uiState = NotificationsUiState(
                loading = false,
                unread = 2,
                items = listOf(
                    NotificationDto(
                        id = "n1", type = "booking", title = "Tunde accepted your booking",
                        body = "Your brand identity package is scheduled for 12 November.",
                        link = "/bookings/${Fixtures.BOOKING_ID}", createdAt = "2026-11-05T09:12:00.000Z",
                    ),
                    NotificationDto(
                        id = "n2", type = "payment", title = "Payment received",
                        body = "₦350,000 is held in escrow until you confirm the work.",
                        link = "/bookings/${Fixtures.BOOKING_ID}", readAt = "2026-11-04T18:02:00.000Z",
                        createdAt = "2026-11-04T17:58:00.000Z",
                    ),
                ),
            ),
            onBackClick = {}, onRefresh = {}, onMarkAllRead = {}, onItemClick = {},
        )
    }

    @Test
    fun phone08Account() = capturePlayPhone("08_account") {
        AccountHubScreen(
            user = Fixtures.user,
            accountRepository = AccountRepository { api },
            updateState = ng.name.servix.app.updates.UpdateUiState(
                context = ApplicationProvider.getApplicationContext(),
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            ),
            city = "Lagos",
            unreadNotifications = 2,
        )
    }

    // ---------------------------------------------------------------- icon + feature graphic

    @Test
    fun playIcon512() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // The launcher icon must BE the brand monogram: the vector has to carry the same path data
        // as the first path of servix-logo-dark.svg, otherwise Play would ship a lookalike mark.
        val brandSvg = readBrandAsset(BRAND_SVG)
        val monogram = brandPaths(brandSvg).first().second
        val vectorFile = File("src/main/res/drawable/ic_launcher_foreground.xml")
        check(vectorFile.isFile) { "ic_launcher_foreground.xml is missing" }
        val vectorData = firstGroupValue(vectorFile.readText(), "android:pathData")
        checkNotNull(vectorData) { "ic_launcher_foreground.xml has no pathData" }
        check(normalizePath(vectorData) == normalizePath(monogram)) {
            "the launcher icon has drifted from the brand monogram in $BRAND_SVG"
        }
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // The adaptive icon (mipmap-anydpi-v26/ic_launcher.xml) declares exactly two layers:
        // @color/deep_forest as the background and @drawable/ic_launcher_foreground as the SX mark.
        // Paint them directly rather than going through AdaptiveIconDrawable: the mask/inset maths of
        // that drawable is the part that is least reliable outside a real device, and Play wants a
        // flat, fully opaque 512 square anyway (the launcher applies its own mask).
        canvas.drawColor(DEEP_FOREST)
        val foreground = ResourcesCompat.getDrawable(
            context.resources, R.drawable.ic_launcher_foreground, context.theme,
        )
        checkNotNull(foreground) { "ic_launcher_foreground is missing" }
        foreground.setBounds(0, 0, 512, 512)
        foreground.draw(canvas)

        // Play requires a 512 x 512 32-bit PNG with no transparency.
        listOf(0 to 0, 511 to 0, 0 to 511, 511 to 511, 256 to 256).forEach { (x, y) ->
            check(Color.alpha(bitmap.getPixel(x, y)) == 255) {
                "icon pixel ($x,$y) is not opaque — Play rejects transparent 512 icons"
            }
        }
        writePlayAsset(bitmap, "icon-512.png")
    }

    @Test
    fun playFeatureGraphic1024x500() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val w = 1024
        val h = 500
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(DEEP_FOREST)

        // The artwork is the brand's own: public/brand/servix-logo-dark.svg, drawn as vectors so
        // the wordmark is the real lettering and not a system font standing in for it.
        val svg = readBrandAsset(BRAND_SVG)
        val viewBox = svgViewBox(svg)
        val paths = brandPaths(svg)
        check(paths.size == 2) {
            "expected the monogram and the wordmark in $BRAND_SVG, found ${paths.size} paths"
        }

        val padding = 72f
        val markWidth = 620f
        val gap = 52f
        val taglineSize = 42f
        val ruleGap = 22f
        val ruleHeight = 6f
        val ruleWidth = 88f
        val safeWidth = w - 2 * padding
        check(markWidth <= safeWidth) { "the brand lockup is wider than the safe area" }

        val scale = markWidth / viewBox[2]
        val markHeight = viewBox[3] * scale

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        brandPaint.color = WARM_IVORY
        brandPaint.style = Paint.Style.FILL

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        taglinePaint.typeface = ResourcesCompat.getFont(context, R.font.inter_500) ?: Typeface.DEFAULT
        taglinePaint.textSize = taglineSize
        taglinePaint.color = WARM_IVORY
        taglinePaint.alpha = 0xDD

        val taglineWidth = taglinePaint.measureText(TAGLINE)
        check(taglineWidth <= safeWidth) { "the tagline is wider than the safe area" }

        val taglineHeight = taglineSize * 0.75f
        val blockHeight = markHeight + gap + taglineHeight + ruleGap + ruleHeight
        val top = (h - blockHeight) / 2f
        val markLeft = (w - markWidth) / 2f
        val baseline = top + markHeight + gap + taglineHeight

        for (entry in paths) {
            val parsed = PathParser.createPathFromPathData(entry.second)
            checkNotNull(parsed) { "could not parse a path from $BRAND_SVG" }
            val matrix = Matrix()
            matrix.postScale(scale, scale)
            matrix.postTranslate(entry.first.first * scale, entry.first.second * scale)
            matrix.postTranslate(markLeft - viewBox[0] * scale, top - viewBox[1] * scale)
            parsed.transform(matrix)
            canvas.drawPath(parsed, brandPaint)
        }

        canvas.drawText(TAGLINE, (w - taglineWidth) / 2f, baseline, taglinePaint)

        // Coral accent rule under the tagline — the palette's accent, used nowhere else here.
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        rulePaint.color = CORAL
        val ruleTop = baseline + ruleGap
        val ruleLeft = (w - ruleWidth) / 2f
        val ruleRight = (w + ruleWidth) / 2f
        canvas.drawRect(ruleLeft, ruleTop, ruleRight, ruleTop + ruleHeight, rulePaint)

        check(Color.alpha(bitmap.getPixel(1, 1)) == 255) { "feature graphic background is not opaque" }
        writePlayAsset(bitmap, "feature-graphic-1024x500.png")
    }

    // ---------------------------------------------------------------- brand assets

    /**
     * The brand SVGs live in the website snapshot (`reference/web/public/brand/`), byte-identical
     * to public/brand/*.svg in toluenetech-spec/servix at ref arena/01a0dc4c-servix. Unit tests run
     * with the module directory as the working directory, hence the relative candidates.
     */
    private fun readBrandAsset(name: String): String {
        val candidates = listOf(
            File("../reference/web/public/brand/$name"),
            File("reference/web/public/brand/$name"),
            File("../../reference/web/public/brand/$name"),
        )
        for (candidate in candidates) {
            if (candidate.isFile) return candidate.readText()
        }
        error("brand asset not found: $name")
    }

    private fun svgViewBox(svg: String): List<Float> {
        val raw = firstGroupValue(svg, "viewBox")
        checkNotNull(raw) { "the brand SVG has no viewBox" }
        val parts = raw.trim().split(Regex("[\\s,]+"))
        check(parts.size == 4) { "the brand SVG viewBox is not four numbers" }
        return parts.map { it.toFloat() }
    }

    /**
     * Returns (translateX, translateY) to pathData for each path, in document order. servix-logo-dark.svg
     * nests the lettering group inside the monogram group, so a stack of offsets is required: a flat
     * read of the transforms puts the wordmark off the canvas.
     */
    private fun brandPaths(svg: String): List<Pair<Pair<Float, Float>, String>> {
        val result = mutableListOf<Pair<Pair<Float, Float>, String>>()
        val offsets = java.util.ArrayDeque<Pair<Float, Float>>()
        offsets.addLast(0f to 0f)
        val tagPattern = Regex("<g\\b[^>]*>|</g>|<path\\b[^>]*>")
        val translatePattern = Regex("translate\\(\\s*(-?[\\d.]+)[\\s,]+(-?[\\d.]+)\\s*\\)")
        for (match in tagPattern.findAll(svg)) {
            val tag = match.value
            if (tag == "</g>") {
                if (offsets.size > 1) offsets.removeLast()
            } else if (tag.startsWith("<g")) {
                val parent = offsets.last()
                var dx = 0f
                var dy = 0f
                val found = translatePattern.find(tag)
                if (found != null) {
                    dx = found.groupValues[1].toFloat()
                    dy = found.groupValues[2].toFloat()
                }
                offsets.addLast(parent.first + dx to parent.second + dy)
            } else {
                val data = firstGroupValue(tag, "d")
                if (data != null) result.add(offsets.last() to data)
            }
        }
        return result
    }

    /** Value of `attribute="…"` in a tag or document, or null. */
    private fun firstGroupValue(text: String, attribute: String): String? {
        val pattern = Regex(attribute + "=\"([^\"]+)\"")
        val match = pattern.find(text)
        return match?.groupValues?.get(1)
    }

    private fun normalizePath(data: String): String {
        val collapsed = data.replace(Regex("\\s+"), " ")
        return collapsed.trim()
    }

    // ---------------------------------------------------------------- plumbing

    private fun capturePlayPhone(name: String, content: @Composable () -> Unit) {
        rule.activity.setContent { ServixTheme { content() } }
        rule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        val decor: View = rule.activity.window.decorView
        val metrics = rule.activity.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        check(width == PLAY_W && height == PLAY_H) {
            "expected a ${PLAY_W}x$PLAY_H window from '$PLAY_PHONE_QUALIFIERS' (360dp x 640dp at density 3), got ${width}x$height"
        }
        if (decor.width != width || decor.height != height) {
            decor.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            decor.layout(0, 0, width, height)
        }
        rule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Full size on purpose: the review renders halve themselves to keep the repo small,
        // but Play needs the exact 1080 x 1920 pixels.
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        decor.draw(Canvas(bitmap))
        writePlayAsset(bitmap, "phone/$name.png")
    }

    private fun writePlayAsset(bitmap: Bitmap, relativePath: String) {
        val file = File("build/reports/screenshots/play/$relativePath")
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                "PNG encode failed for $relativePath"
            }
        }
        check(file.length() > 8_000) { "Play asset $relativePath looks empty (${file.length()} bytes)" }
        println("PLAY_ASSET ${bitmap.width}x${bitmap.height} ${file.length()}B ${file.absolutePath}")
        bitmap.recycle()
    }
}
