package ng.name.servix.app.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import kotlinx.serialization.decodeFromString
import ng.name.servix.app.ui.components.ServixButton
import ng.name.servix.app.ui.components.ServixButtonStyle
import ng.name.servix.app.ui.components.ServixField
import ng.name.servix.app.ui.components.ServixTopBar
import ng.name.servix.app.ui.components.StatusChip
import ng.name.servix.app.ui.components.StatusTone
import ng.name.servix.app.ui.components.statusLabel
import ng.name.servix.app.ui.components.statusTone
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.ServixMotion
import ng.name.servix.app.ui.theme.ServixTheme
import ng.name.servix.app.ui.theme.ServixType
import ng.name.servix.app.ui.theme.WarmIvory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Stage A of Release 1.2: the foundation layer on its own, before any screen consumes it.
 * These are the previews the prompt asks for, captured through the same harness as the screens so a
 * regression in a token shows up as a picture rather than a diff.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = PIXEL_7_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StitchFoundationTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun exploreDiscoveryCategoriesAndPriceFilter() {
        var selected = ""
        val state = ng.name.servix.app.ui.catalogue.SearchUiState(
            loading = false, categories = Fixtures.categories, professionals = Fixtures.professionals,
            total = Fixtures.professionals.size,
        )
        org.junit.Assert.assertEquals(1, state.copy(priceLimit = 25000).activeFilterCount)
        rule.setContent {
            ServixTheme {
                ng.name.servix.app.ui.explore.ExploreScreen(
                    uiState = state, categoryNameFor = { "" }, onQueryChange = {}, onQuerySubmit = {},
                    onModeChange = {}, onCategorySelect = { selected = it }, onLocationChange = {},
                    onRatingSelect = {}, onPriceSelect = {}, onPriceLimitChange = {}, onAvailabilitySelect = {},
                    onSortSelect = {}, onClearFilters = {}, onRefresh = {}, onLoadNextPage = {},
                    onOpenProfessional = {}, onOpenService = {},
                )
            }
        }
        rule.onNodeWithTag("explore_category_graphic-design").performClick()
        org.junit.Assert.assertEquals("graphic-design", selected)
    }

    @Test
    fun exploreDiscovery() = rule.capture("69_explore_discovery_stitch") {
        ng.name.servix.app.ui.explore.ExploreScreen(
            uiState = ng.name.servix.app.ui.catalogue.SearchUiState(loading = false, categories = Fixtures.categories,
                professionals = Fixtures.professionals, total = Fixtures.professionals.size),
            categoryNameFor = { "" }, onQueryChange = {}, onQuerySubmit = {}, onModeChange = {}, onCategorySelect = {},
            onLocationChange = {}, onRatingSelect = {}, onPriceSelect = {}, onPriceLimitChange = {},
            onAvailabilitySelect = {}, onSortSelect = {}, onClearFilters = {}, onRefresh = {}, onLoadNextPage = {},
            onOpenProfessional = {}, onOpenService = {},
        )
    }

    @Test
    fun exploreSearchSubmitClearAndFilterSheet() {
        val query = mutableStateOf("plumbing")
        var submissions = 0
        var clearedFilters = 0
        rule.setContent {
            ServixTheme {
                ng.name.servix.app.ui.explore.ExploreScreen(
                    uiState = ng.name.servix.app.ui.catalogue.SearchUiState(query = query.value, loading = false,
                        selectedCategory = "graphic-design", categories = Fixtures.categories, total = 0),
                    categoryNameFor = { "Graphic Design" }, onQueryChange = { query.value = it },
                    onQuerySubmit = { submissions++ }, onModeChange = {}, onCategorySelect = {},
                    onLocationChange = {}, onRatingSelect = {}, onPriceSelect = {}, onPriceLimitChange = {},
                    onAvailabilitySelect = {}, onSortSelect = {}, onClearFilters = { clearedFilters++ },
                    onRefresh = {}, onLoadNextPage = {}, onOpenProfessional = {}, onOpenService = {},
                )
            }
        }
        rule.onNode(hasSetTextAction()).performClick().performImeAction()
        rule.runOnIdle { org.junit.Assert.assertEquals(1, submissions) }
        rule.onNodeWithTag("explore_search_field_clear").performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals("", query.value); org.junit.Assert.assertEquals(2, submissions) }
        rule.onNodeWithTag("explore_filters_chip").performClick()
        rule.onNodeWithTag("explore_show_results").assertIsDisplayed()
        rule.onNodeWithTag("explore_clear_filters").performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals(1, clearedFilters) }
        rule.onNodeWithTag("explore_show_results").performClick()
        rule.onNodeWithTag("explore_clear_filters").assertDoesNotExist()
    }

    @Test
    fun exploreResultUsesRealFieldsAndOpensProfile() {
        var opened = ""
        val pro = ng.name.servix.app.data.model.ProfessionalSummaryDto(id = "pro-1", name = "Ada Example",
            title = "Technician", location = "Ibadan", rating = 4.8, reviewCount = 12,
            startingPrice = 12000, completedProjects = 7, verified = false)
        rule.setContent {
            ServixTheme {
                ng.name.servix.app.ui.explore.ExploreScreen(
                    uiState = ng.name.servix.app.ui.catalogue.SearchUiState(query = "technician", loading = false,
                        professionals = listOf(pro), total = 1), categoryNameFor = { "" },
                    onQueryChange = {}, onQuerySubmit = {}, onModeChange = {}, onCategorySelect = {},
                    onLocationChange = {}, onRatingSelect = {}, onPriceSelect = {}, onPriceLimitChange = {},
                    onAvailabilitySelect = {}, onSortSelect = {}, onClearFilters = {}, onRefresh = {}, onLoadNextPage = {},
                    onOpenProfessional = { opened = it }, onOpenService = {},
                )
            }
        }
        rule.onNodeWithText("Ada Example").assertExists()
        rule.onNodeWithText("7 jobs completed").assertExists()
        rule.onNodeWithContentDescription("Verified professional").assertDoesNotExist()
        rule.onNodeWithTag("explore_profile_pro-1").performScrollTo().performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals("pro-1", opened) }
    }

    @Test fun sessionSplash() = rule.capture("49_session_splash_stitch") {
        ng.name.servix.app.ui.auth.SessionSplashScreen()
    }

    @Test
    fun register() = rule.capture("56_register_stitch") {
        ng.name.servix.app.ui.auth.RegisterScreen(
            uiState = ng.name.servix.app.ui.auth.AuthUiState(),
            onNameChange = {}, onEmailChange = {}, onPasswordChange = {}, onConfirmPasswordChange = {},
            onSubmit = {}, onSignInClick = {}, onBackClick = {}, providers = listOf("google", "github"),
        )
    }

    @Test
    fun registerBottom() = rule.capture("56b_register_stitch_bottom", afterContent = {
        rule.onNodeWithTag("go_to_login_button").performScrollTo()
    }) {
        ng.name.servix.app.ui.auth.RegisterScreen(
            uiState = ng.name.servix.app.ui.auth.AuthUiState(),
            onNameChange = {}, onEmailChange = {}, onPasswordChange = {}, onConfirmPasswordChange = {},
            onSubmit = {}, onSignInClick = {}, onBackClick = {}, providers = listOf("google", "github"),
        )
    }

    @Test
    fun verifyEmail() = rule.capture("57_verify_email_stitch") {
        ng.name.servix.app.ui.auth.VerifyEmailScreen(
            userEmail = "olumide@example.ng", isAlreadyVerified = false,
            uiState = ng.name.servix.app.ui.auth.AuthUiState(verificationMethod = "otp", resendWaitSeconds = 45),
            onLoadMethod = {}, onOtpCodeChange = {}, onSubmitOtp = {}, onResend = {},
            onCheckVerifiedStatus = {}, onContinueToHome = {}, onSignOut = {}, onRegisterAgain = {},
        )
    }

    @Test
    fun verifyEmailRegisterAgainUsesDedicatedCallbackAndKeepsCooldown() {
        var registeredAgain = false
        var signedOut = false
        rule.setContent {
            ServixTheme {
                ng.name.servix.app.ui.auth.VerifyEmailScreen(
                    userEmail = "olumide@example.ng", isAlreadyVerified = false,
                    uiState = ng.name.servix.app.ui.auth.AuthUiState(verificationMethod = "otp", resendWaitSeconds = 45),
                    onLoadMethod = {}, onOtpCodeChange = {}, onSubmitOtp = {}, onResend = {},
                    onCheckVerifiedStatus = {}, onContinueToHome = {}, onSignOut = { signedOut = true },
                    onRegisterAgain = { registeredAgain = true },
                )
            }
        }
        rule.onNodeWithTag("otp_resend_button").assertIsNotEnabled()
        rule.onNodeWithTag("verify_register_again_button").performClick()
        rule.runOnIdle {
            org.junit.Assert.assertTrue(registeredAgain)
            org.junit.Assert.assertFalse(signedOut)
        }
    }

    @Test fun customerHome() = rule.capture("68_home_customer_stitch") { customerHomePreview() }

    @Test fun customerHomeBottom() = rule.capture("68b_home_customer_bottom", afterContent = {
        rule.onNodeWithText("Manage plan").performScrollTo()
    }) { customerHomePreview() }

    @Test fun customerHomeRetainsBookingAndChatActions() {
        var opened = ""
        rule.setContent { ServixTheme { customerHomePreview(onBooking = { opened = it }, onChats = { opened = "chats" }) } }
        rule.onNodeWithText("Confirm").performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals("delivered", opened) }
        rule.onNodeWithTag("home_chats_card").performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals("chats", opened) }
    }

    @Test fun customerHomeHidesChatsWhenFeatureOff() {
        rule.setContent { ServixTheme { customerHomePreview(chatsEnabled = false) } }
        rule.onNodeWithTag("home_chats_card").assertDoesNotExist()
    }

    @androidx.compose.runtime.Composable
    private fun customerHomePreview(chatsEnabled: Boolean = true, onBooking: (String) -> Unit = {}, onChats: () -> Unit = {}) {
        ng.name.servix.app.ui.home.HomeTabScreen(
            user = ng.name.servix.app.data.model.UserDto(id = "customer", fullName = "Olumide Adeleke"), city = "Lagos",
            uiState = ng.name.servix.app.ui.home.HomeUiState(loading = false,
                bookings = listOf(
                    ng.name.servix.app.data.model.BookingDto(id = "delivered", status = "delivered", serviceTitle = "Electrical wiring repair"),
                    ng.name.servix.app.data.model.BookingDto(id = "active", status = "in_progress", serviceTitle = "Plumbing pipe maintenance", professionalName = "Tunde Adeleke", scheduledAt = "2026-11-09T15:00:00Z")
                ),
                categories = listOf(ng.name.servix.app.data.model.CategoryDto(id = "electric", name = "Electrical", icon = "bolt"),
                    ng.name.servix.app.data.model.CategoryDto(id = "plumbing", name = "Plumbing", icon = "plumbing")),
                professionals = listOf(ng.name.servix.app.data.model.ProfessionalSummaryDto(id = "emeka", name = "Emeka Nwosu", title = "AC & Fridge technician", rating = 4.9, reviewCount = 128, startingPrice = 15000, verified = true))
            ),
            chatsEnabled = chatsEnabled, chatsUnread = 2, unreadNotifications = 1,
            onOpenBookingDetail = onBooking, onOpenChats = onChats,
        )
    }

    @Test fun guestHome() = rule.capture("67_home_guest_stitch") { guestHomePreview() }

    @Test fun guestHomeBottom() = rule.capture("67b_home_guest_bottom", afterContent = {
        rule.onNodeWithText("Ask Servix AI").performScrollTo()
    }) { guestHomePreview() }

    @Test fun guestHomeActions() {
        var opened = ""
        rule.setContent { ServixTheme { guestHomePreview(onSearch = { opened = "search" }, onCreate = { opened = "register" }) } }
        rule.onNodeWithTag("home_search_field").performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals("search", opened) }
        rule.onNodeWithTag("home_create_account_button").performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals("register", opened) }
        rule.onNodeWithText("Chats").assertDoesNotExist()
    }

    @androidx.compose.runtime.Composable
    private fun guestHomePreview(onSearch: () -> Unit = {}, onCreate: () -> Unit = {}) {
        ng.name.servix.app.ui.home.GuestHomeScreen(city = "Lagos", state = ng.name.servix.app.ui.home.HomeUiState(
            loading = false,
            categories = listOf(ng.name.servix.app.data.model.CategoryDto(id = "electrical", name = "Electrical", icon = "bolt"),
                ng.name.servix.app.data.model.CategoryDto(id = "plumbing", name = "Plumbing", icon = "plumbing")),
            professionals = listOf(ng.name.servix.app.data.model.ProfessionalSummaryDto(id = "emeka", name = "Emeka Nwosu",
                title = "AC & Fridge technician", rating = 4.9, reviewCount = 128, location = "Ikeja", startingPrice = 15000, verified = true))),
            onRefresh = {}, onSearch = onSearch, onCategory = {}, onProfessional = {}, onSignIn = {}, onCreateAccount = onCreate, onAi = {})
    }

    @Test fun professionalHome() = rule.capture("66_home_professional_stitch") { professionalHomePreview() }

    @Test fun professionalHomeBottom() = rule.capture("66b_home_professional_bottom", afterContent = {
        rule.onNodeWithText("Ask Servix AI").performScrollTo()
    }) { professionalHomePreview() }

    @Test fun professionalHomeHonoursActionsAndChatFlag() {
        var action = ""
        rule.setContent { ServixTheme { professionalHomePreview(chats = false, onAction = { _, value -> action = value }) } }
        rule.onNodeWithText("Message").assertDoesNotExist()
        rule.onNodeWithTag("professional_job_action_request").performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals("accept", action) }
        rule.onNodeWithText("Decline").performClick()
        rule.onNodeWithText("Decline request").performClick()
        rule.runOnIdle { org.junit.Assert.assertEquals("decline", action) }
    }

    @Test fun professionalWorkspaceSchemasDecode() {
        val json = ng.name.servix.app.data.network.ApiErrorParser.json
        val analytics = json.decodeFromString<ng.name.servix.app.data.model.ProfessionalAnalyticsDto>(
            """{"totals":{"earnings":86000.5,"completed":4,"responseRate":null,"ratingAvg":4.8,"reviewCount":5}}"""
        )
        org.junit.Assert.assertEquals(86000.5, analytics.totals!!.earnings!!, 0.001)
        org.junit.Assert.assertNull(analytics.totals!!.responseRate)
        val services = json.decodeFromString<List<ng.name.servix.app.data.model.OwnServiceDto>>(
            """[{"id":"one","title":"Repairs","status":"draft","publishProblems":{"gallery":"Add an image."}},{"id":"two","title":"Done","status":"active","publishProblems":{}}]"""
        )
        org.junit.Assert.assertEquals("Add an image.", services[0].publishProblems["gallery"])
        org.junit.Assert.assertTrue(services[1].publishProblems.isEmpty())
    }

    @Test fun professionalHomeHidesCompleteServiceAndShowsRetry() {
        var retried = false
        rule.setContent { ServixTheme {
            ng.name.servix.app.ui.home.ProfessionalHomeScreen(
                user = ng.name.servix.app.data.model.UserDto(id = "pro", role = "professional"),
                state = ng.name.servix.app.ui.home.ProfessionalHomeState(userId = "pro", loading = false,
                    services = listOf(ng.name.servix.app.data.model.OwnServiceDto(id = "complete", status = "active")),
                    jobsError = "Could not load jobs"),
                unreadNotifications = 0, chatsEnabled = false, onNotifications = {}, onAccount = {}, onSeeAllJobs = {},
                onViewJob = {}, onJobAction = { _, _ -> }, onMessage = {}, onEditService = {}, onAi = {}, onRetry = { retried = true },
            )
        } }
        rule.onNodeWithText("Finish your service").assertDoesNotExist()
        rule.onNodeWithText("Could not load jobs").assertExists()
        rule.onNodeWithText("Try again").performClick()
        rule.runOnIdle { org.junit.Assert.assertTrue(retried) }
    }

    @Test fun professionalActionStateMachine() {
        val allowed: (String, String) -> Boolean = { status, action -> ng.name.servix.app.ui.home.professionalActionAllowed(status, action) }
        org.junit.Assert.assertTrue(allowed("requested", "accept"))
        org.junit.Assert.assertTrue(allowed("requested", "decline"))
        org.junit.Assert.assertTrue(allowed("accepted", "start"))
        org.junit.Assert.assertTrue(allowed("in_progress", "deliver"))
        org.junit.Assert.assertFalse(allowed("delivered", "deliver"))
        org.junit.Assert.assertFalse(allowed("accepted", "decline"))
    }

    @androidx.compose.runtime.Composable
    private fun professionalHomePreview(chats: Boolean = true, onAction: (String, String) -> Unit = { _, _ -> }) {
        ng.name.servix.app.ui.home.ProfessionalHomeScreen(
            user = ng.name.servix.app.data.model.UserDto(id = "pro", fullName = "Bayo Adebayo", role = "professional"),
            state = ng.name.servix.app.ui.home.ProfessionalHomeState(
                userId = "pro", loading = false,
                jobs = listOf(
                    ng.name.servix.app.data.model.BookingDto(id = "request", status = "requested", serviceTitle = "Electrical wiring repair", customerName = "Olumide A.", scheduledAt = "2026-11-09T10:30:00.000Z"),
                    ng.name.servix.app.data.model.BookingDto(id = "progress", status = "in_progress", serviceTitle = "Distribution box fix", customerName = "Chinwe O."),
                    ng.name.servix.app.data.model.BookingDto(id = "delivered", status = "delivered", serviceTitle = "Socket replacement", customerName = "Tolu B."),
                ),
                totals = ng.name.servix.app.data.model.ProfessionalAnalyticsTotalsDto(earnings = 86000.0, completed = 4, ratingAvg = 4.8, reviewCount = 5, responseRate = 90),
                services = listOf(ng.name.servix.app.data.model.OwnServiceDto(id = "service", title = "Electrical repairs", status = "draft", publishProblems = mapOf("gallery" to "Add at least one image to the gallery."))),
            ), unreadNotifications = 1, chatsEnabled = chats,
            onNotifications = {}, onAccount = {}, onSeeAllJobs = {}, onViewJob = {}, onJobAction = onAction,
            onMessage = {}, onEditService = {}, onAi = {}, onRetry = {},
        )
    }

    @Test fun securityEmail() = rule.capture("58_security_email") { securityPreview("email") }
    @Test fun securityEnroll() = rule.capture("59_security_enroll") { securityPreview("enroll") }
    @Test fun securityFactor() = rule.capture("60_security_factor") { securityPreview("factor") }
    @Test fun securityRecovery() = rule.capture("61_security_recovery") { securityPreview("recovery") }
    @Test fun securityReset() = rule.capture("62_security_reset") { securityPreview("reset_password") }
    @Test fun securityFinish() = rule.capture("63_security_finish") { securityPreview("finish") }

    @Test fun securityFactorKeepsRecoveryToggleAndPasskeyCopy() {
        var toggled = false
        rule.setContent {
            ServixTheme {
                securityPreview("factor", method = "passkey", onToggle = { toggled = true })
            }
        }
        rule.onNodeWithText("Use your authenticator app or a recovery code").assertExists()
        rule.onNodeWithTag("security_recovery_toggle").performClick()
        rule.runOnIdle { org.junit.Assert.assertTrue(toggled) }
    }

    @androidx.compose.runtime.Composable
    private fun securityPreview(stage: String, method: String? = null, onToggle: () -> Unit = {}) {
        val state = ng.name.servix.app.ui.auth.AuthUiState(
            securityFlow = ng.name.servix.app.data.model.SecurityFlowState(next = stage, email = "test@example.ng", method = method),
            securityTotpSetup = if (stage == "enroll") ng.name.servix.app.data.model.SecurityTotpSetupResponse(secret = "EXAMPLE-KEY", uri = "otpauth://totp/example") else null,
            securityRecoveryCodes = if (stage == "recovery") listOf("TEST-1111", "TEST-2222", "TEST-3333", "TEST-4444") else emptyList(),
        )
        ng.name.servix.app.ui.auth.SecurityCheckScreen(
            uiState = state, onLoadSecurityCheck = {}, onCodeChange = {}, onSubmitCode = {}, onResendEmailCode = {},
            onBeginTotpSetup = {}, onClearTotpSetup = {}, onToggleUseRecovery = onToggle, onSavedRecoveryChange = {},
            onFinishSecurity = {}, onNewPasswordChange = {}, onConfirmPasswordChange = {}, onSubmitResetPassword = {}, onCancelAndReturnLogin = {},
        )
    }

    @Test fun forgotPassword() = rule.capture("64_forgot_password_stitch") {
        ng.name.servix.app.ui.auth.ForgotPasswordScreen(
            uiState = ng.name.servix.app.ui.auth.AuthUiState(), onEmailChange = {}, onSubmit = {}, onBackToSignIn = {},
        )
    }

    @Test fun forgotPasswordSent() = rule.capture("65_forgot_password_sent") {
        ng.name.servix.app.ui.auth.ForgotPasswordScreen(
            uiState = ng.name.servix.app.ui.auth.AuthUiState(forgotEmail = "test@example.ng", forgotSent = true),
            onEmailChange = {}, onSubmit = {}, onBackToSignIn = {},
        )
    }

    @Test
    fun buttons() = rule.capture("50_foundation_buttons") {
        Column(
            modifier = Modifier.fillMaxSize().background(WarmIvory).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("Buttons", style = ServixType.headlineMd, color = DeepForest)
            Text("Primary · 52 dp · radius 20 · press 0.96", style = ServixType.bodySm, color = MutedGray)
            ServixButton(text = "Sign in", onClick = {}, modifier = Modifier.fillMaxWidth(), trailingIcon = Icons.Outlined.Lock)
            ServixButton(text = "Continue with Google", onClick = {}, modifier = Modifier.fillMaxWidth(), style = ServixButtonStyle.Secondary)
            ServixButton(text = "Forgot password?", onClick = {}, modifier = Modifier.fillMaxWidth(), style = ServixButtonStyle.Text)
            HorizontalDivider(color = BorderSand)
            Text("States", style = ServixType.labelMd, color = MutedGray)
            ServixButton(text = "Confirm completion", onClick = {}, modifier = Modifier.fillMaxWidth(), loading = true)
            ServixButton(text = "Not available", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
            ServixButton(text = "Secondary disabled", onClick = {}, modifier = Modifier.fillMaxWidth(), style = ServixButtonStyle.Secondary, enabled = false)
            Text(
                text = "Content easing ${ServixMotion.CONTENT_MS} ms · press scale ${ServixMotion.PRESS_SCALE}",
                style = ServixType.bodySm,
                color = MutedGray,
            )
        }
    }

    @Test
    fun fields() = rule.capture("51_foundation_fields") {
        Column(modifier = Modifier.fillMaxSize().background(WarmIvory)) {
            ServixTopBar(title = "Sign in", back = {})
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ServixField(
                    value = "",
                    onValueChange = {},
                    label = "Email",
                    placeholder = "olumide@example.ng",
                    leadingIcon = Icons.Outlined.Email,
                )
                ServixField(
                    value = "olumide@example.ng",
                    onValueChange = {},
                    label = "Email",
                    leadingIcon = Icons.Outlined.Email,
                )
                ServixField(
                    value = "short",
                    onValueChange = {},
                    label = "Password",
                    leadingIcon = Icons.Outlined.Lock,
                    error = "Use at least 8 characters",
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailing = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Outlined.Visibility, contentDescription = "Show password", tint = MutedGray)
                        }
                    },
                )
                ServixField(
                    value = "",
                    onValueChange = {},
                    label = "Recovery code",
                    placeholder = "XXXX-XXXX",
                    supporting = "Find this in your authenticator app.",
                )
            }
        }
    }

    @Test
    fun chips() = rule.capture("52_foundation_chips") {
        val statuses = listOf(
            "pending_payment", "requested", "accepted", "in_progress", "delivered",
            "completed", "disputed", "cancelled", "declined", "refunded",
        )
        Column(
            modifier = Modifier.fillMaxSize().background(WarmIvory).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Booking status chips", style = ServixType.headlineMd, color = DeepForest)
            Text("Tones from s13b — warn, info, ok, bad, neutral", style = ServixType.bodySm, color = MutedGray)
            statuses.forEach { status ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(status, style = ServixType.bodyMd, color = Charcoal)
                    StatusChip(text = statusLabel(status), tone = statusTone(status))
                }
            }
            HorizontalDivider(color = BorderSand)
            Text("Explicit tones", style = ServixType.labelMd, color = MutedGray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusTone.values().forEach { tone -> StatusChip(text = tone.name, tone = tone) }
            }
        }
    }

    @Test
    fun typeScale() = rule.capture("53_foundation_type") {
        Column(
            modifier = Modifier.fillMaxSize().background(WarmIvory).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Display 40 / 48", style = ServixType.displayLg, color = DeepForest)
            Text("Headline 32 / 40", style = ServixType.headlineLg, color = DeepForest)
            Text("Headline mobile 26 / 34", style = ServixType.headlineLgMobile, color = DeepForest)
            Text("Headline 22 / 28", style = ServixType.headlineMd, color = DeepForest)
            Text("Title 18 / 24", style = ServixType.titleLg, color = Charcoal)
            Text("Title 16 / 22", style = ServixType.titleMd, color = Charcoal)
            Text("Body 16 / 24 — the quick brown fox jumps over the lazy dog.", style = ServixType.bodyLg, color = Charcoal)
            Text("Body 14 / 20 — the quick brown fox jumps over the lazy dog.", style = ServixType.bodyMd, color = Charcoal)
            Text("Body 12 / 16 — the quick brown fox jumps over the lazy dog.", style = ServixType.bodySm, color = MutedGray)
            Text("LABEL 14 / 20", style = ServixType.labelLg, color = Charcoal)
            Text("LABEL 12 / 16", style = ServixType.labelMd, color = MutedGray)
            Text("₦350,000", style = ServixType.currencyDisplay, color = DeepForest)
        }
    }
}
