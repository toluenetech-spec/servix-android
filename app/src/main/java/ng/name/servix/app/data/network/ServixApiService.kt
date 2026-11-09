package ng.name.servix.app.data.network

import ng.name.servix.app.data.model.AuthSessionResponse
import ng.name.servix.app.data.model.AvailabilitySummaryDto
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.CancelBookingRequest
import ng.name.servix.app.data.model.CategoryDto
import ng.name.servix.app.data.model.CountViewRequest
import ng.name.servix.app.data.model.CreateBookingRequest
import ng.name.servix.app.data.model.DisputeBookingRequest
import ng.name.servix.app.data.model.ForgotPasswordRequest
import ng.name.servix.app.data.model.ForgotPasswordResponse
import ng.name.servix.app.data.model.LoginRequest
import ng.name.servix.app.data.model.MeResponse
import ng.name.servix.app.data.model.MarkNotificationsReadRequest
import ng.name.servix.app.data.model.MarkNotificationsReadResponse
import ng.name.servix.app.data.model.NotificationListResponse
import ng.name.servix.app.data.model.OAuthConfigResponse
import ng.name.servix.app.data.model.OAuthNativeExchangeRequest
import ng.name.servix.app.data.model.OkResponse
import ng.name.servix.app.data.model.UpdateProfileRequest
import ng.name.servix.app.data.model.UnreadCountResponse
import ng.name.servix.app.data.model.PayBookingResponse
import ng.name.servix.app.data.model.ProfessionalDetailDto
import ng.name.servix.app.data.model.ProfessionalsPageResponse
import ng.name.servix.app.data.model.RebookDraftDto
import ng.name.servix.app.data.model.RegisterRequest
import ng.name.servix.app.data.model.RemoveSavedResponse
import ng.name.servix.app.data.model.ResendVerificationResponse
import ng.name.servix.app.data.model.ReviewDto
import ng.name.servix.app.data.model.SaveProfessionalRequest
import ng.name.servix.app.data.model.SavedProfessionalItemDto
import ng.name.servix.app.data.model.SecurityCodeRequest
import ng.name.servix.app.data.model.SecurityConfigResponse
import ng.name.servix.app.data.model.SecurityFinishRequest
import ng.name.servix.app.data.model.SecurityFlowState
import ng.name.servix.app.data.model.SecurityRecoveryCodesResponse
import ng.name.servix.app.data.model.SecurityResetPasswordRequest
import ng.name.servix.app.data.model.SecurityTotpSetupResponse
import ng.name.servix.app.data.model.ServiceAvailabilityResponse
import ng.name.servix.app.data.model.ServiceDetailDto
import ng.name.servix.app.data.model.ServiceSummaryDto
import ng.name.servix.app.data.model.ServicesPageResponse
import ng.name.servix.app.data.model.StatsDto
import ng.name.servix.app.data.model.SubmitReviewRequest
import ng.name.servix.app.data.model.SubmitReviewResponse
import ng.name.servix.app.data.model.VerificationMethodResponse
import ng.name.servix.app.data.model.VerifyEmailCodeRequest
import ng.name.servix.app.data.model.VerifyEmailResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ServixApiService {
    // reference/api-routes/ai.ts:104-108,147-152,185-190; aiFeedback.ts:43-78.
    @POST("ai/assistant")
    suspend fun aiAssistant(@Body request: ng.name.servix.app.data.model.AiChatRequest): ng.name.servix.app.data.model.AiAnswer
    @GET("ai/usage")
    suspend fun aiUsage(): ng.name.servix.app.data.model.AiUsage
    @POST("ai/pricing/guidance")
    suspend fun aiPricing(@Body request: ng.name.servix.app.data.model.AiPricingRequest): ng.name.servix.app.data.model.AiPricingResponse
    @POST("ai/feedback")
    suspend fun aiFeedback(@Body request: ng.name.servix.app.data.model.AiFeedbackRequest): Response<Unit>

    // reference/api-routes/account.ts:40-46: device summary for the current request.
    @GET("auth/oauth/config")
    suspend fun getOAuthConfig(): OAuthConfigResponse

    @POST("auth/oauth/native/exchange")
    suspend fun exchangeOAuthTicket(@Body body: OAuthNativeExchangeRequest): AuthSessionResponse

    @GET("account/notifications")
    suspend fun getNotifications(
        @Query("before") before: String? = null,
        @Query("unread") unread: String? = null,
    ): NotificationListResponse

    @GET("account/notifications/unread")
    suspend fun getUnreadNotificationCount(): UnreadCountResponse

    @POST("account/notifications/read")
    suspend fun markNotificationsRead(@Body body: MarkNotificationsReadRequest): MarkNotificationsReadResponse

    @POST("uploads")
    suspend fun uploadMedia(
        @Query("kind") kind: String,
        @Query("fileName") fileName: String,
        @Header("Content-Type") contentType: String,
        @Body body: okhttp3.RequestBody,
    ): ng.name.servix.app.data.model.UploadAck

    @PATCH("account/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): OkResponse

    @GET("billing/plan")
    suspend fun billingPlan(): kotlinx.serialization.json.JsonElement

    @GET("me/entitlements")
    suspend fun entitlements(): kotlinx.serialization.json.JsonElement

    @GET("account/device")
    suspend fun getAccountDevice(): ng.name.servix.app.data.model.AccountDeviceDto

    /* -------- Stage 1: Auth & Session -------- */

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

    @POST("auth/verify-email/resend")
    suspend fun resendVerification(): ResendVerificationResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): ForgotPasswordResponse

    /* -------- Stage 1: Security / MFA routes (security.ts / securityFlow.ts) -------- */

    @GET("auth/security/config")
    suspend fun getSecurityConfig(): SecurityConfigResponse

    @POST("auth/security/status")
    suspend fun securityStatus(): SecurityFlowState

    @POST("auth/security/email/verify")
    suspend fun securityEmailVerify(@Body body: SecurityCodeRequest): SecurityFlowState

    @POST("auth/security/email/resend")
    suspend fun securityEmailResend(): SecurityFlowState

    @POST("auth/security/totp/setup")
    suspend fun securityTotpSetup(): SecurityTotpSetupResponse

    @POST("auth/security/totp/enroll")
    suspend fun securityTotpEnroll(@Body body: SecurityCodeRequest): SecurityFlowState

    @POST("auth/security/totp/verify")
    suspend fun securityTotpVerify(@Body body: SecurityCodeRequest): SecurityFlowState

    @POST("auth/security/recovery/codes")
    suspend fun securityRecoveryCodes(): SecurityRecoveryCodesResponse

    @POST("auth/security/recovery/verify")
    suspend fun securityRecoveryVerify(@Body body: SecurityCodeRequest): SecurityFlowState

    @POST("auth/security/finish")
    suspend fun securityFinish(@Body body: SecurityFinishRequest): AuthSessionResponse

    @POST("auth/security/reset-password")
    suspend fun securityResetPassword(@Body body: SecurityResetPasswordRequest): OkResponse

    @POST("auth/security/cancel")
    suspend fun securityCancel(): OkResponse

    /* -------- Stage 2: Catalogue (categories, stats, professionals, services) -------- */

    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("stats")
    suspend fun getStats(): StatsDto

    @GET("professionals")
    suspend fun getProfessionals(
        @Query("q") q: String? = null,
        @Query("category") category: String? = null,
        @Query("location") location: String? = null,
        @Query("minRating") minRating: Double? = null,
        @Query("maxPrice") maxPrice: Int? = null,
        @Query("availability") availability: String? = null,
        @Query("available") available: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
    ): ProfessionalsPageResponse

    @GET("professionals/{slug}")
    suspend fun getProfessional(
        @Path("slug") slug: String,
    ): ProfessionalDetailDto

    @GET("professionals/{slug}/services")
    suspend fun getProfessionalServices(
        @Path("slug") slug: String,
    ): List<ServiceSummaryDto>

    @GET("professionals/{slug}/reviews")
    suspend fun getProfessionalReviews(
        @Path("slug") slug: String,
    ): List<ReviewDto>

    @GET("professionals/{slug}/availability/summary")
    suspend fun getProfessionalAvailabilitySummary(
        @Path("slug") slug: String,
    ): AvailabilitySummaryDto

    @GET("services")
    suspend fun getServices(
        @Query("q") q: String? = null,
        @Query("category") category: String? = null,
        @Query("minPrice") minPrice: Int? = null,
        @Query("maxPrice") maxPrice: Int? = null,
        @Query("minRating") minRating: Double? = null,
        @Query("location") location: String? = null,
        @Query("availability") availability: String? = null,
        @Query("available") available: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
    ): ServicesPageResponse

    @GET("services/{slug}")
    suspend fun getService(
        @Path("slug") slug: String,
    ): ServiceDetailDto

    @GET("services/{slug}/reviews")
    suspend fun getServiceReviews(
        @Path("slug") slug: String,
    ): List<ReviewDto>

    @GET("services/{slug}/availability")
    suspend fun getServiceAvailability(
        @Path("slug") slug: String,
        @Query("days") days: Int = 14,
    ): ServiceAvailabilityResponse

    @POST("views")
    suspend fun countView(
        @Body body: CountViewRequest,
    ): Response<Unit>

    /* -------- Saved professionals -------- */

    @GET("account/saved")
    suspend fun getSavedProfessionals(): List<SavedProfessionalItemDto>

    @POST("account/saved")
    suspend fun saveProfessional(
        @Body body: SaveProfessionalRequest,
    ): SavedProfessionalItemDto

    @DELETE("account/saved/{slug}")
    suspend fun unsaveProfessional(
        @Path("slug") slug: String,
    ): RemoveSavedResponse

    /* -------- Stage 3: Bookings & Paystack Payment -------- */

    @POST("bookings")
    suspend fun createBooking(
        @Body body: CreateBookingRequest,
        @Header("Idempotency-Key") idempotencyKey: String? = null,
    ): BookingDto

    @POST("bookings/{id}/pay")
    suspend fun payBooking(
        @Path("id") id: String,
    ): PayBookingResponse

    @GET("bookings")
    suspend fun getMyBookings(): List<BookingDto>

    @GET("bookings/{id}")
    suspend fun getBooking(
        @Path("id") id: String,
    ): BookingDto

    @POST("bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Path("id") id: String,
        @Body body: CancelBookingRequest = CancelBookingRequest(),
    ): BookingDto

    @POST("bookings/{id}/confirm")
    suspend fun confirmBooking(
        @Path("id") id: String,
    ): BookingDto

    @POST("bookings/{id}/dispute")
    suspend fun disputeBooking(
        @Path("id") id: String,
        @Body body: DisputeBookingRequest,
    ): BookingDto

    @POST("bookings/{id}/review")
    suspend fun reviewBooking(
        @Path("id") id: String,
        @Body body: SubmitReviewRequest,
    ): SubmitReviewResponse

    @GET("bookings/{id}/rebook")
    suspend fun getRebookDraft(
        @Path("id") id: String,
    ): RebookDraftDto

    /* -------- Dashboard & Account (reference/api-routes/account.ts) -------- */

    @GET("account/overview")
    suspend fun getAccountOverviewRaw(): kotlinx.serialization.json.JsonElement

    @GET("account/insights")
    suspend fun getAccountInsightsRaw(): kotlinx.serialization.json.JsonElement

    @GET("account/security-summary")
    suspend fun getAccountSecuritySummaryRaw(): kotlinx.serialization.json.JsonElement

    @GET("account/payments")
    suspend fun getAccountPaymentsRaw(): kotlinx.serialization.json.JsonElement

    /* ---------------------------------------------------------------- Release 1.1: chat + flags */

    /** `GET /features` (marketplace.ts:25 → features.ts featureSnapshot). */
    @GET("features")
    suspend fun getFeatures(): ng.name.servix.app.data.model.FeaturesDto

    /** `GET /community/threads` — a bare array, newest activity first (community.ts:100-115). */
    @GET("community/threads")
    suspend fun getChatThreads(): List<ng.name.servix.app.data.model.ChatThreadDto>

    /** `GET /community/threads/:id/messages?before=<id>` — newest 50, reversed by the API. */
    @GET("community/threads/{id}/messages")
    suspend fun getChatMessages(
        @Path("id") threadId: String,
        @Query("before") before: String? = null,
    ): ng.name.servix.app.data.model.ChatMessagesResponse

    /** `POST /community/threads { bookingId | connectionId }` → `{ id }`. */
    @POST("community/threads")
    suspend fun createChatThread(
        @Body body: ng.name.servix.app.data.model.CreateChatThreadRequest,
    ): ng.name.servix.app.data.model.CreateChatThreadResponse

    /** `POST /community/threads/:id/messages` → the saved message (idempotent on `senderId + clientId`). */
    @POST("community/threads/{id}/messages")
    suspend fun sendChatMessage(
        @Path("id") threadId: String,
        @Body body: ng.name.servix.app.data.model.SendChatMessageRequest,
    ): ng.name.servix.app.data.model.ChatMessageDto

    @POST("community/threads/{id}/read")
    suspend fun markChatThreadRead(@Path("id") threadId: String): OkResponse

    @POST("community/threads/{id}/delivered")
    suspend fun markChatThreadDelivered(@Path("id") threadId: String): OkResponse

    /** `POST /community/threads/:id/block` — used from the contact card. */
    @POST("community/threads/{id}/block")
    suspend fun blockChatThread(@Path("id") threadId: String): OkResponse

    /**
     * `POST /uploads?kind=chat-image|chat-audio|chat-file&fileName=<name>` — the raw file is the body
     * and its real `Content-Type` is required (`api/src/routes/uploads.ts:79-99`).
     */
    @POST("uploads")
    suspend fun uploadChatFile(
        @Query("kind") kind: String,
        @Query("fileName") fileName: String,
        @Header("Content-Type") contentType: String,
        @Body body: okhttp3.RequestBody,
    ): ng.name.servix.app.data.model.UploadedFile

    /** `POST /community/threads/:id/messages/:mid/react { emoji }` → the full, updated message. */
    @POST("community/threads/{id}/messages/{mid}/react")
    suspend fun reactToChatMessage(
        @Path("id") threadId: String,
        @Path("mid") messageId: String,
        @Body body: ng.name.servix.app.data.model.ChatReactionRequest,
    ): ng.name.servix.app.data.model.ChatMessageDto

    /** `DELETE /community/threads/:id/messages/:mid` — sender only; returns the tombstone. */
    @DELETE("community/threads/{id}/messages/{mid}")
    suspend fun deleteChatMessage(
        @Path("id") threadId: String,
        @Path("mid") messageId: String,
    ): ng.name.servix.app.data.model.ChatMessageDto

    @POST("community/threads/{id}/typing")
    suspend fun sendChatTyping(
        @Path("id") threadId: String,
        @Body body: ng.name.servix.app.data.model.ChatTypingRequest,
    ): OkResponse
}
