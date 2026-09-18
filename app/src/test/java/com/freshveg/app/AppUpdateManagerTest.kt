package com.freshveg.app

import com.freshveg.app.core.update.AppUpdateManager
import org.junit.Assert.*
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testSemanticVersionComparison_newerVersions() {
        assertTrue(AppUpdateManager.compareSemanticVersions("1.0.1", "1.0.0") > 0)
        assertTrue(AppUpdateManager.compareSemanticVersions("1.1.0", "1.0.9") > 0)
        assertTrue(AppUpdateManager.compareSemanticVersions("2.0.0", "1.99.99") > 0)
        assertTrue(AppUpdateManager.compareSemanticVersions("v1.0.1", "1.0.0") > 0)
        assertTrue(AppUpdateManager.compareSemanticVersions("V2.1.0", "v2.0.5") > 0)
    }

    @Test
    fun testSemanticVersionComparison_equalVersions() {
        assertEquals(0, AppUpdateManager.compareSemanticVersions("1.0.0", "1.0.0"))
        assertEquals(0, AppUpdateManager.compareSemanticVersions("v1.0.0", "1.0.0"))
        assertEquals(0, AppUpdateManager.compareSemanticVersions("1.2.3", "v1.2.3"))
    }

    @Test
    fun testSemanticVersionComparison_olderVersions() {
        assertTrue(AppUpdateManager.compareSemanticVersions("0.9.9", "1.0.0") < 0)
        assertTrue(AppUpdateManager.compareSemanticVersions("1.0.0", "1.0.1") < 0)
        assertTrue(AppUpdateManager.compareSemanticVersions("v1.1.5", "1.2.0") < 0)
    }

    @Test
    fun testIsUpdateAvailable_versionCodePriority() {
        // When remote version code is higher, update must be available
        assertTrue(
            AppUpdateManager.isUpdateAvailable(
                remoteVersion = "1.0.0",
                remoteCode = 2,
                currentVersion = "1.0.0",
                currentCode = 1
            )
        )

        // When remote version code is lower or equal, check semantic string
        assertFalse(
            AppUpdateManager.isUpdateAvailable(
                remoteVersion = "1.0.0",
                remoteCode = 1,
                currentVersion = "1.0.0",
                currentCode = 1
            )
        )

        // Remote has higher semantic version even if code is same or unset
        assertTrue(
            AppUpdateManager.isUpdateAvailable(
                remoteVersion = "1.0.5",
                remoteCode = 0,
                currentVersion = "1.0.0",
                currentCode = 1
            )
        )
    }

    @Test
    fun testDefaultUrlsConfigured() {
        assertEquals(
            "https://github.com/gahlotanirudh07/veg-mobile-android/releases/latest/download/MandiExpress.apk",
            AppUpdateManager.DEFAULT_APK_DOWNLOAD_URL
        )
        assertEquals(
            "https://api.github.com/repos/gahlotanirudh07/veg-mobile-android/releases/latest",
            AppUpdateManager.GITHUB_LATEST_RELEASE_API
        )
    }
}
