package ng.name.servix.app.release

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.decodeFromString
import ng.name.servix.app.BuildConfig
import ng.name.servix.app.data.model.AiUsage
import ng.name.servix.app.data.model.BookingDto
import ng.name.servix.app.data.model.NotificationListResponse
import ng.name.servix.app.data.model.SavedProfessionalItemDto
import ng.name.servix.app.data.model.UserDto
import ng.name.servix.app.data.network.ApiErrorParser
import ng.name.servix.app.di.AppContainer
import ng.name.servix.app.updates.UpdateUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Release-variant smoke test: constructs the real DI graph and decodes one fixture per DTO family
 * with the same Json instance the app uses. Run in CI as `:app:testReleaseUnitTest`, i.e. against the
 * release compilation with R8/shrink enabled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReleaseSmokeTest {

    private val json = ApiErrorParser.json

    @Test
    fun buildConfigIsReleaseReady() {
        assertEquals("1.0.1", BuildConfig.VERSION_NAME)
        assertEquals(101, BuildConfig.VERSION_CODE)
        // "github" only in debug; Play builds must never reach the updater.
        assertTrue(
            "Unexpected update channel ${BuildConfig.SELF_UPDATE_CHANNEL}",
            BuildConfig.SELF_UPDATE_CHANNEL in setOf("github", "none"),
        )
    }

    @Test
    fun updaterIsUnreachableOnPlayChannel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val state = UpdateUiState(context, CoroutineScope(Dispatchers.Unconfined))
        if (BuildConfig.SELF_UPDATE_CHANNEL == "none") {
            assertFalse("Self-update must be disabled in release builds", state.enabled)
        } else {
            assertTrue(state.enabled)
        }
    }

    @Test
    fun containerBuildsOnReleaseVariant() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = AppContainer.getInstance(context)
        assertEquals("https://api.servix.name.ng/api/v1/", BuildConfig.API_BASE_URL)
        assertTrue(container.catalogueRepository != null)
        assertTrue(container.accountRepository != null)
        assertTrue(container.authRepository != null)
    }

    @Test
    fun decodesUserAndAccountFamily() {
        val user = json.decodeFromString<UserDto>(
            """{"id":"11111111-1111-4111-8111-111111111111","email":"a@b.co","fullName":"Adaeze Okonkwo","role":"customer","emailVerified":true}""",
        )
        assertEquals("Adaeze Okonkwo", user.fullName)

        val notifications = json.decodeFromString<NotificationListResponse>(
            """{"unread":1,"before":null,"items":[{"id":"n1","type":"booking","title":"Accepted","body":"Your booking was accepted.","link":"/bookings/1","readAt":null,"createdAt":"2026-11-05T09:12:00.000Z"}]}""",
        )
        assertEquals(1, notifications.items.size)
        assertEquals(null, notifications.items.first().readAt)

        val saved = json.decodeFromString<SavedProfessionalItemDto>(
            """{"id":"s1","savedAt":"2026-10-01T00:00:00.000Z","preferred":false,"note":"","professional":{"slug":"tunde-bakare","name":"Tunde Bakare","title":"Designer","ratingAvg":4.9,"reviewCount":156,"verification":"verified","availability":"available","startingPrice":120000,"currency":"NGN"}}""",
        )
        assertEquals("tunde-bakare", saved.professional.slug)
    }

    @Test
    fun decodesCatalogueBookingAndAiFamilies() {
        val booking = json.decodeFromString<BookingDto>(
            """{"id":"22222222-2222-4222-8222-222222222222","reference":"SVX-4712","status":"requested","scheduledAt":"2026-11-12T10:00:00.000Z","amount":350000,"serviceTitle":"Brand Identity","priceUnit":"per project","serviceId":"brand","professionalId":"tunde-bakare","professionalName":"Tunde Bakare","customerName":"Adaeze Okonkwo","createdAt":"2026-10-28T09:15:00.000Z"}""",
        )
        assertEquals("requested", booking.status)

        val usage = json.decodeFromString<AiUsage>(
            """{"planLabel":"Free","subject":"user","used":2300,"allowed":20000,"percent":11.5,"level":"ok","enabled":true}""",
        )
        assertEquals(20000L, usage.allowed)
    }
}
