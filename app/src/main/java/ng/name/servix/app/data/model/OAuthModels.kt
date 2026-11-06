package ng.name.servix.app.data.model

import kotlinx.serialization.Serializable

/**
 * GET /auth/oauth/config (api/src/routes/oauth.ts:25-28) returns `{ providers }` with
 * Cache-Control: no-store. Flags are server-side, so the app renders only what it is given.
 */
@Serializable
data class OAuthConfigResponse(
    val providers: List<String> = emptyList(),
)

/** POST /auth/oauth/native/exchange (api/src/routes/oauth.ts:85-90): `{ ticket }`, 32–128 chars. */
@Serializable
data class OAuthNativeExchangeRequest(
    val ticket: String,
)

/**
 * The one-time ticket the Custom Tab hands back on the app scheme
 * (`ng.name.servix.app://oauth/callback?provider=…&ticket=…`, api/src/routes/oauth.ts:74,80).
 */
data class OAuthReturn(
    val provider: String? = null,
    val ticket: String? = null,
    val error: String? = null,
)
