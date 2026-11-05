package ng.name.servix.app.data.model

import kotlinx.serialization.Serializable

/**
 * Bound to POST /bookings request body (reference/api-routes/bookings.ts:32-37):
 * { serviceId: <service slug>, scheduledAt: ISO-8601, notes?: string <= 2000, rebookedFrom?: <booking uuid> }
 */
@Serializable
data class CreateBookingRequest(
    val serviceId: String,
    val scheduledAt: String,
    val notes: String? = null,
    val rebookedFrom: String? = null,
)

/**
 * Bound to POST /bookings/:id/pay response (reference/api-routes/bookings.ts:241, 254):
 * { authorizationUrl, reference }
 */
@Serializable
data class PayBookingResponse(
    val authorizationUrl: String,
    val reference: String,
)

/**
 * Bound to POST /bookings/:id/cancel request body (reference/api-routes/bookings.ts:39):
 * { reason?: string <= 1000 }
 */
@Serializable
data class CancelBookingRequest(
    val reason: String? = null,
)

/**
 * Bound to POST /bookings/:id/dispute request body (reference/api-routes/bookings.ts:40):
 * { reason: string 10..2000 }
 */
@Serializable
data class DisputeBookingRequest(
    val reason: String,
)

/**
 * Bound to POST /bookings/:id/review request body (reference/api-routes/bookings.ts:41-44):
 * { rating: 1..5, text?: string <= 2000 }
 */
@Serializable
data class SubmitReviewRequest(
    val rating: Int,
    val text: String? = null,
)

/**
 * Bound to POST /bookings/:id/review 201 response (reference/api-routes/bookings.ts:400):
 * { id, rating, text }
 */
@Serializable
data class SubmitReviewResponse(
    val id: String,
    val rating: Int = 5,
    val text: String = "",
)

/**
 * Bound to GET /bookings/:id/rebook response (reference/api-routes/bookings.ts:144-151):
 * {
 *   previousBookingId,
 *   previousStatus,
 *   professional: { id, name, image },
 *   services: [{ id, title, price, priceUnit, deliveryDays, wasBooked }],
 *   previousServiceAvailable,
 *   previousNotes
 * }
 */
@Serializable
data class RebookDraftDto(
    val previousBookingId: String,
    val previousStatus: String = "",
    val professional: RebookProfessionalDto,
    val services: List<RebookServiceOptionDto> = emptyList(),
    val previousServiceAvailable: Boolean = true,
    val previousNotes: String = "",
)

@Serializable
data class RebookProfessionalDto(
    val id: String,
    val name: String = "",
    val image: String? = null,
)

@Serializable
data class RebookServiceOptionDto(
    val id: String,
    val title: String = "",
    val price: Long = 0L,
    val priceUnit: String = "per project",
    val deliveryDays: Int? = null,
    val wasBooked: Boolean = false,
)
