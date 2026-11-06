package ng.name.servix.app.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromJsonElement
import ng.name.servix.app.data.model.AccountInsightsDto
import ng.name.servix.app.data.model.AccountOverviewDto
import ng.name.servix.app.data.model.AccountPaymentsResponse
import ng.name.servix.app.data.model.MarkNotificationsReadRequest
import ng.name.servix.app.data.model.MarkNotificationsReadResponse
import ng.name.servix.app.data.model.NotificationListResponse
import ng.name.servix.app.data.model.SecuritySummaryDto
import ng.name.servix.app.data.model.UnreadCountResponse
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.ServixApiService

class AccountRepository(
    private val apiProvider: () -> ServixApiService,
) {
    private val api: ServixApiService
        get() = apiProvider()

    @Volatile
    private var loggedOverviewOnce = false

    @Volatile
    private var loggedInsightsOnce = false

    @Volatile
    private var loggedSecurityOnce = false

    @Volatile
    private var loggedPaymentsOnce = false

    suspend fun getAccountOverview(): Result<AccountOverviewDto> = safeCall {
        val raw = api.getAccountOverviewRaw()
        if (!loggedOverviewOnce) {
            loggedOverviewOnce = true
            val msg = "GET /account/overview raw JSON: $raw"
            Log.i("ServixAccount", msg)
            println("SERVIX_LIVE_JSON $msg")
        }
        ApiErrorParser.json.decodeFromJsonElement<AccountOverviewDto>(raw)
    }

    suspend fun getAccountInsights(): Result<AccountInsightsDto> = safeCall {
        val raw = api.getAccountInsightsRaw()
        if (!loggedInsightsOnce) {
            loggedInsightsOnce = true
            val msg = "GET /account/insights raw JSON: $raw"
            Log.i("ServixAccount", msg)
            println("SERVIX_LIVE_JSON $msg")
        }
        ApiErrorParser.json.decodeFromJsonElement<AccountInsightsDto>(raw)
    }

    suspend fun getSecuritySummary(): Result<SecuritySummaryDto> = safeCall {
        val raw = api.getAccountSecuritySummaryRaw()
        if (!loggedSecurityOnce) {
            loggedSecurityOnce = true
            val msg = "GET /account/security-summary raw JSON: $raw"
            Log.i("ServixAccount", msg)
            println("SERVIX_LIVE_JSON $msg")
        }
        ApiErrorParser.json.decodeFromJsonElement<SecuritySummaryDto>(raw)
    }

    suspend fun getPayments(): Result<AccountPaymentsResponse> = safeCall {
        val raw = api.getAccountPaymentsRaw()
        if (!loggedPaymentsOnce) {
            loggedPaymentsOnce = true
            val msg = "GET /account/payments raw JSON: $raw"
            Log.i("ServixAccount", msg)
            println("SERVIX_LIVE_JSON $msg")
        }
        ApiErrorParser.json.decodeFromJsonElement<AccountPaymentsResponse>(raw)
    }

    suspend fun getDevice() = safeCall { api.getAccountDevice() }

    /* -------- notifications (android/reference/api-routes/account.ts:44-63) -------- */

    suspend fun getNotifications(before: String? = null): Result<NotificationListResponse> =
        safeCall { api.getNotifications(before = before) }

    suspend fun getUnreadNotificationCount(): Result<UnreadCountResponse> =
        safeCall { api.getUnreadNotificationCount() }

    suspend fun markNotificationsRead(ids: List<String>? = null, all: Boolean = false): Result<MarkNotificationsReadResponse> =
        safeCall {
            api.markNotificationsRead(
                MarkNotificationsReadRequest(ids = ids, all = if (all) true else null),
            )
        }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(block())
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }
}
