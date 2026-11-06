package ng.name.servix.app.data.repository

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.data.model.*
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.ServixApiService
import ng.name.servix.app.data.network.readAiEvents
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class AiRepository(private val api: ServixApiService, private val client: OkHttpClient) {
    suspend fun usage(): Result<AiUsage> = safe { api.aiUsage() }
    suspend fun pricing(request: AiPricingRequest): Result<AiPricingResponse> = safe { api.aiPricing(request) }
    suspend fun feedback(request: AiFeedbackRequest): Result<Unit> = safe {
        val response = api.aiFeedback(request)
        if (!response.isSuccessful) throw ApiErrorParser.parseErrorBody(response.code(), response.errorBody()?.string())
    }
    suspend fun ask(messages: List<AiMessage>, onDelta: (String) -> Unit): Result<AiAnswer> = safe {
        val body = AiChatRequest(messages.takeLast(20))
        withTimeoutOrNull(75_000) {
            try { stream(body, onDelta) }
            catch (e: StreamNotFound) { api.aiAssistant(body) }
        } ?: throw ServixApiException(504, "AI_TIMEOUT", "Servix AI took too long. Please try again.")
    }
    private class StreamNotFound : Exception()

    // Uses the same authentication, refresh, cookies and client headers as the rest of the app.
    private suspend fun stream(body: AiChatRequest, onDelta: (String) -> Unit): AiAnswer = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(BuildConfig.API_BASE_URL + "ai/assistant/stream")
            .header("Accept", "text/event-stream")
            .post(ApiErrorParser.json.encodeToString(body).toRequestBody("application/json".toMediaType())).build()
        val call = client.newCall(request)
        call.timeout().timeout(75, TimeUnit.SECONDS)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val answer = response.use { res ->
                        if (res.code == 404) throw StreamNotFound()
                        if (!res.isSuccessful) throw ApiErrorParser.parseErrorBody(res.code, res.body?.string())
                        val responseBody = res.body ?: throw IOException("Empty response")
                        if (res.header("Content-Type")?.contains("text/event-stream", ignoreCase = true) == true) {
                            readAiEvents(responseBody.charStream().buffered()) { if (continuation.isActive) onDelta(it) }
                        } else ApiErrorParser.json.decodeFromString<AiAnswer>(responseBody.string())
                    }
                    if (continuation.isActive) continuation.resume(answer)
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
        })
    }
    private suspend fun <T> safe(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) { throw e }
      catch (e: Exception) { Result.failure(ApiErrorParser.toServixException(e)) }
}
