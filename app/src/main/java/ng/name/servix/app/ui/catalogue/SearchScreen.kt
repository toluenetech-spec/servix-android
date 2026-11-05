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
        _uiState.update { it.copy(selectedPriceId = priceId, page = 1) }
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
            val maxPrice = PRICE_OPTIONS.firstOrNull { it.id == snapshot.selectedPriceId }?.maxPrice
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    categoryNameFor: (String?) -> String,
    onQueryChange: (String) -> Unit,
    onQuerySubmit: () -> Unit,
    onModeChange: (SearchMode) -> Unit,
    onCategorySelect: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onRatingSelect: (String) -> Unit,
    onPriceSelect: (String) -> Unit,
    onAvailabilitySelect: (String) -> Unit,
    onSortSelect: (String) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenProfessional: (slug: String) -> Unit,
    onOpenService: (slug: String) -> Unit,
) {
    var filterSheetOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisible >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore, uiState.canLoadMore, uiState.loading, uiState.loadingMore) {
        if (shouldLoadMore && uiState.canLoadMore && !uiState.loading && !uiState.loadingMore) {
            onLoadNextPage()
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("search_results_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(
                            if (uiState.mode == SearchMode.PROFESSIONALS) R.string.search_pros_title
                            else R.string.search_services_title
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepForest,
                    )
                    Text(
                        text = stringResource(R.string.search_pros_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )

                    // Segmented switcher: Professionals vs Services
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ModeChip(
                            label = "Professionals",
                            selected = uiState.mode == SearchMode.PROFESSIONALS,
                            onClick = { onModeChange(SearchMode.PROFESSIONALS) },
                            modifier = Modifier.weight(1f),
                            testTag = "search_mode_professionals",
                        )
                        ModeChip(
                            label = "Services",
                            selected = uiState.mode == SearchMode.SERVICES,
                            onClick = { onModeChange(SearchMode.SERVICES) },
                            modifier = Modifier.weight(1f),
                            testTag = "search_mode_services",
                        )
                    }
                }
            }

            // Search input + Filter sheet button
            item(key = "toolbar") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = uiState.query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = stringResource(
                                        if (uiState.mode == SearchMode.PROFESSIONALS) R.string.search_pros_placeholder
                                        else R.string.search_services_placeholder
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedGray,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = stringResource(R.string.nav_search),
                                    tint = MutedGray,
                                )
                            },
                            trailingIcon = if (uiState.query.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { onQueryChange("") },
                                        modifier = Modifier.testTag("search_clear_query_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Clear,
                                            contentDescription = "Clear search",
                                            tint = MutedGray,
                                        )
                                    }
                                }
                            } else null,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onQuerySubmit() }),
                            shape = RoundedCornerShape(6.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = PureWhite,
                                focusedBorderColor = DeepForest,
                                unfocusedBorderColor = BorderSand,
                                focusedTextColor = Charcoal,
                                unfocusedTextColor = Charcoal,
                                cursorColor = DeepForest,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("search_query_input"),
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (uiState.activeFilterCount > 0) DeepForest else PureWhite,
                            border = BorderStroke(1.dp, if (uiState.activeFilterCount > 0) DeepForest else BorderSand),
                            modifier = Modifier
                                .height(56.dp)
                                .clickable { filterSheetOpen = true }
                                .testTag("search_open_filters_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = stringResource(R.string.filters_button),
                                    tint = if (uiState.activeFilterCount > 0) WarmIvory else Charcoal,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = if (uiState.activeFilterCount > 0) {
                                        "Filters (${uiState.activeFilterCount})"
                                    } else {
                                        stringResource(R.string.filters_button)
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (uiState.activeFilterCount > 0) WarmIvory else Charcoal,
                                )
                            }
                        }
                    }

                    // Horizontal category filter chips
                    if (uiState.categories.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterPillChip(
                                label = "All categories",
                                selected = uiState.selectedCategory.isBlank(),
                                onClick = { onCategorySelect("") },
                                testTag = "search_cat_chip_all",
                            )
                            uiState.categories.forEach { cat ->
                                FilterPillChip(
                                    label = cat.name,
                                    selected = uiState.selectedCategory == cat.id,
                                    onClick = {
                                        onCategorySelect(if (uiState.selectedCategory == cat.id) "" else cat.id)
                                    },
                                    testTag = "search_cat_chip_${cat.id}",
                                )
                            }
                        }
                    }

                    // Results count + current sort indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val noun = if (uiState.mode == SearchMode.PROFESSIONALS) {
                            if (uiState.total == 1) "professional" else "professionals"
                        } else {
                            if (uiState.total == 1) "service" else "services"
                        }
                        Text(
                            text = if (uiState.loading) "Loading $noun…" else "${uiState.total} $noun found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                            modifier = Modifier.testTag("search_results_count"),
                        )
                        val sortLabel = SORT_OPTIONS.firstOrNull { it.id == uiState.sort }?.label ?: "Recommended"
                        Text(
                            text = "Sort: $sortLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepForest,
                            modifier = Modifier
                                .clickable { filterSheetOpen = true }
                                .padding(vertical = 4.dp)
                                .testTag("search_sort_label"),
                        )
                    }
                }
            }

            // Loading state
            if (uiState.loading) {
                items(4) {
                    ProfessionalCardSkeleton()
                }
            } else if (uiState.error != null) {
                item(key = "error") {
                    ErrorStateCard(
                        message = uiState.error,
                        onRetry = onRefresh,
                    )
                }
            } else if (uiState.mode == SearchMode.PROFESSIONALS && uiState.professionals.isEmpty()) {
                item(key = "empty_pros") {
                    EmptyStateCard(
                        title = stringResource(R.string.empty_pros_title),
                        message = stringResource(R.string.empty_search_message),
                        actionLabel = stringResource(R.string.filters_clear),
                        onActionClick = {
                            onQueryChange("")
                            onClearFilters()
                        },
                    )
                }
            } else if (uiState.mode == SearchMode.SERVICES && uiState.services.isEmpty()) {
                item(key = "empty_services") {
                    EmptyStateCard(
                        title = stringResource(R.string.empty_services_title),
                        message = stringResource(R.string.empty_search_message),
                        actionLabel = stringResource(R.string.filters_clear),
                        onActionClick = {
                            onQueryChange("")
                            onClearFilters()
                        },
                    )
                }
            } else if (uiState.mode == SearchMode.PROFESSIONALS) {
                items(
                    items = uiState.professionals,
                    key = { it.id },
                ) { pro ->
                    ProfessionalCardItem(
                        professional = pro,
                        onClick = { onOpenProfessional(pro.id) },
                    )
                }
            } else {
                items(
                    items = uiState.services,
                    key = { it.id },
                ) { service ->
                    ServiceCardItem(
                        service = service,
                        categoryName = categoryNameFor(service.categoryId),
                        onClick = { onOpenService(service.id) },
                    )
                }
            }

            // Pagination / Infinite Scroll indicator
            if (uiState.loadingMore) {
                item(key = "loading_more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = DeepForest,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }

    if (filterSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { filterSheetOpen = false },
            sheetState = sheetState,
            containerColor = WarmIvory,
            contentColor = Charcoal,
        ) {
            SearchFilterSheetContent(
                uiState = uiState,
                onCategorySelect = onCategorySelect,
                onLocationChange = onLocationChange,
                onSortSelect = onSortSelect,
                onRatingSelect = onRatingSelect,
                onPriceSelect = onPriceSelect,
                onAvailabilitySelect = onAvailabilitySelect,
                onClearAll = onClearFilters,
                onClose = { filterSheetOpen = false },
            )
        }
    }
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) DeepForest else PureWhite,
        border = BorderStroke(1.dp, if (selected) DeepForest else BorderSand),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) WarmIvory else Charcoal,
            )
        }
    }
}

