package ng.name.servix.app

import ng.name.servix.app.location.nearestCity
import ng.name.servix.app.location.nigerianCities
import ng.name.servix.app.updates.Release
import ng.name.servix.app.updates.ReleaseAsset
import ng.name.servix.app.updates.selectUpdate
import org.junit.Assert.*
import org.junit.Test

class OfflineFeaturesTest {
    private val apk = ReleaseAsset("servix.apk", "https://github.com/toluenetech-spec/servix-android/releases/download/test-build-4/servix.apk", 100)
    private fun release(name: String, assets: List<ReleaseAsset> = listOf(apk)) = Release("test-build-4", name, "https://github.com/toluenetech-spec/servix-android/releases/tag/test-build-4", assets = assets)

    @Test fun findsNearestCityAndRejectsOutsideCoverage() {
        assertEquals("Ibadan", nearestCity(7.3775, 3.9470)?.name)
        assertEquals("Lagos", nearestCity(6.4550, 3.3841)?.name)
        assertNull(nearestCity(0.0, 0.0))
        assertNull(nearestCity(Double.NaN, 3.0))
        assertTrue(nigerianCities.size >= 40)
    }
    @Test fun selectsFirstReleaseWithApkAndComparesNumericVersions() {
        val versions = listOf(release("no asset v99.0.0", emptyList()), release("Servix test build 4 (v0.3.10)"))
        assertEquals("0.3.10", selectUpdate(versions, "0.3.2")?.version)
        assertNull(selectUpdate(versions, "0.3.10"))
        assertNull(selectUpdate(versions, "1.0.0"))
    }
    @Test fun ignoresInvalidVersionsAndUntrustedDownloadHosts() {
        assertNull(selectUpdate(listOf(release("Build without version")), "0.3.2"))
        assertNull(selectUpdate(listOf(release("Build v0.3.3", listOf(apk.copy(browser_download_url = "https://evil.example/servix.apk")))), "0.3.2"))
        assertNull(selectUpdate(listOf(release("Build v0.3.3", listOf(apk.copy(browser_download_url = "https://github.com/other/repo/releases/download/tag/file.apk")))), "0.3.2"))
    }
}
