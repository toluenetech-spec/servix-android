package ng.name.servix.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ng.name.servix.app.data.local.TokenStore
import ng.name.servix.app.data.model.ForgotPasswordRequest
import ng.name.servix.app.data.model.LoginRequest
import ng.name.servix.app.data.model.RegisterRequest
import ng.name.servix.app.data.model.ResendVerificationResponse
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.model.VerifyEmailCodeRequest
import ng.name.servix.app.data.model.VerifyEmailTokenRequest
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.PersistentCookieJar
import ng.name.servix.app.data.network.ServixApiService
import ng.name.servix.app.data.network.ServixNetworkModule

sealed interface SessionState {
    data object Initializing : SessionState
    data object Unauthenticated : SessionState
    data class Authenticated(val user: UserDto) : SessionState
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

    /**
     * Restores the user's session on app launch:
     * 1. If we have a saved accessToken or refresh cookie, call GET /me (which automatically triggers
     *    POST /auth/refresh via the OkHttp Authenticator on 401).
     * 2. If we only have a refresh cookie and no access token, call executeSyncRefresh() first.
     * 3. If offline (airplane mode) and we have a cached user + token, keep the user signed in locally
     *    without crashing.
     */
    suspend fun restoreSession(): Result<UserDto?> = withContext(Dispatchers.IO) {
        val existingToken = tokenStore.getAccessToken()
        val cachedUser = tokenStore.getCachedUser()
        val hasRefreshCookie = cookieJar.hasRefreshCookie()

        if (existingToken.isNullOrBlank() && !hasRefreshCookie) {
            _sessionState.value = SessionState.Unauthenticated
            return@withContext Result.success(null)
        }

        // Optimistically expose cached user if available while verifying with server
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
                // Airplane mode / offline: preserve cached user session gracefully
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

    suspend fun register(fullName: String, email: String, password: String): Result<UserDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.register(
                    RegisterRequest(
                        fullName = fullName.trim(),
                        email = email.trim().lowercase(),
                        password = password,
                    )
                )
                val user = response.user
                    ?: throw ServixApiException(
                        status = 400,
                        code = "SECURITY_REQUIRED",
                        message = "Additional security verification is required on www.servix.name.ng.",
                    )
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

    suspend fun login(email: String, password: String): Result<UserDto> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.login(
                    LoginRequest(
                        email = email.trim().lowercase(),
                        password = password,
                    )
                )
                val user = response.user
                    ?: throw ServixApiException(
                        status = 400,
                        code = "SECURITY_REQUIRED",
                        message = "Additional security verification is required on www.servix.name.ng.",
                    )
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

    suspend fun getVerificationMethod(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getVerificationMethod()
            val method = if (response.method == "link") "link" else "otp"
            Result.success(method)
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

    suspend fun verifyEmailToken(token: String): Result<UserDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.verifyEmailToken(VerifyEmailTokenRequest(token = token.trim()))
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
                    // Refresh /me so user.emailVerified is up-to-date
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

    suspend fun forgotPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            api.forgotPassword(ForgotPasswordRequest(email = email.trim().lowercase()))
            Result.success(Unit)
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
