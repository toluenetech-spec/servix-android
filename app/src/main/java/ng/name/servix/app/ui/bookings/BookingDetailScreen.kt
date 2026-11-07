package ng.name.servix.app.ui.bookings

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import ng.name.servix.app.ui.motion.rememberSuccessTick
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ng.name.servix.app.ui.a11y.heading
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.name.servix.app.R
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.RebookDraftDto
import ng.name.servix.app.data.model.ServixApiException
import ng.name.servix.app.data.repository.BookingRepository
import ng.name.servix.app.ui.catalogue.EmptyStateCard
import ng.name.servix.app.ui.components.ErrorStateCard
import ng.name.servix.app.ui.components.ServixBadge
import ng.name.servix.app.ui.components.ServixCard
import ng.name.servix.app.ui.components.ServixPrimaryButton
import ng.name.servix.app.ui.components.ServixSecondaryButton
import ng.name.servix.app.ui.components.SkeletonBox
import ng.name.servix.app.ui.theme.BorderSand
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.Coral
import ng.name.servix.app.ui.theme.DangerRed
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.ForestGreen
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.PureWhite
import ng.name.servix.app.ui.theme.SoftSand
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.CustomTabLauncher
import ng.name.servix.app.util.ServixFormatters

enum class BookingActionModal {
    CANCEL,
    CONFIRM_COMPLETION,
    DISPUTE,
    REVIEW,
    REBOOK,
}

data class BookingDetailUiState(
    val bookingId: String = "",
    val loading: Boolean = true,
    val notFound: Boolean = false,
    val error: String? = null,
    val booking: BookingDto? = null,
    val actionBusy: Boolean = false,
    val actionError: String? = null,
    val actionSuccessMessage: String? = null,
    val activeModal: BookingActionModal? = null,
    val isPollingPayment: Boolean = false,
    val paymentConfirmedBanner: String? = null,
    val paymentPollTimedOut: Boolean = false,
    val rebookLoading: Boolean = false,
    val rebookDraft: RebookDraftDto? = null,
)

