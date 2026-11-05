package ng.name.servix.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import ng.name.servix.app.data.local.TokenStore
import ng.name.servix.app.data.network.PersistentCookieJar
import ng.name.servix.app.data.network.ServixNetworkModule
import ng.name.servix.app.data.repository.AuthRepository
import ng.name.servix.app.data.repository.CatalogueRepository
import ng.name.servix.app.ui.catalogue.ReviewsTabSection
import ng.name.servix.app.ui.theme.Charcoal
import ng.name.servix.app.ui.theme.DeepForest
import ng.name.servix.app.ui.theme.MutedGray
import ng.name.servix.app.ui.theme.ServixTheme
import ng.name.servix.app.ui.theme.WarmIvory
import ng.name.servix.app.util.ServixFormatters
import okhttp3.OkHttpClient
import okhttp3.Request
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
class Stage2CatalogueVerificationTest {

    @Test
    fun verifyLiveCatalogueSearchFiltersProfilesServicesAndImages() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tokenStore = TokenStore(context)
        val cookieJar = PersistentCookieJar(context)
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

        // 1. GET /categories & GET /stats & Home top-rated professionals
        val categories = catalogueRepo.getCategories(forceRefresh = true).getOrThrow()
        println("STAGE2 STEP 1a: GET /categories -> HTTP 200 count=${categories.size} first=${categories.first().id} (${categories.first().name})")
        assertTrue(categories.isNotEmpty())

        val stats = catalogueRepo.getStats().getOrThrow()
        println("STAGE2 STEP 1b: GET /stats -> HTTP 200 verifiedProfessionals=${stats.verifiedProfessionals} activeServices=${stats.activeServices} categories=${stats.serviceCategories} avgRating=${stats.averageRating}")
        assertNotNull(stats.verifiedProfessionals)
        assertNotNull(stats.activeServices)
        assertNotNull(stats.serviceCategories)
        assertNotNull(stats.averageRating)

        val topPros = catalogueRepo.getProfessionals(sort = "rating", page = 1, pageSize = 10).getOrThrow()
        println("STAGE2 STEP 1c: GET /professionals?sort=rating&pageSize=10 -> HTTP 200 total=${topPros.total} items=${topPros.items.size}")
        assertTrue(topPros.items.isNotEmpty())

        // 2. Search "designer" and "developer" return real results with photos
        val designerRes = catalogueRepo.getProfessionals(q = "designer").getOrThrow()
        println("STAGE2 STEP 2a: GET /professionals?q=designer -> HTTP 200 total=${designerRes.total} names=${designerRes.items.map { "${it.name} (verified=${it.verified}, price=${ServixFormatters.formatNaira(it.startingPrice)})" }}")
        assertTrue("Expected real results for q=designer", designerRes.items.isNotEmpty())

        val developerRes = catalogueRepo.getProfessionals(q = "developer").getOrThrow()
        println("STAGE2 STEP 2b: GET /professionals?q=developer -> HTTP 200 total=${developerRes.total} names=${developerRes.items.map { "${it.name} (verified=${it.verified}, price=${ServixFormatters.formatNaira(it.startingPrice)})" }}")
        assertTrue("Expected real results for q=developer", developerRes.items.isNotEmpty())

        // Verify resolved photo URLs return HTTP 200 image/*
        val plainClient = OkHttpClient()
        for (pro in listOf(designerRes.items.first(), developerRes.items.first())) {
            val resolvedPhoto = ServixFormatters.resolveServixMediaUrl(pro.image)
            assertNotNull("Expected photo URL for ${pro.id}", resolvedPhoto)
            val req = Request.Builder().url(resolvedPhoto!!).head().build()
            plainClient.newCall(req).execute().use { resp ->
                println("STAGE2 STEP 2c: Photo HEAD $resolvedPhoto -> HTTP ${resp.code} Content-Type=${resp.header("Content-Type")}")
                assertEquals(200, resp.code)
            }
        }

        // 3. Filters change results (category, location, minRating, maxPrice, sort, available)
        val lagosPros = catalogueRepo.getProfessionals(location = "Lagos").getOrThrow()
        println("STAGE2 STEP 3a: GET /professionals?location=Lagos -> HTTP 200 total=${lagosPros.total} cities=${lagosPros.items.map { it.location }}")
        assertTrue(lagosPros.items.isNotEmpty())
        assertTrue(lagosPros.total < topPros.total)

        val priceAscPros = catalogueRepo.getProfessionals(sort = "price-asc").getOrThrow()
        val priceDescPros = catalogueRepo.getProfessionals(sort = "price-desc").getOrThrow()
        println("STAGE2 STEP 3b: sort=price-asc first=${priceAscPros.items.first().startingPrice}, sort=price-desc first=${priceDescPros.items.first().startingPrice}")
        assertTrue(priceAscPros.items.first().startingPrice <= priceDescPros.items.first().startingPrice)

