package ng.name.servix.app.data.network

import ng.name.servix.app.data.model.AuthSessionResponse
import ng.name.servix.app.data.model.ForgotPasswordRequest
import ng.name.servix.app.data.model.LoginRequest
import ng.name.servix.app.data.model.MeResponse
import ng.name.servix.app.data.model.OkResponse
import ng.name.servix.app.data.model.RegisterRequest
import ng.name.servix.app.data.model.ResendVerificationResponse
import ng.name.servix.app.data.model.VerificationMethodResponse
import ng.name.servix.app.data.model.VerifyEmailCodeRequest
import ng.name.servix.app.data.model.VerifyEmailResponse
import ng.name.servix.app.data.model.VerifyEmailTokenRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ServixApiService {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthSessionResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthSessionResponse

    @POST("auth/refresh")
    suspend fun refresh(): AuthSessionResponse

    @POST("auth/logout")
    suspend fun logout(): OkResponse

    @GET("me")
    suspend fun getMe(): MeResponse

    @GET("auth/verification-method")
    suspend fun getVerificationMethod(): VerificationMethodResponse

    @POST("auth/verify-email/code")
    suspend fun verifyEmailCode(@Body body: VerifyEmailCodeRequest): VerifyEmailResponse

    @POST("auth/verify-email")
    suspend fun verifyEmailToken(@Body body: VerifyEmailTokenRequest): VerifyEmailResponse

    @POST("auth/verify-email/resend")
    suspend fun resendVerification(): ResendVerificationResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): OkResponse
}
