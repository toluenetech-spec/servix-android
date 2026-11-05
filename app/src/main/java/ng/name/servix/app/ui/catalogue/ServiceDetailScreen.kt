package ng.name.servix.app.ui.catalogue

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import ng.name.servix.app.data.model.ReviewDto
import ng.name.servix.app.data.model.ServiceDetailDto
import ng.name.servix.app.data.model.ServiceFaqDto
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.repository.BookingRepository
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
import java.util.UUID

data class ServiceDetailUiState(
    val slug: String = "",
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val error: String? = null,
    val service: ServiceDetailDto? = null,
    val reviews: List<ReviewDto> = emptyList(),
    val availabilitySlots: List<String> = emptyList(),
    val availabilityLoading: Boolean = false,
    val selectedSlot: String = "",
    val bookingNotes: String = "",
    val rebookedFrom: String? = null,
    val bookingBusy: Boolean = false,
    val bookingError: String? = null,
)

class ServiceDetailViewModel(
    private val catalogueRepository: CatalogueRepository,
    private val bookingRepository: BookingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceDetailUiState())
    val uiState: StateFlow<ServiceDetailUiState> = _uiState.asStateFlow()

    fun loadService(
        slug: String,
        rebookedFrom: String? = null,
        initialNotes: String? = null,
        forceRefresh: Boolean = false,
    ) {
        if (!forceRefresh && _uiState.value.slug == slug && _uiState.value.service != null && _uiState.value.error == null) {
            if (rebookedFrom != null || initialNotes != null) {
                _uiState.update {
                    it.copy(
                        rebookedFrom = rebookedFrom ?: it.rebookedFrom,
                        bookingNotes = initialNotes ?: it.bookingNotes,
                    )
                }
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                ServiceDetailUiState(
                    slug = slug,
                    loading = true,
                    rebookedFrom = rebookedFrom,
                    bookingNotes = initialNotes.orEmpty(),
                )
            }

            val serviceDeferred = async { catalogueRepository.getService(slug) }
            val reviewsDeferred = async { catalogueRepository.getServiceReviews(slug) }
            val availDeferred = async { catalogueRepository.getServiceAvailability(slug, days = 14) }

            val serviceRes = serviceDeferred.await()
            if (serviceRes.isFailure) {
                val ex = serviceRes.exceptionOrNull() as? ServixApiException
                _uiState.update {
                    it.copy(
                        loading = false,
                        notFound = ex?.status == 404,
                        error = ex?.message ?: "We couldn't load this service. Please try again.",
                    )
                }
                return@launch
            }

            val service = serviceRes.getOrThrow()
            val reviews = reviewsDeferred.await().getOrDefault(emptyList())
            val slots = availDeferred.await().getOrNull()?.slots.orEmpty()

            _uiState.update {
                it.copy(
                    loading = false,
                    notFound = false,
                    error = null,
                    service = service,
                    reviews = reviews,
                    availabilitySlots = slots,
                    selectedSlot = slots.firstOrNull().orEmpty(),
                )
            }

            // Fire-and-forget view count (ServiceDetailPage.jsx:171)
            catalogueRepository.countView("service", service.id)
        }
    }

    fun selectSlot(slotIso: String) {
        _uiState.update { it.copy(selectedSlot = slotIso, bookingError = null) }
    }

    fun updateBookingNotes(notes: String) {
        if (notes.length <= 2000) {
            _uiState.update { it.copy(bookingNotes = notes, bookingError = null) }
        }
    }

    fun clearBookingError() {
        _uiState.update { it.copy(bookingError = null) }
    }

    fun refreshAvailability() {
        val slug = _uiState.value.slug.ifBlank { return }
        viewModelScope.launch {
            _uiState.update { it.copy(availabilityLoading = true) }
            val slots = bookingRepository.getServiceAvailability(slug, days = 14)
                .getOrNull()
                ?.slots
                .orEmpty()
            _uiState.update {
                it.copy(
                    availabilityLoading = false,
                    availabilitySlots = slots,
                    selectedSlot = if (it.selectedSlot in slots) it.selectedSlot else slots.firstOrNull().orEmpty(),
                )
            }
        }
    }

    /**
     * Mirrors ServiceDetailPage.jsx:96-114:
     * 1. POST /bookings { serviceId, scheduledAt, notes?, rebookedFrom? }
     * 2. POST /bookings/:id/pay -> { authorizationUrl, reference }
     * If SLOT_TAKEN (409), refreshes availability and clears selectedSlot.
     */
    fun submitBookAndPay(
        onCreatedAndPayReady: (bookingId: String, authorizationUrl: String) -> Unit,
    ) {
        val current = _uiState.value
        val service = current.service ?: return
        val slot = current.selectedSlot.trim()
        if (slot.isEmpty() || current.bookingBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(bookingBusy = true, bookingError = null) }

            val idempotencyKey = if (!current.rebookedFrom.isNullOrBlank()) {
                "rebook-${current.rebookedFrom}-$slot-${UUID.randomUUID().toString().take(8)}"
            } else {
                "book-${service.id}-$slot-${UUID.randomUUID().toString().take(8)}"
            }

            val createRes = bookingRepository.createBooking(
                serviceId = service.id,
                scheduledAt = slot,
                notes = current.bookingNotes.trim().takeIf { it.isNotEmpty() },
                rebookedFrom = current.rebookedFrom,
                idempotencyKey = idempotencyKey,
            )

            if (createRes.isFailure) {
                val err = createRes.exceptionOrNull()
                val apiEx = err as? ServixApiException
                if (apiEx?.code == "SLOT_TAKEN") {
                    val refreshed = bookingRepository.getServiceAvailability(service.id, days = 14)
                        .getOrNull()
                        ?.slots
                        .orEmpty()
                    _uiState.update {
                        it.copy(
                            bookingBusy = false,
                            availabilitySlots = refreshed,
                            selectedSlot = refreshed.firstOrNull().orEmpty(),
                            bookingError = apiEx.message,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            bookingBusy = false,
                            bookingError = err?.message ?: "Could not create the booking.",
                        )
                    }
                }
                return@launch
            }

            val booking = createRes.getOrThrow()
            val payRes = bookingRepository.payBooking(booking.id)
            if (payRes.isFailure) {
                val err = payRes.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        bookingBusy = false,
                        bookingError = err?.message ?: "Could not start payment.",
                    )
                }
                return@launch
            }

            val payData = payRes.getOrThrow()
            _uiState.update { it.copy(bookingBusy = false, bookingError = null) }
            onCreatedAndPayReady(booking.id, payData.authorizationUrl)
        }
    }

    fun categoryNameFor(slug: String?): String = catalogueRepository.categoryNameFor(slug)

    companion object {
        fun factory(
            catalogueRepository: CatalogueRepository,
            bookingRepository: BookingRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ServiceDetailViewModel(catalogueRepository, bookingRepository) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServiceDetailScreen(
    user: UserDto?,
    uiState: ServiceDetailUiState,
    openBookingSheetInitially: Boolean = false,
    categoryNameFor: (String?) -> String,
    onSelectSlot: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmitBookAndPay: (onReady: (bookingId: String, authorizationUrl: String) -> Unit) -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onBrowseServicesClick: () -> Unit,
    onOpenProfessional: (slug: String) -> Unit,
    onSignInToBookClick: () -> Unit,
    onCreateAccountToBookClick: () -> Unit,
    onNavigateToBookingAfterPay: (bookingId: String) -> Unit,
) {
    BackHandler(onBack = onBackClick)

    var bookingSheetOpen by rememberSaveable { mutableStateOf(openBookingSheetInitially) }
    var selectedImageIndex by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(openBookingSheetInitially) {
        if (openBookingSheetInitially) {
            bookingSheetOpen = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Back button + Breadcrumb (ServiceDetailPage.jsx:211-220)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("service_detail_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = DeepForest,
                )
            }
            Text(
                text = buildString {
                    append("Services")
                    uiState.service?.let { svc ->
                        val cat = categoryNameFor(svc.categoryId)
                        if (cat.isNotBlank()) append(" / $cat")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }

        if (uiState.loading) {
            ServiceDetailSkeleton()
            return@Column
        }

        if (uiState.notFound) {
            EmptyStateCard(
                title = stringResource(R.string.service_not_found_title),
                message = stringResource(R.string.service_not_found_message),
                actionLabel = "Browse all services",
                onActionClick = onBrowseServicesClick,
            )
            return@Column
        }

        if (uiState.error != null || uiState.service == null) {
            ErrorStateCard(
                message = uiState.error ?: stringResource(R.string.error_load_service_detail),
                onRetry = onRetry,
            )
            return@Column
        }

        val service = uiState.service
        val categoryName = categoryNameFor(service.categoryId)

        // Category, Title, Rating, Location, Duration (ServiceDetailPage.jsx:225-246)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = service.title,
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
                modifier = Modifier.testTag("service_detail_title"),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                RatingRow(rating = service.rating, reviewCount = service.reviewCount)
                if (service.location.isNotBlank()) {
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
                            text = service.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                }
                if (service.duration.isNotBlank()) {
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
                            text = service.duration,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                }
            }
        }

        // Gallery (images; video/PDF items shown as a link to the website)
        val galleryUrls = remember(service) {
            val fromGallery = service.gallery.filter { it.isNotBlank() }
            if (fromGallery.isNotEmpty()) {
                fromGallery
            } else {
                listOfNotNull(service.image?.takeIf { it.isNotBlank() })
            }
        }
        val activeIdx = selectedImageIndex.coerceIn(0, (galleryUrls.size - 1).coerceAtLeast(0))
        val activeUrl = galleryUrls.getOrNull(activeIdx)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ServixMediaImage(
                rawImageUrl = activeUrl,
                contentDescription = service.title,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .testTag("service_detail_main_image"),
            )
            if (galleryUrls.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    galleryUrls.forEachIndexed { idx, imgUrl ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                width = if (idx == activeIdx) 2.dp else 1.dp,
                                color = if (idx == activeIdx) DeepForest else BorderSand,
                            ),
                            modifier = Modifier
                                .size(width = 76.dp, height = 56.dp)
                                .clickable { selectedImageIndex = idx }
                                .testTag("service_gallery_thumb_$idx"),
                        ) {
                            ServixMediaImage(
                                rawImageUrl = imgUrl,
                                contentDescription = "${service.title} image ${idx + 1}",
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }

            // Video item link to website (Stage 2 requirement)
            val videoUrl = service.video?.url?.takeIf { it.isNotBlank() }
            if (videoUrl != null) {
                val resolvedVideo = ServixFormatters.resolveServixMediaUrl(videoUrl) ?: videoUrl
                ServixCard(
                    modifier = Modifier
                        .clickable { CustomTabLauncher.launchUrl(context, resolvedVideo) }
                        .testTag("service_video_web_link"),
                    containerColor = SoftSand,
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircleOutline,
                            contentDescription = null,
                            tint = DeepForest,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = service.video.fileName.ifBlank { "Watch service video on www.servix.name.ng" },
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepForest,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = "Open video",
                            tint = DeepForest,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // Professional Strip Card (ServiceDetailPage.jsx:258-272)
        service.professional?.let { pro ->
            ServixCard(
                modifier = Modifier
                    .clickable { onOpenProfessional(pro.id) }
                    .testTag("service_detail_pro_card"),
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ServixAvatarImage(
                        rawImageUrl = pro.image,
                        name = pro.name,
                        size = 52.dp,
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
                                style = MaterialTheme.typography.titleSmall,
                                color = Charcoal,
                            )
                            VerifiedBadgeChip(verified = pro.verified, compact = true)
                        }
                        Text(
                            text = "Professional on Servix",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                    Text(
                        text = stringResource(R.string.view_profile),
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepForest,
                    )
                }
            }
        }

        // Booking Card (ServiceDetailPage.jsx:382-423)
        ServixCard(contentPadding = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = ServixFormatters.formatNaira(service.price),
                        style = MaterialTheme.typography.headlineMedium,
                        color = DeepForest,
                        modifier = Modifier.testTag("service_detail_price"),
                    )
                    Text(
                        text = service.priceUnit,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val deliveryText = if (service.deliveryDays != null) {
                        "Delivery: ${if (service.deliveryDays == 1) "1 day" else "${service.deliveryDays} days"}"
                    } else {
                        "Duration: ${service.duration.ifBlank { "On request" }}"
                    }
                    MetaRowItem(icon = Icons.Outlined.Schedule, text = deliveryText)

                    if (service.revisions != null) {
                        val revText = when {
                            service.revisions == 0 -> "No revisions"
                            service.revisions >= 20 -> "Unlimited revisions"
                            service.revisions == 1 -> "1 revision"
                            else -> "${service.revisions} revisions"
                        }
                        MetaRowItem(icon = Icons.Outlined.Edit, text = revText)
                    }

                    if (service.location.isNotBlank()) {
                        MetaRowItem(icon = Icons.Outlined.LocationOn, text = service.location)
                    }

                    val availText = if (service.availability == "available") {
                        "Available now"
                    } else {
                        "Limited availability"
                    }
                    MetaRowItem(icon = Icons.Outlined.CalendarMonth, text = availText)
                }

                ServixPrimaryButton(
                    text = stringResource(R.string.request_booking),
                    onClick = { bookingSheetOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "service_request_booking_button",
                )

                Text(
                    text = stringResource(R.string.no_payment_taken_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )

                HorizontalDivider(color = SoftSand)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Charcoal)) {
                                append("${stringResource(R.string.servix_protection_title)} ")
                            }
                            append(stringResource(R.string.servix_protection_body))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                }
            }
        }

        // About this service (ServiceDetailPage.jsx:274-292)
        ServixCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "About this service",
                    style = MaterialTheme.typography.titleMedium,
                    color = Charcoal,
                )
                Text(
                    text = service.description.ifBlank { service.shortDescription },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal,
                )

                if (service.searchTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        service.searchTags.forEach { tag ->
                            ServixBadge(
                                text = tag,
                                containerColor = SoftSand,
                                contentColor = Charcoal,
                            )
                        }
                    }
                }

                if (service.documents.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        service.documents.forEach { doc ->
                            val docUrl = ServixFormatters.resolveServixMediaUrl(doc.url) ?: doc.url
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clickable { CustomTabLauncher.launchUrl(context, docUrl) }
                                    .padding(vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = DeepForest,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = doc.fileName.ifBlank { "Download document (PDF)" },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepForest,
                                )
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = null,
                                    tint = DeepForest,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // What's included (ServiceDetailPage.jsx:294-304)
        if (service.included.isNotEmpty()) {
            ServixCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "What’s included",
                        style = MaterialTheme.typography.titleMedium,
                        color = Charcoal,
                    )
                    service.included.forEach { item ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = ForestGreen,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Charcoal,
                            )
                        }
                    }
                }
            }
        }

        // What the professional needs from you (ServiceDetailPage.jsx:306-326)
        if (service.requirements.isNotEmpty()) {
            ServixCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "What the professional needs from you",
                        style = MaterialTheme.typography.titleMedium,
                        color = Charcoal,
                    )
                    service.requirements.forEach { req ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = DeepForest,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = buildString {
                                    append(req.question)
                                    if (req.required) append(" (required)")
                                    if (req.type == "choice" && req.options.isNotEmpty()) {
                                        append(" — ${req.options.joinToString(" / ")}")
                                    }
                                    if (req.type == "file") {
                                        append(" — attach a file")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Charcoal,
                            )
                        }
                    }
                }
            }
        }

        // Availability preview from live GET /services/:slug/availability
        AvailabilityPreviewSection(slots = uiState.availabilitySlots)

        // Reviews (ServiceDetailPage.jsx:348-371)
        ServixCard(modifier = Modifier.testTag("service_reviews_card")) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Reviews",
                    style = MaterialTheme.typography.titleMedium,
                    color = Charcoal,
                )
                if (uiState.reviews.isEmpty()) {
                    Text(
                        text = "No reviews yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGray,
                    )
                } else {
                    uiState.reviews.forEachIndexed { idx, review ->
                        if (idx > 0) HorizontalDivider(color = SoftSand)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

        // Frequently asked questions (ServiceDetailPage.jsx:373-378)
        if (service.faqs.isNotEmpty()) {
            FaqAccordionSection(faqs = service.faqs)
        }
    }

    // Stage 3: Booking Bottom Sheet (ServiceDetailPage.jsx:58-160, 426-446)
    if (bookingSheetOpen && uiState.service != null) {
        val svc = uiState.service
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!uiState.bookingBusy) bookingSheetOpen = false },
            sheetState = sheetState,
            containerColor = WarmIvory,
            contentColor = Charcoal,
        ) {
            BookingSheetContent(
                user = user,
                service = svc,
                slots = uiState.availabilitySlots,
                availabilityLoading = uiState.availabilityLoading,
                selectedSlot = uiState.selectedSlot,
                notes = uiState.bookingNotes,
                rebookedFrom = uiState.rebookedFrom,
                busy = uiState.bookingBusy,
                error = uiState.bookingError,
                onSelectSlot = onSelectSlot,
                onNotesChange = onNotesChange,
                onBookAndPayClick = {
                    onSubmitBookAndPay { bookingId, authorizationUrl ->
                        bookingSheetOpen = false
                        CustomTabLauncher.launchUrl(context, authorizationUrl)
                        onNavigateToBookingAfterPay(bookingId)
                    }
                },
                onCloseClick = { bookingSheetOpen = false },
                onSignInClick = {
                    bookingSheetOpen = false
                    onSignInToBookClick()
                },
                onCreateAccountClick = {
                    bookingSheetOpen = false
                    onCreateAccountToBookClick()
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BookingSheetContent(
    user: UserDto?,
    service: ServiceDetailDto,
    slots: List<String>,
    availabilityLoading: Boolean,
    selectedSlot: String,
    notes: String,
    rebookedFrom: String?,
    busy: Boolean,
    error: String?,
    onSelectSlot: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onBookAndPayClick: () -> Unit,
    onCloseClick: () -> Unit,
    onSignInClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
) {
    val groupedDays = remember(slots) {
        val map = LinkedHashMap<String, MutableList<String>>()
        for (iso in slots) {
            val dayKey = ServixFormatters.formatSlotDayHeader(iso)
            map.getOrPut(dayKey) { mutableListOf() }.add(iso)
        }
        map
    }

    var selectedDayKey by remember(groupedDays, selectedSlot) {
        val matchingDay = groupedDays.entries.firstOrNull { (_, daySlots) -> selectedSlot in daySlots }?.key
        mutableStateOf(matchingDay ?: groupedDays.keys.firstOrNull().orEmpty())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 24.dp)
            .testTag("booking_sheet_container"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.booking_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = DeepForest,
            )
            if (!rebookedFrom.isNullOrBlank()) {
                ServixBadge(
                    text = "Book again",
                    containerColor = SoftSand,
                    contentColor = DeepForest,
                )
            }
        }

        if (user == null) {
            // Guest flow from ServiceDetailPage.jsx:74-85
            Text(
                text = stringResource(R.string.sign_in_to_book),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ServixPrimaryButton(
                    text = stringResource(R.string.sign_in),
                    onClick = onSignInClick,
                    modifier = Modifier.weight(1f),
                    testTag = "service_sheet_sign_in_button",
                )
                ServixSecondaryButton(
                    text = stringResource(R.string.create_account),
                    onClick = onCreateAccountClick,
                    modifier = Modifier.weight(1f),
                    testTag = "service_sheet_create_account_button",
                )
            }
            return@Column
        }

        // Price & escrow line verbatim from ServiceDetailPage.jsx:118-122
        Text(
            text = stringResource(
                R.string.booking_sheet_price_line,
                ServixFormatters.formatNaira(service.price),
                service.priceUnit,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
            modifier = Modifier.testTag("booking_sheet_price_line"),
        )

        if (availabilityLoading) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(4) {
                    SkeletonBox(modifier = Modifier.width(88.dp), height = 36.dp)
                }
            }
        } else if (slots.isEmpty()) {
            Text(
                text = stringResource(R.string.booking_sheet_no_slots),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${stringResource(R.string.booking_sheet_choose_time)} * (Africa/Lagos)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Charcoal,
                )

                // Day selector row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groupedDays.keys.forEachIndexed { dayIndex, dayLabel ->
                        val isDaySelected = dayLabel == selectedDayKey
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDaySelected) DeepForest else PureWhite,
                            border = BorderStroke(1.dp, if (isDaySelected) DeepForest else BorderSand),
                            modifier = Modifier
                                .clickable {
                                    selectedDayKey = dayLabel
                                    val firstForDay = groupedDays[dayLabel]?.firstOrNull()
                                    if (!firstForDay.isNullOrBlank()) {
                                        onSelectSlot(firstForDay)
                                    }
                                }
                                .testTag("booking_day_chip_$dayIndex"),
                        ) {
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isDaySelected) WarmIvory else Charcoal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                // Time slot pills for the selected day
                val daySlots = groupedDays[selectedDayKey].orEmpty()
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    daySlots.forEachIndexed { slotIndex, slotIso ->
                        val isSelected = slotIso == selectedSlot
                        val timeText = ServixFormatters.formatSlotTime(slotIso)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) DeepForest else PureWhite,
                            border = BorderStroke(1.dp, if (isSelected) DeepForest else BorderSand),
                            modifier = Modifier
                                .clickable { onSelectSlot(slotIso) }
                                .testTag("booking_slot_chip_$slotIndex"),
                        ) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) WarmIvory else Charcoal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            )
                        }
                    }
                }

                if (selectedSlot.isNotBlank()) {
                    Text(
                        text = "Selected: ${ServixFormatters.formatDateTimeFull(selectedSlot)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepForest,
                        modifier = Modifier.testTag("booking_selected_slot_summary"),
                    )
                }
            }
        }

        // Notes for the professional (optional, <= 2000 chars)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.booking_sheet_notes_label),
                style = MaterialTheme.typography.labelLarge,
                color = Charcoal,
            )
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                minLines = 3,
                maxLines = 5,
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
                    .testTag("booking_notes_input"),
            )
        }

        if (!error.isNullOrBlank()) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = DangerRed,
                modifier = Modifier.testTag("booking_sheet_error_text"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServixPrimaryButton(
                text = if (busy) {
                    stringResource(R.string.booking_sheet_redirecting)
                } else {
                    stringResource(
                        R.string.booking_sheet_book_and_pay,
                        ServixFormatters.formatNaira(service.price),
                    )
                },
                onClick = onBookAndPayClick,
                enabled = selectedSlot.isNotBlank() && !busy,
                loading = busy,
                modifier = Modifier.weight(1.4f),
                testTag = "booking_book_and_pay_button",
            )
            ServixSecondaryButton(
                text = stringResource(R.string.cancel),
                onClick = onCloseClick,
                enabled = !busy,
                modifier = Modifier.weight(0.8f),
                testTag = "booking_cancel_sheet_button",
            )
        }
    }
}

