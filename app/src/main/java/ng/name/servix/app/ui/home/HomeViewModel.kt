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

    private var lastLoadedUserId: String? = "__uninitialized__"

    fun loadHome(userId: String?, forceRefresh: Boolean = false) {
        if (!forceRefresh && lastLoadedUserId == userId && !_uiState.value.loading && _uiState.value.error == null) {
            return
        }
        lastLoadedUserId = userId
        val signedIn = !userId.isNullOrBlank()

        viewModelScope.launch {
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
                    categories = categoriesRes.getOrDefault(current.categories),
                    professionals = professionalsRes.getOrDefault(current.professionals),
                    overview = overviewRes?.getOrNull() ?: current.overview,
                    bookings = bookingsRes?.getOrDefault(current.bookings) ?: if (signedIn) current.bookings else emptyList(),
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
