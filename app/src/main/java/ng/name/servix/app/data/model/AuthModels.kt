package ng.name.servix.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorBody? = null,
    val message: String? = null,
)

@Serializable
data class ApiErrorBody(
    val status: Int? = null,
    val code: String? = null,
    val message: String? = null,
    val errors: Map<String, String>? = null,
    val meta: Map<String, JsonElement>? = null,
)

class ServixApiException(
    val status: Int,
    val code: String? = null,
    override val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    val meta: Map<String, JsonElement> = emptyMap(),
    val isNetworkError: Boolean = false,
) : Exception(message)

/**
 * Exact user object returned by serializeUser() in reference/api-routes/auth.ts and GET /me:
 * { id, email, fullName, avatarUrl, role, status, kycStatus, emailVerified, createdAt }
 */
@Serializable
data class UserDto(
    val id: String,
    val email: String = "",
    val fullName: String = "",
    val avatarUrl: String? = null,
    val role: String = "customer",
    val status: String = "active",
    val kycStatus: String? = null,
    val emailVerified: Boolean = false,
    val createdAt: String? = null,
) {
    val firstName: String
        get() = fullName.trim().substringBefore(" ").ifBlank { email.substringBefore("@") }
}

@Serializable
data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

/**
 * Returned by POST /auth/register, POST /auth/login, POST /auth/refresh, and POST /auth/security/finish.
 * Contains either { user, accessToken } or { security } when the security/MFA flow is enabled.
 */
@Serializable
data class AuthSessionResponse(
    val user: UserDto? = null,
    val accessToken: String? = null,
    val security: SecurityFlowState? = null,
)

@Serializable
data class MeResponse(
    val user: UserDto,
)

@Serializable
data class VerificationMethodResponse(
    val method: String = "otp",
)

@Serializable
data class VerifyEmailCodeRequest(
    val code: String,
)

@Serializable
data class VerifyEmailResponse(
    val user: UserDto,
)

@Serializable
data class ResendVerificationResponse(
    val ok: Boolean = true,
    val alreadyVerified: Boolean = false,
    val retryAfterSeconds: Int? = null,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

@Serializable
data class ForgotPasswordResponse(
    val ok: Boolean = true,
    val security: SecurityFlowState? = null,
)

@Serializable
data class SecurityConfigResponse(
    val enabled: Boolean = false,
)

/**
 * Returned by /auth/security/status, /auth/security/email/verify, /auth/security/email/resend,
 * /auth/security/totp/enroll, /auth/security/totp/verify, /auth/security/recovery/verify,
 * and inside { security } on register/login/forgot-password.
 */
@Serializable
data class SecurityFlowState(
    val next: String = "",
    val purpose: String = "",
    val method: String? = null,
    val email: String? = null,
    val expiresAt: String? = null,
    val passwordPolicy: String? = null,
    val recoveryCodes: List<String>? = null,
    val retryAfterSeconds: Int? = null,
)

@Serializable
data class SecurityCodeRequest(
    val code: String,
)

@Serializable
data class SecurityTotpSetupResponse(
    val uri: String = "",
    val secret: String = "",
)

@Serializable
data class SecurityRecoveryCodesResponse(
    val recoveryCodes: List<String> = emptyList(),
)

@Serializable
data class SecurityFinishRequest(
    val savedRecovery: Boolean? = null,
)

@Serializable
data class SecurityResetPasswordRequest(
    val password: String,
)

@Serializable
data class OkResponse(
    val ok: Boolean = true,
)