        val max150Pros = catalogueRepo.getProfessionals(maxPrice = 150000).getOrThrow()
        println("STAGE2 STEP 3c: GET /professionals?maxPrice=150000 -> HTTP 200 total=${max150Pros.total} maxObserved=${max150Pros.items.maxOfOrNull { it.startingPrice }}")
        assertTrue(max150Pros.items.all { it.startingPrice <= 150000L })

        val nonsenseQuery = "zzzznonexistent99999"
        val emptyRes = catalogueRepo.getProfessionals(q = nonsenseQuery).getOrThrow()
        println("STAGE2 STEP 3d: GET /professionals?q=$nonsenseQuery -> HTTP 200 total=${emptyRes.total} items=${emptyRes.items.size}")
        assertEquals(0, emptyRes.total)
        assertTrue(emptyRes.items.isEmpty())

        // 4. Open at least two professional profiles (tunde-bakare & toluwalase-olawumi)
        for (slug in listOf("tunde-bakare", "toluwalase-olawumi")) {
            val detail = catalogueRepo.getProfessional(slug).getOrThrow()
            val svcs = catalogueRepo.getProfessionalServices(slug).getOrThrow()
            val revs = catalogueRepo.getProfessionalReviews(slug).getOrThrow()
            val avail = catalogueRepo.getProfessionalAvailabilitySummary(slug).getOrThrow()
            println("STAGE2 STEP 4: Profile '$slug' -> HTTP 200 name='${detail.name}' verified=${detail.verified} skills=${detail.skills.size} services=${svcs.size} reviews=${revs.size} availableToday=${avail.availableToday}")
            revs.forEach { rev ->
                println("  ReviewDto[id=${rev.id}, serviceId=${rev.serviceId}, professionalId=${rev.professionalId}, author='${rev.author}', rating=${rev.rating}, date='${rev.date}', text='${rev.text}']")
                assertTrue(rev.id.isNotBlank())
                assertTrue(rev.author.isNotBlank())
                assertTrue(rev.rating in 1..5)
                assertTrue(rev.date.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
                assertTrue(rev.text.isNotBlank())
            }
            assertEquals(slug, detail.id)
        }

        // 5. Open at least two services (brand-identity-package & business-website-development)
        val websiteReviews = catalogueRepo.getServiceReviews("business-website-development").getOrThrow()
        val brandReviews = catalogueRepo.getServiceReviews("brand-identity-package").getOrThrow()
        for (serviceSlug in listOf("brand-identity-package", "business-website-development")) {
            val svcDetail = catalogueRepo.getService(serviceSlug).getOrThrow()
            val svcRevs = catalogueRepo.getServiceReviews(serviceSlug).getOrThrow()
            val svcAvail = catalogueRepo.getServiceAvailability(serviceSlug, days = 14).getOrThrow()
            println("STAGE2 STEP 5: Service '$serviceSlug' -> HTTP 200 title='${svcDetail.title}' price=${ServixFormatters.formatNaira(svcDetail.price)} gallery=${svcDetail.gallery.size} faqs=${svcDetail.faqs.size} reviews=${svcRevs.size} slots=${svcAvail.slots.size}")
            assertEquals(serviceSlug, svcDetail.id)
            assertTrue(svcAvail.slots.isNotEmpty())
        }

        // 6. Render real Compose Review List with live reviews from GET /services/business-website-development/reviews and capture PNG screenshot
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            ServixTheme {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmIvory)
                        .padding(20.dp),
                ) {
                    Text(
                        text = "Business Website Design & Development",
                        style = MaterialTheme.typography.headlineSmall,
                        color = DeepForest,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reviews (${websiteReviews.size} from live GET /services/business-website-development/reviews)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    ReviewsTabSection(reviews = websiteReviews + brandReviews)
                }
            }
        }
        shadowOf( android.os.Looper.getMainLooper() ).idle()

        val rootView = activity.window.decorView
        val widthPx = 1080
        val heightPx = 1650
        rootView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
        )
        rootView.layout(0, 0, widthPx, heightPx)
        shadowOf( android.os.Looper.getMainLooper() ).idle()

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        rootView.draw(canvas)

        val outFiles = listOf(
            File("/.aistudio/artifacts/brain/461da82f-55c4-496d-ad3e-97dd59e099df/stage2_review_list.png"),
            File("build/reports/stage2_review_list.png"),
        )
        for (outFile in outFiles) {
            outFile.parentFile?.mkdirs()
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            println("STAGE2 STEP 6: Saved rendered review list screenshot to ${outFile.absolutePath} (${outFile.length()} bytes)")
            assertTrue(outFile.exists() && outFile.length() > 1000)
        }
    }
}
