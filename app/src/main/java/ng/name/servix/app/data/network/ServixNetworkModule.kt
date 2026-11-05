package ng.name.servix.app.data.network

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.data.local.TokenStore
import ng.name.servix.app.data.model.AuthSessionResponse
import ng.name.servix.app.data.model.UserDto
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ServixNetworkModule(
    context: Context,
    val tokenStore: TokenStore,
    val cookieJar: PersistentCookieJar,
    private val onSessionRefreshed: (UserDto?, String) -> Unit,
    private val onSessionExpired: () -> Unit,
) {
    private val refreshLock = Any()

    // Dedicated OkHttp client for calling POST /auth/refresh without triggering the Authenticator recursively
    private val refreshClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(servixUserAgentInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val authHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val path = original.url.encodedPath
        val isPublicOrSecurityAuthRoute = path.endsWith("/auth/login") ||
            path.endsWith("/auth/register") ||
            path.endsWith("/auth/refresh") ||
            path.endsWith("/auth/forgot-password") ||
            path.endsWith("/auth/verification-method") ||
            path.contains("/auth/security/")

        val token = tokenStore.getAccessToken()
        val builder = original.newBuilder()
            .header("X-Servix-Client", "ng.name.servix.app")

        // Fastify rejects empty POST bodies if Content-Type: application/json is set (authApi.js:46-48)
        if (original.body != null && original.body!!.contentLength() == 0L) {
            builder.removeHeader("Content-Type")
        }

        if (!token.isNullOrBlank() && !isPublicOrSecurityAuthRoute) {
            builder.header("Authorization", "Bearer $token")
        }

        chain.proceed(builder.build())
    }

    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            val path = response.request.url.encodedPath
            // Do not attempt refresh on login/register/refresh/logout/security endpoints or if we already retried
            if (path.endsWith("/auth/login") ||
                path.endsWith("/auth/register") ||
                path.endsWith("/auth/refresh") ||
                path.endsWith("/auth/logout") ||
                path.contains("/auth/security/") ||
                responseCount(response) >= 2
            ) {
                return null
            }

            synchronized(refreshLock) {
                val currentToken = tokenStore.getAccessToken()
                val requestAuthHeader = response.request.header("Authorization")
                val requestToken = requestAuthHeader?.removePrefix("Bearer ")?.trim()

                // Another thread already refreshed the token while we waited for the lock
                if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                // Perform single-flight POST /auth/refresh
                val refreshResult = executeSyncRefresh()
                return if (refreshResult != null && !refreshResult.accessToken.isNullOrBlank()) {
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${refreshResult.accessToken}")
                        .build()
                } else {
                    null
                }
            }
        }
    }

    /**
     * Single-flight synchronous token refresh using the httpOnly refresh cookie (POST /auth/refresh).
     * Returns AuthSessionResponse on 200, or null if refresh failed.
     * Only clears session and fires [onSessionExpired] when the server explicitly rejects the session (401/403),
     * preserving offline state during airplane mode / transient network failures.
     */
    fun executeSyncRefresh(): AuthSessionResponse? {
        synchronized(refreshLock) {
            val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
            // Empty body with no Content-Type header so Fastify accepts it cleanly
            val emptyBody = ByteArray(0).toRequestBody(null, 0, 0)
            val request = Request.Builder()
                .url("$baseUrl/auth/refresh")
                .post(emptyBody)
                .header("Accept", "application/json")
                .header("X-Servix-Client", "ng.name.servix.app")
                .build()

            return try {
                refreshClient.newCall(request).execute().use { res ->
                    if (res.isSuccessful) {
                        val raw = res.body?.string().orEmpty()
                        val parsed = ApiErrorParser.json.decodeFromString<AuthSessionResponse>(raw)
                        val newToken = parsed.accessToken
                        if (!newToken.isNullOrBlank()) {
                            tokenStore.setAccessToken(newToken)
                            parsed.user?.let { tokenStore.setCachedUser(it) }
                            onSessionRefreshed(parsed.user, newToken)
                            parsed
                        } else {
                            null
                        }
                    } else {
                        if (res.code == 401 || res.code == 403) {
                            tokenStore.clear()
                            cookieJar.clear()
                            onSessionExpired()
                        }
                        null
                    }
                }
            } catch (_: Exception) {
                // Network error (e.g. airplane mode): do NOT wipe local session
                null
            }
        }
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(servixUserAgentInterceptor)
        .addInterceptor(authHeaderInterceptor)
        .authenticator(tokenAuthenticator)
        .apply {
            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                    redactHeader("Authorization")
                    redactHeader("Cookie")
                    redactHeader("Set-Cookie")
                }
                addInterceptor(logging)
            }
        }
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    val apiService: ServixApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(ApiErrorParser.json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ServixApiService::class.java)

    // AI tasks may take up to the backend's 60-second limit (web client: 75 seconds).
    val aiHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(75, TimeUnit.SECONDS).callTimeout(75, TimeUnit.SECONDS).build()

    val aiApiService: ServixApiService = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(aiHttpClient)
        .addConverterFactory(ApiErrorParser.json.asConverterFactory("application/json".toMediaType()))
        .build().create(ServixApiService::class.java)

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
