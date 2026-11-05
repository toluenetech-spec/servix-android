package ng.name.servix.app.data.network

import kotlinx.serialization.json.Json
import ng.name.servix.app.data.model.ApiErrorEnvelope
import ng.name.servix.app.data.model.ServixApiException
import retrofit2.HttpException
import java.io.IOException

object ApiErrorParser {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun toServixException(throwable: Throwable): ServixApiException {
        if (throwable is ServixApiException) return throwable

        if (throwable is HttpException) {
            val statusCode = throwable.code()
            val rawBody = try {
                throwable.response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            return parseErrorBody(statusCode, rawBody)
        }

        if (throwable is IOException) {
            return ServixApiException(
                status = 0,
                code = "NETWORK_ERROR",
                message = "We couldn't reach Servix right now. Check your internet connection and try again.",
                isNetworkError = true,
            )
        }

        return ServixApiException(
            status = 500,
            code = "UNKNOWN_ERROR",
            message = throwable.message?.takeIf { it.isNotBlank() }
                ?: "Something went wrong. Please try again.",
        )
    }

    fun parseErrorBody(statusCode: Int, rawBody: String?): ServixApiException {
        val envelope = if (!rawBody.isNullOrBlank()) {
            try {
                json.decodeFromString<ApiErrorEnvelope>(rawBody)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        val errObj = envelope?.error
        val serverMessage = errObj?.message?.takeIf { it.isNotBlank() }
            ?: envelope?.message?.takeIf { it.isNotBlank() }

        val defaultMessage = when (statusCode) {
            400 -> "Please check your details and try again."
            401 -> "Your session has expired. Please sign in again."
            403 -> "You don't have permission to perform this action."
            404 -> "The requested item could not be found."
            409 -> "This action conflicts with the current status."
            422 -> "Please check the highlighted fields and try again."
            429 -> "Too many attempts. Please wait a minute and try again."
            503 -> "This service is temporarily unavailable. Please try again shortly."
            else -> "Request failed ($statusCode)."
        }

        val fieldErrors = errObj?.errors.orEmpty()
        val firstFieldMessage = fieldErrors.values.firstOrNull { it.isNotBlank() }

        return ServixApiException(
            status = errObj?.status ?: statusCode,
            code = errObj?.code,
            message = serverMessage ?: firstFieldMessage ?: defaultMessage,
            fieldErrors = fieldErrors,
            meta = errObj?.meta.orEmpty(),
            isNetworkError = false,
        )
    }
}
