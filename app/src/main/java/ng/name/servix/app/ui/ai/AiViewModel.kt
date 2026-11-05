package ng.name.servix.app.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.data.model.*
import ng.name.servix.app.data.repository.AiRepository
import ng.name.servix.app.data.repository.CatalogueRepository

data class ChatTurn(
    val id: Long, val role: String, val content: String, val prompt: String = "", val model: String? = null,
    val feedback: String = "", val feedbackError: String? = null,
)
data class AiUiState(
    val ownerId: String? = null,
    val turns: List<ChatTurn> = emptyList(), val input: String = "", val streaming: String = "", val busy: Boolean = false,
    val chatError: ServixApiException? = null, val usage: AiUsage? = null, val usageError: String? = null,
    val categories: List<CategoryDto> = emptyList(), val categoriesError: String? = null,
    val category: String = "", val brief: String = "", val days: String = "", val pricingBusy: Boolean = false,
    val pricing: AiPricingResponse? = null, val pricingError: ServixApiException? = null,
)

class AiViewModel(private val repository: AiRepository, private val catalogue: CatalogueRepository) : ViewModel() {
    private val _state = MutableStateFlow(AiUiState())
    val state: StateFlow<AiUiState> = _state.asStateFlow()
    private var userId: String? = null
    private var chatJob: Job? = null
    private var pricingJob: Job? = null
    private var initialization: Job? = null
    private val feedbackJobs = mutableListOf<Job>()
    private val usageJobs = mutableListOf<Job>()
    private var nextId = 0L

    fun setUser(id: String?) {
        if (id == userId) return
        chatJob?.cancel(); pricingJob?.cancel(); initialization?.cancel(); feedbackJobs.forEach { it.cancel() }; feedbackJobs.clear(); usageJobs.forEach { it.cancel() }; usageJobs.clear()
        userId = id
        _state.value = AiUiState(ownerId = id)
        if (id != null) reload()
    }
    fun reload() {
        if (userId == null) return
        initialization?.cancel()
        initialization = viewModelScope.launch {
            val usage = repository.usage()
            _state.update { it.copy(usage = usage.getOrNull(), usageError = usage.exceptionOrNull()?.message) }
            val cats = catalogue.getCategories()
            currentCoroutineContext().ensureActive()
            _state.update { old -> old.copy(categories = cats.getOrNull().orEmpty(), categoriesError = cats.exceptionOrNull()?.message, category = old.category.ifBlank { cats.getOrNull()?.firstOrNull()?.id.orEmpty() }) }
        }
    }
    fun updateInput(text: String) { _state.update { it.copy(input = text.take(4000)) } }
    fun selectCategory(id: String) { _state.update { it.copy(category = id, pricing = null, pricingError = null) } }
    fun updateBrief(text: String) { _state.update { it.copy(brief = text.take(1500)) } }
    fun updateDays(text: String) { if (text.length <= 3 && text.all(Char::isDigit)) _state.update { it.copy(days = text) } }

    fun send(text: String = _state.value.input) {
        val prompt = text.trim().take(4000)
        if (userId == null || prompt.isEmpty() || _state.value.busy) return
        val newTurn = ChatTurn(++nextId, "user", prompt)
        val turns = (_state.value.turns + newTurn).takeLast(20)
        _state.update { it.copy(turns = turns, input = "", streaming = "", busy = true, chatError = null) }
        chatJob = viewModelScope.launch {
            val result = repository.ask(turns.map { AiMessage(it.role, it.content.take(4000)) }) { delta ->
                _state.update { it.copy(streaming = it.streaming + delta) }
            }
            result.fold(onSuccess = { response ->
                val answer = ChatTurn(++nextId, "assistant", response.answer, prompt, response.ai?.model)
                _state.update { it.copy(turns = (it.turns + answer).takeLast(20), streaming = "", busy = false) }
                refreshUsage()
            }, onFailure = { error ->
                _state.update { it.copy(streaming = "", busy = false, chatError = asError(error)) }
            })
        }
    }
    fun retry() {
        val last = _state.value.turns.lastOrNull()?.takeIf { it.role == "user" } ?: return
        _state.update { it.copy(turns = it.turns.dropLast(1), chatError = null) }
        send(last.content)
    }
    fun stop() {
        chatJob?.cancel()
        _state.update { it.copy(busy = false, streaming = "", turns = if (it.busy && it.turns.lastOrNull()?.role == "user") it.turns.dropLast(1) else it.turns) }
    }
    fun stopAll() { stop(); pricingJob?.cancel(); _state.update { it.copy(pricingBusy = false) } }
    fun getPricing() {
        val current = _state.value
        if (userId == null || current.pricingBusy || current.category.isBlank()) return
        val days = current.days.takeIf { it.isNotBlank() }?.toIntOrNull()
        if (current.days.isNotBlank() && (days == null || days !in 1..365)) {
            _state.update { it.copy(pricingError = ServixApiException(422, "VALIDATION_ERROR", "Delivery days must be between 1 and 365.")) }; return
        }
        _state.update { it.copy(pricingBusy = true, pricingError = null, pricing = null) }
        pricingJob = viewModelScope.launch {
            val result = repository.pricing(AiPricingRequest(current.category, current.brief.trim().takeIf { it.isNotEmpty() }, days))
            result.fold(onSuccess = { response ->
                _state.update { it.copy(pricing = response, pricingBusy = false) }; refreshUsage()
            }, onFailure = { e -> _state.update { it.copy(pricingBusy = false, pricingError = asError(e)) } })
        }
    }
    fun feedback(turnId: Long, rating: String, comment: String? = null) {
        val turn = _state.value.turns.find { it.id == turnId && it.role == "assistant" } ?: return
        if (userId == null || turn.feedback == "sending" || turn.feedback == "sent" || rating !in setOf("up", "down")) return
        updateTurn(turnId) { it.copy(feedback = "sending", feedbackError = null) }
        feedbackJobs += viewModelScope.launch {
            repository.feedback(feedbackRequest(rating, turn.prompt, turn.content, comment, turn.model)).fold(
                onSuccess = { updateTurn(turnId) { it.copy(feedback = "sent") } },
                onFailure = { e -> updateTurn(turnId) { it.copy(feedback = "", feedbackError = e.message) } },
            )
        }
    }
    private fun updateTurn(id: Long, transform: (ChatTurn) -> ChatTurn) { _state.update { state -> state.copy(turns = state.turns.map { if (it.id == id) transform(it) else it }) } }
    private fun refreshUsage() {
        val owner = userId ?: return
        usageJobs += viewModelScope.launch {
            repository.usage().getOrNull()?.let { meter ->
                if (userId == owner) _state.update { it.copy(usage = meter, usageError = null) }
            }
        }
    }
    private fun asError(e: Throwable): ServixApiException = e as? ServixApiException ?: ServixApiException(0, message = e.message ?: "We couldn't reach Servix. Please try again.")
    companion object {
        fun factory(repo: AiRepository, catalogue: CatalogueRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AiViewModel(repo, catalogue) as T
        }
    }
}
