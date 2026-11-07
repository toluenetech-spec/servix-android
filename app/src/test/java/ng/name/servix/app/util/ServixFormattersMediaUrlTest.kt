package ng.name.servix.app.util

import ng.name.servix.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `resolveServixMediaUrl` decides which host an image path belongs to, and getting it wrong is
 * exactly how profile photos break: the API serves `/media/…` and `/uploads/…`, while catalogue seed
 * assets live on the marketing site. These are the three cases, plus the edges that used to matter.
 */
class ServixFormattersMediaUrlTest {

    private val apiBase = ServixFormatters.apiMediaBase()          // https://<api-origin>/api/v1
    private val webBase = BuildConfig.WEB_BASE_URL.trimEnd('/')    // https://www.servix.name.ng

    @Test
    fun `absolute http and https urls are returned unchanged`() {
        assertEquals(
            "https://cdn.servix.name.ng/avatars/a1b2c3.jpg",
            ServixFormatters.resolveServixMediaUrl("https://cdn.servix.name.ng/avatars/a1b2c3.jpg"),
        )
        assertEquals(
            "http://10.0.2.2:8080/media/avatar/legacy.jpg",
            ServixFormatters.resolveServixMediaUrl("http://10.0.2.2:8080/media/avatar/legacy.jpg"),
        )
    }

    @Test
    fun `media paths resolve against the API origin plus api-v1`() {
        assertEquals(
            "$apiBase/media/avatar/adaeze-okonkwo.jpg",
            ServixFormatters.resolveServixMediaUrl("/media/avatar/adaeze-okonkwo.jpg"),
        )
    }

    @Test
    fun `upload paths resolve against the API origin plus api-v1`() {
        assertEquals(
            "$apiBase/uploads/2026/11/portfolio.webp",
            ServixFormatters.resolveServixMediaUrl("/uploads/2026/11/portfolio.webp"),
        )
    }

    @Test
    fun `other root-relative paths stay on the website`() {
        assertEquals(
            "$webBase/images/professionals/adaeze-okafor.jpg",
            ServixFormatters.resolveServixMediaUrl("/images/professionals/adaeze-okafor.jpg"),
        )
        assertEquals(
            "$webBase/brand/servix-social.png",
            ServixFormatters.resolveServixMediaUrl("/brand/servix-social.png"),
        )
    }

    @Test
    fun `paths without a leading slash follow the same rule`() {
        assertEquals(
            "$apiBase/media/avatar/adaeze-okonkwo.jpg",
            ServixFormatters.resolveServixMediaUrl("media/avatar/adaeze-okonkwo.jpg"),
        )
        assertEquals(
            "$webBase/images/services/brand-identity.jpg",
            ServixFormatters.resolveServixMediaUrl("images/services/brand-identity.jpg"),
        )
    }

    @Test
    fun `the api base is the configured origin plus api-v1 and is built cleanly`() {
        val origin = java.net.URI(BuildConfig.API_BASE_URL.trim()).let { "${it.scheme}://${it.authority}" }
        assertEquals("$origin/api/v1", apiBase)
        assertTrue("api base must not end with a slash", !apiBase.endsWith("/"))
        // Careful: "https://api.servix.name.ng/api/v1" does contain "//api" — the host starts with
        // "api.". The repeated-slash check has to look at the path, not the whole URL.
        assertTrue(
            "api base must not repeat a slash after the scheme",
            !apiBase.substringAfter("://").contains("//"),
        )
        // The website host and the API host are different in the shipped configuration.
        assertTrue(apiBase.startsWith("https://"))
    }

    @Test
    fun `blank and null resolve to nothing`() {
        assertNull(ServixFormatters.resolveServixMediaUrl(null))
        assertNull(ServixFormatters.resolveServixMediaUrl(""))
        assertNull(ServixFormatters.resolveServixMediaUrl("   "))
    }
}
