package ng.name.servix.app.ui.catalogue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.location.LocationButton
import ng.name.servix.app.R
import ng.name.servix.app.data.model.CategoryDto
import ng.name.servix.app.data.model.ProfessionalSummaryDto
import ng.name.servix.app.data.model.ServiceSummaryDto
import ng.name.servix.app.data.repository.CatalogueRepository
import ng.name.servix.app.ui.components.ErrorStateCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.ServixTextField
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory

enum class SearchMode {
    PROFESSIONALS,
    SERVICES,
}

data class SortOption(
    val id: String,
    val label: String,
)

val SORT_OPTIONS = listOf(
    SortOption("recommended", "Recommended"),
    SortOption("rating", "Highest rated"),
    SortOption("reviews", "Most reviewed"),
    SortOption("price-asc", "Starting price: low to high"),
    SortOption("price-desc", "Starting price: high to low"),
)

data class RatingFilterOption(
    val id: String,
    val label: String,
    val minRating: Double?,
)

val RATING_OPTIONS = listOf(
    RatingFilterOption("all", "Any rating", null),
    RatingFilterOption("4.8", "4.8 and above", 4.8),
    RatingFilterOption("4.5", "4.5 and above", 4.5),
)

data class PriceFilterOption(
    val id: String,
    val label: String,
    val maxPrice: Int?,
)

val PRICE_OPTIONS = listOf(
    PriceFilterOption("all", "Any starting price", null),
    PriceFilterOption("150", "Up to ₦150,000", 150000),
    PriceFilterOption("250", "Up to ₦250,000", 250000),
    PriceFilterOption("500", "Up to ₦500,000", 500000),
)

data class AvailabilityFilterOption(
    val id: String,
    val label: String,
    val availability: String? = null,
    val availableWindow: String? = null,
)

val AVAILABILITY_OPTIONS = listOf(
    AvailabilityFilterOption("all", "Any availability"),
    AvailabilityFilterOption("available", "Available now", availability = "available"),
    AvailabilityFilterOption("limited", "Limited availability", availability = "limited"),
    AvailabilityFilterOption("today", "Free today", availableWindow = "today"),
    AvailabilityFilterOption("tomorrow", "Free tomorrow", availableWindow = "tomorrow"),
    AvailabilityFilterOption("week", "Free this week", availableWindow = "week"),
)

data class SearchUiState(
    val mode: SearchMode = SearchMode.PROFESSIONALS,
    val query: String = "",
    val selectedCategory: String = "",
    val location: String = "",
    val selectedRatingId: String = "all",
    val selectedPriceId: String = "all",
    val priceLimit: Int? = null,
    val selectedAvailabilityId: String = "all",
    val sort: String = "recommended",
    val categories: List<CategoryDto> = emptyList(),
    val professionals: List<ProfessionalSummaryDto> = emptyList(),
    val services: List<ServiceSummaryDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 6,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
) {
    val activeFilterCount: Int
        get() = listOf(
            selectedCategory.isNotBlank(),
            location.isNotBlank(),
            selectedRatingId != "all",
            selectedPriceId != "all",
            selectedAvailabilityId != "all",
            sort != "recommended",
        ).count { it }

    val canLoadMore: Boolean
        get() = when (mode) {
            SearchMode.PROFESSIONALS -> professionals.size < total
            SearchMode.SERVICES -> services.size < total
        }
}

