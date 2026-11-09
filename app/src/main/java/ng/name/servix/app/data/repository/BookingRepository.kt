package ng.name.servix.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.CancelBookingRequest
import ng.name.servix.app.data.model.CreateBookingRequest
import ng.name.servix.app.data.model.DisputeBookingRequest
import ng.name.servix.app.data.model.PayBookingResponse
import ng.name.servix.app.data.model.RebookDraftDto
import ng.name.servix.app.data.model.ServiceAvailabilityResponse
import ng.name.servix.app.data.model.SubmitReviewRequest
import ng.name.servix.app.data.model.SubmitReviewResponse
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.ServixApiService

class BookingRepository(
    private val apiProvider: () -> ServixApiService,
) {
    private val api: ServixApiService
        get() = apiProvider()

    suspend fun getServiceAvailability(
        serviceSlug: String,
        days: Int = 14,
    ): Result<ServiceAvailabilityResponse> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.getServiceAvailability(serviceSlug, days))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun createBooking(
        serviceId: String,
        scheduledAt: String,
        notes: String? = null,
        rebookedFrom: String? = null,
        idempotencyKey: String? = null,
    ): Result<BookingDto> = withContext(Dispatchers.IO) {
        try {
            val cleanNotes = notes?.trim()?.takeIf { it.isNotEmpty() }?.take(2000)
            val body = CreateBookingRequest(
                serviceId = serviceId,
                scheduledAt = scheduledAt,
                notes = cleanNotes,
                rebookedFrom = rebookedFrom?.trim()?.takeIf { it.isNotEmpty() },
            )
            Result.success(api.createBooking(body = body, idempotencyKey = idempotencyKey))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun payBooking(id: String): Result<PayBookingResponse> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.payBooking(id))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun getProfessionalBookings(): Result<List<BookingDto>> = withContext(Dispatchers.IO) {
        try { Result.success(api.getProfessionalBookings()) }
        catch (t: Throwable) { Result.failure(ApiErrorParser.toServixException(t)) }
    }

    suspend fun getProfessionalAnalytics(): Result<ng.name.servix.app.data.model.ProfessionalAnalyticsDto> = withContext(Dispatchers.IO) {
        try { Result.success(api.getProfessionalAnalytics(7)) }
        catch (t: Throwable) { Result.failure(ApiErrorParser.toServixException(t)) }
    }

    suspend fun getOwnServices(): Result<List<ng.name.servix.app.data.model.OwnServiceDto>> = withContext(Dispatchers.IO) {
        try { Result.success(api.getOwnServices()) }
        catch (t: Throwable) { Result.failure(ApiErrorParser.toServixException(t)) }
    }

    suspend fun performProfessionalAction(id: String, action: String): Result<BookingDto> = withContext(Dispatchers.IO) {
        try {
            Result.success(when (action) {
                "accept" -> api.acceptProfessionalBooking(id)
                "decline" -> api.declineProfessionalBooking(id)
                "start" -> api.startProfessionalBooking(id)
                "deliver" -> api.deliverProfessionalBooking(id)
                else -> throw IllegalArgumentException("Unsupported booking action")
            })
        } catch (t: Throwable) { Result.failure(ApiErrorParser.toServixException(t)) }
    }

    suspend fun getMyBookings(): Result<List<BookingDto>> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.getMyBookings())
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun getBooking(id: String): Result<BookingDto> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.getBooking(id))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun cancelBooking(id: String, reason: String? = null): Result<BookingDto> =
        withContext(Dispatchers.IO) {
            try {
                val cleanReason = reason?.trim()?.takeIf { it.isNotEmpty() }?.take(1000)
                Result.success(api.cancelBooking(id, CancelBookingRequest(reason = cleanReason)))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun confirmBooking(id: String): Result<BookingDto> = withContext(Dispatchers.IO) {
        try {
            Result.success(api.confirmBooking(id))
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun disputeBooking(id: String, reason: String): Result<BookingDto> =
        withContext(Dispatchers.IO) {
            try {
                val cleanReason = reason.trim().take(2000)
                Result.success(api.disputeBooking(id, DisputeBookingRequest(reason = cleanReason)))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }

    suspend fun reviewBooking(
        id: String,
        rating: Int,
        text: String? = null,
    ): Result<SubmitReviewResponse> = withContext(Dispatchers.IO) {
        try {
            val cleanText = text?.trim()?.takeIf { it.isNotEmpty() }?.take(2000)
            Result.success(
                api.reviewBooking(
                    id = id,
                    body = SubmitReviewRequest(rating = rating, text = cleanText),
                )
            )
        } catch (t: Throwable) {
            Result.failure(ApiErrorParser.toServixException(t))
        }
    }

    suspend fun getRebookDraft(id: String): Result<RebookDraftDto> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getRebookDraft(id))
            } catch (t: Throwable) {
                Result.failure(ApiErrorParser.toServixException(t))
            }
        }
}
