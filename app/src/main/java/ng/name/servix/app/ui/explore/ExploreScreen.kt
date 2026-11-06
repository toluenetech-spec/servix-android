package ng.name.servix.app.ui.explore

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ng.name.servix.app.R
import ng.name.servix.app.ui.catalogue.AVAILABILITY_OPTIONS
import ng.name.servix.app.ui.catalogue.PRICE_OPTIONS
import ng.name.servix.app.ui.catalogue.RATING_OPTIONS
import ng.name.servix.app.ui.catalogue.SORT_OPTIONS
import ng.name.servix.app.ui.catalogue.SearchMode
import ng.name.servix.app.ui.catalogue.SearchUiState
import ng.name.servix.app.ui.components.CategoryChip
import ng.name.servix.app.ui.components.EmptyState
import ng.name.servix.app.ui.components.ErrorCard
import ng.name.servix.app.ui.components.FilterChipPill
import ng.name.servix.app.ui.components.ListSkeleton
import ng.name.servix.app.ui.components.ProfessionalRow
import ng.name.servix.app.ui.components.SearchField
import ng.name.servix.app.ui.components.ServiceCard
import ng.name.servix.app.ui.components.ServixBottomSheet
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSegmentedControl
import ng.name.servix.app.ui.components.ServixTopBar
import ng.name.servix.app.ui.components.ServixTextButton
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.ServixFormatters

private const val PRICE_SLIDER_MAX = 500_000

