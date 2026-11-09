package ng.name.servix.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.data.model.AccountOverviewDto
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.CategoryDto
import ng.name.servix.app.data.model.ProfessionalSummaryDto
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.data.repository.BookingRepository
import ng.name.servix.app.data.repository.CatalogueRepository

/** One row of the "needs your attention" strip: what happened, the verb, and the booking it belongs to. */
data class HomeAttention(
    val message: String,
    val action: String,
    val bookingId: String,
)

/**
 * Plan §2 makes Home the dashboard, so a single state carries the catalogue blocks (categories,
 * recommended professionals) and the account blocks (overview, bookings). Guests simply get empty
 * account data — no second screen, no landing page.
 */
data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val categories: List<CategoryDto> = emptyList(),
    val professionals: List<ProfessionalSummaryDto> = emptyList(),
    val overview: AccountOverviewDto? = null,
    val bookings: List<BookingDto> = emptyList(),
    val loadedForUserId: String? = null,
) {
    val planName: String
        get() = overview?.plan.orEmpty().ifBlank { "free" }.replaceFirstChar { it.uppercase() }

    val recommended: List<ProfessionalSummaryDto>
        get() = professionals.take(4)

    private val activeStatuses = setOf("pending_payment", "requested", "accepted", "in_progress")

    /** Section 4: the bookings that are still going somewhere, soonest first. */
    val upcomingBookings: List<BookingDto>
        get() = bookings
            .filter { it.status in activeStatuses }
            .sortedBy { it.scheduledAt }
            .take(3)

    /** Section 3: at most three jobs the customer has to act on. */
    val attention: List<HomeAttention>
        get() = bookings.mapNotNull { booking ->
            when {
                booking.status == "pending_payment" -> HomeAttention(
                    message = "Payment due for ${booking.serviceTitle.ifBlank { "your booking" }}",
                    action = "Pay now",
                    bookingId = booking.id,
                )
                booking.status == "delivered" -> HomeAttention(
                    message = "${booking.serviceTitle.ifBlank { "Your booking" }} was delivered",
                    action = "Confirm",
                    bookingId = booking.id,
                )
                booking.status == "completed" && !booking.hasReview -> HomeAttention(
                    message = "How was ${booking.serviceTitle.ifBlank { "your booking" }}?",
                    action = "Review",
                    bookingId = booking.id,
                )
                else -> null
            }
        }.take(3)
}

