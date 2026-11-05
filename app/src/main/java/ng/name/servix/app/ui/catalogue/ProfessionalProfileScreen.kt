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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
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
            val services = servicesDeferred.await().getOrDefault(emptyList())
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    var selectedTab by rememberSaveable { mutableStateOf(ProfileTab.ABOUT) }
    var bookingSheetOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Top navigation & Breadcrumb (ProfessionalProfilePage.jsx:99-105)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("profile_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = DeepForest,
                )
            }
            Text(
                text = buildString {
                    append("Professionals")
                    uiState.professional?.name?.takeIf { it.isNotBlank() }?.let {
                        append(" / $it")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }

        if (uiState.loading) {
            ProfileLoadingSkeleton()
            return@Column
        }

        if (uiState.notFound) {
            EmptyStateCard(
                title = stringResource(R.string.pro_not_found_title),
                message = stringResource(R.string.pro_not_found_message),
                actionLabel = stringResource(R.string.home_browse_all_pros),
                onActionClick = onBrowseProfessionalsClick,
            )
            return@Column
        }

        if (uiState.error != null || uiState.professional == null) {
            ErrorStateCard(
                message = uiState.error ?: stringResource(R.string.error_load_profile),
                onRetry = onRetry,
            )
            return@Column
        }

        val pro = uiState.professional
        val availableTabs = remember(pro.trust) {
            if (pro.trust != null) {
                listOf(
                    ProfileTab.ABOUT,
                    ProfileTab.TRUST,
                    ProfileTab.SERVICES,
                    ProfileTab.PORTFOLIO,
                    ProfileTab.REVIEWS,
                )
            } else {
                listOf(
                    ProfileTab.ABOUT,
                    ProfileTab.SERVICES,
                    ProfileTab.PORTFOLIO,
                    ProfileTab.REVIEWS,
                )
            }
        }

        // Profile Header Card (ProfessionalProfilePage.jsx:108-147)
        ServixCard(contentPadding = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ServixAvatarImage(
                        rawImageUrl = pro.image,
                        name = pro.name,
                        size = 84.dp,
                        shape = CircleShape,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = pro.name,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Charcoal,
                                modifier = Modifier.testTag("profile_name_text"),
                            )
                            VerifiedBadgeChip(verified = pro.verified)
                        }
                        if (pro.plan in listOf("pro", "team", "enterprise")) {
                            val planLabel = when (pro.plan) {
                                "pro" -> "Servix Pro"
                                "team" -> "Servix Team"
                                else -> "Servix Enterprise"
                            }
                            ServixBadge(
                                text = planLabel,
                                containerColor = DeepForest,
                                contentColor = WarmIvory,
                            )
                        }
                        Text(
                            text = pro.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MutedGray,
                        )
                    }
                }

                // Meta row: location, responseTime, memberSince
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (pro.location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = pro.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                    }
                    if (pro.responseTime.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "Responds ${pro.responseTime.lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                    }
                    if (pro.memberSince.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "Member since ${pro.memberSince}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                    }
                }

                // Availability status chip (ProfessionalProfilePage.jsx:141-145)
                val availLabel = remember(uiState.availability, pro.availability) {
                    val avail = uiState.availability
                    when {
                        avail?.availableToday == true -> "Free today"
                        avail?.availableTomorrow == true -> "Free tomorrow"
                        avail?.availableThisWeek == true -> "Free this week"
                        !avail?.nextAvailableAt.isNullOrBlank() ->
                            "Next: ${ServixFormatters.formatShortDayMonth(avail?.nextAvailableAt)}"
                        pro.availability == "available" -> "Available now"
                        else -> "Limited availability"
                    }
                }
                ServixBadge(
                    text = availLabel,
                    containerColor = SoftSand,
                    contentColor = DeepForest,
                )

                // Primary & Secondary CTAs: Request Booking + Save professional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ServixPrimaryButton(
                        text = stringResource(R.string.request_booking),
                        onClick = { bookingSheetOpen = true },
                        modifier = Modifier.weight(1f),
                        testTag = "profile_request_booking_button",
                    )

                    if (user == null) {
                        ServixSecondaryButton(
                            text = stringResource(R.string.sign_in_to_save),
                            onClick = onSignInClick,
                            modifier = Modifier.weight(1f),
                            testTag = "profile_sign_in_to_save_button",
                        )
                    } else {
                        val saved = uiState.isSaved == true
                        ServixSecondaryButton(
                            text = if (saved) {
                                "✓ ${stringResource(R.string.saved_professional)}"
                            } else {
                                stringResource(R.string.save_professional)
                            },
                            enabled = !uiState.saveBusy && uiState.isSaved != null,
                            onClick = onToggleSave,
                            modifier = Modifier.weight(1f),
                            testTag = "profile_save_button",
                        )
                    }
                }

                if (!uiState.saveError.isNullOrBlank()) {
                    Text(
                        text = uiState.saveError,
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed,
                    )
                }
            }
        }

        // 4-Stat Strip (ProfessionalProfilePage.jsx:149-166)
        ServixCard(contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ProfileStatCell(
                        value = String.format(Locale.US, "%.1f", pro.rating),
                        label = "Rating",
                        modifier = Modifier.weight(1f),
                    )
                    ProfileStatCell(
                        value = pro.reviewCount.toString(),
                        label = "Reviews",
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider(color = SoftSand)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val completedLabel = buildString {
                        append("Completed projects")
                        if (pro.verifiedProjects > 0) {
                            append(" · ${pro.verifiedProjects} verified")
                        }
                    }
                    ProfileStatCell(
                        value = pro.completedProjects.toString(),
                        label = completedLabel,
                        modifier = Modifier.weight(1f),
                    )
                    ProfileStatCell(
                        value = ServixFormatters.formatNaira(pro.startingPrice),
                        label = "Starting price",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Section Tabs (About, Trust & performance, Services, Portfolio, Reviews)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            availableTabs.forEach { tab ->
                val isSelected = selectedTab == tab
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) DeepForest else PureWhite,
                    border = BorderStroke(1.dp, if (isSelected) DeepForest else BorderSand),
                    modifier = Modifier
                        .clickable { selectedTab = tab }
                        .testTag("profile_tab_${tab.name.lowercase()}"),
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

        // Tab Content
        when (selectedTab) {
            ProfileTab.ABOUT -> {
                AboutTabSection(
                    pro = pro,
                    onOpenWebsite = { url -> CustomTabLauncher.launchUrl(context, url) },
                )
            }
            ProfileTab.TRUST -> {
                pro.trust?.let { trust ->
                    TrustTabSection(trust = trust)
                }
            }
            ProfileTab.SERVICES -> {
                ServicesTabSection(
                    services = uiState.services,
                    categoryNameFor = categoryNameFor,
                    onOpenService = onOpenService,
                )
            }
            ProfileTab.PORTFOLIO -> {
                PortfolioTabSection(portfolio = pro.portfolio)
            }
            ProfileTab.REVIEWS -> {
                ReviewsTabSection(reviews = uiState.reviews)
            }
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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (uiState.services.isNotEmpty()) {
                        "Select a service from ${uiState.professional.name}"
                    } else {
                        "Booking coming soon"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = DeepForest,
                )
                if (uiState.services.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.escrow_copy),
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
                    ServixBadge(
                        text = "Pre-launch preview",
                        containerColor = SoftSand,
                        contentColor = DeepForest,
                    )
                    Text(
                        text = "Direct booking with professionals launches with the full Servix platform. Nothing has been booked and no payment has been taken.",
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