@Composable
private fun FilterPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = if (selected) DeepForest else PureWhite,
        border = BorderStroke(1.dp, if (selected) DeepForest else BorderSand),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) WarmIvory else Charcoal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun SearchFilterSheetContent(
    uiState: SearchUiState,
    onCategorySelect: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onSortSelect: (String) -> Unit,
    onRatingSelect: (String) -> Unit,
    onPriceSelect: (String) -> Unit,
    onAvailabilitySelect: (String) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.filters_button),
                style = MaterialTheme.typography.headlineSmall,
                color = DeepForest,
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Charcoal,
                )
            }
        }

        // Sort
        FilterGroupTitle("Sort by")
        Column {
            SORT_OPTIONS.forEach { opt ->
                RadioRow(
                    label = opt.label,
                    selected = uiState.sort == opt.id,
                    onClick = { onSortSelect(opt.id) },
                    testTag = "filter_sort_${opt.id}",
                )
            }
        }

        HorizontalDivider(color = BorderSand)

        // Category
        FilterGroupTitle("Category")
        Column {
            RadioRow(
                label = "All categories",
                selected = uiState.selectedCategory.isBlank(),
                onClick = { onCategorySelect("") },
                testTag = "filter_category_all",
            )
            uiState.categories.forEach { cat ->
                RadioRow(
                    label = cat.name,
                    selected = uiState.selectedCategory == cat.id,
                    onClick = { onCategorySelect(cat.id) },
                    testTag = "filter_category_${cat.id}",
                )
            }
        }

        HorizontalDivider(color = BorderSand)

        // Location
        ServixTextField(
            value = uiState.location,
            onValueChange = onLocationChange,
            label = "Location",
            placeholder = "e.g. Lagos",
            testTag = "filter_location_input",
        )

        LocationButton(onCityChosen = onLocationChange)

        HorizontalDivider(color = BorderSand)

        // Rating
        FilterGroupTitle("Rating")
        Column {
            RATING_OPTIONS.forEach { opt ->
                RadioRow(
                    label = opt.label,
                    selected = uiState.selectedRatingId == opt.id,
                    onClick = { onRatingSelect(opt.id) },
                    testTag = "filter_rating_${opt.id}",
                )
            }
        }

        HorizontalDivider(color = BorderSand)

        // Starting price
        FilterGroupTitle("Starting price")
        Column {
            PRICE_OPTIONS.forEach { opt ->
                RadioRow(
                    label = opt.label,
                    selected = uiState.selectedPriceId == opt.id,
                    onClick = { onPriceSelect(opt.id) },
                    testTag = "filter_price_${opt.id}",
                )
            }
        }

        HorizontalDivider(color = BorderSand)

        // Availability
        FilterGroupTitle("Availability")
        Column {
            AVAILABILITY_OPTIONS.forEach { opt ->
                RadioRow(
                    label = opt.label,
                    selected = uiState.selectedAvailabilityId == opt.id,
                    onClick = { onAvailabilitySelect(opt.id) },
                    testTag = "filter_avail_${opt.id}",
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServixSecondaryButton(
                text = stringResource(R.string.filters_clear_all),
                onClick = onClearAll,
                modifier = Modifier.weight(1f),
                testTag = "filter_clear_all_button",
            )
            ServixPrimaryButton(
                text = stringResource(R.string.filters_show_results),
                onClick = onClose,
                modifier = Modifier.weight(1f),
                testTag = "filter_show_results_button",
            )
        }
    }
}

@Composable
private fun FilterGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = Charcoal,
    )
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = DeepForest,
                unselectedColor = MutedGray,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Charcoal,
        )
    }
}
