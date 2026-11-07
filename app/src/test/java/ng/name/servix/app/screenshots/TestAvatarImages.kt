package ng.name.servix.app.screenshots

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.Coil
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

/**
 * Serves profile photos in tests from a **local image file** instead of the network.
 *
 * The screenshot harness must never reach the network, but the avatar path is exactly what these
 * renders are supposed to prove, so a fetch that always fails would make every "avatar" render a
 * letter tile and hide the bug. Any request whose URL contains [MATCHES] is answered with
 * `src/test/resources/servix_avatar_fixture.png` — a test resource, so nothing ships in the app and
 * the release unit-test source set still compiles (it never sees a debug-only `R` id).
 */
object TestAvatarImages {
    /** The distinguishing colour of the fixture, used by [countFixturePixels]. */
    const val FIXTURE_RGB = 0xFFF26B4B.toInt()

    /** Any URL containing this is answered locally. Matches the API path `/media/avatar/…`. */
    const val MATCHES = "/media/avatar/"

    private const val FIXTURE_RESOURCE = "servix_avatar_fixture.png"

    /** Last load error Coil reported, so a failed render says why instead of "did not load". */
    @Volatile
    var lastError: String? = null
        private set

    private val fixtureBitmap: Bitmap by lazy {
        val stream = TestAvatarImages::class.java.classLoader?.getResourceAsStream(FIXTURE_RESOURCE)
            ?: error("$FIXTURE_RESOURCE missing from app/src/test/resources")
        stream.use {
            BitmapFactory.decodeStream(it)
                ?: error("$FIXTURE_RESOURCE could not be decoded as an image")
        }
    }

    /** Install a Coil loader whose fetcher resolves [MATCHES] to the bundled fixture. */
    fun install(context: Context) {
        // Compose tests share the process: drop any loader created by an earlier test.
        Coil.reset()
        lastError = null
        Coil.setImageLoader(
            ImageLoader.Builder(context)
                // Robolectric draws into a software Canvas, and a hardware bitmap cannot be drawn
                // there: without this the photo loads and then silently paints nothing.
                .allowHardware(false)
                .components { add(Factory()) }
                .eventListener(object : coil.EventListener {
                    override fun onError(request: coil.request.ImageRequest, result: coil.request.ErrorResult) {
                        lastError = "${request.data} -> ${result.throwable.javaClass.simpleName}: ${result.throwable.message}"
                    }
                })
                .build(),
        )
    }

    /**
     * Number of pixels close to [FIXTURE_RGB] in [bitmap]. A rendered photo leaves thousands; the
     * initials fallback (SoftSand circle + Deep Forest letters) leaves none.
     */
    fun countFixturePixels(bitmap: Bitmap, tolerance: Int = 12): Int {
        val targetR = (FIXTURE_RGB shr 16) and 0xFF
        val targetG = (FIXTURE_RGB shr 8) and 0xFF
        val targetB = FIXTURE_RGB and 0xFF
        var count = 0
        // Sample every 2nd pixel: plenty of signal, and keeps the assertion cheap on 1080x2280.
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                val dr = ((pixel shr 16) and 0xFF) - targetR
                val dg = ((pixel shr 8) and 0xFF) - targetG
                val db = (pixel and 0xFF) - targetB
                if (dr * dr + dg * dg + db * db <= tolerance * tolerance * 3) count++
                x += 2
            }
            y += 2
        }
        return count
    }

    private class FixtureFetcher(private val context: Context, private val bitmap: Bitmap) : Fetcher {
        override suspend fun fetch(): FetchResult = DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    /** Claims only URLs under [MATCHES]; everything else falls through to Coil's real fetchers. */
    class Factory : Fetcher.Factory<Uri> {
        // Coil maps String data to Uri before factories are consulted, so this must claim Uri.
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!data.toString().contains(MATCHES)) return null
            return FixtureFetcher(options.context, fixtureBitmap)
        }
    }
}