class BookingDetailViewModel(
    private val bookingRepository: BookingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingDetailUiState())
    val uiState: StateFlow<BookingDetailUiState> = _uiState.asStateFlow()

    private var paymentPollJob: Job? = null

    fun loadBooking(
        id: String,
        startPollingIfPending: Boolean = false,
        forceRefresh: Boolean = false,
    ) {
        if (!forceRefresh && _uiState.value.bookingId == id && _uiState.value.booking != null && _uiState.value.error == null) {
            if (startPollingIfPending && _uiState.value.booking?.status == "pending_payment") {
                startPaymentPolling(id)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                BookingDetailUiState(
                    bookingId = id,
                    loading = true,
                )
            }

            val res = bookingRepository.getBooking(id)
            res.fold(
                onSuccess = { booking ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            notFound = false,
                            error = null,
                            booking = booking,
                        )
                    }
                    if (startPollingIfPending) {
                        if (booking.status == "pending_payment") {
                            startPaymentPolling(id)
                        } else if (booking.status == "requested") {
                            val proName = booking.professionalName?.takeIf { it.isNotBlank() } ?: "the professional"
                            _uiState.update {
                                it.copy(
                                    paymentConfirmedBanner = "Payment received — request sent to $proName",
                                )
                            }
                        }
                    }
                },
                onFailure = { err ->
                    val apiEx = err as? ServixApiException
                    _uiState.update {
                        it.copy(
                            loading = false,
                            notFound = apiEx?.status == 404,
                            error = err.message ?: "We couldn't load this booking.",
                        )
                    }
                }
            )
        }
    }

    /**
     * Polls GET /bookings/:id every 2 s for up to 90 s until status != "pending_payment".
     * Shows "Payment received — request sent to <professionalName>" when it becomes "requested",
     * or a neutral "We're still confirming your payment" with a Refresh button if it times out.
     */
    fun startPaymentPolling(
        bookingId: String = _uiState.value.bookingId,
        intervalMs: Long = 2_000L,
        maxDurationMs: Long = 90_000L,
    ) {
        if (bookingId.isBlank()) return
        paymentPollJob?.cancel()
        paymentPollJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPollingPayment = true,
                    paymentPollTimedOut = false,
                    actionError = null,
                )
            }

            val maxAttempts = (maxDurationMs / intervalMs).toInt().coerceAtLeast(1)
            for (attempt in 1..maxAttempts) {
                val res = bookingRepository.getBooking(bookingId)
                val latest = res.getOrNull()
                if (latest != null) {
                    _uiState.update { it.copy(booking = latest) }
                    if (latest.status != "pending_payment") {
                        val proName = latest.professionalName?.takeIf { it.isNotBlank() } ?: "the professional"
                        val banner = if (latest.status == "requested") {
                            "Payment received — request sent to $proName"
                        } else {
                            "Booking status updated: ${BookingStatusMeta.labelFor(latest.status)}"
                        }
                        _uiState.update {
                            it.copy(
                                isPollingPayment = false,
                                paymentPollTimedOut = false,
                                paymentConfirmedBanner = banner,
                            )
                        }
                        return@launch
                    }
                }
                if (attempt < maxAttempts) {
                    delay(intervalMs)
                }
            }

            // Timed out after 90 s while still pending_payment
            _uiState.update {
                it.copy(
                    isPollingPayment = false,
                    paymentPollTimedOut = true,
                )
            }
        }
    }

    fun onScreenResumed(shouldPollWhenPending: Boolean) {
        val current = _uiState.value
        val id = current.bookingId.ifBlank { return }
        if (current.booking?.status == "pending_payment" && (shouldPollWhenPending || current.isPollingPayment || current.paymentPollTimedOut)) {
            startPaymentPolling(id)
        }
    }

    fun openModal(modal: BookingActionModal) {
        _uiState.update {
            it.copy(
                activeModal = modal,
                actionError = null,
            )
        }
    }

    fun closeModal() {
        if (_uiState.value.actionBusy) return
        _uiState.update { it.copy(activeModal = null) }
    }

    fun initiatePayNow(onReadyToOpenCustomTab: (authorizationUrl: String) -> Unit) {
        val id = _uiState.value.booking?.id ?: return
        if (_uiState.value.actionBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccessMessage = null) }
            val res = bookingRepository.payBooking(id)
            res.fold(
                onSuccess = { pay ->
                    _uiState.update { it.copy(actionBusy = false) }
                    onReadyToOpenCustomTab(pay.authorizationUrl)
                    startPaymentPolling(id)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            actionBusy = false,
                            actionError = err.message ?: "Could not start payment.",
                        )
                    }
                }
            )
        }
    }

    fun submitCancel(reason: String?) {
        val booking = _uiState.value.booking ?: return
        if (_uiState.value.actionBusy) return
        val wasPaid = booking.status != "pending_payment"

        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccessMessage = null) }
            paymentPollJob?.cancel()
            val res = bookingRepository.cancelBooking(booking.id, reason)
            res.fold(
                onSuccess = { updated ->
                    val refreshed = bookingRepository.getBooking(booking.id).getOrNull()
                        ?: updated.copy(
                            serviceId = updated.serviceId ?: booking.serviceId,
                            professionalId = updated.professionalId ?: booking.professionalId,
                            professionalName = updated.professionalName ?: booking.professionalName,
                            customerName = updated.customerName ?: booking.customerName,
                        )
                    val msg = if (wasPaid) {
                        "Booking cancelled. A full refund has been initiated under the Servix refund policy."
                    } else {
                        "Booking cancelled."
                    }
                    _uiState.update {
                        it.copy(
                            actionBusy = false,
                            activeModal = null,
                            isPollingPayment = false,
                            paymentPollTimedOut = false,
                            booking = refreshed,
                            actionSuccessMessage = msg,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            actionBusy = false,
                            actionError = err.message ?: "Could not cancel this booking.",
                        )
                    }
                }
            )
        }
    }

    fun submitConfirmCompletion() {
        val booking = _uiState.value.booking ?: return
        if (_uiState.value.actionBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccessMessage = null) }
            val res = bookingRepository.confirmBooking(booking.id)
            res.fold(
                onSuccess = { updated ->
                    val refreshed = bookingRepository.getBooking(booking.id).getOrNull()
                        ?: updated.copy(
                            serviceId = updated.serviceId ?: booking.serviceId,
                            professionalId = updated.professionalId ?: booking.professionalId,
                            professionalName = updated.professionalName ?: booking.professionalName,
                            customerName = updated.customerName ?: booking.customerName,
                        )
                    _uiState.update {
                        it.copy(
                            actionBusy = false,
                            activeModal = null,
                            booking = refreshed,
                            actionSuccessMessage = "Completion confirmed. Payment released.",
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            actionBusy = false,
                            actionError = err.message ?: "Could not confirm completion.",
                        )
                    }
                }
            )
        }
    }

    fun submitDispute(reason: String) {
        val booking = _uiState.value.booking ?: return
        if (_uiState.value.actionBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccessMessage = null) }
            val res = bookingRepository.disputeBooking(booking.id, reason)
            res.fold(
                onSuccess = { updated ->
                    val refreshed = bookingRepository.getBooking(booking.id).getOrNull()
                        ?: updated.copy(
                            serviceId = updated.serviceId ?: booking.serviceId,
                            professionalId = updated.professionalId ?: booking.professionalId,
                            professionalName = updated.professionalName ?: booking.professionalName,
                            customerName = updated.customerName ?: booking.customerName,
                        )
                    _uiState.update {
                        it.copy(
                            actionBusy = false,
                            activeModal = null,
                            booking = refreshed,
                            actionSuccessMessage = "Dispute opened. Funds are on hold.",
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            actionBusy = false,
                            actionError = err.message ?: "Could not open dispute.",
                        )
                    }
                }
            )
        }
    }

    fun submitReview(rating: Int, text: String?) {
        val booking = _uiState.value.booking ?: return
        if (_uiState.value.actionBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(actionBusy = true, actionError = null, actionSuccessMessage = null) }
            val res = bookingRepository.reviewBooking(booking.id, rating, text)
            res.fold(
                onSuccess = {
                    val refreshed = bookingRepository.getBooking(booking.id).getOrNull()
                        ?: booking.copy(hasReview = true)
                    _uiState.update {
                        it.copy(
                            actionBusy = false,
                            activeModal = null,
                            booking = refreshed,
                            actionSuccessMessage = "Review published. Thank you!",
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            actionBusy = false,
                            actionError = err.message ?: "Could not publish review.",
                        )
                    }
                }
            )
        }
    }

    fun startBookAgain() {
        val booking = _uiState.value.booking ?: return
        if (_uiState.value.rebookLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(rebookLoading = true, actionError = null) }
            val res = bookingRepository.getRebookDraft(booking.id)
            res.fold(
                onSuccess = { draft ->
                    Log.i(
                        "ServixRebook",
                        "GET /bookings/${booking.id}/rebook -> previousBookingId=${draft.previousBookingId}, " +
                            "previousStatus=${draft.previousStatus}, pro=${draft.professional.id}, " +
                            "services=${draft.services.map { it.id }}, previousServiceAvailable=${draft.previousServiceAvailable}"
                    )
                    _uiState.update {
                        it.copy(
                            rebookLoading = false,
                            rebookDraft = draft,
                            activeModal = BookingActionModal.REBOOK,
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            rebookLoading = false,
                            actionError = err.message ?: "Could not prepare rebooking.",
                        )
                    }
                }
            )
        }
    }

    override fun onCleared() {
        paymentPollJob?.cancel()
        super.onCleared()
    }

    companion object {
        fun factory(bookingRepository: BookingRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BookingDetailViewModel(bookingRepository) as T
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingDetailScreen(
    uiState: BookingDetailUiState,
    pollingAfterPay: Boolean,
    onResumeCheck: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onRefreshPaymentStatus: () -> Unit,
    onPayClick: (onOpenUrl: (String) -> Unit) -> Unit,
    onOpenModal: (BookingActionModal) -> Unit,
    onCloseModal: () -> Unit,
    onSubmitCancel: (reason: String?) -> Unit,
    onSubmitConfirmCompletion: () -> Unit,
    onSubmitDispute: (reason: String) -> Unit,
    onSubmitReview: (rating: Int, text: String?) -> Unit,
    onStartBookAgain: () -> Unit,
    onContinueRebook: (serviceSlug: String, rebookedFromId: String, initialNotes: String) -> Unit,
    onOpenProfessional: (slug: String) -> Unit,
    onOpenService: (slug: String) -> Unit,
    /** Release 1.1: opens (or creates) the chat thread for this booking. Null when chats are gated off. */
    onMessageClick: (() -> Unit)? = null,
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var moreSheetOpen by remember { mutableStateOf(false) }
    // One light tick on a successful action (Book & Pay, Confirm completion, Review submitted).
    val successTick = rememberSuccessTick()
    LaunchedEffect(uiState.actionSuccessMessage) {
        if (!uiState.actionSuccessMessage.isNullOrBlank()) successTick()
    }

    DisposableEffect(lifecycleOwner, pollingAfterPay) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResumeCheck(pollingAfterPay)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top back row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("booking_detail_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = DeepForest,
                )
            }
            Text(
                text = stringResource(R.string.bookings_title),
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }

        if (uiState.loading) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.45f), height = 16.dp)
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.75f), height = 28.dp)
                SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 180.dp)
            }
            return@Column
        }

        if (uiState.notFound) {
            EmptyStateCard(
                title = stringResource(R.string.booking_not_found_title),
                message = stringResource(R.string.booking_not_found_message),
                actionLabel = stringResource(R.string.bookings_title),
                onActionClick = onBackClick,
            )
            return@Column
        }

        if (uiState.error != null || uiState.booking == null) {
            ErrorStateCard(
                message = uiState.error ?: stringResource(R.string.booking_error_load),
                onRetry = onRetry,
            )
            return@Column
        }

        val booking = uiState.booking

        // Header: Eyebrow + Service Title (BookingDetailPage.jsx:112-113)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Booking ${booking.reference}",
                style = MaterialTheme.typography.labelMedium,
                color = MutedGray,
                modifier = Modifier.testTag("booking_detail_reference"),
            )
            Text(
                text = booking.serviceTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = DeepForest,
                modifier = Modifier.testTag("booking_detail_title").heading(),
            )
        }

        // Status header: one plain sentence of what happens next in this state.
        val nextStep = bookingNextStepSentence(booking.status)
        ServixCard(
            modifier = Modifier.testTag("booking_status_header"),
            containerColor = SoftSand,
            borderColor = BorderSand,
            contentPadding = PaddingValues(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BookingStatusChip(status = booking.status)
                    Text(
                        text = ServixFormatters.formatNaira(booking.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = DeepForest,
                    )
                }
                Text(
                    text = nextStep,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Charcoal,
                    modifier = Modifier.testTag("booking_next_step"),
                )
            }
        }

        // Payment received banner (when status transitions from pending_payment -> requested)
        if (!uiState.paymentConfirmedBanner.isNullOrBlank()) {
            ServixCard(
                modifier = Modifier.testTag("booking_payment_received_banner"),
                containerColor = ForestGreen.copy(alpha = 0.12f),
                borderColor = ForestGreen,
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = uiState.paymentConfirmedBanner,
                        style = MaterialTheme.typography.titleSmall,
                        color = DeepForest,
                    )
                }
            }
        }

        // Polling indicator / Timeout banner
        if (booking.status == "pending_payment" && uiState.isPollingPayment) {
            ServixCard(
                modifier = Modifier.testTag("booking_payment_polling_banner"),
                containerColor = SoftSand,
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = DeepForest,
                    )
                    Text(
                        text = "Confirming your payment… checking every 2 seconds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                    )
                }
            }
        } else if (booking.status == "pending_payment" && uiState.paymentPollTimedOut) {
            ServixCard(
                modifier = Modifier.testTag("booking_payment_timeout_banner"),
                containerColor = SoftSand,
                contentPadding = PaddingValues(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.booking_payment_confirming_banner),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    ServixSecondaryButton(
                        text = stringResource(R.string.booking_refresh),
                        onClick = onRefreshPaymentStatus,
                        testTag = "booking_payment_refresh_button",
                    )
                }
            }
        }

        // Action feedback banners (success or server 409/422 error)
        if (!uiState.actionSuccessMessage.isNullOrBlank()) {
            ServixCard(
                modifier = Modifier.testTag("booking_action_success_banner"),
                containerColor = SoftSand,
                borderColor = ForestGreen,
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = ForestGreen,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = uiState.actionSuccessMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepForest,
                    )
                }
            }
        }

        if (!uiState.actionError.isNullOrBlank()) {
            ServixCard(
                modifier = Modifier.testTag("booking_action_error_banner"),
                containerColor = DangerRed.copy(alpha = 0.08f),
                borderColor = DangerRed,
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = uiState.actionError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DangerRed,
                    )
                }
            }
        }

        // Main Booking Detail Card (BookingDetailPage.jsx:115-208)
        ServixCard(contentPadding = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Status chip + Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BookingStatusChip(
                        status = booking.status,
                        modifier = Modifier.testTag("booking_detail_status_chip"),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = ServixFormatters.formatNaira(booking.amount),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = DeepForest,
                            modifier = Modifier.testTag("booking_detail_amount"),
                        )
                        Text(
                            text = booking.priceUnit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                }

                HorizontalDivider(color = SoftSand)

                // Meta details: Scheduled date/time, Professional (tap -> profile), Notes
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MutedGray,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = ServixFormatters.formatDateTimeFull(booking.scheduledAt),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Charcoal,
                        )
                    }

                    val proName = booking.professionalName?.takeIf { it.isNotBlank() } ?: "Professional"
                    val proSlug = booking.professionalId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .then(
                                if (!proSlug.isNullOrBlank()) {
                                    Modifier.clickable { onOpenProfessional(proSlug) }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(vertical = 2.dp)
                            .testTag("booking_detail_professional_row"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonOutline,
                            contentDescription = null,
                            tint = MutedGray,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Professional: $proName",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (!proSlug.isNullOrBlank()) FontWeight.Medium else FontWeight.Normal,
                            ),
                            color = if (!proSlug.isNullOrBlank()) DeepForest else Charcoal,
                            modifier = Modifier.weight(1f),
                        )
                        if (!proSlug.isNullOrBlank()) {
                            Text(
                                text = "View profile →",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForestGreen,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }

                    if (booking.notes.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = MutedGray,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = booking.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Charcoal,
                            )
                        }
                    }
                }

                // Timestamps summary built from status + timestamps only (no events array)
                HorizontalDivider(color = SoftSand)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (booking.createdAt.isNotBlank()) {
                        Text(
                            text = "Booked on ${ServixFormatters.formatDate(booking.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                    if (!booking.deliveredAt.isNullOrBlank()) {
                        Text(
                            text = "Delivered on ${ServixFormatters.formatDateTimeMedium(booking.deliveredAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                    if (!booking.completedAt.isNullOrBlank()) {
                        val autoNote = if (booking.autoConfirmed) " (auto-confirmed)" else ""
                        Text(
                            text = "Completed on ${ServixFormatters.formatDateTimeMedium(booking.completedAt)}$autoNote",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                    if (booking.status == "cancelled") {
                        val cancelBy = booking.cancelledBy?.takeIf { it.isNotBlank() }
                        val reason = booking.cancelReason?.takeIf { it.isNotBlank() }
                        Text(
                            text = buildString {
                                append("Status: Cancelled")
                                if (cancelBy != null) append(" by $cancelBy")
                                if (reason != null) append(" — “$reason”")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
                        )
                    }
                }

                // Contextual status notices (BookingDetailPage.jsx:137-161)
                if (booking.status == "pending_payment") {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SoftSand,
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = DeepForest,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Charcoal)) {
                                        append("${stringResource(R.string.booking_payment_pending_title)} ")
                                    }
                                    append(stringResource(R.string.booking_payment_pending_body))
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                    }
                }

                if (booking.status == "delivered") {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SoftSand,
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Coral,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Charcoal)) {
                                        append("${stringResource(R.string.booking_delivered_notice_title)} ")
                                    }
                                    append(stringResource(R.string.booking_delivered_notice_body))
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedGray,
                            )
                        }
                    }
                }

                if (booking.status == "disputed") {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DangerRed.copy(alpha = 0.08f),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Charcoal)) {
                                        append("${stringResource(R.string.booking_disputed_notice_title)} ")
                                    }
                                    append(
                                        stringResource(
                                            R.string.booking_disputed_notice_body,
                                            booking.disputeReason.orEmpty(),
                                        )
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Charcoal,
                            )
                        }
                    }
                }

                // Actions moved to the sticky bottom action bar + "More" sheet below.
            }
        }
    }

    val barBooking = uiState.booking
    if (!uiState.loading && barBooking != null) {
        val barActions = bookingActionsFor(
            booking = barBooking,
            busy = uiState.actionBusy,
            rebookLoading = uiState.rebookLoading,
            payLabel = stringResource(R.string.booking_pay_cta, ServixFormatters.formatNaira(barBooking.amount)),
            cancelLabel = if (barBooking.status == "pending_payment") stringResource(R.string.cancel) else stringResource(R.string.booking_cancel_cta),
            onPay = { onPayClick { url -> CustomTabLauncher.launchUrl(context, url) } },
            onOpenModal = onOpenModal,
            onStartBookAgain = onStartBookAgain,
        )
        BookingActionBar(
            actions = barActions,
            onMore = { moreSheetOpen = true },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        if (moreSheetOpen) {
            val moreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { moreSheetOpen = false },
                sheetState = moreSheetState,
                containerColor = WarmIvory,
                contentColor = Charcoal,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "More actions",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepForest,
                    )
                    if (onMessageClick != null) {
                        ServixSecondaryButton(
                            text = "Message",
                            onClick = {
                                moreSheetOpen = false
                                onMessageClick()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "booking_message_button",
                        )
                    }
                    barActions.filter { it.secondary }.forEach { action ->
                        ServixSecondaryButton(
                            text = action.label,
                            onClick = {
                                moreSheetOpen = false
                                action.onClick()
                            },
                            enabled = action.enabled,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = action.testTag + "_more",
                        )
                    }
                }
            }
        }
    }
    }

    // Action Bottom Sheets / Modals
    val activeModal = uiState.activeModal
    if (activeModal != null && uiState.booking != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onCloseModal,
            sheetState = sheetState,
            containerColor = WarmIvory,
            contentColor = Charcoal,
        ) {
            when (activeModal) {
                BookingActionModal.CANCEL -> {
                    CancelBookingModalContent(
                        busy = uiState.actionBusy,
                        onConfirmCancel = onSubmitCancel,
                        onDismiss = onCloseModal,
                    )
                }
                BookingActionModal.CONFIRM_COMPLETION -> {
                    ConfirmCompletionModalContent(
                        busy = uiState.actionBusy,
                        onConfirm = onSubmitConfirmCompletion,
                        onDismiss = onCloseModal,
                    )
                }
                BookingActionModal.DISPUTE -> {
                    DisputeModalContent(
                        busy = uiState.actionBusy,
                        onSubmitDispute = onSubmitDispute,
                        onDismiss = onCloseModal,
                    )
                }
                BookingActionModal.REVIEW -> {
                    ReviewModalContent(
                        busy = uiState.actionBusy,
                        onSubmitReview = onSubmitReview,
                        onDismiss = onCloseModal,
                    )
                }
                BookingActionModal.REBOOK -> {
                    val draft = uiState.rebookDraft
                    if (draft != null) {
                        RebookModalContent(
                            draft = draft,
                            onContinue = { serviceSlug, reuseNotes ->
                                onCloseModal()
                                val initialNotes = if (reuseNotes && draft.previousNotes.isNotBlank()) {
                                    draft.previousNotes.take(1000)
                                } else {
                                    ""
                                }
                                onContinueRebook(serviceSlug, draft.previousBookingId, initialNotes)
                            },
                            onOpenProfessional = { slug ->
                                onCloseModal()
                                onOpenProfessional(slug)
                            },
                            onDismiss = onCloseModal,
                        )
                    }
                }
            }
        }
    }
}

