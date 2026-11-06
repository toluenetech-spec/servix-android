package ng.name.servix.app.data.model

import kotlinx.serialization.Serializable

// reference/api-routes/ai.ts:104-113; api/src/ai/departments.ts:80-82 at 389b778.
@Serializable data class AiMessage(val role: String, val content: String)
@Serializable data class AiChatRequest(val messages: List<AiMessage>)
@Serializable data class AiMeta(val model: String? = null, val fallbackUsed: Boolean = false, val durationMs: Long? = null, val quota: AiMeter? = null)
@Serializable data class AiAnswer(val answer: String, val ai: AiMeta? = null)
@Serializable data class AiStreamEvent(val type: String = "", val text: String? = null, val answer: String? = null, val ai: AiMeta? = null, val code: String? = null, val message: String? = null, val status: Int? = null)

// reference/api-routes/ai.ts:185-190; api/src/lib/entitlements/usage.ts:24-35 at 389b778.
@Serializable data class AiMeter(val used: Long = 0, val reserved: Long = 0, val allowed: Long? = null, val remaining: Long? = null, val percent: Double? = null, val level: String? = null, val resetAt: String? = null)
@Serializable data class AiUsage(val planLabel: String? = null, val subject: String? = null, val used: Long = 0, val reserved: Long = 0, val allowed: Long? = null, val percent: Double? = null, val level: String? = null, val resetAt: String? = null, val member: AiMeter? = null, val enabled: Boolean? = null)

// reference/api-routes/ai.ts:147-152; api/src/ai/departments.ts:248-280 at 389b778.
@Serializable data class AiPricingRequest(val categorySlug: String, val brief: String? = null, val deliveryDays: Int? = null)
@Serializable data class AiPriceStats(val count: Int = 0, val insufficient: Boolean = true, val min: Long? = null, val median: Long? = null, val max: Long? = null)
@Serializable data class AiPricingResponse(val basis: String, val summary: String, val rangeLow: Long? = null, val rangeHigh: Long? = null, val tips: List<String> = emptyList(), val stats: AiPriceStats? = null, val ai: AiMeta? = null)

// reference/api-routes/aiFeedback.ts:25-31,43-78. Upvotes must not send conversation content.
@Serializable data class AiFeedbackRequest(val rating: String, val department: String = "assistant", val comment: String? = null, val prompt: String? = null, val answer: String? = null, val modelAlias: String? = null)
fun feedbackRequest(rating: String, prompt: String, answer: String, comment: String?, model: String?): AiFeedbackRequest =
    if (rating == "down") AiFeedbackRequest(rating, comment = comment?.trim()?.take(500)?.takeIf { it.isNotEmpty() }, prompt = prompt.take(4000), answer = answer.take(12000), modelAlias = model)
    else AiFeedbackRequest(rating, modelAlias = model)
