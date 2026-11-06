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

/**
 * Renders the Google Play store assets from the same offline fixture harness as the review
 * renders, but at store sizes and WITHOUT the 50% downscale the review renders use:
 *
 *  * `phone/NN_name.png`          — 1080 x 1920 (360 x 640 dp xxxhdpi)
 *  * `icon-512.png`               — 512 x 512, opaque, the real adaptive launcher icon
 *  * `feature-graphic-1024x500.png` — Deep Forest canvas, the real servix-logo-dark.svg wordmark, tagline
 *
 * Everything is drawn from the brand artwork: the wordmark comes from public/brand/servix-logo-dark.svg
 * (toluenetech-spec/servix, ref arena/01a0dc4c-servix), the icon from the adaptive-icon layers whose
 * foreground is asserted byte-for-byte against that SVG's monogram path, and the palette from
 * ui/theme/Color.kt. No network access: the Retrofit binding points at 127.0.0.1:9.
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

        // The launcher icon must BE the brand monogram, not a lookalike. The brand source is
        // public/brand/servix-logo-dark.svg in toluenetech-spec/servix (ref arena/01a0dc4c-servix);
        // its first path is the SX monogram, and the vector below must carry exactly that path data.
        val brandSvg = brandAsset("servix-logo-dark.svg").readText()
        val monogram = svgPaths(brandSvg).firstOrNull()?.third
            ?: error("servix-logo-dark.svg exposes no paths")
        val launcherVector = File("src/main/res/drawable/ic_launcher_foreground.xml")
        check(launcherVector.isFile) { "launcher foreground vector is missing" }
        val launcherPath = Regex("android:pathData=\"([^\"]+)\"")
            .find(launcherVector.readText())?.groupValues?.get(1)
            ?: error("ic_launcher_foreground.xml has no pathData")
        check(normalizedPath(launcherPath) == normalizedPath(monogram)) {
            "the launcher icon has drifted from the brand monogram in servix-logo-dark.svg"
        }

        // mipmap-anydpi-v26/ic_launcher.xml declares exactly two layers: @color/deep_forest and that
        // foreground vector. Paint them directly — AdaptiveIconDrawable's mask/inset maths is the
        // least reliable part outside a real device, and Play wants a flat opaque 512 square anyway
        // (the launcher applies its own mask).
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
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

        // The wordmark is the real brand artwork — public/brand/servix-logo-dark.svg in
        // toluenetech-spec/servix (ref arena/01a0dc4c-servix), the file served on the website for
        // dark backgrounds. Two paths: the SX monogram and the "servix" lettering, each inside its
        // own translate() group. Both are parsed with androidx PathParser and drawn as vectors, so
        // this is the real outline data, not a type approximation.
        val svg = brandAsset("servix-logo-dark.svg").readText()
        val viewBox = Regex("viewBox=\"([^\"]+)\"").find(svg)?.groupValues?.get(1)
            ?.trim()?.split(Regex("[\\s,]+"))?.map { it.toFloat() }
            ?: error("servix-logo-dark.svg has no viewBox")
        check(viewBox.size == 4) { "unexpected viewBox '${viewBox.joinToString(" ")}'" }
        val paths = svgPaths(svg)
        check(paths.size == 2) { "expected 2 paths in servix-logo-dark.svg, found ${paths.size}" }

        val padding = 72
        val markWidth = 620f
        val scale = markWidth / viewBox[2]
        val markHeight = viewBox[3] * scale
        val gap = 52f
        val taglineSize = 42f
        val ruleGap = 22f
        val ruleHeight = 6f
        check(markWidth <= w - 2 * padding) { "wordmark is $markWidth px, wider than the safe area" }

        val blockHeight = markHeight + gap + taglineSize * 0.75f + ruleGap + ruleHeight
        val top = (h - blockHeight) / 2f
        val markX = (w - markWidth) / 2f

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = WARM_IVORY // the SVG's own fill is #F7F4EC
        }
        for ((groupX, groupY, data) in paths) {
            val path = PathParser.createPathFromPathData(data)
                ?: error("could not parse a path from servix-logo-dark.svg")
            val matrix = Matrix()
            matrix.postScale(scale, scale)
            matrix.postTranslate(groupX * scale, groupY * scale)
            matrix.postTranslate(markX - viewBox[0] * scale, top - viewBox[1] * scale)
            path.transform(matrix)
            canvas.drawPath(path, brandPaint)
        }

        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = ResourcesCompat.getFont(context, R.font.inter_500) ?: Typeface.DEFAULT
            textSize = taglineSize
            color = WARM_IVORY
            alpha = 0xDD
        }
        val taglineWidth = taglinePaint.measureText(TAGLINE)
        check(taglineWidth <= w - 2 * padding) {
            "tagline is $taglineWidth px wide, wider than the ${w - 2 * padding} px safe area"
        }
        val taglineBaseline = top + markHeight + gap + taglineSize * 0.75f
        canvas.drawText(TAGLINE, (w - taglineWidth) / 2f, taglineBaseline, taglinePaint)

        // Coral accent rule under the tagline — the palette's accent, used nowhere else here.
        val ruleTop = taglineBaseline + ruleGap
        val ruleWidth = 88f
        canvas.drawRect(
            (w - ruleWidth) / 2f,
            ruleTop,
            (w + ruleWidth) / 2f,
            ruleTop + ruleHeight,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CORAL },
        )

        check(Color.alpha(bitmap.getPixel(1, 1)) == 255) { "feature graphic background is not opaque" }
        writePlayAsset(bitmap, "feature-graphic-1024x500.png")
    }

    // ---------------------------------------------------------------- brand assets

    /**
     * The brand SVGs live in the website snapshot (`reference/web/public/brand/`), byte-identical to
     * public/brand/*.svg in toluenetech-spec/servix at ref arena/01a0dc4c-servix. Unit tests run with
     * the module directory as the working directory, so `app/../reference/...` resolves.
     */
    private fun brandAsset(name: String): File {
        val candidates = listOf(
            File("../reference/web/public/brand/$name"),
            File("reference/web/public/brand/$name"),
            File("../../reference/web/public/brand/$name"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("brand asset $name not found; looked in ${candidates.joinToString { it.path }}")
    }

    /**
     * Minimal, data-driven reader for the brand SVGs: walks `<g>` / `</g>` / `<path>` in document
     * order, keeps a stack so nested groups compose (servix-logo-dark.svg nests the lettering group
     * inside the monogram group — a flat read would put the wordmark off-canvas), and returns
     * (translateX, translateY, pathData) per path. No coordinate is hardcoded.
     */
    private fun svgPaths(svg: String): List<Triple<Float, Float, String>> {
        val out = mutableListOf<Triple<Float, Float, String>>()
        val stack = ArrayDeque<Pair<Float, Float>>()
        stack.addLast(0f to 0f)
        for (match in Regex("""<g\b[^>]*>|</g>|<path\b[^>]*>""").findAll(svg)) {
            val tag = match.value
            if (tag == "</g>") {
                if (stack.size > 1) stack.removeLast()
            } else if (tag.startsWith("<g")) {
                val parent = stack.last()
                val translate = Regex("""translate\(\s*(-?[\d.]+)[\s,]+(-?[\d.]+)\s*\)""").find(tag)
                val dx = translate?.groupValues?.get(1)?.toFloat() ?: 0f
                val dy = translate?.groupValues?.get(2)?.toFloat() ?: 0f
                stack.addLast((parent.first + dx) to (parent.second + dy))
            } else {
                val data = Regex("\\sd=\"([^\"]+)\"").find(tag)?.groupValues?.get(1)
                if (data != null) {
                    val current = stack.last()
                    out += Triple(current.first, current.second, data)
                }
            }
        }
        return out
    }

    private fun normalizedPath(data: String): String = data.replace(Regex("""\s+"""), " ").trim()

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
