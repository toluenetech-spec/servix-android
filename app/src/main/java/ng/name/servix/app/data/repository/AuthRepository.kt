package ng.name.servix.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import ng.name.servix.app.data.local.TokenStore
import ng.name.servix.app.data.model.ForgotPasswordRequest
import ng.name.servix.app.data.model.ForgotPasswordResponse
import ng.name.servix.app.data.model.LoginRequest
import ng.name.servix.app.data.model.RegisterRequest
import ng.name.servix.app.data.model.ResendVerificationResponse
import ng.name.servix.app.data.model.SecurityCodeRequest
import ng.name.servix.app.data.model.SecurityConfigResponse
import ng.name.servix.app.data.model.SecurityFinishRequest
import ng.name.servix.app.data.model.OAuthNativeExchangeRequest
import ng.name.servix.app.data.model.SecurityFlowState
import ng.name.servix.app.data.model.SecurityResetPasswordRequest
import ng.name.servix.app.data.model.SecurityTotpSetupResponse
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.model.VerifyEmailCodeRequest
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.PersistentCookieJar
import ng.name.servix.app.data.network.ServixApiService
import ng.name.servix.app.data.network.ServixNetworkModule

sealed interface SessionState {
    data object Initializing : SessionState
    data object Unauthenticated : SessionState
    data class Authenticated(val user: UserDto) : SessionState
}

sealed interface AuthOutcome {
    data class Session(val user: UserDto) : AuthOutcome
    data class SecurityRequired(val security: SecurityFlowState) : AuthOutcome
}

