package ng.name.servix.app.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.ui.account.AccountHubScreen
import ng.name.servix.app.ui.auth.AuthUiState
import ng.name.servix.app.ui.auth.LoginScreen
import ng.name.servix.app.ui.auth.RegisterScreen
import ng.name.servix.app.ui.catalogue.SearchUiState
import ng.name.servix.app.ui.chat.ConversationScreen
import ng.name.servix.app.ui.chat.ConversationUiState
import ng.name.servix.app.ui.explore.ExploreScreen
import ng.name.servix.app.ui.navigation.ServixInsetScaffold
import ng.name.servix.app.ui.theme.ServixTheme
import ng.name.servix.app.updates.UpdateUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real screen composables in the production shell, with a deterministic 300 dp IME inset. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = PIXEL_7_QUALIFIERS, instrumentedPackages = ["androidx.loader.content"])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImeInsetsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val imeOpen = mutableStateOf(false)

    @Composable
    private fun Fixture(withBar: Boolean, screen: @Composable () -> Unit) {
        val ime = imeOpen.value
        val bottomPx = with(LocalDensity.current) { if (ime) 300.dp.roundToPx() else 0 }
        Box(Modifier.fillMaxSize()) {
            ServixInsetScaffold(
                contentInsets = WindowInsets(left = 0, top = 0, right = 0, bottom = bottomPx),
                imeVisible = ime,
                bottomBar = {
                    if (withBar) Box(Modifier.fillMaxWidth().height(64.dp).testTag("test_bottom_bar"))
                },
            ) {
                Box(Modifier.fillMaxSize().testTag("test_shell_content")) { screen() }
            }
            if (ime) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(300.dp)
                .background(Color.DarkGray).testTag("fake_ime")) { Text("Keyboard", color = Color.White) }
        }
    }

    private fun showImeAndCheck(fieldTag: String, withBar: Boolean, isComposer: Boolean = false) {
        rule.onNodeWithTag(fieldTag).performClick().assertIsFocused()
        val closedContent = rule.onNodeWithTag("test_shell_content").getUnclippedBoundsInRoot()
        val closedField = rule.onNodeWithTag(fieldTag).getUnclippedBoundsInRoot()
        if (withBar) rule.onNodeWithTag("test_bottom_bar").assertExists()
        rule.runOnIdle { imeOpen.value = true }
        rule.waitForIdle()
        val keyboard = rule.onNodeWithTag("fake_ime").getUnclippedBoundsInRoot()
        val content = rule.onNodeWithTag("test_shell_content").getUnclippedBoundsInRoot()
        val field = rule.onNodeWithTag(fieldTag).getUnclippedBoundsInRoot()
        rule.onNodeWithTag(fieldTag).assertIsDisplayed().assertIsFocused()
        rule.onNodeWithTag("test_bottom_bar").assertDoesNotExist()
        assertEquals("Keyboard must be 300 dp", 300f, (keyboard.bottom - keyboard.top).value, 1f)
        assertEquals("No extra inset or blank band above the keyboard", keyboard.top.value, content.bottom.value, 1f)
        assertTrue("Focused field must be above the keyboard: $field vs $keyboard", field.bottom <= keyboard.top)
        if (isComposer) assertTrue("Composer must sit directly above keyboard", (keyboard.top - field.bottom) <= 16.dp)
        rule.runOnIdle { imeOpen.value = false }
        rule.waitForIdle()
        val restoredContent = rule.onNodeWithTag("test_shell_content").getUnclippedBoundsInRoot()
        assertEquals("Content height must restore when keyboard closes", closedContent.bottom.value, restoredContent.bottom.value, 1f)
        if (withBar) rule.onNodeWithTag("test_bottom_bar").assertExists()
        if (isComposer) {
            val restoredField = rule.onNodeWithTag(fieldTag).getUnclippedBoundsInRoot()
            assertEquals("Composer must restore when keyboard closes", closedField.bottom.value, restoredField.bottom.value, 1f)
        }
    }

    @Test fun login() {
        rule.setContent { ServixTheme { Fixture(withBar = false) {
            LoginScreen(uiState = AuthUiState(), onEmailChange = {}, onPasswordChange = {}, onSubmit = {},
                onForgotPasswordClick = {}, onCreateAccountClick = {}, onBackClick = {})
        } } }
        showImeAndCheck("login_password_input", withBar = false)
    }

    @Test fun register() {
        rule.setContent { ServixTheme { Fixture(withBar = false) {
            RegisterScreen(uiState = AuthUiState(), onNameChange = {}, onEmailChange = {}, onPasswordChange = {},
                onConfirmPasswordChange = {}, onSubmit = {}, onSignInClick = {}, onBackClick = {})
        } } }
        rule.onNodeWithTag("register_confirm_password_input").performScrollTo()
        showImeAndCheck("register_confirm_password_input", withBar = false)
    }

    @Test fun conversation() {
        rule.setContent { ServixTheme { Fixture(withBar = true) {
            ConversationScreen(uiState = ConversationUiState(loading = false), thread = null,
                onBack = {}, onInput = {}, onSend = {}, onRetry = {}, onLoadOlder = {}, onOpenContactCard = {})
        } } }
        showImeAndCheck("composer_input", withBar = true, isComposer = true)
    }

    @Test fun explore() {
        rule.setContent { ServixTheme { Fixture(withBar = true) {
            ExploreScreen(uiState = SearchUiState(loading = false), categoryNameFor = { "" },
                onQueryChange = {}, onQuerySubmit = {}, onModeChange = {}, onCategorySelect = {},
                onLocationChange = {}, onRatingSelect = {}, onPriceSelect = {}, onPriceLimitChange = {},
                onAvailabilitySelect = {}, onSortSelect = {}, onClearFilters = {}, onRefresh = {}, onLoadNextPage = {},
                onOpenProfessional = {}, onOpenService = {})
        } } }
        // The search tag belongs to a container around the editable field.
        rule.onNode(hasSetTextAction()).performClick().assertIsFocused()
        val before = rule.onNodeWithTag("test_shell_content").getUnclippedBoundsInRoot()
        rule.runOnIdle { imeOpen.value = true }
        rule.waitForIdle()
        val keyboard = rule.onNodeWithTag("fake_ime").getUnclippedBoundsInRoot()
        val content = rule.onNodeWithTag("test_shell_content").getUnclippedBoundsInRoot()
        val search = rule.onNodeWithTag("explore_search_field").getUnclippedBoundsInRoot()
        rule.onNode(hasSetTextAction()).assertIsFocused().assertIsDisplayed()
        rule.onNodeWithTag("test_bottom_bar").assertDoesNotExist()
        assertEquals(keyboard.top.value, content.bottom.value, 1f)
        assertTrue(search.bottom <= keyboard.top)
        rule.runOnIdle { imeOpen.value = false }
        rule.waitForIdle()
        assertEquals(before.bottom.value, rule.onNodeWithTag("test_shell_content").getUnclippedBoundsInRoot().bottom.value, 1f)
        rule.onNodeWithTag("test_bottom_bar").assertExists()
    }

    @Test fun accountProfile() {
        val repository = AccountRepository { error("Unexpected network request") }
        val update = UpdateUiState(context = ApplicationProvider.getApplicationContext(), scope = CoroutineScope(Dispatchers.Unconfined))
        rule.setContent { ServixTheme { Fixture(withBar = true) {
            AccountHubScreen(user = UserDto(id = "user", fullName = "Test User"), accountRepository = repository,
                updateState = update, city = null)
        } } }
        rule.onNodeWithTag("account_row_profile").performClick()
        rule.onNodeWithTag("profile_full_name").performScrollTo()
        showImeAndCheck("profile_full_name", withBar = true)
    }
}
