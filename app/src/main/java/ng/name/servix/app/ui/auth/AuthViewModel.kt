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
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.repository.AuthRepository
import ng.name.servix.app.data.repository.SessionState

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
    val registerNameError: String? = null,
    val registerEmailError: String? = null,
    val registerPasswordError: String? = null,
    val registerConfirmError: String? = null,
    val registerGeneralError: String? = null,
    val registerSubmitting: Boolean = false,

    // Email verification (OTP / Link)
    val verificationMethod: String? = null, // "otp" | "link" | null (loading)
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
) {
    val passwordRules: List<PasswordRuleStatus>
        get() = listOf(
            PasswordRuleStatus("length", "At least 8 characters", registerPassword.length >= 8),
            PasswordRuleStatus("uppercase", "One uppercase letter", registerPassword.any { it.isUpperCase() }),
            PasswordRuleStatus("lowercase", "One lowercase letter", registerPassword.any { it.isLowerCase() }),
            PasswordRuleStatus("number", "One number", registerPassword.any { it.isDigit() }),
            PasswordRuleStatus("symbol", "One symbol", registerPassword.any { !it.isLetterOrDigit() && !it.isWhitespace() }),
            PasswordRuleStatus("maximum", "No more than 200 characters", registerPassword.isNotEmpty() && registerPassword.length <= 200),
        )
}

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = authRepository.sessionState

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.restoreSession()
        }
    }

    // ---- Login ----
    fun updateLoginEmail(value: String) {
        _uiState.update { it.copy(loginEmail = value, loginEmailError = null, loginGeneralError = null) }
    }

    fun updateLoginPassword(value: String) {
        _uiState.update { it.copy(loginPassword = value, loginPasswordError = null, loginGeneralError = null) }
    }

    fun submitLogin(onSuccess: (needsVerification: Boolean) -> Unit) {
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
                onSuccess = { user ->
                    _uiState.update { it.copy(loginSubmitting = false, loginPassword = "") }
                    onSuccess(!user.isEmailVerified)
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

    fun updateRegisterConfirmPassword(value: String) {
        _uiState.update { it.copy(registerConfirmPassword = value, registerConfirmError = null, registerGeneralError = null) }
    }

    fun submitRegister(onSuccess: () -> Unit) {
        val current = _uiState.value
        if (current.registerSubmitting) return

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
        if (!current.passwordRules.all { it.met }) pwdErr = "Please meet all the password requirements below."
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
            val result = authRepository.register(fullName = name, email = email, password = password)
            result.fold(
                onSuccess = {
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
                    onSuccess()
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

    // ---- Email Verification ----
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
                        apiErr?.status == 400 -> apiErr.message.ifBlank {
                            "That code is incorrect, expired, or already used. Use the newest email or request a new code."
                        }
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

    fun checkEmailVerifiedStatus() {
        _uiState.update { it.copy(otpBusy = true, otpError = null) }
        viewModelScope.launch {
            val result = authRepository.refreshMe()
            result.fold(
                onSuccess = { user ->
                    if (user.isEmailVerified) {
                        _uiState.update { it.copy(otpBusy = false, emailConfirmed = true) }
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

    // ---- Forgot Password ----
    fun updateForgotEmail(value: String) {
        _uiState.update { it.copy(forgotEmail = value, forgotError = null) }
    }

    fun resetForgotState() {
        _uiState.update { it.copy(forgotSent = false, forgotError = null, forgotSubmitting = false) }
    }

    fun submitForgotPassword() {
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
                onSuccess = {
                    _uiState.update { it.copy(forgotSubmitting = false, forgotSent = true) }
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
    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState()
            onComplete()
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository) as T
                }
            }
    }
}
