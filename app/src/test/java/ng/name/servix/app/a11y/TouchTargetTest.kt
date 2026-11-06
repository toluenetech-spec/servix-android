package ng.name.servix.app.a11y

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.Json
import ng.name.servix.app.data.model.AiUsage
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.ServixApiService
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.data.repository.AiRepository
import ng.name.servix.app.data.repository.CatalogueRepository
import ng.name.servix.app.screenshots.PIXEL_7_QUALIFIERS
import ng.name.servix.app.ui.account.AccountHubScreen
import ng.name.servix.app.ui.ai.AiScreen
import ng.name.servix.app.ui.ai.AiUiState
import ng.name.servix.app.ui.ai.AiViewModel
import ng.name.servix.app.ui.auth.AuthUiState
import ng.name.servix.app.ui.auth.LoginScreen
import ng.name.servix.app.ui.auth.WelcomeScreen
import ng.name.servix.app.ui.bookings.BookingDetailScreen
import ng.name.servix.app.ui.bookings.BookingDetailUiState
import ng.name.servix.app.ui.bookings.BookingsScreen
import ng.name.servix.app.ui.bookings.BookingsUiState
import ng.name.servix.app.ui.catalogue.SearchMode
import ng.name.servix.app.ui.catalogue.SearchUiState
import ng.name.servix.app.ui.explore.ExploreScreen
import ng.name.servix.app.ui.notifications.NotificationsScreen
import ng.name.servix.app.ui.notifications.NotificationsUiState
import ng.name.servix.app.ui.theme.ServixTheme
import ng.name.servix.app.updates.UpdateUiState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule

/**
 * Walks the semantics tree of every captured screen and fails when a clickable node is smaller than
 * the 48 dp minimum touch target or has no accessible label (text / contentDescription).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PIXEL_7_QUALIFIERS)
class TouchTargetTest {
    @get:Rule
    val rule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
        createAndroidComposeRule()

    private val api: ServixApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://127.0.0.1:9/")
            .addConverterFactory(ApiErrorParser.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ServixApiService::class.java)
    }

    private val user = UserDto(
        id = "11111111-1111-4111-8111-111111111111",
        email = "adaeze@example.com",
        fullName = "Adaeze Okonkwo",
        emailVerified = true,
        role = "customer",
    )

    private fun booking(status: String) = BookingDto(
        id = "22222222-2222-4222-8222-222222222222",
        reference = "SVX-4712",
        status = status,
        scheduledAt = "2026-11-12T10:00:00.000Z",
        amount = 350000,
        serviceTitle = "Complete Brand Identity Package",
        priceUnit = "per project",
        serviceId = "brand-identity-package",
        professionalId = "tunde-bakare",
        professionalName = "Tunde Bakare",
        customerName = "Adaeze Okonkwo",
        notes = "I need the identity before a trade fair in December.",
        createdAt = "2026-10-28T09:15:00.000Z",
    )

    private fun audit(name: String, content: @Composable () -> Unit) {
        rule.activity.setContent { ServixTheme { content() } }
        rule.waitForIdle()
        val density = rule.density.density
        val minPx = 48f * density
        val problems = mutableListOf<String>()

        fun walk(node: SemanticsNode) {
            val semantics = node.config
            val clickable = semantics.contains(SemanticsActions.OnClick) ||
                semantics.contains(SemanticsActions.OnLongClick)
            if (clickable) {
                val bounds = node.boundsInRoot
                val label = semantics.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ").orEmpty() +
                    semantics.getOrNull(SemanticsProperties.Text)?.joinToString(" ").orEmpty()
                if (bounds.width + 0.5f < minPx || bounds.height + 0.5f < minPx) {
                    problems += "small target ${bounds.width.toInt()}x${bounds.height.toInt()}px [$label]"
                }
                if (label.isBlank()) {
                    problems += "unlabelled clickable node at ${bounds.left.toInt()},${bounds.top.toInt()}"
                }
            }
            node.children.forEach(::walk)
        }

        walk(rule.onRoot().fetchSemanticsNode())
        assertTrue("$name:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun welcome() = audit("welcome") {
        WelcomeScreen(onSignInClick = {}, onCreateAccountClick = {}, onBrowseFirstClick = {}, providers = listOf("google", "github"))
    }

    @Test
    fun signIn() = audit("sign_in") {
        LoginScreen(
            uiState = AuthUiState(loginEmail = "adaeze@example.com", loginPassword = "secret-pass"),
            onEmailChange = {}, onPasswordChange = {}, onSubmit = {},
            onForgotPasswordClick = {}, onCreateAccountClick = {}, onBackClick = {},
            providers = listOf("google"),
        )
    }

    @Test
    fun explore() = audit("explore") {
        ExploreScreen(
            uiState = SearchUiState(mode = SearchMode.PROFESSIONALS, query = "logo designer"),
            categoryNameFor = { null },
            onQueryChange = {}, onQuerySubmit = {}, onModeChange = {}, onCategorySelect = {},
            onLocationChange = {}, onRatingSelect = {}, onPriceSelect = {}, onPriceLimitChange = {},
            onAvailabilitySelect = {}, onSortSelect = {}, onClearFilters = {}, onRefresh = {},
            onLoadNextPage = {}, onOpenProfessional = {}, onOpenService = {},
        )
    }

    @Test
    fun bookings() = audit("bookings") {
        BookingsScreen(
            user = user,
            uiState = BookingsUiState(loading = false, bookings = listOf(booking("requested"), booking("completed"))),
            onTabSelect = {}, onRefresh = {}, onExploreServicesClick = {}, onSignInClick = {}, onOpenBooking = {},
        )
    }

    @Test
    fun bookingDetail() = audit("booking_detail") {
        BookingDetailScreen(
            uiState = BookingDetailUiState(loading = false, booking = booking("delivered")),
            pollingAfterPay = false,
            onResumeCheck = {}, onBackClick = {}, onRetry = {}, onRefreshPaymentStatus = {},
            onPayClick = {}, onOpenModal = {}, onCloseModal = {}, onSubmitCancel = {},
            onSubmitConfirmCompletion = {}, onSubmitDispute = {}, onSubmitReview = { _, _ -> },
            onStartBookAgain = {}, onContinueRebook = { _, _, _ -> }, onOpenProfessional = {}, onOpenService = {},
        )
    }

    @Test
    fun account() = audit("account") {
        AccountHubScreen(
            user = user,
            accountRepository = AccountRepository { api },
            updateState = UpdateUiState(
                context = androidx.test.core.app.ApplicationProvider.getApplicationContext(),
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            ),
            city = "Lagos",
            unreadNotifications = 2,
        )
    }

    @Test
    fun notifications() = audit("notifications") {
        NotificationsScreen(
            uiState = NotificationsUiState(loading = false, unread = 1, items = emptyList()),
            onBackClick = {}, onRefresh = {}, onMarkAllRead = {}, onItemClick = {},
        )
    }

    @Test
    fun ai() = audit("ai") {
        AiScreen(
            user = user,
            state = AiUiState(
                ownerId = user.id,
                usage = AiUsage(planLabel = "Free", subject = "user", used = 2300, allowed = 20000, percent = 11.5, enabled = true),
            ),
            viewModel = AiViewModel(AiRepository(api, OkHttpClient()), CatalogueRepository { api }),
            onSignIn = {},
        )
    }
}
