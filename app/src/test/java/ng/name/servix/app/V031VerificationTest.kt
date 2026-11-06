package ng.name.servix.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ng.name.servix.app.data.local.TermsAcceptanceStore
import ng.name.servix.app.data.local.TokenStore
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.PersistentCookieJar
import ng.name.servix.app.data.network.ServixNetworkModule
import ng.name.servix.app.data.repository.AccountRepository
import ng.name.servix.app.data.repository.AuthOutcome
import ng.name.servix.app.data.repository.AuthRepository
import ng.name.servix.app.data.repository.BookingRepository
import ng.name.servix.app.ui.auth.AuthUiState
import ng.name.servix.app.ui.auth.RegisterScreen
import ng.name.servix.app.ui.auth.WelcomeScreen
import ng.name.servix.app.ui.home.HomeTabScreen
import ng.name.servix.app.ui.home.HomeUiState
import ng.name.servix.app.ui.theme.ServixTheme
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class V031VerificationTest {

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
        val address = "servixv031_$randomSuffix@$domain"
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
        val testPassword = "ServixLive#2026!D"
        val seenIds = mutableSetOf<String>()
        println("V031 AUTH: Registering live customer $testEmail")
        val regOutcome = authRepo.register("Adaeze Okafor", testEmail, testPassword).getOrThrow()
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
        heightPx: Int = 2000,
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
            println("V031 SCREENSHOT: Saved $fileName to ${outFile.absolutePath} (${outFile.length()} bytes)")
            assertTrue(outFile.exists() && outFile.length() > 1000)
        }
    }

    @Test
    fun verifyV031WelcomeTermsAndLiveDashboard() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = TokenStore(context)
        val cookieJar = PersistentCookieJar(context)
        val termsStore = TermsAcceptanceStore(context)
        tokenStore.clear()
        cookieJar.clear()

        // 1. Verify DataStore TermsAcceptanceStore records timestamp locally
        val expectedMs = 1762332000000L
        termsStore.recordTermsAccepted(expectedMs)
        val storedMs = termsStore.getAcceptedTimestampMillis()
        val storedIso = termsStore.getAcceptedIsoTimestamp()
        println("V031 TERMS DATASTORE: expectedMs=$expectedMs storedMs=$storedMs storedIso=$storedIso")
        assertEquals(expectedMs, storedMs)
        assertNotNull(storedIso)

        // 2. Render WelcomeScreen screenshot
        val welcomeActivity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        welcomeActivity.setContent {
            ServixTheme {
                WelcomeScreen(
                    onSignInClick = {},
                    onCreateAccountClick = {},
                    onBrowseFirstClick = {},
                )
            }
        }
        saveActivityScreenshot(
            activity = welcomeActivity,
            widthPx = 1080,
            heightPx = 1920,
            fileName = "v031_welcome.png",
        )

        // 3. Render RegisterScreen with Terms checkbox unticked vs ticked
        val registerActivity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        registerActivity.setContent {
            ServixTheme {
                RegisterScreen(
                    uiState = AuthUiState(
                        registerFullName = "Adaeze Okafor",
                        registerEmail = "adaeze.okafor@example.com",
                        registerPassword = "ServixLive#2026!D",
                        registerConfirmPassword = "ServixLive#2026!D",
                        registerTermsAccepted = false,
                    ),
                    onNameChange = {},
                    onEmailChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onTermsAcceptedChange = {},
                    onSubmit = {},
                    onSignInClick = {},
                    onBackClick = {},
                )
            }
        }
        saveActivityScreenshot(
            activity = registerActivity,
            widthPx = 1080,
            heightPx = 3100,
            fileName = "v031_register_unticked.png",
        )

        registerActivity.setContent {
            ServixTheme {
                RegisterScreen(
                    uiState = AuthUiState(
                        registerFullName = "Adaeze Okafor",
                        registerEmail = "adaeze.okafor@example.com",
                        registerPassword = "ServixLive#2026!D",
                        registerConfirmPassword = "ServixLive#2026!D",
                        registerTermsAccepted = true,
                    ),
                    onNameChange = {},
                    onEmailChange = {},
                    onPasswordChange = {},
                    onConfirmPasswordChange = {},
                    onTermsAcceptedChange = {},
                    onSubmit = {},
                    onSignInClick = {},
                    onBackClick = {},
                )
            }
        }
        saveActivityScreenshot(
            activity = registerActivity,
            widthPx = 1080,
            heightPx = 3100,
            fileName = "v031_register_ticked.png",
        )

        // 4. Authenticate against live API and call GET /account/overview, /account/insights, /account/security-summary, /account/payments, and /bookings
        val authRepo = AuthRepository(tokenStore, cookieJar)
        val networkModule = ServixNetworkModule(
            context = context,
            tokenStore = tokenStore,
            cookieJar = cookieJar,
            onSessionRefreshed = authRepo::onSessionRefreshedFromAuthenticator,
            onSessionExpired = authRepo::onSessionExpiredFromAuthenticator,
        )
        authRepo.attachNetworkModule(networkModule)
        val accountRepo = AccountRepository { networkModule.apiService }
        val bookingRepo = BookingRepository { networkModule.apiService }

        val customer = authenticateLiveCustomer(authRepo)
        println("V031 AUTH COMPLETE: id=${customer.id} fullName='${customer.fullName}' emailVerified=${customer.emailVerified}")

        val overview = accountRepo.getAccountOverview().getOrThrow()
        val insights = accountRepo.getAccountInsights().getOrThrow()
        val securitySummary = accountRepo.getSecuritySummary().getOrThrow()
        val payments = accountRepo.getPayments().getOrThrow()
        val bookings = bookingRepo.getMyBookings().getOrThrow()

        println("V031PARSED GET /account/overview -> $overview")
        println("V031PARSED GET /account/insights -> $insights")
        println("V031PARSED GET /account/security-summary -> $securitySummary")
        println("V031PARSED GET /account/payments -> $payments")
        println("V031PARSED GET /bookings -> count=${bookings.size}")

        // 5. Render the Home tab (pass 2 merged the dashboard into it) with the live data
        val dashboardActivity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        dashboardActivity.setContent {
            ServixTheme {
                HomeTabScreen(
                    user = customer,
                    city = "Lagos",
                    uiState = HomeUiState(
                        loading = false,
                        overview = overview,
                        bookings = bookings,
                        loadedForUserId = customer.id,
                    ),
                )
            }
        }
        saveActivityScreenshot(
            activity = dashboardActivity,
            widthPx = 1080,
            heightPx = 3100,
            fileName = "v031_dashboard.png",
        )
    }
}