/** One plain sentence describing what happens next, per booking state. */
internal fun bookingNextStepSentence(status: String): String = when (status) {
    "pending_payment" -> "Pay now to hold this slot — your money stays in escrow until you confirm the work."
    "requested" -> "Waiting for the professional to accept. You can cancel free of charge until they do."
    "accepted" -> "The professional accepted. Payment is held in escrow until you confirm the work is done."
    "in_progress" -> "Work is under way. Confirm when it is delivered and the payment is released."
    "delivered" -> "The professional marked this delivered — check the work, then confirm to release payment."
    "completed" -> "All done and paid. Leave a review to help the next customer decide."
    "cancelled" -> "This booking was cancelled. Nothing was charged, or your payment was refunded."
    "declined" -> "The professional could not take this booking. Try another professional or time."
    "disputed" -> "You reported a problem. Servix support reviews the details and contacts both sides."
    "refunded" -> "Your payment was refunded. It can take a few working days to reach your bank."
    else -> "We will show the next step here as this booking moves along."
}

/** One offered action: exactly one primary is shown in the bar, the rest live under "More". */
data class BookingAction(
    val label: String,
    val secondary: Boolean,
    val enabled: Boolean,
    val testTag: String,
    val onClick: () -> Unit,
)

/** Same gates as Stage 3 — pay, confirm, cancel, dispute, review, book again. */
internal fun bookingActionsFor(
    booking: ng.name.servix.app.data.model.BookingDto,
    busy: Boolean,
    rebookLoading: Boolean,
    payLabel: String,
    cancelLabel: String,
    onPay: () -> Unit,
    onOpenModal: (BookingActionModal) -> Unit,
    onStartBookAgain: () -> Unit,
): List<BookingAction> {
    val actions = mutableListOf<BookingAction>()
    if (booking.status == "pending_payment") {
        actions += BookingAction(payLabel, secondary = false, enabled = !busy, testTag = "booking_action_pay", onClick = onPay)
    }
    if (booking.status == "delivered") {
        actions += BookingAction("Confirm work is done", secondary = false, enabled = !busy, testTag = "booking_action_confirm", onClick = { onOpenModal(BookingActionModal.CONFIRM_COMPLETION) })
    }
    if (booking.status == "completed" && !booking.hasReview) {
        actions += BookingAction("Leave a review", secondary = false, enabled = !busy, testTag = "booking_action_review", onClick = { onOpenModal(BookingActionModal.REVIEW) })
    }
    if (booking.status in setOf("completed", "cancelled", "declined")) {
        actions += BookingAction(if (rebookLoading) "Preparing…" else "Book again", secondary = false, enabled = !busy && !rebookLoading, testTag = "booking_action_rebook", onClick = onStartBookAgain)
    }
    if (booking.status in setOf("pending_payment", "requested", "accepted")) {
        actions += BookingAction(cancelLabel, secondary = true, enabled = !busy, testTag = "booking_action_cancel", onClick = { onOpenModal(BookingActionModal.CANCEL) })
    }
    if (booking.status in setOf("in_progress", "delivered")) {
        actions += BookingAction("Report a problem", secondary = true, enabled = !busy, testTag = "booking_action_dispute", onClick = { onOpenModal(BookingActionModal.DISPUTE) })
    }
    return actions
}

