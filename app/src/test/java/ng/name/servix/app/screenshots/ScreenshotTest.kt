package ng.name.servix.app.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import ng.name.servix.app.ui.account.AccountHubScreen
import ng.name.servix.app.ui.notifications.NotificationsScreen
import ng.name.servix.app.ui.notifications.NotificationsUiState
import ng.name.servix.app.ui.auth.SecurityCheckScreen
import ng.name.servix.app.data.repository.AccountRepository
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ng.name.servix.app.data.model.*
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.ServixApiService
import ng.name.servix.app.data.repository.AiRepository
import ng.name.servix.app.data.repository.CatalogueRepository
import ng.name.servix.app.ui.ai.AiScreen
import ng.name.servix.app.ui.home.HomeTabScreen
import ng.name.servix.app.ui.home.HomeUiState
import ng.name.servix.app.ui.ai.AiUiState
import ng.name.servix.app.ui.ai.AiViewModel
import ng.name.servix.app.ui.ai.ChatTurn
import ng.name.servix.app.ui.auth.AuthUiState
import ng.name.servix.app.ui.auth.LoginScreen
import ng.name.servix.app.ui.auth.WelcomeScreen
import ng.name.servix.app.ui.bookings.BookingDetailScreen
import ng.name.servix.app.ui.bookings.BookingDetailUiState
import ng.name.servix.app.ui.bookings.BookingsScreen
import ng.name.servix.app.ui.bookings.BookingsUiState
import ng.name.servix.app.ui.catalogue.ProfessionalProfileScreen
import ng.name.servix.app.ui.catalogue.ProfessionalProfileUiState
import ng.name.servix.app.ui.catalogue.SearchMode
import ng.name.servix.app.ui.explore.ExploreScreen
import ng.name.servix.app.ui.catalogue.SearchUiState
import ng.name.servix.app.ui.catalogue.ServiceDetailScreen
import ng.name.servix.app.ui.catalogue.ServiceDetailUiState
import ng.name.servix.app.ui.components.ComponentsGallery
import androidx.compose.runtime.Composable

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = PIXEL_7_QUALIFIERS,
    instrumentedPackages = ["androidx.loader.content"],
)
class ScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /** Local, offline Retrofit binding: constructing it performs no network call. */
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

    @Test
    fun components() = rule.capture("00_components") {
        ComponentsGallery()
    }

    @Test
    fun welcome() = rule.capture("01_welcome") {
        WelcomeScreen(onSignInClick = {}, onCreateAccountClick = {}, onBrowseFirstClick = {})
    }

    @Test
    fun welcomeProviders() = rule.capture("01b_welcome_providers") {
        WelcomeScreen(
            onSignInClick = {},
            onCreateAccountClick = {},
            onBrowseFirstClick = {},
            providers = listOf("google", "github"),
        )
    }

    @Test
    fun signInProviders() = rule.capture("02b_sign_in_providers") {
        LoginScreen(
            uiState = AuthUiState(loginEmail = "adaeze@example.com", loginPassword = "secret-pass"),
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {},
            onForgotPasswordClick = {}, onCreateAccountClick = {}, onBackClick = {},
            providers = listOf("google", "github"),
        )
    }

    @Test
    fun signIn() = rule.capture("02_sign_in") {
        LoginScreen(
            uiState = AuthUiState(loginEmail = "adaeze@example.com", loginPassword = "secret-pass"),
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {},
            onForgotPasswordClick = {}, onCreateAccountClick = {}, onBackClick = {},
        )
    }

    @Test
    fun homeSignedIn() = rule.capture("03_home_signed_in") {
        HomeTabScreen(
            user = Fixtures.user,
            city = "Lagos",
            uiState = HomeUiState(
                loading = false,
                categories = Fixtures.categories,
                professionals = Fixtures.professionals,
                overview = AccountOverviewDto(emailVerified = true),
            ),
            aiUsage = Fixtures.usage,
        )
    }

    @Test
    fun homeGuest() = rule.capture("04_home_guest") {
        HomeTabScreen(
            user = null,
            city = "Lagos",
            uiState = HomeUiState(
                loading = false,
                categories = Fixtures.categories,
                professionals = Fixtures.professionals,
            ),
        )
    }

    @Test
    fun homeWithBookings() = rule.capture("05_home_bookings") {
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
    fun homeLoading() = rule.capture("05b_home_loading") {
        HomeTabScreen(
            user = Fixtures.user,
            city = "Lagos",
            uiState = HomeUiState(loading = true),
        )
    }

    @Test
    fun exploreResults() = rule.capture("06_explore_results") {
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
    fun exploreEmpty() = rule.capture("07_explore_empty") {
        ExploreScreen(
            uiState = SearchUiState(
                mode = SearchMode.SERVICES,
                query = "drone pilot for a wedding in Katsina",
                categories = Fixtures.categories,
                total = 0,
                loading = false,
            ),
            categoryNameFor = { "" },
            onQueryChange = {}, onQuerySubmit = {}, onModeChange = {}, onCategorySelect = {},
            onLocationChange = {}, onRatingSelect = {}, onPriceSelect = {}, onAvailabilitySelect = {},
            onSortSelect = {}, onPriceLimitChange = {}, onClearFilters = {}, onRefresh = {}, onLoadNextPage = {},
            onOpenProfessional = {}, onOpenService = {},
        )
    }

    @Test
    fun professionalProfile() = rule.capture("08_professional_profile") {
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
    fun serviceDetail() = rule.capture("09_service_detail") {
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
    fun bookingsGrouped() = rule.capture("10_bookings_grouped") {
        val zone = java.time.ZoneId.of("Africa/Lagos")
        val today = java.time.LocalDate.now(zone)
        fun at(date: java.time.LocalDate) = date.atTime(10, 0).atZone(zone).toInstant().toString()
        BookingsScreen(
            user = Fixtures.user,
            uiState = BookingsUiState(
                loading = false,
                bookings = listOf(
                    Fixtures.booking("requested", "55555555-5555-4555-8555-555555555555", "Logo suite").copy(scheduledAt = at(today)),
                    Fixtures.booking("accepted", "66666666-6666-4666-8666-666666666666", "Social media starter kit").copy(scheduledAt = at(today.plusDays(2))),
                    Fixtures.booking("in_progress", "77777777-7777-4777-8777-777777777777", "Pitch deck design").copy(scheduledAt = at(today.plusDays(30))),
                ),
            ),
            onTabSelect = {}, onRefresh = {}, onExploreServicesClick = {}, onSignInClick = {},
            onOpenBooking = {},
        )
    }

    @Test
    fun aiEmpty() = rule.capture("14b_ai_empty") {
        AiScreen(
            user = Fixtures.user,
            state = aiState().copy(turns = emptyList()),
            viewModel = AiViewModel(AiRepository(api, OkHttpClient()), CatalogueRepository { api }),
            onSignIn = {},
        )
    }

    @Test
    fun accountHub() = rule.capture("15_account") {
        AccountHubScreen(
            user = Fixtures.user,
            accountRepository = AccountRepository { api },
            updateState = ng.name.servix.app.updates.UpdateUiState(
                context = androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            ),
            city = "Lagos",
            unreadNotifications = 2,
        )
    }

    @Test
    fun notificationsList() = rule.capture("16_notifications") {
        NotificationsScreen(
            uiState = NotificationsUiState(
                loading = false,
                unread = 2,
                items = listOf(
                    ng.name.servix.app.data.model.NotificationDto(
                        id = "n1", type = "booking", title = "Tunde accepted your booking",
                        body = "Your brand identity package is scheduled for 12 November.",
                        link = "/bookings/${Fixtures.BOOKING_ID}", createdAt = "2026-11-05T09:12:00.000Z",
                    ),
                    ng.name.servix.app.data.model.NotificationDto(
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
    fun planCard() = rule.capture("17_plan") {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(16.dp)) {
            ng.name.servix.app.ui.account.PlanCard(
                label = "Free",
                source = "account",
                expiresAt = null,
            )
        }
    }

    @Test
    fun securityCheckStep() = rule.capture("18_security_check_step") {
        SecurityCheckScreen(
            uiState = AuthUiState(
                securityFlow = ng.name.servix.app.data.model.SecurityFlowState(next = "factor"),
            ),
            onLoadSecurityCheck = {}, onCodeChange = {}, onSubmitCode = {}, onResendEmailCode = {},
            onBeginTotpSetup = {}, onClearTotpSetup = {}, onToggleUseRecovery = {},
            onSavedRecoveryChange = {}, onFinishSecurity = {}, onNewPasswordChange = {},
            onConfirmPasswordChange = {}, onSubmitResetPassword = {}, onCancelAndReturnLogin = {},
        )
    }

    @Test
    fun bookingDetailPendingPayment() = rule.capture("11_booking_pending_payment") {
        BookingDetail(
            BookingDetailUiState(bookingId = Fixtures.BOOKING_ID, loading = false, booking = Fixtures.booking("pending_payment")),
        )
    }

    @Test
    fun bookingDetailRequested() = rule.capture("12_booking_requested") {
        BookingDetail(
            BookingDetailUiState(bookingId = Fixtures.BOOKING_ID, loading = false, booking = Fixtures.booking("requested")),
        )
    }

    @Test
    fun bookingDetailCompleted() = rule.capture("13_booking_completed") {
        BookingDetail(
            BookingDetailUiState(
                bookingId = "44444444-4444-4444-8444-444444444444",
                loading = false,
                booking = Fixtures.booking("completed", "44444444-4444-4444-8444-444444444444", "Pitch deck design"),
            ),
        )
    }

    @Test
    fun aiChat() = rule.capture("14_ai_chat") {
        AiScreen(
            user = Fixtures.user,
            state = aiState().copy(
                turns = Fixtures.aiTurns + listOf(
                    ChatTurn(3L, "user", "Does that price include brand guidelines?"),
                    ChatTurn(
                        4L, "assistant",
                        "Yes — the **₦120,000** package includes a logo suite, colour and type system, and a short brand guidelines PDF. Revisions are included; extra deliverables are quoted separately.",
                    ),
                ),
            ),
            viewModel = AiViewModel(AiRepository(api, OkHttpClient()), CatalogueRepository { api }),
            onSignIn = {},
        )
    }

    @Test
    fun homeFontScale130() = rule.capture("03c_home_fontscale130", fontScale = 1.3f) {
        HomeTabScreen(
            user = Fixtures.user,
            city = "Lagos",
            uiState = HomeUiState(
                loading = false,
                categories = Fixtures.categories,
                professionals = Fixtures.professionals,
                overview = AccountOverviewDto(emailVerified = true),
            ),
            aiUsage = Fixtures.usage,
            onRefresh = {}, onOpenSearch = {}, onOpenCategory = {}, onOpenProfessional = {},
            onOpenBookingDetail = {}, onOpenBookingsTab = {}, onOpenAi = {}, onOpenAccount = {},
        )
    }

    @Test
    fun exploreFontScale130() = rule.capture("06b_explore_fontscale130", fontScale = 1.3f) {
        ExploreScreen(
            uiState = SearchUiState(mode = SearchMode.PROFESSIONALS, query = "logo designer"),
            categoryNameFor = { _ -> "" },
            onQueryChange = {}, onQuerySubmit = {}, onModeChange = {}, onCategorySelect = {},
            onLocationChange = {}, onRatingSelect = {}, onPriceSelect = {}, onPriceLimitChange = {},
            onAvailabilitySelect = {}, onSortSelect = {}, onClearFilters = {}, onRefresh = {},
            onLoadNextPage = {}, onOpenProfessional = {}, onOpenService = {},
        )
    }

    @Test
    fun bookingDetailFontScale130() = rule.capture("11b_booking_detail_fontscale130", fontScale = 1.3f) {
        BookingDetail(
            BookingDetailUiState(bookingId = Fixtures.BOOKING_ID, loading = false, booking = Fixtures.booking("delivered")),
        )
    }

    @Test
    fun aiFontScale130() = rule.capture("14c_ai_fontscale130", fontScale = 1.3f) {
        AiScreen(
            user = Fixtures.user,
            state = aiState(),
            viewModel = AiViewModel(AiRepository(api, OkHttpClient()), CatalogueRepository { api }),
            onSignIn = {},
        )
    }

    @Composable
    private fun BookingDetail(state: BookingDetailUiState) {
        BookingDetailScreen(
            uiState = state,
            pollingAfterPay = false,
            onResumeCheck = {}, onBackClick = {}, onRetry = {}, onRefreshPaymentStatus = {},
            onPayClick = {}, onOpenModal = {}, onCloseModal = {}, onSubmitCancel = {},
            onSubmitConfirmCompletion = {}, onSubmitDispute = {}, onSubmitReview = { _, _ -> },
            onStartBookAgain = {}, onContinueRebook = { _, _, _ -> }, onOpenProfessional = {},
            onOpenService = {},
        )
    }
}
