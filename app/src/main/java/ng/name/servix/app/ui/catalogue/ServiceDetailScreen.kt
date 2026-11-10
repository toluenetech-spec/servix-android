package ng.name.servix.app.ui.catalogue

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.background
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.clip
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
import ng.name.servix.app.data.model.ServiceMediaDto
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
    val context = LocalContext.current

    LaunchedEffect(openBookingSheetInitially) {
        if (openBookingSheetInitially) {
            bookingSheetOpen = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().testTag("service_detail_scroll").verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp).padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBackClick, modifier = Modifier.size(48.dp).testTag("service_detail_back_button")) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back), tint = DeepForest)
                }
                Text("Service Detail", style = MaterialTheme.typography.titleLarge, color = DeepForest)
            }
            when {
                uiState.loading -> ServiceDetailSkeleton()
                uiState.notFound -> EmptyStateCard(title = stringResource(R.string.service_not_found_title),
                    message = stringResource(R.string.service_not_found_message), actionLabel = "Browse all services",
                    onActionClick = onBrowseServicesClick)
                uiState.error != null || uiState.service == null -> ErrorStateCard(
                    message = uiState.error ?: stringResource(R.string.error_load_service_detail), onRetry = onRetry)
                else -> {
                    val service = uiState.service
                    ServiceMediaGallery(service = service, onOpenMedia = { url ->
                        val resolved = ServixFormatters.resolveServixMediaUrl(url) ?: url
                        CustomTabLauncher.launchUrl(context, resolved)
                    })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        val categoryName = categoryNameFor(service.categoryId)
                        if (categoryName.isNotBlank()) ServixBadge(text = categoryName, containerColor = SoftSand, contentColor = DeepForest)
                        if (service.duration.isNotBlank()) Text(service.duration, style = MaterialTheme.typography.labelMedium, color = MutedGray)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(service.title, style = MaterialTheme.typography.headlineMedium, color = DeepForest,
                            modifier = Modifier.testTag("service_detail_title"))
                        Text(ServixFormatters.formatNaira(service.price), style = MaterialTheme.typography.headlineMedium,
                            color = ForestGreen, modifier = Modifier.testTag("service_detail_price"))
                        if (service.priceUnit.isNotBlank()) Text(service.priceUnit, style = MaterialTheme.typography.bodySmall, color = MutedGray)
                    }
                    service.professional?.let { pro ->
                        ServixCard(modifier = Modifier.clickable { onOpenProfessional(pro.id) }.testTag("service_detail_pro_card"),
                            contentPadding = PaddingValues(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ServixAvatarImage(rawImageUrl = pro.image, name = pro.name, size = 52.dp, shape = CircleShape)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(pro.name, style = MaterialTheme.typography.titleSmall, color = DeepForest)
                                        VerifiedBadgeChip(verified = pro.verified, compact = true)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Service rating", style = MaterialTheme.typography.labelSmall, color = MutedGray)
                                        RatingRow(rating = service.rating, reviewCount = service.reviewCount)
                                    }
                                }
                                Text(stringResource(R.string.view_profile), style = MaterialTheme.typography.labelLarge, color = DeepForest)
                            }
                        }
                    }
                    if (service.included.isNotEmpty()) {
                        ServixCard {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("What’s Included in This Service", style = MaterialTheme.typography.titleMedium, color = DeepForest)
                                service.included.forEach { item ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                                        Icon(Icons.Outlined.Check, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                        Text(item, style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                                    }
                                }
                            }
                        }
                    }
                    ServixCard(containerColor = DeepForest, modifier = Modifier.testTag("service_protection_card")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, tint = WarmIvory, modifier = Modifier.size(22.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Protected payment", style = MaterialTheme.typography.titleMedium, color = WarmIvory)
                                Text(stringResource(R.string.payment_protection_copy), style = MaterialTheme.typography.bodySmall, color = WarmIvory)
                            }
                        }
                    }
                    if (service.description.isNotBlank() || service.shortDescription.isNotBlank()) {
                        ServixCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("About this service", style = MaterialTheme.typography.titleMedium, color = DeepForest)
                                Text(service.description.ifBlank { service.shortDescription }, style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                            }
                        }
                    }
                    if (service.requirements.isNotEmpty()) {
                        ServixCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("What the professional needs from you", style = MaterialTheme.typography.titleMedium, color = DeepForest)
                                service.requirements.forEach { req ->
                                    Text(buildString {
                                        append(req.question)
                                        if (req.required) append(" (required)")
                                        if (req.type == "choice" && req.options.isNotEmpty()) append(" — ${req.options.joinToString(" / ")}")
                                        if (req.type == "file") append(" — attach a file")
                                    }, style = MaterialTheme.typography.bodyMedium, color = Charcoal)
                                }
                            }
                        }
                    }
                    if (uiState.reviews.isNotEmpty()) {
                        Text("Reviews", style = MaterialTheme.typography.titleLarge, color = DeepForest)
                        ReviewsTabSection(uiState.reviews)
                    }
                }
            }
        }
        val barService = uiState.service
        if (barService != null && !uiState.loading && uiState.error == null) {
            ServiceBookBar(priceLabel = ServixFormatters.formatNaira(barService.price), priceUnit = barService.priceUnit,
                onClick = { bookingSheetOpen = true }, modifier = Modifier.align(Alignment.BottomCenter))
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
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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


/** Typed service media only: the API already identifies images, documents and the first video. */
internal fun serviceGalleryItems(service: ServiceDetailDto): List<ServiceMediaDto> {
    val source = if (service.media.isNotEmpty()) service.media else buildList {
        service.gallery.forEach { add(ServiceMediaDto(url = it, kind = "image")) }
        if (isEmpty()) service.image?.let { add(ServiceMediaDto(url = it, kind = "image")) }
        service.video?.let { add(it) }
        addAll(service.documents)
    }
    var sawVideo = false
    return source.filter { item ->
        if (item.url.isBlank() || item.kind !in setOf("image", "video", "document")) false
        else if (item.kind == "video") { if (sawVideo) false else { sawVideo = true; true } }
        else true
    }.distinctBy { it.kind to it.url }
}

@Composable
private fun ServiceMediaGallery(service: ServiceDetailDto, onOpenMedia: (String) -> Unit) {
    val items = remember(service) { serviceGalleryItems(service) }
    val pagerState = rememberPagerState(pageCount = { items.size.coerceAtLeast(1) })
    Box {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f)
            .testTag("service_detail_pager")) { page ->
            val item = items.getOrNull(page)
            if (item == null || item.kind == "image") {
                ServixMediaImage(rawImageUrl = item?.url, contentDescription = service.title,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxSize())
            } else {
                Surface(onClick = { onOpenMedia(item.url) }, color = SoftSand, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxSize().testTag("service_media_${item.kind}")) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(20.dp)) {
                        Icon(if (item.kind == "video") Icons.Outlined.PlayCircleOutline else Icons.Outlined.Description,
                            contentDescription = null, tint = ForestGreen, modifier = Modifier.size(40.dp))
                        Text(item.fileName.ifBlank { if (item.kind == "video") "Open video" else "Open PDF" },
                            style = MaterialTheme.typography.labelLarge, color = DeepForest)
                    }
                }
            }
        }
        if (items.size > 1) Surface(color = DeepForest, shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
            Text("${pagerState.currentPage + 1}/${items.size}", color = PureWhite,
                style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
        }
    }
}

/** Sticky bottom action bar for service detail (plan §5). */
@Composable
private fun ServiceBookBar(
    priceLabel: String,
    priceUnit: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(priceLabel, style = MaterialTheme.typography.titleMedium, color = DeepForest)
                Text(priceUnit, style = MaterialTheme.typography.labelSmall, color = MutedGray)
            }
            ServixPrimaryButton(
                text = stringResource(R.string.service_book_cta),
                onClick = onClick,
                modifier = Modifier.weight(1.2f),
                testTag = "service_book_bar_button",
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

        // Price and payment-protection line, wording aligned with the web page (ServiceDetailPage.jsx:118-122)
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
                shape = RoundedCornerShape(12.dp),
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

        Text(
            text = stringResource(R.string.booking_sheet_held_copy),
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
        )
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
