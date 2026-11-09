package ng.name.servix.app.ui.catalogue

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import ng.name.servix.app.data.model.AvailabilitySummaryDto
import ng.name.servix.app.data.model.PortfolioItemDto
import ng.name.servix.app.data.model.ProfessionalDetailDto
import ng.name.servix.app.data.model.ReviewDto
import ng.name.servix.app.data.model.ServiceSummaryDto
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.model.TrustSummaryDto
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.repository.CatalogueRepository
import ng.name.servix.app.ui.components.ErrorStateCard
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixTopBar
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.SkeletonBox
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DangerRed
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.CustomTabLauncher
import ng.name.servix.app.util.ServixFormatters
import java.util.Locale

data class ProfessionalProfileUiState(
    val slug: String = "",
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val error: String? = null,
    val professional: ProfessionalDetailDto? = null,
    val services: List<ServiceSummaryDto> = emptyList(),
    val servicesError: String? = null,
    val reviews: List<ReviewDto> = emptyList(),
    val availability: AvailabilitySummaryDto? = null,
    val isSaved: Boolean? = null,
    val saveBusy: Boolean = false,
    val saveError: String? = null,
)

class ProfessionalProfileViewModel(
    private val catalogueRepository: CatalogueRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfessionalProfileUiState())
    val uiState: StateFlow<ProfessionalProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(slug: String, isAuthenticated: Boolean, forceRefresh: Boolean = false) {
        if (!forceRefresh && _uiState.value.slug == slug && _uiState.value.professional != null && _uiState.value.error == null) {
            return
        }
        viewModelScope.launch {
            _uiState.update {
                ProfessionalProfileUiState(
                    slug = slug,
                    loading = true,
                )
            }

            val proDeferred = async { catalogueRepository.getProfessional(slug) }
            val servicesDeferred = async { catalogueRepository.getProfessionalServices(slug) }
            val reviewsDeferred = async { catalogueRepository.getProfessionalReviews(slug) }
            val availDeferred = async { catalogueRepository.getProfessionalAvailabilitySummary(slug) }
            val savedDeferred = if (isAuthenticated) {
                async { catalogueRepository.getSavedProfessionals() }
            } else {
                null
            }

            val proRes = proDeferred.await()
            if (proRes.isFailure) {
                val ex = proRes.exceptionOrNull() as? ServixApiException
                _uiState.update {
                    it.copy(
                        loading = false,
                        notFound = ex?.status == 404,
                        error = ex?.message ?: "We couldn't load this profile. Please try again.",
                    )
                }
                return@launch
            }

            val pro = proRes.getOrThrow()
            val servicesResult = servicesDeferred.await()
            val services = servicesResult.getOrDefault(emptyList())
            val servicesError = servicesResult.exceptionOrNull()?.let {
                (it as? ServixApiException)?.message ?: "We couldn't load services. Please try again."
            }
            val reviews = reviewsDeferred.await().getOrDefault(emptyList())
            val availability = availDeferred.await().getOrNull()
            val savedList = savedDeferred?.await()?.getOrNull()
            val isSaved = savedList?.any { it.professional.slug == slug }

            _uiState.update {
                it.copy(
                    loading = false,
                    notFound = false,
                    error = null,
                    professional = pro,
                    services = services,
                    servicesError = servicesError,
                    reviews = reviews,
                    availability = availability,
                    isSaved = if (isAuthenticated) (isSaved ?: false) else null,
                )
            }

            // Fire-and-forget view count (ProfessionalProfilePage.jsx:61)
            catalogueRepository.countView("professional", pro.id)
        }
    }

    fun toggleSaveProfessional() {
        val current = _uiState.value
        val slug = current.professional?.id ?: return
        if (current.saveBusy) return
        val currentlySaved = current.isSaved == true

        viewModelScope.launch {
            _uiState.update { it.copy(saveBusy = true, saveError = null) }
            val res = if (currentlySaved) {
                catalogueRepository.unsaveProfessional(slug)
            } else {
                catalogueRepository.saveProfessional(slug).map { }
            }
            res.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            saveBusy = false,
                            isSaved = !currentlySaved,
                            saveError = null,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            saveBusy = false,
                            saveError = err.message ?: "Could not update saved state.",
                        )
                    }
                }
            )
        }
    }

    fun categoryNameFor(slug: String?): String = catalogueRepository.categoryNameFor(slug)

    companion object {
        fun factory(catalogueRepository: CatalogueRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProfessionalProfileViewModel(catalogueRepository) as T
                }
            }
    }
}

