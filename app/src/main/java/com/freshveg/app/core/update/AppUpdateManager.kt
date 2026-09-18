package com.freshveg.app.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.freshveg.app.BuildConfig
import com.freshveg.app.core.network.AppVersionDto
import com.freshveg.app.core.network.VegApiService
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val latestVersionName: String = "",
    val latestVersionCode: Int = 0,
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val isForceUpdate: Boolean = false,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE
)

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    object Checking : UpdateDownloadState()
    data class UpdateAvailable(val info: UpdateInfo) : UpdateDownloadState()
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateDownloadState()
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
    object UpToDate : UpdateDownloadState()
}

data class GitHubReleaseAsset(
    @SerializedName("name") val name: String = "",
    @SerializedName("browser_download_url") val browserDownloadUrl: String = "",
    @SerializedName("size") val size: Long = 0L
)

data class GitHubReleaseResponse(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("assets") val assets: List<GitHubReleaseAsset>? = null
)

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val apiService: VegApiService,
    private val gson: Gson
) {
    private val _updateState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val updateState: StateFlow<UpdateDownloadState> = _updateState.asStateFlow()

    companion object {
        const val DEFAULT_APK_DOWNLOAD_URL =
            "https://github.com/gahlotanirudh07/veg-mobile-android/releases/latest/download/MandiExpress.apk"
        const val GITHUB_LATEST_RELEASE_API =
            "https://api.github.com/repos/gahlotanirudh07/veg-mobile-android/releases/latest"

        /**
         * Compares semantic version strings like "1.0.1" vs "1.0.0" or "v1.2.0" vs "1.1.9".
         * Returns >0 if remote is newer, <0 if older, 0 if equal.
         */
        fun compareSemanticVersions(remote: String, current: String): Int {
            val cleanRemote = remote.trim().removePrefix("v").removePrefix("V")
            val cleanCurrent = current.trim().removePrefix("v").removePrefix("V")

            val remoteParts = cleanRemote.split(".").mapNotNull { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull()
            }
            val currentParts = cleanCurrent.split(".").mapNotNull { part ->
                part.takeWhile { it.isDigit() }.toIntOrNull()
            }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val rPart = remoteParts.getOrElse(i) { 0 }
                val cPart = currentParts.getOrElse(i) { 0 }
                if (rPart != cPart) {
                    return rPart.compareTo(cPart)
                }
            }
            return 0
        }

        fun isUpdateAvailable(
            remoteVersion: String,
            remoteCode: Int,
            currentVersion: String = BuildConfig.VERSION_NAME,
            currentCode: Int = BuildConfig.VERSION_CODE
        ): Boolean {
            if (remoteCode > currentCode && remoteCode > 0) return true
            return compareSemanticVersions(remoteVersion, currentVersion) > 0
        }
    }

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        _updateState.value = UpdateDownloadState.Checking

        var latestVersion = BuildConfig.VERSION_NAME
        var latestCode = BuildConfig.VERSION_CODE
        var downloadUrl = DEFAULT_APK_DOWNLOAD_URL
        var releaseNotes = ""
        var isForce = false

        // 1. Try Backend /app/version endpoint first
        try {
            val backendRes = apiService.getAppVersion()
            if (backendRes.isSuccessful && backendRes.body() != null) {
                val data: AppVersionDto = backendRes.body()!!
                latestVersion = data.latestVersion
                latestCode = data.versionCode ?: latestCode
                downloadUrl = data.downloadUrl ?: DEFAULT_APK_DOWNLOAD_URL
                releaseNotes = data.releaseNotes ?: ""
                isForce = data.forceUpdate
            }
        } catch (_: Exception) {
            // Backend endpoint fallback to GitHub direct API
        }

        // 2. If backend gave current version or failed, check GitHub Releases API
        if (latestVersion == BuildConfig.VERSION_NAME) {
            try {
                val request = Request.Builder()
                    .url(GITHUB_LATEST_RELEASE_API)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string()
                        if (!bodyStr.isNullOrBlank()) {
                            val ghRelease = gson.fromJson(bodyStr, GitHubReleaseResponse::class.java)
                            val tag = ghRelease.tagName.trim().removePrefix("v").removePrefix("V")
                            if (tag.isNotBlank()) {
                                latestVersion = tag
                            }
                            releaseNotes = ghRelease.body ?: "Performance improvements and regular updates."
                            val apkAsset = ghRelease.assets?.firstOrNull { it.name.endsWith(".apk") }
                            if (apkAsset != null) {
                                downloadUrl = apkAsset.browserDownloadUrl
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore network errors during GitHub release check
            }
        }

        val hasUpdate = isUpdateAvailable(
            remoteVersion = latestVersion,
            remoteCode = latestCode,
            currentVersion = BuildConfig.VERSION_NAME,
            currentCode = BuildConfig.VERSION_CODE
        )

        val info = UpdateInfo(
            isUpdateAvailable = hasUpdate,
            latestVersionName = latestVersion,
            latestVersionCode = latestCode,
            downloadUrl = downloadUrl,
            releaseNotes = releaseNotes.ifBlank { "Exciting new features and optimizations for MandiExpress." },
            isForceUpdate = isForce,
            currentVersionName = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE
        )

        if (hasUpdate) {
            _updateState.value = UpdateDownloadState.UpdateAvailable(info)
        } else {
            _updateState.value = UpdateDownloadState.UpToDate
        }

        info
    }

    suspend fun downloadAndInstall(
        downloadUrl: String = DEFAULT_APK_DOWNLOAD_URL,
        onComplete: (File) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updateDir, "MandiExpress-vLatest.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = "Download failed with HTTP ${response.code}"
                    _updateState.value = UpdateDownloadState.Error(err)
                    return@withContext Result.failure(Exception(err))
                }

                val body = response.body ?: run {
                    val err = "Empty response body"
                    _updateState.value = UpdateDownloadState.Error(err)
                    return@withContext Result.failure(Exception(err))
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytes = input.read(buffer)
                        var lastReportTime = 0L

                        while (bytes >= 0) {
                            output.write(buffer, 0, bytes)
                            downloadedBytes += bytes

                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 100 || downloadedBytes == totalBytes) {
                                lastReportTime = now
                                val percent = if (totalBytes > 0) {
                                    ((downloadedBytes * 100) / totalBytes).toInt()
                                } else {
                                    -1
                                }
                                _updateState.value = UpdateDownloadState.Downloading(
                                    progressPercent = percent,
                                    bytesDownloaded = downloadedBytes,
                                    totalBytes = totalBytes
                                )
                            }
                            bytes = input.read(buffer)
                        }
                        output.flush()
                    }
                }
            }

            _updateState.value = UpdateDownloadState.ReadyToInstall(apkFile)
            onComplete(apkFile)
            installApk(apkFile)
            Result.success(apkFile)
        } catch (e: Exception) {
            val errMsg = e.message ?: "Failed to download update"
            _updateState.value = UpdateDownloadState.Error(errMsg)
            Result.failure(e)
        }
    }

    fun installApk(apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.value = UpdateDownloadState.Error("Failed to launch package installer: ${e.message}")
        }
    }

    fun resetState() {
        _updateState.value = UpdateDownloadState.Idle
    }
}