class SearchViewModel(
    private val catalogueRepository: CatalogueRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null
    private var fetchJob: Job? = null
    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            catalogueRepository.getCategories().onSuccess { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
        executeSearch(resetPage = true, isRefresh = false)
    }

    fun applyExternalNavigation(query: String?, categorySlug: String?) {
        initialized = true
        debounceJob?.cancel()
        viewModelScope.launch {
            if (_uiState.value.categories.isEmpty()) {
                catalogueRepository.getCategories().onSuccess { cats ->
                    _uiState.update { it.copy(categories = cats) }
                }
            }
        }
        _uiState.update {
            it.copy(
                query = query.orEmpty(),
                selectedCategory = categorySlug.orEmpty(),
                page = 1,
            )
        }
        executeSearch(resetPage = true, isRefresh = false)
    }

    fun updateQuery(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(350)
            executeSearch(resetPage = true, isRefresh = false)
        }
    }

    fun submitQueryImmediate() {
        debounceJob?.cancel()
        executeSearch(resetPage = true, isRefresh = false)
    }

    fun setMode(mode: SearchMode) {
        if (_uiState.value.mode == mode) return
        _uiState.update { it.copy(mode = mode, page = 1) }
        executeSearch(resetPage = true, isRefresh = false)
    }

    fun setCategory(categorySlug: String) {
        _uiState.update { it.copy(selectedCategory = categorySlug, page = 1) }
        executeSearch(resetPage = true, isRefresh = false)
    }

    fun setLocation(location: String) {
        _uiState.update { it.copy(location = location, page = 1) }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(350)
            executeSearch(resetPage = true, isRefresh = false)
        }
    }

    fun setRatingFilter(ratingId: String) {
        _uiState.update { it.copy(selectedRatingId = ratingId, page = 1) }
        executeSearch(resetPage = true, isRefresh = false)
    }

    fun setPriceFilter(priceId: String) {
        _uiState.update { it.copy(selectedPriceId = priceId, priceLimit = null, page = 1) }
        executeSearch(resetPage = true, isRefresh = false)
    }

    /** Plan §5: one price slider with naira labels instead of a list of price buckets. */
    fun setPriceLimit(maxNaira: Int?) {
        _uiState.update { it.copy(priceLimit = maxNaira, selectedPriceId = "all", page = 1) }
        executeSearch(resetPage = true, isRefresh = false)
    }

    fun setAvailabilityFilter(availId: String) {
        _uiState.update { it.copy(selectedAvailabilityId = availId, page = 1) }
        executeSearch(resetPage = true, isRefresh = false)
    }

    fun setSort(sortId: String) {
        _uiState.update { it.copy(sort = sortId, page = 1) }
        executeSearch(resetPage = true, isRefresh = false)
    }

    fun clearFilters() {
        debounceJob?.cancel()
        _uiState.update {
            it.copy(
                selectedCategory = "",
                location = "",
                selectedRatingId = "all",
                selectedPriceId = "all",
                priceLimit = null,
                selectedAvailabilityId = "all",
                sort = "recommended",
                page = 1,
            )
        }
        executeSearch(resetPage = true, isRefresh = false)
    }

    fun refresh() {
        executeSearch(resetPage = true, isRefresh = true)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.loading || state.refreshing || state.loadingMore || !state.canLoadMore) return
        executeSearch(resetPage = false, isRefresh = false)
    }

    private fun executeSearch(resetPage: Boolean, isRefresh: Boolean) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            val snapshot = _uiState.value
            val targetPage = if (resetPage) 1 else snapshot.page + 1

            _uiState.update {
                when {
                    isRefresh -> it.copy(refreshing = true, error = null)
                    resetPage -> it.copy(loading = true, error = null, page = 1)
                    else -> it.copy(loadingMore = true, error = null)
                }
            }

            val minRating = RATING_OPTIONS.firstOrNull { it.id == snapshot.selectedRatingId }?.minRating
            val maxPrice = snapshot.priceLimit
                ?: PRICE_OPTIONS.firstOrNull { it.id == snapshot.selectedPriceId }?.maxPrice
            val availOpt = AVAILABILITY_OPTIONS.firstOrNull { it.id == snapshot.selectedAvailabilityId }
                ?: AVAILABILITY_OPTIONS.first()

            if (snapshot.mode == SearchMode.PROFESSIONALS) {
                val res = catalogueRepository.getProfessionals(
                    q = snapshot.query,
                    category = snapshot.selectedCategory,
                    location = snapshot.location,
                    minRating = minRating,
                    maxPrice = maxPrice,
                    availability = availOpt.availability,
                    available = availOpt.availableWindow,
                    sort = snapshot.sort,
                    page = targetPage,
                    pageSize = snapshot.pageSize,
                )
                res.fold(
                    onSuccess = { pageData ->
                        _uiState.update { curr ->
                            val combined = if (resetPage) {
                                pageData.items
                            } else {
                                (curr.professionals + pageData.items).distinctBy { it.id }
                            }
                            curr.copy(
                                loading = false,
                                refreshing = false,
                                loadingMore = false,
                                error = null,
                                professionals = combined,
                                total = pageData.total,
                                page = pageData.page,
                            )
                        }
                    },
                    onFailure = { err ->
                        _uiState.update { curr ->
                            curr.copy(
                                loading = false,
                                refreshing = false,
                                loadingMore = false,
                                error = err.message ?: "We couldn't load the professionals. Please try again.",
                            )
                        }
                    }
                )
            } else {
                val res = catalogueRepository.getServices(
                    q = snapshot.query,
                    category = snapshot.selectedCategory,
                    maxPrice = maxPrice,
                    minRating = minRating,
                    location = snapshot.location,
                    availability = availOpt.availability,
                    available = availOpt.availableWindow,
                    sort = snapshot.sort,
                    page = targetPage,
                    pageSize = snapshot.pageSize,
                )
                res.fold(
                    onSuccess = { pageData ->
                        _uiState.update { curr ->
                            val combined = if (resetPage) {
                                pageData.items
                            } else {
                                (curr.services + pageData.items).distinctBy { it.id }
                            }
                            curr.copy(
                                loading = false,
                                refreshing = false,
                                loadingMore = false,
                                error = null,
                                services = combined,
                                total = pageData.total,
                                page = pageData.page,
                            )
                        }
                    },
                    onFailure = { err ->
                        _uiState.update { curr ->
                            curr.copy(
                                loading = false,
                                refreshing = false,
                                loadingMore = false,
                                error = err.message ?: "We couldn't load the services. Please try again.",
                            )
                        }
                    }
                )
            }
        }
    }

    fun categoryNameFor(slug: String?): String = catalogueRepository.categoryNameFor(slug)

    companion object {
        fun factory(catalogueRepository: CatalogueRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SearchViewModel(catalogueRepository) as T
                }
            }
    }
}