/** Sticky bottom bar: the single next step, plus "More" when other actions exist. */
@Composable
private fun BookingActionBar(
    actions: List<BookingAction>,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = actions.firstOrNull { !it.secondary }
    val extras = actions.filter { it.secondary }
    if (primary == null && extras.isEmpty()) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = WarmIvory,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSand),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (extras.isNotEmpty()) {
                ServixSecondaryButton(
                    text = "More",
                    onClick = onMore,
                    modifier = Modifier.weight(0.4f),
                    testTag = "booking_action_more",
                )
            }
            if (primary != null) {
                ServixPrimaryButton(
                    text = primary.label,
                    onClick = primary.onClick,
                    enabled = primary.enabled,
                    modifier = Modifier.weight(if (extras.isNotEmpty()) 0.6f else 1f),
                    testTag = primary.testTag,
                )
            }
        }
    }
}

@Composable
private fun CancelBookingModalContent(
    busy: Boolean,
    onConfirmCancel: (reason: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.modal_cancel_title),
            style = MaterialTheme.typography.headlineSmall,
            color = DeepForest,
        )
        Text(
            text = stringResource(R.string.modal_cancel_policy),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.modal_cancel_reason_label),
                style = MaterialTheme.typography.labelLarge,
                color = Charcoal,
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { if (it.length <= 1000) reason = it },
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
                    .testTag("modal_cancel_reason_input"),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServixPrimaryButton(
                text = stringResource(R.string.booking_cancel_cta),
                onClick = { onConfirmCancel(reason.trim().takeIf { it.isNotEmpty() }) },
                enabled = !busy,
                loading = busy,
                modifier = Modifier.weight(1f),
                testTag = "modal_confirm_cancel_button",
            )
            ServixSecondaryButton(
                text = stringResource(R.string.modal_keep_booking),
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                testTag = "modal_keep_booking_button",
            )
        }
    }
}

