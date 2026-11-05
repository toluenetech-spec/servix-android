package ng.name.servix.app.ui.catalogue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.R
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.CategoryDto
import ng.name.servix.app.data.model.ProfessionalSummaryDto
import ng.name.servix.app.data.model.StatsDto
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.repository.CatalogueRepository
import ng.name.servix.app.ui.components.ErrorStateCard
import ng.name.servix.app.ui.components.ProfessionalToolsWebCard
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixLogoMark
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.SkeletonBox
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.CustomTabLauncher
import ng.name.servix.app.util.ServixFormatters
import java.text.NumberFormat
import java.util.Locale

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val categories: List<CategoryDto> = emptyList(),
    val topProfessionals: List<ProfessionalSummaryDto> = emptyList(),
    val stats: StatsDto? = null,
    val nextBooking: BookingDto? = null,
)

class HomeViewModel(
    private val catalogueRepository: CatalogueRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastLoadedUserId: String? = "__uninitialized__"

    fun loadHome(userId: String?, forceRefresh: Boolean = false) {
        if (!forceRefresh && lastLoadedUserId == userId && !_uiState.value.loading && _uiState.value.error == null) {
            return
        }
        lastLoadedUserId = userId

        viewModelScope.launch {
            _uiState.update {
                if (forceRefresh && it.categories.isNotEmpty()) {
                    it.copy(refreshing = true, error = null)
                } else {
                    it.copy(loading = true, error = null)
                }
            }

            val categoriesDeferred = async { catalogueRepository.getCategories(forceRefresh) }
            val topProsDeferred = async {
                catalogueRepository.getProfessionals(
                    sort = "rating",
                    page = 1,
                    pageSize = 10,
                )
            }
            val statsDeferred = async { catalogueRepository.getStats() }
            // Only fetch bookings when signed in; skip the call for guests (Stage 2 requirement)
            val bookingsDeferred = if (!userId.isNullOrBlank()) {
                async { catalogueRepository.getMyBookings() }
            } else {
                null
            }

            val categoriesRes = categoriesDeferred.await()
            val topProsRes = topProsDeferred.await()
            val statsRes = statsDeferred.await()
            val bookingsRes = bookingsDeferred?.await()

            val errorMsg = topProsRes.exceptionOrNull()?.message
                ?: categoriesRes.exceptionOrNull()?.message

            if (errorMsg != null && topProsRes.isFailure && categoriesRes.isFailure) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        error = errorMsg,
                    )
                }
                return@launch
            }

            val activeStatuses = setOf(
                "pending_payment",
                "requested",
                "accepted",
                "in_progress",
                "delivered",
            )
            val upcomingBooking = bookingsRes?.getOrNull()
                ?.filter { it.status in activeStatuses }
                ?.minByOrNull { it.scheduledAt }

            _uiState.update { curr ->
                curr.copy(
                    loading = false,
                    refreshing = false,
                    error = null,
                    categories = categoriesRes.getOrElse { curr.categories },
                    topProfessionals = topProsRes.getOrNull()?.items ?: curr.topProfessionals,
                    stats = statsRes.getOrNull() ?: curr.stats,
                    nextBooking = upcomingBooking,
                )
            }
        }
    }

    companion object {
        fun factory(catalogueRepository: CatalogueRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(catalogueRepository) as T
                }
            }
    }
}

private val SEARCH_SUGGESTIONS = listOf(
    "Web Development",
    "Graphic Design",
    "Photography",
    "Video Editing",
    "Marketing",
    "Consulting",
)

private data class HowStep(
    val num: String,
    val title: String,
    val desc: String,
)

private val HOW_STEPS = listOf(
    HowStep("01", "Discover", "Find the right service or professional for what you need."),
    HowStep("02", "Compare", "Review services, portfolios, ratings and availability."),
    HowStep("03", "Book", "Choose a suitable date and time that works for you."),
    HowStep("04", "Get It Done", "Manage your service from start to finish through Servix."),
)

private data class WhyItem(
    val icon: ImageVector,
    val title: String,
    val desc: String,
)