private enum class ProfileTab(val label: String) {
    ABOUT("About"),
    TRUST("Trust & performance"),
    SERVICES("Services"),
    PORTFOLIO("Portfolio"),
    REVIEWS("Reviews"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ProfessionalProfileScreen(
    user: UserDto?,
    uiState: ProfessionalProfileUiState,
    categoryNameFor: (String?) -> String,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onBrowseProfessionalsClick: () -> Unit,
    onToggleSave: () -> Unit,
    onSignInClick: () -> Unit,
    onOpenService: (slug: String) -> Unit,
) {
    BackHandler(onBack = onBackClick)

    var selectedTab by rememberSaveable { mutableStateOf(ProfileTab.SERVICES) }
    var bookingSheetOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val pro = uiState.professional

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (pro == null) 24.dp else 108.dp),
        ) {
            item(key = "top_bar") { ServixTopBar(back = onBackClick) }

            when {
                uiState.loading -> item(key = "loading") { ProfileLoadingSkeleton() }

                uiState.notFound -> item(key = "missing") {
                    EmptyStateCard(
                        title = stringResource(R.string.pro_not_found_title),
                        message = stringResource(R.string.pro_not_found_message),
                        actionLabel = stringResource(R.string.home_browse_all_pros),
                        onActionClick = onBrowseProfessionalsClick,
                    )
                }

                pro == null -> item(key = "error") {
                    ErrorStateCard(
                        message = uiState.error ?: stringResource(R.string.error_load_profile),
                        onRetry = onRetry,
                    )
                }

                else -> {
                    item(key = "header") {
                        ProfileHeader(
                            pro = pro,
                            availability = uiState.availability,
                        )
                    }
                    item(key = "stats") { ProfileStatsStrip(pro = pro) }
                    stickyHeader(key = "tabs") {
                        ProfileTabStrip(
                            tabs = listOf(
                                ProfileTab.SERVICES,
                                ProfileTab.ABOUT,
                                ProfileTab.PORTFOLIO,
                                ProfileTab.REVIEWS,
                            ),
                            selected = selectedTab,
                            onSelect = { selectedTab = it },
                        )
                    }
                    item(key = "content") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            when (selectedTab) {
                                ProfileTab.ABOUT -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    AboutTabSection(
                                        pro = pro,
                                        onOpenWebsite = { url -> CustomTabLauncher.launchUrl(context, url) },
                                    )
                                    pro.trust?.let { trust -> TrustTabSection(trust = trust) }
                                }
                                ProfileTab.SERVICES -> if (uiState.servicesError != null) {
                                    ErrorStateCard(message = uiState.servicesError, onRetry = onRetry)
                                } else ServicesTabSection(
                                    services = uiState.services,
                                    categoryNameFor = categoryNameFor,
                                    onOpenService = onOpenService,
                                )
                                ProfileTab.PORTFOLIO -> PortfolioTabSection(portfolio = pro.portfolio)
                                ProfileTab.REVIEWS -> ReviewsTabSection(reviews = uiState.reviews)
                                ProfileTab.TRUST -> pro.trust?.let { trust -> TrustTabSection(trust = trust) }
                            }
                        }
                    }
                }
            }
        }

        if (pro != null && !uiState.loading) {
            ProfileBookBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                pro = pro,
                user = user,
                isSaved = uiState.isSaved,
                saveBusy = uiState.saveBusy,
                onBook = { bookingSheetOpen = true },
                onToggleSave = onToggleSave,
                onSignInClick = onSignInClick,
            )
        }
    }

    // Request Booking Bottom Sheet on Profile
    if (bookingSheetOpen && uiState.professional != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { bookingSheetOpen = false },
            sheetState = sheetState,
            containerColor = WarmIvory,
            contentColor = Charcoal,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (uiState.services.isNotEmpty()) {
                        "Select a service from ${uiState.professional.name}"
                    } else {
                        "Services"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = DeepForest,
                )
                if (uiState.servicesError != null) {
                    Box(Modifier.testTag("profile_sheet_services_error")) {
                        ErrorStateCard(message = uiState.servicesError, onRetry = {
                            bookingSheetOpen = false
                            onRetry()
                        })
                    }
                } else if (uiState.services.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.payment_protection_copy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    uiState.services.forEach { svc ->
                        ServixCard(
                            modifier = Modifier
                                .clickable {
                                    bookingSheetOpen = false
                                    onOpenService(svc.id)
                                }
                                .testTag("profile_sheet_service_${svc.id}"),
                            contentPadding = PaddingValues(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = svc.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Charcoal,
                                    )
                                    Text(
                                        text = "${ServixFormatters.formatNaira(svc.price)} ${svc.priceUnit}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = DeepForest,
                                    )
                                }
                                Text(
                                    text = "View →",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepForest,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "This professional has no listed services to book.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                }
                ServixSecondaryButton(
                    text = stringResource(R.string.close),
                    onClick = { bookingSheetOpen = false },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "profile_booking_sheet_close",
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    pro: ProfessionalDetailDto,
    availability: AvailabilitySummaryDto?,
) {
    val availLabel = when {
        availability?.availableToday == true -> "Free today"
        availability?.availableTomorrow == true -> "Free tomorrow"
        availability?.availableThisWeek == true -> "Free this week"
        !availability?.nextAvailableAt.isNullOrBlank() ->
            "Next: ${ServixFormatters.formatShortDayMonth(availability?.nextAvailableAt)}"
        pro.availability == "available" -> "Available now"
        else -> null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ServixAvatarImage(
                rawImageUrl = pro.image,
                name = pro.name,
                size = 72.dp,
                shape = CircleShape,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = pro.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepForest,
                        modifier = Modifier.testTag("profile_name_text"),
                    )
                    VerifiedBadgeChip(verified = pro.verified, compact = true)
                }
                Text(
                    text = pro.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                    maxLines = 2,
                )
                Text(
                    text = listOfNotNull(
                        pro.location.takeIf { it.isNotBlank() },
                        pro.responseTime.takeIf { it.isNotBlank() }?.let { "Responds ${it.lowercase()}" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
            }
        }
        if (availLabel != null) ServixBadge(
            text = availLabel,
            containerColor = SoftSand,
            contentColor = DeepForest,
        )
    }
}

/** Rating / reviews / completed projects as three compact stats (plan §5). */
@Composable
private fun ProfileStatsStrip(pro: ProfessionalDetailDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProfileStatCompact(
            value = String.format(Locale.US, "%.1f", pro.rating),
            label = "Rating",
            modifier = Modifier.weight(1f),
        )
        ProfileStatCompact(
            value = pro.reviewCount.toString(),
            label = "Reviews",
            modifier = Modifier.weight(1f),
        )
        ProfileStatCompact(
            value = pro.completedProjects.toString(),
            label = "Projects",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ProfileStatCompact(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = DeepForest)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedGray)
    }
}

/** Pill tabs that stick under the top bar while the profile content scrolls. */
@Composable
private fun ProfileTabStrip(
    tabs: List<ProfileTab>,
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
) {
    Surface(color = WarmIvory, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEach { tab ->
                val isSelected = selected == tab
                Surface(
                    onClick = { onSelect(tab) },
                    shape = CircleShape,
                    color = if (isSelected) DeepForest else PureWhite,
                    border = BorderStroke(1.dp, if (isSelected) DeepForest else BorderSand),
                    modifier = Modifier.testTag("profile_tab_${tab.name.lowercase()}"),
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) WarmIvory else Charcoal,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

/** Sticky bottom action bar: Book (primary) + Save (icon), plan §5. */
@Composable
private fun ProfileBookBar(
    modifier: Modifier = Modifier,
    pro: ProfessionalDetailDto,
    user: UserDto?,
    isSaved: Boolean?,
    saveBusy: Boolean,
    onBook: () -> Unit,
    onToggleSave: () -> Unit,
    onSignInClick: () -> Unit,
) {
    Surface(
        color = WarmIvory,
        border = BorderStroke(1.dp, BorderSand),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (pro.startingPrice > 0) Text(
                    text = "From ${ServixFormatters.formatNaira(pro.startingPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepForest,
                )
                Text(
                    text = "Protected payment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
            Surface(
                onClick = { if (user == null) onSignInClick() else onToggleSave() },
                shape = CircleShape,
                color = SoftSand,
                modifier = Modifier
                    .size(52.dp)
                    .testTag(if (user == null) "profile_sign_in_to_save_button" else "profile_save_button"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSaved == true) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = stringResource(
                            if (isSaved == true) R.string.saved_professional else R.string.save_professional
                        ),
                        tint = if (isSaved == true) ForestGreen else DeepForest,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            ServixPrimaryButton(
                text = stringResource(R.string.profile_book_cta),
                onClick = onBook,
                enabled = !saveBusy,
                modifier = Modifier.weight(1.3f),
                testTag = "profile_request_booking_button",
            )
        }
    }
}

@Composable
private fun ProfileStatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = DeepForest,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutTabSection(
    pro: ProfessionalDetailDto,
    onOpenWebsite: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ServixCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = Charcoal,
                )
                Text(
                    text = pro.about.ifBlank { "No bio added yet." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
            }
        }

        if (pro.skills.isNotEmpty()) {
            ServixCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Skills",
                        style = MaterialTheme.typography.titleMedium,
                        color = Charcoal,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        pro.skills.forEach { skill ->
                            ServixBadge(
                                text = skill,
                                containerColor = SoftSand,
                                contentColor = Charcoal,
                            )
                        }
                    }
                }
            }
        }

        val details = pro.details
        if (details != null) {
            if (details.experience.isNotEmpty()) {
                ServixCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Experience",
                            style = MaterialTheme.typography.titleMedium,
                            color = Charcoal,
                        )
                        details.experience.forEach { exp ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = buildString {
                                        append(exp.title)
                                        if (exp.company.isNotBlank()) append(" · ${exp.company}")
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Charcoal,
                                )
                                if (exp.start.isNotBlank() || exp.end.isNotBlank()) {
                                    Text(
                                        text = listOf(exp.start, exp.end).filter { it.isNotBlank() }.joinToString(" – "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedGray,
                                    )
                                }
                                if (exp.description.isNotBlank()) {
                                    Text(
                                        text = exp.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedGray,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (details.education.isNotEmpty()) {
                ServixCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Education",
                            style = MaterialTheme.typography.titleMedium,
                            color = Charcoal,
                        )
                        details.education.forEach { edu ->
                            Text(
                                text = buildString {
                                    append(edu.school)
                                    if (edu.degree.isNotBlank()) append(" · ${edu.degree}")
                                    if (edu.year.isNotBlank()) append(" · ${edu.year}")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Charcoal,
                            )
                        }
                    }
                }
            }

            if (details.certifications.isNotEmpty()) {
                ServixCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Certifications",
                            style = MaterialTheme.typography.titleMedium,
                            color = Charcoal,
                        )
                        details.certifications.forEach { cert ->
                            Text(
                                text = buildString {
                                    append(cert.name)
                                    if (cert.issuer.isNotBlank()) append(" · ${cert.issuer}")
                                    if (cert.year.isNotBlank()) append(" · ${cert.year}")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Charcoal,
                            )
                        }
                    }
                }
            }

            if (details.languages.isNotEmpty() || details.website.isNotBlank() || details.occupation.isNotBlank()) {
                ServixCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.titleMedium,
                            color = Charcoal,
                        )
                        if (details.occupation.isNotBlank()) {
                            Text(
                                text = "Occupation: ${details.occupation}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedGray,
                            )
                        }
                        if (details.languages.isNotEmpty()) {
                            val langs = details.languages.joinToString(", ") {
                                if (it.level.isNotBlank()) "${it.name} (${it.level})" else it.name
                            }
                            Text(
                                text = "Languages: $langs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedGray,
                            )
                        }
                        if (details.website.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable { onOpenWebsite(details.website) },
                            ) {
                                Text(
                                    text = "Website: ${details.website}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepForest,
                                )
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = "Open website",
                                    tint = DeepForest,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustTabSection(trust: TrustSummaryDto) {
    val notEnough = "Not enough data"
    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Trust & performance",
                style = MaterialTheme.typography.titleMedium,
                color = Charcoal,
            )
            Text(
                text = "Calculated from real Servix bookings and reviews — nothing is self-reported.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
            val rows = listOf(
                Triple(
                    "Delivery reliability",
                    if (trust.reliability?.enough == true && trust.reliability.percent != null) "${trust.reliability.percent}%" else notEnough,
                    if (trust.reliability?.enough == true) "Based on ${trust.reliability.measurable ?: 0} deliveries with an agreed deadline"
                    else "Shown after ${trust.reliability?.minimum ?: 5} deliveries with an agreed deadline",
                ),
                Triple(
                    "Response rate",
                    if (trust.responseRate?.enough == true && trust.responseRate.percent != null) "${trust.responseRate.percent}%" else notEnough,
                    if (trust.responseRate?.enough == true) "Replies to paid requests"
                    else "Shown after ${trust.responseRate?.minimum ?: 5} paid requests",
                ),
                Triple(
                    "Repeat customers",
                    if (trust.repeatCustomers?.enough == true && trust.repeatCustomers.percent != null) "${trust.repeatCustomers.percent}%" else notEnough,
                    if (trust.repeatCustomers?.enough == true) "Of ${trust.repeatCustomers.customers ?: 0} customers, this share booked again"
                    else "Shown after ${trust.repeatCustomers?.minimum ?: 5} customers",
                ),
                Triple(
                    "Completed jobs",
                    trust.completedJobs.toString(),
                    "${trust.verifiedProjects} shown as Verified Servix Projects",
                ),
            )
            rows.forEach { (label, value, hint) ->
                HorizontalDivider(color = SoftSand)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = label, style = MaterialTheme.typography.titleSmall, color = Charcoal)
                        Text(text = value, style = MaterialTheme.typography.labelLarge, color = DeepForest)
                    }
                    Text(text = hint, style = MaterialTheme.typography.bodySmall, color = MutedGray)
                }
            }
        }
    }
}

@Composable
private fun ServicesTabSection(
    services: List<ServiceSummaryDto>,
    categoryNameFor: (String?) -> String,
    onOpenService: (String) -> Unit,
) {
    if (services.isEmpty()) {
        EmptyStateCard(
            title = "No services listed",
            message = "This professional has not published any services yet.",
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Service offerings & pricing", style = MaterialTheme.typography.titleLarge, color = DeepForest)
        services.forEach { service ->
            ServiceCardItem(
                service = service,
                categoryName = categoryNameFor(service.categoryId),
                onClick = { onOpenService(service.id) },
            )
        }
    }
}

@Composable
private fun PortfolioTabSection(portfolio: List<PortfolioItemDto>) {
    if (portfolio.isEmpty()) {
        ServixCard {
            Text(
                text = "This professional hasn’t added portfolio items yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        portfolio.forEach { item ->
            ServixCard(contentPadding = PaddingValues(0.dp)) {
                Column {
                    ServixMediaImage(
                        rawImageUrl = item.image,
                        contentDescription = item.title,
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Charcoal,
                        )
                        if (item.category.isNotBlank()) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                        if (item.description.isNotBlank()) {
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Charcoal,
                            )
                        }
                        if (item.verified) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = "Verified Servix Project",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = "Verified Servix Project",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepForest,
                                )
                            }
                            val metaParts = listOfNotNull(
                                item.completedAt?.let { "Completed ${ServixFormatters.formatDate(it)}" },
                                item.deliveryDays?.let { "$it day${if (it == 1) "" else "s"}" },
                                item.customerRating?.let { "Rated $it/5" },
                            )
                            if (metaParts.isNotEmpty()) {
                                Text(
                                    text = metaParts.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedGray,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReviewsTabSection(reviews: List<ReviewDto>) {
    if (reviews.isEmpty()) {
        EmptyStateCard(
            title = "No reviews yet",
            message = "This professional has no reviews yet.",
        )
        return
    }
    Column(
        modifier = Modifier.testTag("profile_reviews_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        reviews.forEach { review ->
            ServixCard(contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = review.author,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Charcoal,
                            )
                            RatingRow(rating = review.rating.toDouble(), showCount = false)
                        }
                        Text(
                            text = ServixFormatters.formatDate(review.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                    if (review.text.isNotBlank()) {
                        Text(
                            text = review.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Charcoal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileLoadingSkeleton() {
    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SkeletonBox(modifier = Modifier.size(84.dp), height = 84.dp, shape = CircleShape)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f), height = 22.dp)
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.45f), height = 16.dp)
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.75f), height = 14.dp)
                }
            }
            SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 48.dp)
        }
    }
    SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 120.dp)
}