class AuthRepository(
    private val tokenStore: TokenStore,
    private val cookieJar: PersistentCookieJar,
) {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Initializing)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    lateinit var networkModule: ServixNetworkModule
        private set

    private val api: ServixApiService
        get() = networkModule.apiService

    fun attachNetworkModule(module: ServixNetworkModule) {
        this.networkModule = module
    }

    fun onSessionRefreshedFromAuthenticator(user: UserDto?, newAccessToken: String) {
        tokenStore.setAccessToken(newAccessToken)
        if (user != null) {
            tokenStore.setCachedUser(user)
            _sessionState.value = SessionState.Authenticated(user)
        }
    }

    fun onSessionExpiredFromAuthenticator() {
        tokenStore.clear()
        cookieJar.clear()
        _sessionState.value = SessionState.Unauthenticated
    }

    suspend fun restoreSession(): Result<UserDto?> = withContext(Dispatchers.IO) {
        val existingToken = tokenStore.getAccessToken()
        val cachedUser = tokenStore.getCachedUser()
        val hasRefreshCookie = cookieJar.hasRefreshCookie()

        if (existingToken.isNullOrBlank() && !hasRefreshCookie) {
            _sessionState.value = SessionState.Unauthenticated
            return@withContext Result.success(null)
        }

        if (cachedUser != null && !existingToken.isNullOrBlank()) {
            _sessionState.value = SessionState.Authenticated(cachedUser)
        }

        try {
            if (existingToken.isNullOrBlank() && hasRefreshCookie) {
                val refreshed = networkModule.executeSyncRefresh()
                if (refreshed?.user != null && !refreshed.accessToken.isNullOrBlank()) {
                    tokenStore.setAccessToken(refreshed.accessToken)
                    tokenStore.setCachedUser(refreshed.user)
                    _sessionState.value = SessionState.Authenticated(refreshed.user)
                    return@withContext Result.success(refreshed.user)
                }
            }

            val meResponse = api.getMe()
            tokenStore.setCachedUser(meResponse.user)
            _sessionState.value = SessionState.Authenticated(meResponse.user)
            Result.success(meResponse.user)
        } catch (t: Throwable) {
            val ex = ApiErrorParser.toServixException(t)
            if (ex.isNetworkError && cachedUser != null) {
                _sessionState.value = SessionState.Authenticated(cachedUser)
                Result.success(cachedUser)
            } else if (ex.status == 401 || ex.status == 403) {
                tokenStore.clear()
                cookieJar.clear()
                _sessionState.value = SessionState.Unauthenticated
                Result.success(null)
            } else {
                if (cachedUser != null) {
                    _sessionState.value = SessionState.Authenticated(cachedUser)
                } else {
                    _sessionState.value = SessionState.Unauthenticated
                }
                Result.failure(ex)
            }
        }
    }

    /**
     * Re-reads `GET /me` and republishes it on [sessionState].
     *
     * This is the single source of truth the whole shell collects, so after a profile write (a new
     * avatar, a renamed account) Home, the Account header and the Profile panel all recompose with the
     * new user without a restart. Failures are returned, never thrown: a stale avatar is not worth
     * breaking the screen that was just saved from.
     */
    suspend fun refreshSessionUser(): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val me = api.getMe()
            tokenStore.setCachedUser(me.user)
            _sessionState.value = SessionState.Authenticated(me.user)
            Result.success(me.user)
        } catch (t: Throwable) {
            val error = ApiErrorParser.toServixException(t)
            if (error.status == 401 || error.status == 403) {
                // The session really is gone; do not keep showing an authenticated shell.
                tokenStore.clear()
                cookieJar.clear()
                _sessionState.value = SessionState.Unauthenticated
            }
            Result.failure(error)
        }
    }

    suspend fun register(fullName: String, email: String, password: String): Result<AuthOutcome> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.register(
                    RegisterRequest(
                        fullName = fullName.trim(),
                        email = email.trim().lowercase(),
                        password = password,
                    )
                )
                tokenStore.setAccessToken(response.accessToken)
                if (response.security != null) {
                    _sessionState.value = SessionState.Unauthenticated
                    return@withContext Result.success(AuthOutcome.SecurityRequired(response.security))
                }
                val user = response.user
                    ?: throw ServixApiException(
                        status = 500,
                        code = "INVALID_RESPONSE",
                        message = "We could not finish creating your account.",
                    )
                tokenStore.setCachedUser(user)
                _sessionState.value = SessionState.Authenticated(user)
                Result.success(AuthOutcome.Session(user))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun login(email: String, password: String): Result<AuthOutcome> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.login(
                    LoginRequest(
                        email = email.trim().lowercase(),
                        password = password,
                    )
                )
                tokenStore.setAccessToken(response.accessToken)
                if (response.security != null) {
                    _sessionState.value = SessionState.Unauthenticated
                    return@withContext Result.success(AuthOutcome.SecurityRequired(response.security))
                }
                val user = response.user
                    ?: throw ServixApiException(
                        status = 500,
                        code = "INVALID_RESPONSE",
                        message = "Sign in is temporarily unavailable. Please try again.",
                    )
                tokenStore.setCachedUser(user)
                _sessionState.value = SessionState.Authenticated(user)
                Result.success(AuthOutcome.Session(user))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun getVerificationMethod(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getVerificationMethod()
            if (response.method != "otp" && response.method != "link") {
                throw ServixApiException(500, "UNKNOWN_METHOD", "Unknown verification method.")
            }
            Result.success(response.method)
        } catch (t: Throwable) {
            val ex = ApiErrorParser.toServixException(t)
            if (ex.status == 404) {
                Result.success("link")
            } else {
                Result.failure(ex)
            }
        }
    }

    suspend fun verifyEmailCode(code: String): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.verifyEmailCode(VerifyEmailCodeRequest(code = code.trim()))
            tokenStore.setCachedUser(response.user)
            _sessionState.value = SessionState.Authenticated(response.user)
            Result.success(response.user)
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun resendVerification(): Result<ResendVerificationResponse> =
        withContext(Dispatchers.IO) {
            try {
                val res = api.resendVerification()
                if (res.alreadyVerified) {
                    runCatching { api.getMe() }.getOrNull()?.user?.let {
                        tokenStore.setCachedUser(it)
                        _sessionState.value = SessionState.Authenticated(it)
                    }
                }
                Result.success(res)
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun forgotPassword(email: String): Result<ForgotPasswordResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.forgotPassword(ForgotPasswordRequest(email = email.trim().lowercase()))
            Result.success(response)
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun refreshMe(): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val me = api.getMe().user
            tokenStore.setCachedUser(me)
            _sessionState.value = SessionState.Authenticated(me)
            Result.success(me)
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    /* -------- Provider sign-in (api/src/routes/oauth.ts:25-28, 85-90) -------- */

    /** GET /auth/oauth/config -> { providers }; the server decides which providers are live. */
    suspend fun oauthProviders(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.getOAuthConfig().providers)
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    /** POST /auth/oauth/native/exchange { ticket } -> the usual security flow response. */
    suspend fun oauthExchange(ticket: String): Result<AuthOutcome> = withContext(Dispatchers.IO) {
        try {
            val response = api.exchangeOAuthTicket(OAuthNativeExchangeRequest(ticket = ticket))
            response.security?.let { flow ->
                Result.success(AuthOutcome.SecurityRequired(flow))
            } ?: Result.failure(ServixApiException(0, "OAUTH_INCOMPLETE", "The provider sign-in did not complete."))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    /* -------- Security / MFA routes (security.ts / securityFlow.ts) -------- */

    suspend fun getSecurityConfig(): Result<SecurityConfigResponse> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.getSecurityConfig())
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun securityStatus(): Result<SecurityFlowState> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.securityStatus())
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun securityEmailVerify(code: String): Result<SecurityFlowState> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.securityEmailVerify(SecurityCodeRequest(code = code.trim())))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun securityEmailResend(): Result<SecurityFlowState> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.securityEmailResend())
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun securityTotpSetup(): Result<SecurityTotpSetupResponse> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.securityTotpSetup())
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun securityTotpEnroll(code: String): Result<SecurityFlowState> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.securityTotpEnroll(SecurityCodeRequest(code = code.trim())))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun securityTotpVerify(code: String): Result<SecurityFlowState> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.securityTotpVerify(SecurityCodeRequest(code = code.trim())))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    /**
     * Calls POST /auth/security/recovery/codes -> { recoveryCodes: [...] }.
     */
    suspend fun securityRecoveryCodes(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = api.securityRecoveryCodes()
            Result.success(response.recoveryCodes)
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun securityRecoveryVerify(code: String): Result<SecurityFlowState> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.securityRecoveryVerify(SecurityCodeRequest(code = code.trim())))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun securityFinish(savedRecovery: Boolean? = null): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.securityFinish(SecurityFinishRequest(savedRecovery = savedRecovery))
            val user = response.user
                ?: throw ServixApiException(500, "INVALID_RESPONSE", "Could not finish security check.")
            if (!response.accessToken.isNullOrBlank()) {
                tokenStore.setAccessToken(response.accessToken)
            }
            tokenStore.setCachedUser(user)
            _sessionState.value = SessionState.Authenticated(user)
            Result.success(user)
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun securityResetPassword(password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.securityResetPassword(SecurityResetPasswordRequest(password = password))
            tokenStore.clear()
            _sessionState.value = SessionState.Unauthenticated
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun securityCancel(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.securityCancel()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            api.logout()
        } catch (_: Throwable) {
            // Always clear local state even if network is unreachable
        } finally {
            tokenStore.clear()
            cookieJar.clear()
            _sessionState.value = SessionState.Unauthenticated
        }
    }
}
