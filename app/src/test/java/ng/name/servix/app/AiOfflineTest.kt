package ng.name.servix.app

import java.io.BufferedReader
import java.io.StringReader
import kotlinx.serialization.encodeToString
import ng.name.servix.app.data.model.AiChatRequest
import ng.name.servix.app.data.model.AiMessage
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.model.feedbackRequest
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.readAiEvents
import org.junit.Assert.*
import org.junit.Test

class AiOfflineTest {
    private fun reader(text: String) = BufferedReader(StringReader(text))
    @Test fun readsDeltaAndTerminalAnswerAcrossSseFrames() {
        val chunks = mutableListOf<String>()
        val answer = readAiEvents(reader(": ping\r\n\r\ndata: {\"type\":\"start\"}\r\n\r\ndata: {\"type\":\"delta\",\"text\":\"Hi \"}\r\n\r\ndata: {\"type\":\"delta\",\"text\":\"there\"}\r\n\r\ndata: {\"type\":\"done\",\"answer\":\"Hi there\"}\r\n\r\n"), { chunks.add(it) })
        assertEquals(listOf("Hi ", "there"), chunks)
        assertEquals("Hi there", answer.answer)
    }
    @Test fun sseErrorRetainsServerCodeAndMessage() {
        try {
            readAiEvents(reader("data: {\"type\":\"error\",\"code\":\"AI_QUOTA_EXCEEDED\",\"message\":\"Quota used\",\"status\":403}\n\n")) {}
            fail("expected error")
        } catch (e: ServixApiException) { assertEquals(403, e.status); assertEquals("AI_QUOTA_EXCEEDED", e.code); assertEquals("Quota used", e.message) }
    }
    @Test fun truncatedStreamIsNotPresentedAsAnAnswer() {
        try { readAiEvents(reader("data: {\"type\":\"delta\",\"text\":\"partial\"}\n\n")) {}; fail("expected error") }
        catch (e: ServixApiException) { assertEquals("AI_UNAVAILABLE", e.code) }
    }
    @Test fun positiveFeedbackDoesNotIncludeConversation() {
        val json = ApiErrorParser.json.encodeToString(feedbackRequest("up", "private question", "private answer", "note", "model"))
        assertFalse(json.contains("private question")); assertFalse(json.contains("private answer")); assertFalse(json.contains("note"))
    }
    @Test fun negativeFeedbackIncludesConsentedConversationAndTruncatesComment() {
        val request = feedbackRequest("down", "question", "answer", "x".repeat(600), null)
        assertEquals("question", request.prompt); assertEquals("answer", request.answer); assertEquals(500, request.comment?.length)
    }
    @Test fun serializesChatRequestWithoutInventedFields() {
        val json = ApiErrorParser.json.encodeToString(AiChatRequest(listOf(AiMessage("user", "Hello"))))
        assertEquals("{\"messages\":[{\"role\":\"user\",\"content\":\"Hello\"}]}", json)
    }
}