class HomeViewModel(
    private val catalogueRepository: CatalogueRepository,
    private val accountRepository: AccountRepository,
    private val bookingRepository: BookingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _professionalState = MutableStateFlow(ProfessionalHomeState())
    val professionalState: StateFlow<ProfessionalHomeState> = _professionalState.asStateFlow()
    private var proLoad: kotlinx.coroutines.Job? = null
    private var proAction: kotlinx.coroutines.Job? = null
    private var proGeneration = 0

    fun clearProfessionalHome() {
        proGeneration++
        proLoad?.cancel()
        proAction?.cancel()
        proLoad = null
        proAction = null
        _professionalState.value = ProfessionalHomeState()
    }

    fun loadProfessionalHome(userId: String, forceRefresh: Boolean = false) {
        val current = _professionalState.value
        if (!forceRefresh && current.userId == userId && (!current.loading || proLoad?.isActive == true)) return
        proLoad?.cancel()
        if (current.userId != userId) {
            proGeneration++
            proAction?.cancel()
            _professionalState.value = ProfessionalHomeState(userId = userId)
        }
        else _professionalState.update { it.copy(loading = true) }
        val generation = proGeneration
        proLoad = viewModelScope.launch {
            val jobs = async { bookingRepository.getProfessionalBookings() }
            val analytics = async { bookingRepository.getProfessionalAnalytics() }
            val services = async { bookingRepository.getOwnServices() }
            val j = jobs.await(); val a = analytics.await(); val s = services.await()
            _professionalState.update { old ->
                if (old.userId != userId || generation != proGeneration) old else old.copy(
                    loading = false,
                    jobs = j.getOrNull() ?: old.jobs,
                    totals = a.getOrNull()?.totals ?: old.totals,
                    services = s.getOrNull() ?: old.services,
                    jobsError = j.exceptionOrNull()?.message,
                    analyticsError = a.exceptionOrNull()?.message,
                    servicesError = s.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun professionalBookingAction(id: String, action: String) {
        val state = _professionalState.value
        val userId = state.userId ?: return
        if (state.busyBookingId != null || !professionalActionAllowed(state.jobs.firstOrNull { it.id == id }?.status.orEmpty(), action)) return
        val generation = proGeneration
        _professionalState.update { it.copy(busyBookingId = id, actionError = null) }
        proAction = viewModelScope.launch {
            val result = bookingRepository.performProfessionalAction(id, action)
            if (_professionalState.value.userId != userId || generation != proGeneration) return@launch
            _professionalState.update { it.copy(busyBookingId = null, actionError = result.exceptionOrNull()?.message) }
            // Refresh server data even on conflict; transition responses omit included names.
            loadProfessionalHome(userId, forceRefresh = true)
        }
    }

    private var lastLoadedUserId: String? = "__uninitialized__"
    private var homeLoad: kotlinx.coroutines.Job? = null
    private var homeGeneration = 0

    /** Clear private customer data and cancel any requests when the shell identity changes. */
    fun clearCustomerHome() {
        homeGeneration++
        homeLoad?.cancel()
        homeLoad = null
        lastLoadedUserId = "__uninitialized__"
        _uiState.update { it.copy(overview = null, bookings = emptyList(), loadedForUserId = null, error = null, loading = true, refreshing = false) }
    }

    fun loadHome(userId: String?, forceRefresh: Boolean = false) {
        if (!forceRefresh && lastLoadedUserId == userId && !_uiState.value.loading && _uiState.value.error == null) {
            return
        }
        if (lastLoadedUserId != userId) clearCustomerHome() else homeLoad?.cancel()
        lastLoadedUserId = userId
        val generation = ++homeGeneration
        val signedIn = !userId.isNullOrBlank()

        homeLoad = viewModelScope.launch {
            _uiState.update {
                if (forceRefresh && (it.categories.isNotEmpty() || it.bookings.isNotEmpty())) {
                    it.copy(refreshing = true, error = null)
                } else {
                    it.copy(loading = true, error = null)
                }
            }

            val categoriesDeferred = async { catalogueRepository.getCategories(forceRefresh) }
            val professionalsDeferred = async {
                catalogueRepository.getProfessionals(sort = "rating", page = 1, pageSize = 6)
            }
            val overviewDeferred = if (signedIn) async { accountRepository.getAccountOverview() } else null
            val bookingsDeferred = if (signedIn) async { bookingRepository.getMyBookings() } else null

            val categoriesRes = categoriesDeferred.await()
            val professionalsRes = professionalsDeferred.await()
            val overviewRes = overviewDeferred?.await()
            val bookingsRes = bookingsDeferred?.await()

            if (generation != homeGeneration) return@launch
            val catalogueError = professionalsRes.exceptionOrNull()?.message
                ?: categoriesRes.exceptionOrNull()?.message
            if (professionalsRes.isFailure && categoriesRes.isFailure) {
                _uiState.update { it.copy(loading = false, refreshing = false, error = catalogueError) }
                return@launch
            }

            val accountError = if (signedIn) {
                overviewRes?.exceptionOrNull()?.message ?: bookingsRes?.exceptionOrNull()?.message
            } else {
                null
            }

            _uiState.update { current ->
                current.copy(
                    loading = false,
                    refreshing = false,
                    error = accountError,
                    categories = categoriesRes.getOrNull() ?: current.categories,
                    professionals = professionalsRes.getOrNull()?.items ?: current.professionals,
                    overview = overviewRes?.getOrNull() ?: current.overview,
                    bookings = bookingsRes?.getOrNull() ?: if (signedIn) current.bookings else emptyList(),
                    loadedForUserId = userId,
                )
            }
        }
    }

    companion object {
        fun factory(
            catalogueRepository: CatalogueRepository,
            accountRepository: AccountRepository,
            bookingRepository: BookingRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(catalogueRepository, accountRepository, bookingRepository) as T
                }
            }
    }
}