private val WHY_ITEMS = listOf(
    WhyItem(
        icon = Icons.Outlined.Shield,
        title = "Trusted Professionals",
        desc = "Discover professionals with complete profiles, portfolios and reviews — so you know exactly who you are working with.",
    ),
    WhyItem(
        icon = Icons.Outlined.CalendarMonth,
        title = "Simple Booking",
        desc = "Find a service and book it without unnecessary complexity. Clear pricing, clear scope, clear timelines.",
    ),
    WhyItem(
        icon = Icons.Outlined.TrackChanges,
        title = "Professional Experience",
        desc = "Every part of Servix is designed around quality service delivery, from discovery to completion.",
    ),
    WhyItem(
        icon = Icons.Outlined.DashboardCustomize,
        title = "One Place",
        desc = "Manage bookings, communication and service information from a single platform instead of scattered chats and emails.",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    city: String? = null,
    user: UserDto?,
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onSearchSubmit: (query: String?, categorySlug: String?) -> Unit,
    onOpenProfessional: (slug: String) -> Unit,
    onOpenBookingsTab: () -> Unit,
    onVerifyEmailClick: () -> Unit,
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(user?.id) {
        // Triggered by parent shell
    }

    PullToRefreshBox(
        isRefreshing = uiState.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            // Top bar: Logo + Greeting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ServixLogoMark()
                if (user != null) {
                    ServixBadge(
                        text = user.role.uppercase(),
                        containerColor = SoftSand,
                        contentColor = DeepForest,
                    )
                } else {
                    ServixSecondaryButton(
                        text = stringResource(R.string.sign_in),
                        onClick = onSignInClick,
                        testTag = "home_top_sign_in_button",
                    )
                }
            }

            if (!city.isNullOrBlank()) {
                Text("Near $city", style = MaterialTheme.typography.labelMedium, color = ForestGreen)
            }

            // Greeting (first name when signed in, else "Welcome")
            Text(
                text = if (user != null) {
                    stringResource(R.string.home_greeting_user, user.firstName)
                } else {
                    stringResource(R.string.home_greeting_guest)
                },
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
                modifier = Modifier.testTag("home_greeting_text"),
            )

            // Email verification notice if signed in & unverified
            if (user != null && !user.emailVerified) {
                ServixCard(
                    containerColor = SoftSand,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.verify_email_banner_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = Charcoal,
                        )
                        ServixPrimaryButton(
                            text = stringResource(R.string.verify_email_cta),
                            onClick = onVerifyEmailClick,
                            testTag = "home_verify_email_button",
                        )
                    }
                }
            }

            if (user != null) {
                ProfessionalToolsWebCard(role = user.role)
            }

            // "Your next booking" card — shown ONLY when signed in AND GET /bookings has an upcoming booking
            if (user != null && uiState.nextBooking != null) {
                NextBookingCard(
                    booking = uiState.nextBooking,
                    onClick = onOpenBookingsTab,
                )
            }

            // HERO + Search Bar (HomePage.jsx:207-227)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DeepForest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.hero_eyebrow).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmIvory.copy(alpha = 0.78f),
                    )
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.headlineLarge,
                        color = WarmIvory,
                    )
                    Text(
                        text = stringResource(R.string.hero_lead),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmIvory.copy(alpha = 0.9f),
                    )

                    // Search input bar -> Search tab
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PureWhite,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.home_search_placeholder),
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
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        onSearchSubmit(searchQuery.trim().takeIf { it.isNotEmpty() }, null)
                                    }
                                ),
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
                                    .fillMaxWidth()
                                    .testTag("home_search_input"),
                            )
                            ServixPrimaryButton(
                                text = stringResource(R.string.nav_search),
                                onClick = {
                                    onSearchSubmit(searchQuery.trim().takeIf { it.isNotEmpty() }, null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "home_search_submit_button",
                            )
                        }
                    }

                    // Popular tags (HomePage.jsx:102-113)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.home_popular_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmIvory.copy(alpha = 0.8f),
                        )
                        SEARCH_SUGGESTIONS.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = WarmIvory.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, WarmIvory.copy(alpha = 0.28f)),
                                modifier = Modifier
                                    .clickable { onSearchSubmit(tag, null) }
                                    .testTag("home_popular_tag_${tag.lowercase().replace(' ', '_')}"),
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WarmIvory,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Trust Strip (HomePage.jsx:163-192)
            TrustStripSection(stats = uiState.stats, loading = uiState.loading)

            // Error state if initial load failed
            if (!uiState.loading && uiState.error != null && uiState.topProfessionals.isEmpty()) {
                ErrorStateCard(
                    message = uiState.error,
                    onRetry = onRefresh,
                )
            }

            // Category chips & cards from GET /categories (HomePage.jsx:233-248)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_categories_eyebrow),
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.home_categories_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = DeepForest,
                        )
                    }
                    Text(
                        text = "View all →",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepForest,
                        modifier = Modifier
                            .clickable { onSearchSubmit(null, null) }
                            .padding(vertical = 4.dp)
                            .testTag("home_view_all_categories"),
                    )
                }
                Text(
                    text = stringResource(R.string.home_categories_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )

                if (uiState.loading && uiState.categories.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        repeat(3) {
                            SkeletonBox(modifier = Modifier.width(120.dp), height = 38.dp)
                        }
                    }
                } else if (uiState.categories.isNotEmpty()) {
                    // Horizontal quick category chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        uiState.categories.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PureWhite,
                                border = BorderStroke(1.dp, BorderSand),
                                modifier = Modifier
                                    .clickable { onSearchSubmit(null, cat.id) }
                                    .testTag("home_category_chip_${cat.id}"),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = categoryIconFor(cat.icon),
                                        contentDescription = cat.name,
                                        tint = DeepForest,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Charcoal,
                                    )
                                    Text(
                                        text = "(${cat.serviceCount})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedGray,
                                    )
                                }
                            }
                        }
                    }

                    // Featured category cards (first 4)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.categories.take(4).forEach { cat ->
                            CategoryCardItem(
                                category = cat,
                                onClick = { onSearchSubmit(null, cat.id) },
                            )
                        }
                    }
                }
            }

            // "Top-rated professionals" from GET /professionals?sort=rating&pageSize=10 (HomePage.jsx:251-269)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_top_pros_eyebrow),
                            style = MaterialTheme.typography.labelSmall,
                            color = ForestGreen,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.home_top_pros_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = DeepForest,
                        )
                    }
                    Text(
                        text = "${stringResource(R.string.home_browse_all_pros)} →",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepForest,
                        modifier = Modifier
                            .clickable { onSearchSubmit(null, null) }
                            .padding(vertical = 4.dp)
                            .testTag("home_browse_all_pros_button"),
                    )
                }
                Text(
                    text = stringResource(R.string.home_top_pros_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )

                if (uiState.loading && uiState.topProfessionals.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) {
                            ProfessionalCardSkeleton()
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.topProfessionals.forEach { pro ->
                            ProfessionalCardItem(
                                professional = pro,
                                onClick = { onOpenProfessional(pro.id) },
                            )
                        }
                    }
                }
            }

            // How Servix Works (HomePage.jsx:272-288)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.home_how_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen,
                )
                Text(
                    text = stringResource(R.string.home_how_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = DeepForest,
                )
                HOW_STEPS.forEach { step ->
                    ServixCard(contentPadding = PaddingValues(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = step.num,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Coral,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = step.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Charcoal,
                                )
                                Text(
                                    text = step.desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedGray,
                                )
                            }
                        }
                    }
                }
            }

            // Why Servix (HomePage.jsx:327-347)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "WHY SERVIX",
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen,
                )
                Text(
                    text = "Built around quality service delivery",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DeepForest,
                )
                WHY_ITEMS.forEach { item ->
                    ServixCard(contentPadding = PaddingValues(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SoftSand,
                                modifier = Modifier.size(36.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = DeepForest,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Charcoal,
                                )
                                Text(
                                    text = item.desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedGray,
                                )
                            }
                        }
                    }
                }
            }

            // For professionals web link card (HomePage.jsx:291-324)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DeepForest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "FOR PROFESSIONALS",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmIvory.copy(alpha = 0.75f),
                    )
                    Text(
                        text = "Turn your skills into a business.",
                        style = MaterialTheme.typography.headlineSmall,
                        color = WarmIvory,
                    )
                    listOf(
                        "Build a professional profile and showcase your work",
                        "List services with clear pricing and scope",
                        "Manage bookings and availability in one place",
                        "Connect with customers who value professional work",
                    ).forEach { point ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircleOutline,
                                contentDescription = null,
                                tint = Coral,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmIvory.copy(alpha = 0.9f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    ServixSecondaryButton(
                        text = "Become a Professional on servix.name.ng",
                        onClick = { CustomTabLauncher.launchWebPath(context, "/professionals/join") },
                        testTag = "home_become_pro_web_button",
                    )
                }
            }

            // Guest sign-in card at bottom
            if (user == null) {
                ServixCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Ready to get started?",
                            style = MaterialTheme.typography.titleMedium,
                            color = Charcoal,
                        )
                        Text(
                            text = stringResource(R.string.escrow_copy),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedGray,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ServixPrimaryButton(
                                text = stringResource(R.string.sign_in),
                                onClick = onSignInClick,
                                modifier = Modifier.weight(1f),
                                testTag = "home_sign_in_button",
                            )
                            ServixSecondaryButton(
                                text = stringResource(R.string.create_account),
                                onClick = onCreateAccountClick,
                                modifier = Modifier.weight(1f),
                                testTag = "home_create_account_button",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextBookingCard(
    booking: BookingDto,
    onClick: () -> Unit,
) {
    ServixCard(
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("home_next_booking_card"),
        containerColor = SoftSand,
        contentPadding = PaddingValues(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_next_booking_eyebrow),
                    style = MaterialTheme.typography.labelSmall,
                    color = ForestGreen,
                )
                ServixBadge(
                    text = booking.status.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    containerColor = PureWhite,
                    contentColor = DeepForest,
                )
            }
            Text(
                text = booking.serviceTitle,
                style = MaterialTheme.typography.titleMedium,
                color = Charcoal,
            )
            Text(
                text = buildString {
                    if (!booking.professionalName.isNullOrBlank()) {
                        append("with ${booking.professionalName} · ")
                    }
                    append(ServixFormatters.formatDateTimeMedium(booking.scheduledAt))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${ServixFormatters.formatNaira(booking.amount)} · ${booking.reference}",
                    style = MaterialTheme.typography.labelLarge,
                    color = DeepForest,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "View booking",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepForest,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = DeepForest,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrustStripSection(
    stats: StatsDto?,
    loading: Boolean,
) {
    if (!loading && stats == null) {
        return
    }
    val nf = NumberFormat.getIntegerInstance(Locale.US)
    val items = listOf(
        (stats?.verifiedProfessionals?.let { nf.format(it) } ?: "—") to "Verified professionals",
        (stats?.activeServices?.let { nf.format(it) } ?: "—") to "Completed services",
        (stats?.serviceCategories?.let { nf.format(it) } ?: "—") to "Service categories",
        (stats?.averageRating?.let { "${String.format(Locale.US, "%.1f", it)}/5" } ?: "—") to "Customer satisfaction",
    )

    ServixCard(
        modifier = Modifier.testTag("home_stats_strip"),
        containerColor = PureWhite,
        contentPadding = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                items.take(2).forEach { (value, label) ->
                    Column(modifier = Modifier.weight(1f)) {
                        if (loading && stats == null) {
                            SkeletonBox(modifier = Modifier.width(64.dp), height = 22.dp)
                        } else {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.headlineSmall,
                                color = DeepForest,
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                }
            }
            HorizontalDivider(color = SoftSand, thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                items.drop(2).forEach { (value, label) ->
                    Column(modifier = Modifier.weight(1f)) {
                        if (loading && stats == null) {
                            SkeletonBox(modifier = Modifier.width(64.dp), height = 22.dp)
                        } else {
                            Text(
                                text = value,
                                style = MaterialTheme.typography.headlineSmall,
                                color = DeepForest,
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                }
            }
        }
    }
}
