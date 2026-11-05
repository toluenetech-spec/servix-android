package ng.name.servix.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ng.name.servix.app.data.local.TokenStore
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.data.network.PersistentCookieJar
import ng.name.servix.app.data.network.ServixNetworkModule
import ng.name.servix.app.data.repository.AuthOutcome
import ng.name.servix.app.data.repository.AuthRepository
import ng.name.servix.app.data.repository.SessionState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Stage1AuthVerificationTest {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun createMailTmInbox(): Pair<String, String> {
        // 1. Get active domain from api.mail.tm
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
        val address = "servixtest$randomSuffix@$domain"
        val password = "MailTmPass#$randomSuffix"

        // 2. Create account
        val createBody = """{"address":"$address","password":"$password"}""".toRequestBody(jsonMedia)
        val createReq = Request.Builder().url("https://api.mail.tm/accounts").post(createBody).build()
        httpClient.newCall(createReq).execute().use { res ->
            assertTrue("mail.tm /accounts HTTP ${res.code}", res.isSuccessful)
        }

        // 3. Get bearer token for inbox
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
        repeat(25) { attempt ->
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

    @Test
    fun verifyLiveEndToEndRegistrationSecuritySessionAndLogout() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = TokenStore(context)
        val cookieJar = PersistentCookieJar(context)
        tokenStore.clear()
        cookieJar.clear()

        val repo = AuthRepository(tokenStore, cookieJar)
        val networkModule = ServixNetworkModule(
            context = context,
            tokenStore = tokenStore,
            cookieJar = cookieJar,
            onSessionRefreshed = repo::onSessionRefreshedFromAuthenticator,
            onSessionExpired = repo::onSessionExpiredFromAuthenticator,
        )
        repo.attachNetworkModule(networkModule)

        // 0. Check GET /auth/security/config and GET /auth/verification-method
        val secConfig = repo.getSecurityConfig().getOrThrow()
        println("E2E STEP 0a: GET /auth/security/config -> HTTP 200 enabled=${secConfig.enabled}")
        val verifMethod = repo.getVerificationMethod().getOrThrow()
        println("E2E STEP 0b: GET /auth/verification-method -> HTTP 200 method=$verifMethod")

        // 1. Provision real disposable email inbox via mail.tm
        val (testEmail, mailToken) = createMailTmInbox()
        val testPassword = "ServixLive#2026!A"
        val seenEmailIds = mutableSetOf<String>()
        println("E2E STEP 1: Provisioned real inbox $testEmail")

        // 2. POST /auth/register (with X-Servix-Client: ng.name.servix.app, no Origin header)
        val regOutcome = repo.register("Adaeze Okafor", testEmail, testPassword).getOrThrow()
        assertTrue("Expected SecurityRequired from live register", regOutcome is AuthOutcome.SecurityRequired)
        val regSecurity = (regOutcome as AuthOutcome.SecurityRequired).security
        println("E2E STEP 2: POST /auth/register -> HTTP 201 security={next=${regSecurity.next}, purpose=${regSecurity.purpose}, email=${regSecurity.email}}")
        assertEquals("email", regSecurity.next)

        // 3. POST /auth/security/status
        val statusFlow = repo.securityStatus().getOrThrow()
        println("E2E STEP 3: POST /auth/security/status -> HTTP 200 next=${statusFlow.next} purpose=${statusFlow.purpose}")
        assertEquals("email", statusFlow.next)

        // 4. Poll real email inbox for 6-digit OTP and call POST /auth/security/email/verify
        val otpCode = pollForSixDigitOtp(mailToken, seenEmailIds)
        println("E2E STEP 4a: Received 6-digit email OTP from Servix: $otpCode")
        val afterEmailFlow = repo.securityEmailVerify(otpCode).getOrThrow()
        println("E2E STEP 4b: POST /auth/security/email/verify -> HTTP 200 next=${afterEmailFlow.next} purpose=${afterEmailFlow.purpose}")
        assertEquals("enroll", afterEmailFlow.next)

        // 5. POST /auth/security/totp/setup -> { secret, uri }
        val totpSetup = repo.securityTotpSetup().getOrThrow()
        println("E2E STEP 5: POST /auth/security/totp/setup -> HTTP 200 secret_len=${totpSetup.secret.length} uri_prefix=${totpSetup.uri.take(25)}")
        assertTrue(totpSetup.secret.isNotBlank())
        assertTrue(totpSetup.uri.startsWith("otpauth://"))

        // 6. Compute RFC 6238 6-digit TOTP code and call POST /auth/security/totp/enroll
        val totpCode = generateTotpCode(totpSetup.secret)
        val afterEnrollFlow = repo.securityTotpEnroll(totpCode).getOrThrow()
        println("E2E STEP 6: POST /auth/security/totp/enroll -> HTTP 200 next=${afterEnrollFlow.next}")
        assertEquals("recovery", afterEnrollFlow.next)

        // 7. Call POST /auth/security/recovery/codes -> SecurityRecoveryCodesResponse({ recoveryCodes: [...] })
        val parsedCodes = repo.securityRecoveryCodes().getOrThrow()
        assertTrue("Expected non-empty recovery codes", parsedCodes.isNotEmpty())
        println("E2E STEP 7: POST /auth/security/recovery/codes -> HTTP 200 recoveryCodes.size=${parsedCodes.size} sample_len=${parsedCodes.first().length}")

        // 8. POST /auth/security/finish { savedRecovery: true } -> { user, accessToken } + servix_refresh cookie
        val finishedUser = repo.securityFinish(savedRecovery = true).getOrThrow()
        println("E2E STEP 8: POST /auth/security/finish -> HTTP 200 user.id=${finishedUser.id} email=${finishedUser.email} role=${finishedUser.role} kycStatus=${finishedUser.kycStatus} emailVerified=${finishedUser.emailVerified}")
        assertNotNull(tokenStore.getAccessToken())
        assertTrue("Expected servix_refresh cookie after security/finish", cookieJar.hasRefreshCookie())

        // 9. GET /me with Bearer token
        val meUser = repo.refreshMe().getOrThrow()
        println("E2E STEP 9: GET /me -> HTTP 200 id=${meUser.id} emailVerified=${meUser.emailVerified} status=${meUser.status}")
        assertEquals(finishedUser.id, meUser.id)
        assertTrue("Expected emailVerified == true after completing security flow", meUser.emailVerified)

        // 10. Simulate app process kill & reopen: clear accessToken in memory/store, keep PersistentCookieJar
        tokenStore.setAccessToken(null)
        assertNull(tokenStore.getAccessToken())
        val reopenedRepo = AuthRepository(tokenStore, cookieJar)
        val reopenedNetwork = ServixNetworkModule(
            context = context,
            tokenStore = tokenStore,
            cookieJar = cookieJar,
            onSessionRefreshed = reopenedRepo::onSessionRefreshedFromAuthenticator,
            onSessionExpired = reopenedRepo::onSessionExpiredFromAuthenticator,
        )
        reopenedRepo.attachNetworkModule(reopenedNetwork)

        val restoredUser = reopenedRepo.restoreSession().getOrThrow()
        println("E2E STEP 10: Kill & reopen restoreSession (POST /auth/refresh + GET /me) -> HTTP 200 restoredUser.id=${restoredUser?.id} newAccessTokenPresent=${!tokenStore.getAccessToken().isNullOrBlank()}")
        assertNotNull(restoredUser)
        assertEquals(finishedUser.id, restoredUser?.id)
        assertTrue(reopenedRepo.sessionState.value is SessionState.Authenticated)

        // 11. POST /auth/logout -> clears tokens and cookies
        reopenedRepo.logout()
        println("E2E STEP 11: POST /auth/logout -> HTTP 200 accessToken=${tokenStore.getAccessToken()} hasRefreshCookie=${cookieJar.hasRefreshCookie()} state=${reopenedRepo.sessionState.value}")
        assertNull(tokenStore.getAccessToken())
        assertTrue(!cookieJar.hasRefreshCookie())
        assertTrue(reopenedRepo.sessionState.value is SessionState.Unauthenticated)
    }
}
