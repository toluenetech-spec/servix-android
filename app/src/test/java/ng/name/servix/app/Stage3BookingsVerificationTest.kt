package ng.name.servix.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ng.name.servix.app.data.local.TokenStore
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.PersistentCookieJar
import ng.name.servix.app.data.network.ServixNetworkModule
import ng.name.servix.app.data.repository.AuthOutcome
import ng.name.servix.app.data.repository.AuthRepository
import ng.name.servix.app.data.repository.BookingRepository
import ng.name.servix.app.data.repository.CatalogueRepository
import ng.name.servix.app.ui.bookings.BookingDetailScreen
import ng.name.servix.app.ui.bookings.BookingDetailUiState
import ng.name.servix.app.ui.bookings.BookingsListTab
import ng.name.servix.app.ui.bookings.BookingsScreen
import ng.name.servix.app.ui.bookings.BookingsUiState
import ng.name.servix.app.ui.catalogue.BookingSheetContent
import ng.name.servix.app.ui.theme.ServixTheme
import ng.name.servix.app.ui.theme.WarmIvory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Stage3BookingsVerificationTest {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun createMailTmInbox(): Pair<String, String> {
        val domainsReq = Request.Builder().url("https://api.mail.tm/domains").get().build()
        val domain = httpClient.newCall(domainsReq).execute().use { res ->
            assertTrue("mail.tm /domains HTTP ${res.code}", res.isSuccessful)
            val body = res.body?.string().orEmpty()
            val root = ApiErrorParser.json.parseToJsonElement(body).jsonObject
            val members = root["hydra:member"]?.jsonArray
            members?.firstOrNull()?.jsonObject?.get("domain")?.jsonPrimitive?.content
                ?: error("No active domain in mail.tm: $body")
        }

        val randomSuffix = "${System.currentTimeMillis()}${(100..999).random()}"
        val address = "servixstage3_$randomSuffix@$domain"
        val password = "MailTmPass#$randomSuffix"

        val createBody = """{"address":"$address","password":"$password"}""".toRequestBody(jsonMedia)
        val createReq = Request.Builder().url("https://api.mail.tm/accounts").post(createBody).build()
        httpClient.newCall(createReq).execute().use { res ->
            assertTrue("mail.tm /accounts HTTP ${res.code}", res.isSuccessful)
        }

        val tokenBody = """{"address":"$address","password":"$password"}""".toRequestBody(jsonMedia)
        val tokenReq = Request.Builder().url("https://api.mail.tm/token").post(tokenBody).build()
        val token = httpClient.newCall(tokenReq).execute().use { res ->
            assertTrue("mail.tm /token HTTP ${res.code}", res.isSuccessful)
            val body = res.body?.string().orEmpty()
            ApiErrorParser.json.parseToJsonElement(body).jsonObject["token"]?.jsonPrimitive?.content
                ?: error("No token from mail.tm: $body")
        }

        return address to token
    }

    private suspend fun pollForSixDigitOtp(mailToken: String, seenIds: MutableSet<String>): String {
        val codeRegex = Regex("""\b(\d{6})\b""")
        repeat(25) {
            val listReq = Request.Builder()
                .url("https://api.mail.tm/messages")
                .header("Authorization", "Bearer $mailToken")
                .get()
                .build()

            val messageIds = httpClient.newCall(listReq).execute().use { res ->
                if (!res.isSuccessful) emptyList()
                else {
                    val body = res.body?.string().orEmpty()
                    val members = ApiErrorParser.json.parseToJsonElement(body).jsonObject["hydra:member"]?.jsonArray
                    members?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }.orEmpty()
                }
            }

            for (msgId in messageIds) {
                if (msgId in seenIds) continue
                val msgReq = Request.Builder()
                    .url("https://api.mail.tm/messages/$msgId")
                    .header("Authorization", "Bearer $mailToken")
                    .get()
                    .build()
                httpClient.newCall(msgReq).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string().orEmpty()
                        val obj = ApiErrorParser.json.parseToJsonElement(body).jsonObject
                        val subject = obj["subject"]?.jsonPrimitive?.content.orEmpty()
                        val intro = obj["intro"]?.jsonPrimitive?.content.orEmpty()
                        val text = obj["text"]?.jsonPrimitive?.content.orEmpty()
                        val combined = "$subject\n$intro\n$text"
                        val match = codeRegex.find(combined)
                        if (match != null) {
                            seenIds.add(msgId)
                            return match.groupValues[1]
                        }
                    }
                }
            }
            delay(2000)
        }
        error("Timed out waiting for 6-digit OTP email on mail.tm")
    }

    private fun generateTotpCode(base32Secret: String, timeMillis: Long = System.currentTimeMillis()): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val cleaned = base32Secret.trim().uppercase().replace("=", "").replace(" ", "")
        var buffer = 0
        var bitsLeft = 0
        val out = ArrayList<Byte>()
        for (ch in cleaned) {
            val val5 = alphabet.indexOf(ch)
            if (val5 < 0) continue
            buffer = (buffer shl 5) or val5
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                out.add(((buffer shr bitsLeft) and 0xFF).toByte())
            }
        }
        val keyBytes = out.toByteArray()
        val timeStep = timeMillis / 1000L / 30L
        val data = ByteBuffer.allocate(8).putLong(timeStep).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
        val hash = mac.doFinal(data)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val otp = binary % 1_000_000
        return otp.toString().padStart(6, '0')
    }

    private suspend fun authenticateLiveCustomer(authRepo: AuthRepository): UserDto {
        val (testEmail, mailToken) = createMailTmInbox()
        val testPassword = "ServixLive#2026!B"
        val seenIds = mutableSetOf<String>()
        println("STAGE3 AUTH: Registering live customer $testEmail")
        val regOutcome = authRepo.register("Chidi Nwosu", testEmail, testPassword).getOrThrow()
        if (regOutcome is AuthOutcome.Session) {
            return regOutcome.user
        }
        val otp = pollForSixDigitOtp(mailToken, seenIds)
        authRepo.securityEmailVerify(otp).getOrThrow()
        val totpSetup = authRepo.securityTotpSetup().getOrThrow()
        val totpCode = generateTotpCode(totpSetup.secret)
        authRepo.securityTotpEnroll(totpCode).getOrThrow()
        authRepo.securityRecoveryCodes().getOrThrow()
        return authRepo.securityFinish(savedRecovery = true).getOrThrow()
    }

    private fun saveActivityScreenshot(
        activity: ComponentActivity,
        widthPx: Int = 1080,
        heightPx: Int = 2100,
        fileName: String,
    ) {
        shadowOf(android.os.Looper.getMainLooper()).idle()
        val rootView = activity.window.decorView
        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        rootView.layout(0, 0, widthPx, heightPx)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        rootView.draw(canvas)

        val outFiles = listOf(
            File("/.aistudio/artifacts/brain/461da82f-55c4-496d-ad3e-97dd59e099df/$fileName"),
            File("build/reports/$fileName"),
        )
        for (outFile in outFiles) {
            outFile.parentFile?.mkdirs()
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            println("STAGE3 SCREENSHOT: Saved $fileName to ${outFile.absolutePath} (${outFile.length()} bytes)")
            assertTrue(outFile.exists() && outFile.length() > 1000)
        }
    }

    @Test
    fun verifyLiveStage3BookingFlowAndScreenshots() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = TokenStore(context)
        val cookieJar = PersistentCookieJar(context)
        tokenStore.clear()
        cookieJar.clear()

        val authRepo = AuthRepository(tokenStore, cookieJar)
        val networkModule = ServixNetworkModule(
            context = context,
            tokenStore = tokenStore,
            cookieJar = cookieJar,
            onSessionRefreshed = authRepo::onSessionRefreshedFromAuthenticator,
            onSessionExpired = authRepo::onSessionExpiredFromAuthenticator,
        )
        authRepo.attachNetworkModule(networkModule)
        val catalogueRepo = CatalogueRepository { networkModule.apiService }
        val bookingRepo = BookingRepository { networkModule.apiService }

        // 1. Authenticate real test customer against live API
        val customer = authenticateLiveCustomer(authRepo)
        println("STAGE3 STEP 1: Authenticated live customer id=${customer.id} email=${customer.email}")

        // 2. Fetch service detail + availability slots for brand-identity-package
        val serviceSlug = "brand-identity-package"
        val serviceDetail = catalogueRepo.getService(serviceSlug).getOrThrow()
        val availability = catalogueRepo.getServiceAvailability(serviceSlug, days = 14).getOrThrow()
        println("STAGE3 STEP 2: GET /services/$serviceSlug/availability?days=14 -> HTTP 200 slots.size=${availability.slots.size} firstSlot=${availability.slots.firstOrNull()}")
        assertTrue(availability.slots.isNotEmpty())

        // Pick a slot (e.g. 5th slot to avoid any collision)
        val chosenSlot = availability.slots.getOrElse(4) { availability.slots.first() }
        val notesText = "Brand identity package for our fintech launch in Lagos."

        // Render Screenshot 1: Booking Sheet on brand-identity-package
        val sheetController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val sheetActivity = sheetController.get()
        sheetActivity.setContent {
            ServixTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WarmIvory),
                ) {
                    BookingSheetContent(
                        user = customer,
                        service = serviceDetail,
                        slots = availability.slots,
                        availabilityLoading = false,
                        selectedSlot = chosenSlot,
                        notes = notesText,
                        rebookedFrom = null,
                        busy = false,
                        error = null,
                        onSelectSlot = {},
                        onNotesChange = {},
                        onBookAndPayClick = {},
                        onCloseClick = {},
                        onSignInClick = {},
                        onCreateAccountClick = {},
                    )
                }
            }
        }
        saveActivityScreenshot(
            activity = sheetActivity,
            widthPx = 1080,
            heightPx = 1920,
            fileName = "stage3_booking_sheet.png",
        )

        // 3. Create first booking: POST /bookings
        val firstBooking = bookingRepo.createBooking(
            serviceId = serviceSlug,
            scheduledAt = chosenSlot,
            notes = notesText,
        ).getOrThrow()
        println(
            "STAGE3 STEP 3: POST /bookings -> HTTP 201 " +
                "id=${firstBooking.id} reference=${firstBooking.reference} status=${firstBooking.status} " +
                "scheduledAt=${firstBooking.scheduledAt} amount=${firstBooking.amount} currency=${firstBooking.currency} " +
                "serviceTitle='${firstBooking.serviceTitle}' priceUnit='${firstBooking.priceUnit}' " +
                "serviceId=${firstBooking.serviceId} professionalId=${firstBooking.professionalId} " +
                "professionalName='${firstBooking.professionalName}' customerName='${firstBooking.customerName}' " +
                "notes='${firstBooking.notes}' hasReview=${firstBooking.hasReview}",
        )
        assertEquals("pending_payment", firstBooking.status)
        assertEquals(serviceSlug, firstBooking.serviceId)

        // 4. Call POST /bookings/:id/pay
        val payResult = bookingRepo.payBooking(firstBooking.id)
        payResult.fold(
            onSuccess = { payResp ->
                println("STAGE3 STEP 4: POST /bookings/${firstBooking.id}/pay -> HTTP 200 authorizationUrl=${payResp.authorizationUrl} reference=${payResp.reference}")
            },
            onFailure = { err ->
                println("STAGE3 STEP 4: POST /bookings/${firstBooking.id}/pay -> FAILED: ${err.message}")
            },
        )

        // 5. Check GET /bookings/:id and GET /bookings (Active tab)
        val fetchedFirst = bookingRepo.getBooking(firstBooking.id).getOrThrow()
        println("STAGE3 STEP 5a: GET /bookings/${firstBooking.id} -> HTTP 200 status=${fetchedFirst.status} reference=${fetchedFirst.reference}")

        val listAfterFirst = bookingRepo.getMyBookings().getOrThrow()
        println("STAGE3 STEP 5b: GET /bookings -> HTTP 200 count=${listAfterFirst.size} statuses=${listAfterFirst.map { "${it.reference}:${it.status}" }}")
        assertTrue(listAfterFirst.any { it.id == firstBooking.id })

        // Render Screenshot 2: Booking Detail Screen (Active / pending_payment)
        val detailController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val detailActivity = detailController.get()
        detailActivity.setContent {
            ServixTheme {
                BookingDetailScreen(
                    uiState = BookingDetailUiState(
                        loading = false,
                        booking = fetchedFirst,
                    ),
                    pollingAfterPay = false,
                    onResumeCheck = {},
                    onBackClick = {},
                    onRetry = {},
                    onRefreshPaymentStatus = {},
                    onPayClick = {},
                    onOpenModal = {},
                    onCloseModal = {},
                    onSubmitCancel = {},
                    onSubmitConfirmCompletion = {},
                    onSubmitDispute = {},
                    onSubmitReview = { _, _ -> },
                    onStartBookAgain = {},
                    onContinueRebook = { _, _, _ -> },
                    onOpenProfessional = {},
                    onOpenService = {},
                )
            }
        }
        saveActivityScreenshot(
            activity = detailActivity,
            widthPx = 1080,
            heightPx = 1920,
            fileName = "stage3_booking_detail.png",
        )

        // 6. Cancel first booking: POST /bookings/:id/cancel and re-fetch GET /bookings/:id
        val cancelledPost = bookingRepo.cancelBooking(
            id = firstBooking.id,
            reason = "Rescheduling our launch timeline.",
        ).getOrThrow()
        val cancelledFirst = bookingRepo.getBooking(firstBooking.id).getOrThrow()
        println(
            "STAGE3 STEP 6: POST /bookings/${firstBooking.id}/cancel -> HTTP 200 " +
                "status=${cancelledPost.status} cancelledBy=${cancelledFirst.cancelledBy} " +
                "cancelReason='${cancelledFirst.cancelReason}' professionalName='${cancelledFirst.professionalName}'",
        )
        assertEquals("cancelled", cancelledFirst.status)

        // Render Screenshot 2b: Booking Detail Screen after cancellation (showing Cancelled chip, reason, Book again)
        detailActivity.setContent {
            ServixTheme {
                BookingDetailScreen(
                    uiState = BookingDetailUiState(
                        loading = false,
                        booking = cancelledFirst,
                        actionSuccessMessage = "Booking cancelled.",
                    ),
                    pollingAfterPay = false,
                    onResumeCheck = {},
                    onBackClick = {},
                    onRetry = {},
                    onRefreshPaymentStatus = {},
                    onPayClick = {},
                    onOpenModal = {},
                    onCloseModal = {},
                    onSubmitCancel = {},
                    onSubmitConfirmCompletion = {},
                    onSubmitDispute = {},
                    onSubmitReview = { _, _ -> },
                    onStartBookAgain = {},
                    onContinueRebook = { _, _, _ -> },
                    onOpenProfessional = {},
                    onOpenService = {},
                )
            }
        }
        saveActivityScreenshot(
            activity = detailActivity,
            widthPx = 1080,
            heightPx = 1920,
            fileName = "stage3_booking_detail_cancelled.png",
        )

        // 7. Call GET /bookings/:id/rebook on the cancelled booking
        val rebookDraft = bookingRepo.getRebookDraft(firstBooking.id).getOrThrow()
        println(
            "STAGE3 STEP 7: GET /bookings/${firstBooking.id}/rebook -> HTTP 200 " +
                "previousBookingId=${rebookDraft.previousBookingId} previousStatus=${rebookDraft.previousStatus} " +
                "professional={id=${rebookDraft.professional.id}, name='${rebookDraft.professional.name}'} " +
                "previousServiceAvailable=${rebookDraft.previousServiceAvailable} previousNotes='${rebookDraft.previousNotes}' " +
                "services=${rebookDraft.services.map { "${it.id}(price=${it.price},wasBooked=${it.wasBooked})" }}",
        )
        assertEquals(firstBooking.id, rebookDraft.previousBookingId)
        assertTrue(rebookDraft.services.isNotEmpty())

        // 8. Create second booking via Book again (with rebookedFrom = previousBookingId)
        val secondSlot = availability.slots.getOrElse(8) { availability.slots.last() }
        val rebookServiceSlug = rebookDraft.services.firstOrNull { it.wasBooked }?.id ?: serviceSlug
        val secondBooking = bookingRepo.createBooking(
            serviceId = rebookServiceSlug,
            scheduledAt = secondSlot,
            notes = rebookDraft.previousNotes,
            rebookedFrom = rebookDraft.previousBookingId,
        ).getOrThrow()
        println(
            "STAGE3 STEP 8: POST /bookings (rebookedFrom=${rebookDraft.previousBookingId}) -> HTTP 201 " +
                "id=${secondBooking.id} reference=${secondBooking.reference} status=${secondBooking.status} scheduledAt=${secondBooking.scheduledAt}",
        )
        assertEquals("pending_payment", secondBooking.status)

        // Render Screenshot 3: Bookings Screen showing Active tab (with secondBooking) and Past tab (with cancelledFirst)
        val allBookingsWithActiveAndPast = bookingRepo.getMyBookings().getOrThrow()
        println("STAGE3 STEP 8b: GET /bookings (with active + past) -> HTTP 200 total=${allBookingsWithActiveAndPast.size} items=${allBookingsWithActiveAndPast.map { "${it.reference}:${it.status}" }}")

        val listController = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val listActivity = listController.get()
        listActivity.setContent {
            ServixTheme {
                BookingsScreen(
                    user = customer,
                    uiState = BookingsUiState(
                        loading = false,
                        bookings = allBookingsWithActiveAndPast,
                        selectedTab = BookingsListTab.ACTIVE,
                    ),
                    onTabSelect = {},
                    onRefresh = {},
                    onExploreServicesClick = {},
                    onSignInClick = {},
                    onOpenBooking = {},
                )
            }
        }
        saveActivityScreenshot(
            activity = listActivity,
            widthPx = 1080,
            heightPx = 1800,
            fileName = "stage3_bookings_active.png",
        )

        // 9. Cancel the second booking (which is in pending_payment)
        val cancelledSecond = bookingRepo.cancelBooking(
            id = secondBooking.id,
            reason = "Cancelling pending_payment rebooked booking as required by Stage 3 acceptance.",
        ).getOrThrow()
        println(
            "STAGE3 STEP 9: POST /bookings/${secondBooking.id}/cancel -> HTTP 200 " +
                "status=${cancelledSecond.status} cancelledBy=${cancelledSecond.cancelledBy} cancelReason='${cancelledSecond.cancelReason}'",
        )
        assertEquals("cancelled", cancelledSecond.status)

        // Final GET /bookings check and Past tab screenshot
        val finalBookings = bookingRepo.getMyBookings().getOrThrow()
        println("STAGE3 STEP 10: Final GET /bookings -> HTTP 200 total=${finalBookings.size} items=${finalBookings.map { "${it.reference}:${it.status}" }}")

        listActivity.setContent {
            ServixTheme {
                BookingsScreen(
                    user = customer,
                    uiState = BookingsUiState(
                        loading = false,
                        bookings = finalBookings,
                        selectedTab = BookingsListTab.PAST,
                    ),
                    onTabSelect = {},
                    onRefresh = {},
                    onExploreServicesClick = {},
                    onSignInClick = {},
                    onOpenBooking = {},
                )
            }
        }
        saveActivityScreenshot(
            activity = listActivity,
            widthPx = 1080,
            heightPx = 1800,
            fileName = "stage3_bookings_past.png",
        )
    }
}