@Composable
private fun MetaRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MutedGray,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Charcoal,
        )
    }
}

@Composable
private fun AvailabilityPreviewSection(slots: List<String>) {
    val groupedByDay = remember(slots) {
        val map = LinkedHashMap<String, MutableList<String>>()
        for (iso in slots) {
            val dayKey = ServixFormatters.formatSlotDayHeader(iso)
            map.getOrPut(dayKey) { mutableListOf() }.add(iso)
        }
        map.entries.take(7)
    }

    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Availability preview",
                style = MaterialTheme.typography.titleMedium,
                color = Charcoal,
            )
            if (groupedByDay.isEmpty()) {
                Text(
                    text = "No open slots in the next two weeks.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groupedByDay.forEach { (dayLabel, daySlots) ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SoftSand,
                            border = BorderStroke(1.dp, BorderSand),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Charcoal,
                                )
                                Text(
                                    text = "${daySlots.size} slot${if (daySlots.size == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepForest,
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
private fun FaqAccordionSection(faqs: List<ServiceFaqDto>) {
    var expandedIndex by rememberSaveable { mutableIntStateOf(0) }
    ServixCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Frequently asked questions",
                style = MaterialTheme.typography.titleMedium,
                color = Charcoal,
            )
            faqs.forEachIndexed { idx, faq ->
                if (idx > 0) HorizontalDivider(color = SoftSand)
                val expanded = expandedIndex == idx
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedIndex = if (expanded) -1 else idx }
                        .padding(vertical = 6.dp)
                        .testTag("service_faq_$idx"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = faq.q,
                            style = MaterialTheme.typography.titleSmall,
                            color = Charcoal,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MutedGray,
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Text(
                            text = faq.a,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedGray,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceDetailSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f), height = 14.dp)
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.85f), height = 28.dp)
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 220.dp)
        SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 140.dp)
    }
}
