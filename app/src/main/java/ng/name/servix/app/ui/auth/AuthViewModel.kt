package ng.name.servix.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.data.local.TermsAcceptanceStore
import ng.name.servix.app.data.model.SecurityFlowState
import ng.name.servix.app.data.model.SecurityTotpSetupResponse
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.repository.AuthOutcome
import ng.name.servix.app.data.repository.AuthRepository
import ng.name.servix.app.data.repository.SessionState

enum class PostAuthDestination {
    DASHBOARD,
    VERIFY_EMAIL,
    SECURITY_CHECK,
}

data class PasswordRuleStatus(
    val id: String,
    val label: String,
    val met: Boolean,
)

data class AuthUiState(
    // Login form
    val loginEmail: String = "",
    val loginPassword: String = "",
    val loginEmailError: String? = null,
    val loginPasswordError: String? = null,
    val loginGeneralError: String? = null,
    val loginSubmitting: Boolean = false,

    // Register form
    val registerFullName: String = "",
    val registerEmail: String = "",
    val registerPassword: String = "",
    val registerConfirmPassword: String = "",
    val registerTermsAccepted: Boolean = false,
    val registerNameError: String? = null,
    val registerEmailError: String? = null,
    val registerPasswordError: String? = null,
    val registerConfirmError: String? = null,
    val registerGeneralError: String? = null,
    val registerSubmitting: Boolean = false,

    // Email verification (GET /auth/verification-method -> "otp" | "link")
    val verificationMethod: String? = null,
    val verificationMethodError: Boolean = false,
    val otpCode: String = "",
    val otpError: String? = null,
    val otpNotice: String? = null,
    val otpBusy: Boolean = false,
    val resendWaitSeconds: Int = 0,
    val emailConfirmed: Boolean = false,

    // Forgot password
    val forgotEmail: String = "",
    val forgotError: String? = null,
    val forgotSubmitting: Boolean = false,
    val forgotSent: Boolean = false,

    // Provider sign-in (Google / GitHub) via a Custom Tab hand-off
    val oauthProviders: List<String> = emptyList(),
    val oauthBusy: Boolean = false,
    val oauthNotice: String? = null,

    // Security / MFA flow (GET /auth/security/config & POST /auth/security/<action>)
    val securityConfigEnabled: Boolean? = null,
    val securityFlow: SecurityFlowState? = null,
    val securityLoading: Boolean = false,
    val securityLoadFailed: Boolean = false,
    val securityExpired: Boolean = false,
    val securityBusy: Boolean = false,
    val securityError: String? = null,
    val securityNotice: String? = null,
    val securityCode: String = "",
    val securityTotpSetup: SecurityTotpSetupResponse? = null,
    val securityRecoveryCodes: List<String> = emptyList(),
    val securitySavedRecovery: Boolean = false,
    val securityUseRecovery: Boolean = false,
    val securityWaitSeconds: Int = 0,
    val securityNewPassword: String = "",
    val securityConfirmPassword: String = "",
    val securitySuccessMode: String? = null, // "reset" | "session" | null
) {
    val passwordRules: List<PasswordRuleStatus>
        get() = evaluatePasswordRules(registerPassword, policy = null).filterNot { it.id == "maximum" }

    val passwordsMatch: Boolean
        get() = registerPassword.isNotEmpty() && registerPassword == registerConfirmPassword

    val canSubmitRegistration: Boolean
        get() = registerTermsAccepted && !registerSubmitting && passwordRules.all { it.met } &&
            passwordsMatch && registerPassword.length <= 200

    val securityResetPasswordRules: List<PasswordRuleStatus>
        get() = evaluatePasswordRules(securityNewPassword, policy = securityFlow?.passwordPolicy)

    companion object {
        fun evaluatePasswordRules(password: String, policy: String?): List<PasswordRuleStatus> {
            val all = listOf(
                PasswordRuleStatus("length", "At least 8 characters", password.length >= 8),
                PasswordRuleStatus("uppercase", "One uppercase letter", password.any { it.isUpperCase() }),
                PasswordRuleStatus("lowercase", "One lowercase letter", password.any { it.isLowerCase() }),
                PasswordRuleStatus("number", "One number", password.any { it.isDigit() }),
                PasswordRuleStatus("symbol", "One symbol", password.any { !it.isLetterOrDigit() && !it.isWhitespace() }),
                PasswordRuleStatus("maximum", "No more than 200 characters", password.isNotEmpty() && password.length <= 200),
            )
            return if (policy == "basic") {
                all.filter { it.id == "length" || it.id == "maximum" }
            } else {
                all
            }
        }
    }
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val termsAcceptanceStore: TermsAcceptanceStore? = null,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = authRepository.sessionState

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null
    private var securityCooldownJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.restoreSession()
        }
    }

    // ---- Provider sign-in (GET /auth/oauth/config, POST /auth/oauth/native/exchange) ----

    private var oauthProvidersLoaded = false

    fun loadOAuthProviders() {
        if (oauthProvidersLoaded) return
        oauthProvidersLoaded = true
        viewModelScope.launch {
            authRepository.oauthProviders().onSuccess { providers ->
                _uiState.update { it.copy(oauthProviders = providers) }
            }
        }
    }

    fun clearOAuthNotice() {
        _uiState.update { it.copy(oauthNotice = null) }
    }

    /**
     * Handles the Custom Tab return: `?ticket=` is exchanged for the normal security flow,
     * `?error=cancelled` returns silently, and the other reasons surface the website's wording.
     */
    fun handleOAuthReturn(ticket: String?, error: String?, onRoute: (PostAuthDestination) -> Unit) {
        _uiState.update { it.copy(oauthNotice = null) }
        when {
            error == "cancelled" -> Unit
            error == "link_required" -> _uiState.update {
                it.copy(oauthNotice = "Sign in using your existing method first, then explicitly connect this provider.")
            }
            !error.isNullOrBlank() -> _uiState.update {
                it.copy(oauthNotice = "We could not finish that sign-in. Please try again.")
            }
            ticket.isNullOrBlank() -> Unit
            else -> {
                _uiState.update { it.copy(oauthBusy = true) }
                viewModelScope.launch {
                    authRepository.oauthExchange(ticket).fold(
                        onSuccess = { outcome ->
                            when (outcome) {
                                is AuthOutcome.SecurityRequired -> {
                                    _uiState.update {
                                        it.copy(
                                            oauthBusy = false,
                                            securityFlow = outcome.security,
                                            securityError = null,
                                            securityNotice = null,
                                            securityCode = "",
                                            securityTotpSetup = null,
                                            securityRecoveryCodes = outcome.security.recoveryCodes.orEmpty(),
                                            securitySavedRecovery = false,
                                            securityUseRecovery = false,
                                            securitySuccessMode = null,
                                        )
                                    }
                                    onRoute(PostAuthDestination.SECURITY_CHECK)
                                }
                                is AuthOutcome.Session -> {
                                    _uiState.update { it.copy(oauthBusy = false) }
                                    onRoute(
                                        if (!outcome.user.emailVerified) PostAuthDestination.VERIFY_EMAIL
                                        else PostAuthDestination.DASHBOARD
                                    )
                                }
                            }
                        },
                        onFailure = { err ->
                            _uiState.update {
                                it.copy(
                                    oauthBusy = false,
                                    oauthNotice = err.message ?: "We could not finish that sign-in. Please try again.",
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    // ---- Login ----
    fun updateLoginEmail(value: String) {
        _uiState.update { it.copy(loginEmail = value, loginEmailError = null, loginGeneralError = null) }
    }

    fun updateLoginPassword(value: String) {
        _uiState.update { it.copy(loginPassword = value, loginPasswordError = null, loginGeneralError = null) }
    }

    fun submitLogin(onRoute: (PostAuthDestination) -> Unit) {
        val current = _uiState.value
        if (current.loginSubmitting) return

        val email = current.loginEmail.trim()
        val password = current.loginPassword
        var emailErr: String? = null
        var pwdErr: String? = null

        if (email.isEmpty()) emailErr = "Please enter your email address."
        else if (!EMAIL_REGEX.matches(email)) emailErr = "Please enter a valid email address."
        if (password.isEmpty()) pwdErr = "Please enter your password."

        if (emailErr != null || pwdErr != null) {
            _uiState.update { it.copy(loginEmailError = emailErr, loginPasswordError = pwdErr) }
            return
        }

        _uiState.update { it.copy(loginSubmitting = true, loginEmailError = null, loginPasswordError = null, loginGeneralError = null) }
        viewModelScope.launch {
            val result = authRepository.login(email = email, password = password)
            result.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is AuthOutcome.SecurityRequired -> {
                            _uiState.update {
                                it.copy(
                                    loginSubmitting = false,
                                    loginPassword = "",
                                    securityFlow = outcome.security,
                                    securityError = null,
                                    securityNotice = null,
                                    securityCode = "",
                                    securityTotpSetup = null,
                                    securityRecoveryCodes = outcome.security.recoveryCodes.orEmpty(),
                                    securitySavedRecovery = false,
                                    securityUseRecovery = false,
                                    securitySuccessMode = null,
                                )
                            }
                            onRoute(PostAuthDestination.SECURITY_CHECK)
                        }
                        is AuthOutcome.Session -> {
                            _uiState.update { it.copy(loginSubmitting = false, loginPassword = "") }
                            val dest = if (!outcome.user.emailVerified) {
                                PostAuthDestination.VERIFY_EMAIL
                            } else {
                                PostAuthDestination.DASHBOARD
                            }
                            onRoute(dest)
                        }
                    }
                },
                onFailure = { err ->
                    val apiErr = err as? ServixApiException
                    val fieldErrors = apiErr?.fieldErrors.orEmpty()
                    when {
                        fieldErrors.isNotEmpty() -> {
                            _uiState.update {
                                it.copy(
                                    loginSubmitting = false,
                                    loginEmailError = fieldErrors["email"],
                                    loginPasswordError = fieldErrors["password"],
                                    loginGeneralError = if (fieldErrors["email"] == null && fieldErrors["password"] == null) apiErr?.message else null,
                                )
                            }
                        }
                        apiErr?.status == 401 -> {
                            _uiState.update {
                                it.copy(
                                    loginSubmitting = false,
                                    loginPasswordError = "Incorrect email or password.",
                                )
                            }
                        }
                        apiErr?.status == 429 -> {
                            _uiState.update {
                                it.copy(
                                    loginSubmitting = false,
                                    loginGeneralError = "Too many attempts. Please wait a minute.",
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    loginSubmitting = false,
                                    loginGeneralError = apiErr?.message ?: "Sign in is temporarily unavailable. Please try again.",
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    // ---- Register (Customer-only per confirmed user choice) ----
    fun updateRegisterName(value: String) {
        _uiState.update { it.copy(registerFullName = value, registerNameError = null, registerGeneralError = null) }
    }

    fun updateRegisterEmail(value: String) {
        _uiState.update { it.copy(registerEmail = value, registerEmailError = null, registerGeneralError = null) }
    }

    fun updateRegisterPassword(value: String) {
        _uiState.update { it.copy(registerPassword = value, registerPasswordError = null, registerGeneralError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(registerConfirmPassword = value, registerConfirmError = null, registerGeneralError = null) }
    }

    fun updateRegisterConfirmPassword(value: String) = onConfirmPasswordChange(value)

    fun updateRegisterTermsAccepted(accepted: Boolean) {
        _uiState.update { it.copy(registerTermsAccepted = accepted, registerGeneralError = null) }
    }

    fun submitRegister(onRoute: (PostAuthDestination) -> Unit) {
        val current = _uiState.value
        if (current.registerSubmitting || !current.registerTermsAccepted) return

        val name = current.registerFullName.trim()
        val email = current.registerEmail.trim().lowercase()
        val password = current.registerPassword
        val confirm = current.registerConfirmPassword

        var nameErr: String? = null
        var emailErr: String? = null
        var pwdErr: String? = null
        var confirmErr: String? = null

        if (name.isEmpty()) nameErr = "Please enter your full name."
        if (!EMAIL_REGEX.matches(email)) emailErr = "Please enter a valid email address."
        if (!current.passwordRules.all { it.met } || password.length > 200) pwdErr = "Please meet all the password requirements below."
        if (password.isEmpty() || password != confirm) confirmErr = "Enter the same password in both fields."

        if (nameErr != null || emailErr != null || pwdErr != null || confirmErr != null) {
            _uiState.update {
                it.copy(
                    registerNameError = nameErr,
                    registerEmailError = emailErr,
                    registerPasswordError = pwdErr,
                    registerConfirmError = confirmErr,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                registerSubmitting = true,
                registerNameError = null,
                registerEmailError = null,
                registerPasswordError = null,
                registerConfirmError = null,
                registerGeneralError = null,
            )
        }

        viewModelScope.launch {
            termsAcceptanceStore?.recordTermsAccepted()
            val result = authRepository.register(fullName = name, email = email, password = password)
            result.fold(
                onSuccess = { outcome ->
                    when (outcome) {
                        is AuthOutcome.SecurityRequired -> {
                            _uiState.update {
                                it.copy(
                                    registerSubmitting = false,
                                    registerPassword = "",
                                    registerConfirmPassword = "",
                                    securityFlow = outcome.security,
                                    securityError = null,
                                    securityNotice = null,
                                    securityCode = "",
                                    securityTotpSetup = null,
                                    securityRecoveryCodes = outcome.security.recoveryCodes.orEmpty(),
                                    securitySavedRecovery = false,
                                    securityUseRecovery = false,
                                    securitySuccessMode = null,
                                )
                            }
                            onRoute(PostAuthDestination.SECURITY_CHECK)
                        }
                        is AuthOutcome.Session -> {
                            _uiState.update {
                                it.copy(
                                    registerSubmitting = false,
                                    registerPassword = "",
                                    registerConfirmPassword = "",
                                    emailConfirmed = false,
                                    otpCode = "",
                                    otpError = null,
                                    otpNotice = null,
                                )
                            }
                            val dest = if (!outcome.user.emailVerified) {
                                PostAuthDestination.VERIFY_EMAIL
                            } else {
                                PostAuthDestination.DASHBOARD
                            }
                            onRoute(dest)
                        }
                    }
                },
                onFailure = { err ->
                    val apiErr = err as? ServixApiException
                    val fieldErrors = apiErr?.fieldErrors.orEmpty()
                    when {
                        fieldErrors.isNotEmpty() -> {
                            _uiState.update {
                                it.copy(
                                    registerSubmitting = false,
                                    registerNameError = fieldErrors["fullName"] ?: fieldErrors["name"],
                                    registerEmailError = fieldErrors["email"],
                                    registerPasswordError = fieldErrors["password"],
                                    registerGeneralError = if (fieldErrors["fullName"] == null && fieldErrors["name"] == null && fieldErrors["email"] == null && fieldErrors["password"] == null) apiErr?.message else null,
                                )
                            }
                        }
                        apiErr?.status == 429 -> {
                            _uiState.update {
                                it.copy(
                                    registerSubmitting = false,
                                    registerGeneralError = "Too many attempts. Please wait a minute before trying again. Your details are still here.",
                                )
                            }
                        }
                        else -> {
                            _uiState.update {
                                it.copy(
                                    registerSubmitting = false,
                                    registerGeneralError = apiErr?.message
                                        ?: "We could not finish creating your account. Check your connection and try again. If you already registered, try signing in.",
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    // ---- Email Verification (GET /auth/verification-method, POST /auth/verify-email/code, POST /auth/verify-email/resend, GET /me) ----
    fun loadVerificationMethod() {
        _uiState.update { it.copy(verificationMethod = null, verificationMethodError = false) }
        viewModelScope.launch {
            val result = authRepository.getVerificationMethod()
            result.fold(
                onSuccess = { method ->
                    _uiState.update { it.copy(verificationMethod = method, verificationMethodError = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(verificationMethod = null, verificationMethodError = true) }
                }
            )
        }
    }

    fun updateOtpCode(raw: String) {
        val digitsOnly = raw.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(otpCode = digitsOnly, otpError = null) }
    }

    fun submitOtpCode() {
        val current = _uiState.value
        if (current.otpBusy) return
        if (current.otpCode.length != 6) {
            _uiState.update { it.copy(otpError = "Enter all six digits from your latest email.") }
            return
        }

        _uiState.update { it.copy(otpBusy = true, otpError = null, otpNotice = null) }
        viewModelScope.launch {
            val result = authRepository.verifyEmailCode(current.otpCode)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            otpBusy = false,
                            emailConfirmed = true,
                            otpCode = "",
                        )
                    }
                },
                onFailure = { err ->
                    val apiErr = err as? ServixApiException
                    val message = when {
                        apiErr?.status == 401 -> "Your session expired. Sign in again to continue verification."
                        apiErr?.status == 429 && apiErr.code == "OTP_LIMIT" -> "Too many attempts. Wait one hour before trying again."
                        apiErr?.status == 429 -> "Please wait a minute before trying again."
                        apiErr?.status == 400 -> "That code is incorrect, expired, or already used. Use the newest email or request a new code."
                        else -> apiErr?.message ?: "We could not check your code. Check your connection and try again."
                    }
                    _uiState.update { it.copy(otpBusy = false, otpError = message) }
                }
            )
        }
    }

    fun resendVerificationEmail() {
        val current = _uiState.value
        if (current.otpBusy || current.resendWaitSeconds > 0) return

        _uiState.update { it.copy(otpBusy = true, otpError = null, otpNotice = null) }
        viewModelScope.launch {
            val result = authRepository.resendVerification()
            result.fold(
                onSuccess = { res ->
                    if (res.alreadyVerified) {
                        _uiState.update { it.copy(otpBusy = false, emailConfirmed = true) }
                    } else {
                        val waitSecs = res.retryAfterSeconds ?: 60
                        _uiState.update {
                            it.copy(
                                otpBusy = false,
                                otpCode = "",
                                otpNotice = if (it.verificationMethod == "otp") {
                                    "A new code is queued for delivery. Check your inbox and spam folder. Only the newest code will work."
                                } else {
                                    "Verification email sent."
                                }
                            )
                        }
                        startResendCooldown(waitSecs)
                    }
                },
                onFailure = { err ->
                    val apiErr = err as? ServixApiException
                    if (apiErr?.status == 429) {
                        val waitSecs = if (apiErr.code == "OTP_LIMIT") 3600 else 60
                        startResendCooldown(waitSecs)
                        _uiState.update {
                            it.copy(
                                otpBusy = false,
                                otpError = "You have reached the request limit. Please wait before requesting another code.",
                            )
                        }
                    } else if (apiErr?.status == 401) {
                        _uiState.update {
                            it.copy(
                                otpBusy = false,
                                otpError = "Sign in again before requesting a code.",
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                otpBusy = false,
                                otpError = apiErr?.message ?: "We could not request another code. Please try again shortly.",
                            )
                        }
                    }
                }
            )
        }
    }

    fun checkEmailVerifiedStatus(onVerified: () -> Unit = {}) {
        _uiState.update { it.copy(otpBusy = true, otpError = null) }
        viewModelScope.launch {
            val result = authRepository.refreshMe()
            result.fold(
                onSuccess = { user ->
                    if (user.emailVerified) {
                        _uiState.update { it.copy(otpBusy = false, emailConfirmed = true) }
                        onVerified()
                    } else {
                        _uiState.update {
                            it.copy(
                                otpBusy = false,
                                otpError = "Your email is not marked verified yet. Click the link in your latest email first.",
                            )
                        }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            otpBusy = false,
                            otpError = err.message ?: "Could not check verification status.",
                        )
                    }
                }
            )
        }
    }

    private fun startResendCooldown(seconds: Int) {
        cooldownJob?.cancel()
        _uiState.update { it.copy(resendWaitSeconds = seconds) }
        cooldownJob = viewModelScope.launch {
            while (_uiState.value.resendWaitSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(resendWaitSeconds = (it.resendWaitSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }

    // ---- Security / MFA Flow (reference/web/src/pages/auth/SecurityCheckPage.jsx) ----
    fun loadSecurityCheck() {
        _uiState.update {
            it.copy(
                securityLoading = true,
                securityLoadFailed = false,
                securityExpired = false,
                securityError = null,
            )
        }
        viewModelScope.launch {
            val configRes = authRepository.getSecurityConfig()
            val config = configRes.getOrElse { err ->
                _uiState.update {
                    it.copy(
                        securityLoading = false,
                        securityLoadFailed = true,
                        securityError = err.message ?: "We could not load your security check. Please try again.",
                    )
                }
                return@launch
            }

            if (!config.enabled) {
                _uiState.update {
                    it.copy(
                        securityLoading = false,
                        securityConfigEnabled = false,
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(securityConfigEnabled = true) }
            val statusRes = authRepository.securityStatus()
            statusRes.fold(
                onSuccess = { state ->
                    var codes = state.recoveryCodes.orEmpty()
                    if (state.next == "recovery" && codes.isEmpty()) {
                        authRepository.securityRecoveryCodes().onSuccess { rec ->
                            codes = rec
                        }
                    }
                    _uiState.update {
                        it.copy(
                            securityLoading = false,
                            securityFlow = state,
                            securityRecoveryCodes = codes,
                        )
                    }
                },
                onFailure = { err ->
                    val apiErr = err as? ServixApiException
                    val expired = apiErr?.status == 401
                    _uiState.update {
                        it.copy(
                            securityLoading = false,
                            securityLoadFailed = true,
                            securityExpired = expired,
                            securityError = if (expired) {
                                "Your verification session expired. Start again."
                            } else {
                                "We could not load your security check. Please try again."
                            },
                        )
                    }
                }
            )
        }
    }

    fun updateSecurityCode(raw: String) {
        val current = _uiState.value
        val cleaned = if (current.securityUseRecovery) {
            raw.take(64)
        } else {
            raw.filter { it.isDigit() }.take(6)
        }
        _uiState.update { it.copy(securityCode = cleaned, securityError = null) }
    }

    fun toggleSecurityUseRecovery() {
        _uiState.update {
            it.copy(
                securityUseRecovery = !it.securityUseRecovery,
                securityCode = "",
                securityError = null,
            )
        }
    }

    fun updateSecuritySavedRecovery(saved: Boolean) {
        _uiState.update { it.copy(securitySavedRecovery = saved) }
    }

    fun beginSecurityTotpSetup() {
        val current = _uiState.value
        if (current.securityBusy) return
        _uiState.update { it.copy(securityBusy = true, securityError = null, securityNotice = null) }
        viewModelScope.launch {
            val res = authRepository.securityTotpSetup()
            res.fold(
                onSuccess = { setup ->
                    _uiState.update {
                        it.copy(
                            securityBusy = false,
                            securityTotpSetup = setup,
                            securityCode = "",
                        )
                    }
                },
                onFailure = ::handleSecurityError,
            )
        }
    }

    fun clearSecurityTotpSetup() {
        _uiState.update { it.copy(securityTotpSetup = null, securityCode = "", securityError = null) }
    }

    fun submitSecurityCode() {
        val current = _uiState.value
        if (current.securityBusy) return
        val flow = current.securityFlow ?: return

        if (!current.securityUseRecovery && !SIX_DIGIT_REGEX.matches(current.securityCode)) {
            _uiState.update { it.copy(securityError = "Enter all six digits.") }
            return
        }
        if (current.securityUseRecovery && current.securityCode.trim().length !in 20..64) {
            _uiState.update { it.copy(securityError = "Enter a valid recovery code (20–64 characters).") }
            return
        }

        _uiState.update { it.copy(securityBusy = true, securityError = null, securityNotice = null) }
        viewModelScope.launch {
            val result = when {
                flow.next == "email" -> authRepository.securityEmailVerify(current.securityCode)
                current.securityTotpSetup != null -> authRepository.securityTotpEnroll(current.securityCode)
                current.securityUseRecovery -> authRepository.securityRecoveryVerify(current.securityCode)
                else -> authRepository.securityTotpVerify(current.securityCode)
            }
            result.fold(
                onSuccess = { nextState ->
                    var codes = nextState.recoveryCodes.orEmpty()
                    if (nextState.next == "recovery" && codes.isEmpty()) {
                        authRepository.securityRecoveryCodes().onSuccess { rec ->
                            codes = rec
                        }
                    }
                    _uiState.update {
                        it.copy(
                            securityBusy = false,
                            securityFlow = nextState,
                            securityCode = "",
                            securityTotpSetup = null,
                            securityUseRecovery = false,
                            securityRecoveryCodes = if (codes.isNotEmpty()) codes else it.securityRecoveryCodes,
                        )
                    }
                },
                onFailure = ::handleSecurityError,
            )
        }
    }

    fun resendSecurityEmailCode() {
        val current = _uiState.value
        if (current.securityBusy || current.securityWaitSeconds > 0) return
        val flow = current.securityFlow

        _uiState.update { it.copy(securityBusy = true, securityError = null, securityNotice = null) }
        viewModelScope.launch {
            val res = authRepository.securityEmailResend()
            res.fold(
                onSuccess = { updatedFlow ->
                    val waitSecs = updatedFlow.retryAfterSeconds ?: 60
                    startSecurityCooldown(waitSecs)
                    _uiState.update {
                        it.copy(
                            securityBusy = false,
                            securityFlow = updatedFlow,
                            securityNotice = if (flow?.purpose == "reset") {
                                "If eligible, a code will be sent. Check Spam too; recent requests may need a little longer."
                            } else {
                                "A new code is queued. Check Spam too. Only the newest code will work."
                            },
                        )
                    }
                },
                onFailure = ::handleSecurityError,
            )
        }
    }

    fun finishSecurityFlow(onComplete: () -> Unit) {
        val current = _uiState.value
        if (current.securityBusy) return
        val isFromRecovery = current.securityFlow?.next == "recovery"
        _uiState.update { it.copy(securityBusy = true, securityError = null, securityNotice = null) }
        viewModelScope.launch {
            val res = authRepository.securityFinish(
                savedRecovery = if (isFromRecovery) true else null
            )
            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            securityBusy = false,
                            securityRecoveryCodes = emptyList(),
                            securitySuccessMode = "session",
                        )
                    }
                    onComplete()
                },
                onFailure = ::handleSecurityError,
            )
        }
    }

    fun updateSecurityNewPassword(value: String) {
        _uiState.update { it.copy(securityNewPassword = value, securityError = null) }
    }

    fun updateSecurityConfirmPassword(value: String) {
        _uiState.update { it.copy(securityConfirmPassword = value, securityError = null) }
    }

    fun submitSecurityResetPassword() {
        val current = _uiState.value
        if (current.securityBusy) return
        val rulesMet = current.securityResetPasswordRules.all { it.met }
        val matches = current.securityNewPassword.isNotEmpty() && current.securityNewPassword == current.securityConfirmPassword
        if (!rulesMet || !matches) {
            _uiState.update {
                it.copy(securityError = "Meet every password requirement and make sure both passwords match.")
            }
            return
        }

        _uiState.update { it.copy(securityBusy = true, securityError = null) }
        viewModelScope.launch {
            val res = authRepository.securityResetPassword(current.securityNewPassword)
            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            securityBusy = false,
                            securityNewPassword = "",
                            securityConfirmPassword = "",
                            securitySuccessMode = "reset",
                        )
                    }
                },
                onFailure = ::handleSecurityError,
            )
        }
    }

    fun cancelSecurityFlow(onCancelled: () -> Unit) {
        viewModelScope.launch {
            authRepository.securityCancel()
            _uiState.update {
                it.copy(
                    securityFlow = null,
                    securityTotpSetup = null,
                    securityRecoveryCodes = emptyList(),
                    securityCode = "",
                    securityError = null,
                    securityNotice = null,
                )
            }
            onCancelled()
        }
    }

    private fun handleSecurityError(err: Throwable) {
        val apiErr = err as? ServixApiException
        when {
            apiErr?.status == 401 -> {
                _uiState.update {
                    it.copy(
                        securityBusy = false,
                        securityExpired = true,
                        securityError = "Your verification session expired. Start again.",
                    )
                }
            }
            apiErr?.status == 429 -> {
                val waitSecs = if (apiErr.code == "SECURITY_LIMIT") 3600 else 60
                startSecurityCooldown(waitSecs)
                _uiState.update {
                    it.copy(
                        securityBusy = false,
                        securityError = apiErr.message.ifBlank { "Please wait before trying again." },
                    )
                }
            }
            else -> {
                _uiState.update {
                    it.copy(
                        securityBusy = false,
                        securityError = apiErr?.message ?: "We could not complete this step. Check your connection and try again.",
                    )
                }
            }
        }
    }

    private fun startSecurityCooldown(seconds: Int) {
        securityCooldownJob?.cancel()
        _uiState.update { it.copy(securityWaitSeconds = seconds) }
        securityCooldownJob = viewModelScope.launch {
            while (_uiState.value.securityWaitSeconds > 0) {
                delay(1000)
                _uiState.update { it.copy(securityWaitSeconds = (it.securityWaitSeconds - 1).coerceAtLeast(0)) }
            }
        }
    }

    // ---- Forgot Password ----
    fun updateForgotEmail(value: String) {
        _uiState.update { it.copy(forgotEmail = value, forgotError = null) }
    }

    fun resetForgotState() {
        _uiState.update { it.copy(forgotSent = false, forgotError = null, forgotSubmitting = false) }
    }

    fun submitForgotPassword(onSecurityCheck: () -> Unit = {}) {
        val current = _uiState.value
        if (current.forgotSubmitting) return

        val email = current.forgotEmail.trim()
        if (email.isEmpty()) {
            _uiState.update { it.copy(forgotError = "Please enter your email address.") }
            return
        }
        if (!EMAIL_REGEX.matches(email)) {
            _uiState.update { it.copy(forgotError = "Please enter a valid email address.") }
            return
        }

        _uiState.update { it.copy(forgotSubmitting = true, forgotError = null) }
        viewModelScope.launch {
            val result = authRepository.forgotPassword(email)
            result.fold(
                onSuccess = { res ->
                    if (res.security != null) {
                        _uiState.update {
                            it.copy(
                                forgotSubmitting = false,
                                securityFlow = res.security,
                                securityError = null,
                                securityNotice = null,
                                securityCode = "",
                                securityTotpSetup = null,
                                securityRecoveryCodes = res.security.recoveryCodes.orEmpty(),
                                securitySavedRecovery = false,
                                securityUseRecovery = false,
                                securitySuccessMode = null,
                            )
                        }
                        onSecurityCheck()
                    } else {
                        _uiState.update { it.copy(forgotSubmitting = false, forgotSent = true) }
                    }
                },
                onFailure = { err ->
                    val apiErr = err as? ServixApiException
                    val msg = if (apiErr?.status == 429) {
                        "Too many requests. Please wait a few minutes."
                    } else {
                        apiErr?.message ?: "Password reset is temporarily unavailable. Please try again."
                    }
                    _uiState.update { it.copy(forgotSubmitting = false, forgotError = msg) }
                }
            )
        }
    }

    // ---- Sign out ----
    /**
     * Re-reads the signed-in user and republishes it on [sessionState]. Used after a profile write so
     * every screen showing the account (Home, Account header, Profile) updates in one emission.
     */
    suspend fun refreshSessionUser() {
        authRepository.refreshSessionUser()
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.logout()
            // Provider availability is public configuration cached for this ViewModel. Keep it
            // when clearing private form/session data so returning directly to registration
            // still shows the providers already loaded by the shell.
            _uiState.value = AuthUiState(oauthProviders = _uiState.value.oauthProviders)
            onComplete()
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        private val SIX_DIGIT_REGEX = Regex("^\\d{6}$")

        fun factory(
            authRepository: AuthRepository,
            termsAcceptanceStore: TermsAcceptanceStore? = null,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository, termsAcceptanceStore) as T
                }
            }
    }
}