@Composable
private fun ConfirmCompletionModalContent(
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.modal_confirm_title),
            style = MaterialTheme.typography.headlineSmall,
            color = DeepForest,
        )
        // Verbatim required sentence
        Text(
            text = stringResource(R.string.modal_confirm_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Charcoal,
            modifier = Modifier.testTag("modal_confirm_completion_body"),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServixPrimaryButton(
                text = stringResource(R.string.booking_confirm_cta),
                onClick = onConfirm,
                enabled = !busy,
                loading = busy,
                modifier = Modifier.weight(1f),
                testTag = "modal_confirm_completion_submit",
            )
            ServixSecondaryButton(
                text = stringResource(R.string.back),
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DisputeModalContent(
    busy: Boolean,
    onSubmitDispute: (reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.modal_dispute_title),
            style = MaterialTheme.typography.headlineSmall,
            color = DeepForest,
        )
        Text(
            text = stringResource(R.string.modal_dispute_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${stringResource(R.string.modal_dispute_reason_label)} * (10–2000 characters)",
                style = MaterialTheme.typography.labelLarge,
                color = Charcoal,
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { if (it.length <= 2000) reason = it },
                minLines = 4,
                maxLines = 6,
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
                    .testTag("modal_dispute_reason_input"),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServixPrimaryButton(
                text = stringResource(R.string.modal_dispute_open_cta),
                onClick = { onSubmitDispute(reason.trim()) },
                enabled = !busy && reason.trim().length >= 10,
                loading = busy,
                modifier = Modifier.weight(1f),
                testTag = "modal_dispute_submit_button",
            )
            ServixSecondaryButton(
                text = stringResource(R.string.back),
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ReviewModalContent(
    busy: Boolean,
    onSubmitReview: (rating: Int, text: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var rating by rememberSaveable { mutableIntStateOf(5) }
    var reviewText by rememberSaveable { mutableStateOf("") }
    val ratingLabels = mapOf(
        5 to "5 — Excellent",
        4 to "4 — Very good",
        3 to "3 — Good",
        2 to "2 — Fair",
        1 to "1 — Poor",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.modal_review_title),
            style = MaterialTheme.typography.headlineSmall,
            color = DeepForest,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${stringResource(R.string.modal_review_rating_label)} *",
                style = MaterialTheme.typography.labelLarge,
                color = Charcoal,
            )
            listOf(5, 4, 3, 2, 1).forEach { r ->
                val isSelected = rating == r
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) DeepForest else PureWhite,
                    border = BorderStroke(1.dp, if (isSelected) DeepForest else BorderSand),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { rating = r }
                        .testTag("modal_review_rating_$r"),
                ) {
                    Text(
                        text = ratingLabels[r].orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) WarmIvory else Charcoal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.modal_review_text_label),
                style = MaterialTheme.typography.labelLarge,
                color = Charcoal,
            )
            OutlinedTextField(
                value = reviewText,
                onValueChange = { if (it.length <= 2000) reviewText = it },
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
                    .testTag("modal_review_text_input"),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServixPrimaryButton(
                text = stringResource(R.string.modal_review_publish_cta),
                onClick = { onSubmitReview(rating, reviewText.trim().takeIf { it.isNotEmpty() }) },
                enabled = !busy,
                loading = busy,
                modifier = Modifier.weight(1f),
                testTag = "modal_review_submit_button",
            )
            ServixSecondaryButton(
                text = stringResource(R.string.back),
                onClick = onDismiss,
                enabled = !busy,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RebookModalContent(
    draft: RebookDraftDto,
    onContinue: (serviceSlug: String, reuseNotes: Boolean) -> Unit,
    onOpenProfessional: (slug: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val defaultService = remember(draft) {
        (draft.services.firstOrNull { it.wasBooked } ?: draft.services.firstOrNull())?.id.orEmpty()
    }
    var selectedService by rememberSaveable(draft.previousBookingId) { mutableStateOf(defaultService) }
    var reuseNotes by rememberSaveable(draft.previousBookingId) { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Book ${draft.professional.name} again",
            style = MaterialTheme.typography.headlineSmall,
            color = DeepForest,
        )

        if (draft.services.isEmpty()) {
            Text(
                text = "${draft.professional.name} has no active gigs right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ServixPrimaryButton(
                    text = stringResource(R.string.view_profile),
                    onClick = { onOpenProfessional(draft.professional.id) },
                    modifier = Modifier.weight(1f),
                )
                ServixSecondaryButton(
                    text = stringResource(R.string.close),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
            }
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Service",
                style = MaterialTheme.typography.labelLarge,
                color = Charcoal,
            )
            draft.services.forEach { svc ->
                val isSelected = svc.id == selectedService
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) DeepForest else PureWhite,
                    border = BorderStroke(1.dp, if (isSelected) DeepForest else BorderSand),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedService = svc.id }
                        .testTag("modal_rebook_service_${svc.id}"),
                ) {
                    Text(
                        text = buildString {
                            append("${svc.title} — ${ServixFormatters.formatNaira(svc.price)}")
                            if (svc.wasBooked) append(" (booked before)")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) WarmIvory else Charcoal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                }
            }
        }

        if (!draft.previousServiceAvailable) {
            Text(
                text = "The gig you booked last time is no longer available; pick one of the current ones.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }

        if (draft.previousNotes.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { reuseNotes = !reuseNotes },
            ) {
                Checkbox(
                    checked = reuseNotes,
                    onCheckedChange = { reuseNotes = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = DeepForest,
                        checkmarkColor = WarmIvory,
                    ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Reuse my previous requirements",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Charcoal,
                    )
                    val preview = if (draft.previousNotes.length > 140) {
                        "${draft.previousNotes.take(140)}…"
                    } else {
                        draft.previousNotes
                    }
                    Text(
                        text = "“$preview”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                }
            }
        }

        Text(
            text = "You will pick a new date and see the current price before anything is booked or paid.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ServixPrimaryButton(
                text = "Continue",
                onClick = { onContinue(selectedService, reuseNotes) },
                enabled = selectedService.isNotBlank(),
                modifier = Modifier.weight(1f),
                testTag = "modal_rebook_continue_button",
            )
            ServixSecondaryButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
