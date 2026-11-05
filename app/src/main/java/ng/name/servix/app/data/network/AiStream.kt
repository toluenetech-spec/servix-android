package ng.name.servix.app.data.network

import java.io.BufferedReader
import kotlinx.serialization.decodeFromString
import ng.name.servix.app.data.model.AiAnswer
import ng.name.servix.app.data.model.AiStreamEvent
import ng.name.servix.app.data.model.ServixApiException

/** SSE protocol: reference/api-routes/ai.ts:67-93. Ignore heartbeats; require a terminal done. */
fun readAiEvents(reader: BufferedReader, onDelta: (String) -> Unit): AiAnswer {
    val data = mutableListOf<String>()
    var result: AiAnswer? = null
    fun dispatch() {
        if (data.isEmpty()) return
        val raw = data.joinToString("\n")
        data.clear()
        val event = try { ApiErrorParser.json.decodeFromString<AiStreamEvent>(raw) } catch (_: Exception) { return }
        when (event.type) {
            "delta" -> event.text?.let(onDelta)
            "done" -> result = event.answer?.let { AiAnswer(it, event.ai) }
            "error" -> throw ServixApiException(event.status ?: 500, event.code, event.message ?: "Servix AI is unavailable.")
        }
    }
    while (true) {
        val line = reader.readLine() ?: break
        if (line.isEmpty()) dispatch()
        else if (line.startsWith("data:")) data.add(line.substring(5).removePrefix(" "))
        if (result != null) break
    }
    if (result == null) dispatch()
    return result ?: throw ServixApiException(503, "AI_UNAVAILABLE", "The answer was cut off. Please try again.")
}
