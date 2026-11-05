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

@Serializable
data class UserDto(
    val id: String,
    val fullName: String = "",
    val email: String = "",
    val role: String = "customer",
    val status: String = "active",
    val emailVerified: Boolean = false,
    val emailVerifiedAt: String? = null,
    val avatarUrl: String? = null,
    val createdAt: String? = null,
) {
    val isEmailVerified: Boolean
        get() = emailVerified || !emailVerifiedAt.isNullOrBlank() || status == "active"

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

@Serializable
data class AuthSessionResponse(
    val user: UserDto? = null,
    val accessToken: String? = null,
    val security: JsonElement? = null,
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
data class VerifyEmailTokenRequest(
    val token: String,
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
data class OkResponse(
    val ok: Boolean = true,
)
