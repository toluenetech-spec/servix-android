package ng.name.servix.app.data.model

import kotlin.math.roundToLong
import kotlinx.serialization.Serializable

/**
 * Bound to GET /account/overview (reference/api-routes/account.ts:127-130 &
 * reference/web/src/pages/DashboardPage.jsx:41,57-75).
 */
@Serializable
data class AccountOverviewDto(
    val kind: String = "customer",
    val emailVerified: Boolean = false,
    val plan: String = "free",
    val canManageServices: Boolean = false,
    val needsChoice: Boolean = false,
    val applicationStatus: String? = null,
    val kycStatus: String? = null,
)

/**
 * Bound to GET /account/insights (reference/api-routes/account.ts:113-126):
 * {
 *   statusCounts: Record<string, number>,
 *   monthly: Array<{ month: string, spent: number, refunded: number }>,
 *   upcoming: Array<{ id: string, reference: string, serviceTitle: string, scheduledAt: string, status: string }>,
 *   pendingReviews: number
 * }
 */
@Serializable
data class AccountInsightsDto(
    val statusCounts: Map<String, Int> = emptyMap(),
    val monthly: List<MonthlySpendDto> = emptyList(),
    val upcoming: List<UpcomingBookingBriefDto> = emptyList(),
    val pendingReviews: Int = 0,
) {
    val totalSpentNaira: Long
        get() = monthly.sumOf { it.spent }.roundToLong()

    val totalRefundedNaira: Long
        get() = monthly.sumOf { it.refunded }.roundToLong()
}

@Serializable
data class MonthlySpendDto(
    val month: String = "",
    val spent: Double = 0.0,
    val refunded: Double = 0.0,
)

@Serializable
data class UpcomingBookingBriefDto(
    val id: String = "",
    val reference: String = "",
    val serviceTitle: String = "",
    val scheduledAt: String = "",
    val status: String = "",
)

/**
 * Bound to GET /account/security-summary (reference/api-routes/account.ts:30-39):
 * { method: "totp" | "passkey" | null, recoveryCodesRemaining: number, passkeys: number }
 */
@Serializable
data class SecuritySummaryDto(
    val method: String? = null,
    val recoveryCodesRemaining: Int = 0,
    val passkeys: Int = 0,
)

/**
 * Bound to GET /account/payments (reference/api-routes/account.ts:49-53):
 * {
 *   mode: "test" | "live",
 *   records: Array<{
 *     id: string,
 *     reference: string,
 *     amount: number,
 *     currency: string,
 *     status: string,
 *     createdAt: string,
 *     booking: { id: string, reference: string, serviceTitle: string }
 *   }>
 * }
 */
@Serializable
data class AccountPaymentsResponse(
    val mode: String = "test",
    val records: List<PaymentRecordDto> = emptyList(),
)

@Serializable
data class PaymentRecordDto(
    val id: String = "",
    val reference: String = "",
    val amount: Double = 0.0,
    val currency: String = "NGN",
    val status: String = "",
    val createdAt: String = "",
    val booking: PaymentBookingBriefDto = PaymentBookingBriefDto(),
) {
    val amountNaira: Long
        get() = amount.roundToLong()
}

@Serializable
data class PaymentBookingBriefDto(
    val id: String = "",
    val reference: String = "",
    val serviceTitle: String = "",
)

// GET /account/device: reference/api-routes/account.ts:40-46 (API 0.9.4).
@kotlinx.serialization.Serializable
data class AccountDeviceDto(
    val ip: String? = null,
    val device: String? = null,
    val os: String? = null,
    val client: String? = null,
    val userAgent: String? = null,
    val seenAt: String? = null,
)

/** PATCH /account/profile (api account.ts:14-31): at least one field, fullName 2-200 chars. */
@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val avatarUrl: String? = null,
)

/** POST /uploads?kind=avatar&fileName=… (api/src/routes/uploads.ts:67-91) -> { url, key, … }. */
@Serializable
data class UploadAck(
    val url: String = "",
    val key: String = "",
    val kind: String = "",
)