/**
 * Plan §5 Explore: pinned search, one filter chip row, a segmented Professionals/Services switch,
 * results as rows, and a filter sheet with pill chips, a naira price slider and a sticky apply button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    uiState: SearchUiState,
    categoryNameFor: (String?) -> String,
    onQueryChange: (String) -> Unit,
    onQuerySubmit: () -> Unit,
    onModeChange: (SearchMode) -> Unit,
    onCategorySelect: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onRatingSelect: (String) -> Unit,
    onPriceSelect: (String) -> Unit,
    onPriceLimitChange: (Int?) -> Unit,
    onAvailabilitySelect: (String) -> Unit,
    onSortSelect: (String) -> Unit,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onOpenProfessional: (slug: String) -> Unit,
    onOpenService: (slug: String) -> Unit,
) {
    var filterSheetOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 2
        }
    }
    LaunchedEffect(shouldLoadMore, uiState.canLoadMore, uiState.loading, uiState.loadingMore) {
        if (shouldLoadMore && uiState.canLoadMore && !uiState.loading && !uiState.loadingMore) onLoadNextPage()
    }

    val sortLabel = SORT_OPTIONS.firstOrNull { it.id == uiState.sort }?.label ?: "Recommended"
    val categoryLabel = categoryNameFor(uiState.selectedCategory)
    val priceLabel = uiState.priceLimit?.let { "to ${ServixFormatters.formatNaira(it.toLong())}" }
        ?: PRICE_OPTIONS.firstOrNull { it.id == uiState.selectedPriceId && it.maxPrice != null }?.label

    Column(modifier = Modifier.fillMaxSize()) {
        ServixTopBar(title = stringResource(R.string.nav_explore))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchField(
                value = uiState.query,
                placeholder = stringResource(R.string.home_search_placeholder),
                onValueChange = onQueryChange,
                testTag = "explore_search_field",
            )
            ServixSegmentedControl(
                options = listOf(
                    SearchMode.PROFESSIONALS.name to stringResource(R.string.explore_professionals),
                    SearchMode.SERVICES.name to stringResource(R.string.explore_services),
                ),
                selectedId = uiState.mode.name,
                onSelect = { id -> onModeChange(SearchMode.valueOf(id)) },
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 8.dp),
                modifier = Modifier.fillMaxWidth().offset(x = (-16).dp),
            ) {
                item {
                    FilterChipPill(
                        label = stringResource(R.string.explore_filters),
                        value = uiState.activeFilterCount.takeIf { it > 0 }?.toString(),
                        active = uiState.activeFilterCount > 0,
                        icon = Icons.Rounded.Tune,
                        onClick = { filterSheetOpen = true },
                        modifier = Modifier.testTag("explore_filters_chip"),
                    )
                }
                item {
                    Surface(
                        onClick = { sortMenuOpen = true },
                        shape = CircleShape,
                        color = if (uiState.sort == "recommended") PureWhite else DeepForest,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.sort == "recommended") ng.name.servix.app.ui.theme.BorderSand else DeepForest,
                        ),
                        modifier = Modifier.testTag("explore_sort_chip"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${stringResource(R.string.explore_sort)} · $sortLabel",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (uiState.sort == "recommended") Charcoal else WarmIvory,
                            )
                        }
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        SORT_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    sortMenuOpen = false
                                    onSortSelect(option.id)
                                },
                            )
                        }
                    }
                }
                item {
                    FilterChipPill(
                        label = stringResource(R.string.explore_category),
                        value = categoryLabel.takeIf { it.isNotBlank() },
                        active = uiState.selectedCategory.isNotBlank(),
                        onClick = { filterSheetOpen = true },
                    )
                }
                item {
                    FilterChipPill(
                        label = stringResource(R.string.explore_city),
                        value = uiState.location.takeIf { it.isNotBlank() },
                        active = uiState.location.isNotBlank(),
                        onClick = { filterSheetOpen = true },
                    )
                }
                item {
                    val availableNow = uiState.selectedAvailabilityId == "available"
                    FilterChipPill(
                        label = stringResource(R.string.explore_available_now),
                        active = availableNow,
                        onClick = {
                            onAvailabilitySelect(if (availableNow) "all" else "available")
                        },
                    )
                }
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
                    .testTag("explore_results_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "count") {
                    Text(
                        text = when (uiState.mode) {
                            SearchMode.PROFESSIONALS -> stringResource(R.string.explore_count_professionals, uiState.total)
                            SearchMode.SERVICES -> stringResource(R.string.explore_count_services, uiState.total)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                }

                if (uiState.loading && uiState.professionals.isEmpty() && uiState.services.isEmpty()) {
                    item(key = "skeleton") { ListSkeleton(rows = 3) }
                }

                if (uiState.error != null) {
                    item(key = "error") { ErrorCard(message = uiState.error, onRetry = onRefresh) }
                }

                if (uiState.mode == SearchMode.PROFESSIONALS) {
                    items(uiState.professionals, key = { it.id }) { pro ->
                        ProfessionalRow(
                            name = pro.name,
                            title = pro.title,
                            rating = pro.rating,
                            reviewCount = pro.reviewCount,
                            startingPrice = pro.startingPrice,
                            verified = pro.verified,
                            initials = pro.name,
                            subtitle = pro.location.takeIf { it.isNotBlank() },
                            avatarUrl = pro.image,
                            onClick = { onOpenProfessional(pro.slug) },
                            modifier = Modifier.testTag("explore_professional_row"),
                        )
                    }
                } else {
                    items(uiState.services, key = { it.id }) { service ->
                        ServiceCard(
                            title = service.title,
                            professionalName = service.professional?.name.orEmpty(),
                            price = service.price,
                            priceUnit = service.priceUnit,
                            rating = service.rating,
                            reviewCount = service.reviewCount,
                            image = service.image,
                            onClick = { onOpenService(service.id) },
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }

                if (uiState.loadingMore) {
                    item(key = "more") { ListSkeleton(rows = 2) }
                }

                val empty = !uiState.loading && uiState.error == null && when (uiState.mode) {
                    SearchMode.PROFESSIONALS -> uiState.professionals.isEmpty()
                    SearchMode.SERVICES -> uiState.services.isEmpty()
                }
                if (empty) {
                    item(key = "empty") {
                        EmptyState(
                            title = stringResource(R.string.explore_empty_title),
                            body = stringResource(R.string.explore_empty_body),
                            actionLabel = stringResource(R.string.explore_clear_filters),
                            onAction = onClearFilters,
                        )
                    }
                }
            }
        }
    }

    if (filterSheetOpen) {
        ServixBottomSheet(title = stringResource(R.string.explore_filters), onDismiss = { filterSheetOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FilterGroup(title = stringResource(R.string.explore_category)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CategoryChip(
                            label = stringResource(R.string.explore_all_categories),
                            selected = uiState.selectedCategory.isBlank(),
                            onClick = { onCategorySelect("") },
                        )
                        uiState.categories.forEach { category ->
                            CategoryChip(
                                label = category.name,
                                selected = uiState.selectedCategory == category.id,
                                onClick = { onCategorySelect(category.id) },
                            )
                        }
                    }
                }

                FilterGroup(title = stringResource(R.string.explore_city)) {
                    SearchField(
                        value = uiState.location,
                        placeholder = stringResource(R.string.explore_city_placeholder),
                        onValueChange = onLocationChange,
                    )
                }

                FilterGroup(title = stringResource(R.string.explore_rating)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RATING_OPTIONS.forEach { option ->
                            CategoryChip(
                                label = option.label,
                                selected = uiState.selectedRatingId == option.id,
                                onClick = { onRatingSelect(option.id) },
                            )
                        }
                    }
                }

                FilterGroup(title = stringResource(R.string.explore_price)) {
                    val limit = (uiState.priceLimit ?: 0).coerceIn(0, PRICE_SLIDER_MAX)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (uiState.priceLimit == null) {
                                stringResource(R.string.explore_price_any)
                            } else {
                                stringResource(R.string.explore_price_up_to, ServixFormatters.formatNaira(uiState.priceLimit.toLong()))
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Charcoal,
                        )
                        Slider(
                            value = limit.toFloat(),
                            onValueChange = { raw -> onPriceLimitChange(((raw / 25000f).toInt() * 25000).takeIf { it > 0 }) },
                            valueRange = 0f..PRICE_SLIDER_MAX.toFloat(),
                            steps = 19,
                            colors = SliderDefaults.colors(
                                thumbColor = DeepForest,
                                activeTrackColor = DeepForest,
                                inactiveTrackColor = SoftSand,
                            ),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("₦0", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                            Text("₦500,000", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                        }
                        ServixTextButton(
                            text = stringResource(R.string.explore_price_reset),
                            onClick = { onPriceLimitChange(null) },
                        )
                    }
                }

                FilterGroup(title = stringResource(R.string.explore_availability)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AVAILABILITY_OPTIONS.forEach { option ->
                            CategoryChip(
                                label = option.label,
                                selected = uiState.selectedAvailabilityId == option.id,
                                onClick = { onAvailabilitySelect(option.id) },
                            )
                        }
                    }
                }

                ServixPrimaryButton(
                    text = stringResource(
                        if (uiState.mode == SearchMode.PROFESSIONALS) R.string.explore_show_results_professionals
                        else R.string.explore_show_results_services,
                        uiState.total,
                    ),
                    onClick = { filterSheetOpen = false },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "explore_show_results",
                )
            }
        }
    }
}

@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = DeepForest)
        content()
    }
}
