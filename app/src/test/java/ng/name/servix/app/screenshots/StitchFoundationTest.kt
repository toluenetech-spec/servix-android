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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.unit.dp
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
    fun register() = rule.capture("56_register_stitch") {
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
