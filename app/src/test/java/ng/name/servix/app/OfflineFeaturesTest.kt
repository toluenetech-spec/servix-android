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
        assertEquals("Maiduguri", nearestCity(12.72, 13.151)?.name) // 98.8 km
        assertNull(nearestCity(12.75, 13.151)) // 102.2 km
        assertNull(nearestCity(0.0, 0.0))
        assertNull(nearestCity(Double.NaN, 3.0))
        assertTrue(nigerianCities.size >= 40)
    }
    @Test fun picksHighestSemverNotTheFirstRelease() {
        // GitHub sorts by tag text, so an old test-build tag can sit above a newer semver tag.
        fun rel(tag: String, name: String, apkName: String = "servix.apk") = Release(
            tag, name, "https://github.com/toluenetech-spec/servix-android/releases/tag/$tag",
            assets = listOf(apk.copy(name = apkName)),
        )
        val unordered = listOf(
            rel("test-build-7", "Servix test build 7 (v0.4.0)"),
            rel("v0.6.1-b19", "Servix v0.6.1 (build 19)", "servix-0.6.1-build19.apk"),
            rel("v0.6.10-b30", "Servix v0.6.10 (build 30)", "servix-0.6.10-build30.apk"),
            rel("v0.6.2-b21", "Servix v0.6.2 (build 21)", "servix-0.6.2-build21.apk"),
        )
        assertEquals("0.6.10", selectUpdate(unordered, "0.6.1")?.version)
        assertEquals("0.6.2", selectUpdate(unordered.filterNot { it.tag_name == "v0.6.10-b30" }, "0.6.1")?.version)
        assertNull(selectUpdate(unordered, "0.6.10"))
    }
    @Test fun ignoresReleasesBeyondTheFirstTen() {
        val filler = (1..10).map { Release("v0.6.1-b$it", "Servix v0.6.1 (build $it)", "https://github.com/toluenetech-spec/servix-android/releases/tag/v0.6.1-b$it", assets = listOf(apk)) }
        val tooFar = Release("v0.9.9-b99", "Servix v0.9.9 (build 99)", "https://github.com/toluenetech-spec/servix-android/releases/tag/v0.9.9-b99", assets = listOf(apk))
        assertNull(selectUpdate(filler + tooFar, "0.6.1"))
        assertEquals("0.9.9", selectUpdate(listOf(filler.first(), tooFar), "0.6.1")?.version)
    }
    @Test fun readsVersionFromTagWhenTheNameHasNone() {
        val tagOnly = Release("v0.7.0-b40", "Servix Android build 40", "https://github.com/toluenetech-spec/servix-android/releases/tag/v0.7.0-b40", assets = listOf(apk))
        assertEquals("0.7.0", selectUpdate(listOf(tagOnly), "0.6.9")?.version)
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
