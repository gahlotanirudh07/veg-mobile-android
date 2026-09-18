package com.freshveg.app.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
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
    @SerializedName("size") val size: Long = 0L,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class GitHubReleaseResponse(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
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

    var lastCheckedInfo: UpdateInfo? = null
        private set

    companion object {
        const val DEFAULT_APK_DOWNLOAD_URL =
            "https://github.com/gahlotanirudh07/veg-mobile-android/releases/latest/download/MandiExpress.apk"
        const val GITHUB_LATEST_RELEASE_API =
            "https://api.github.com/repos/gahlotanirudh07/veg-mobile-android/releases/latest"

        /**
         * Extracts semantic version numbers like "1.1.2" or "1.0" from text strings.
         */
        fun extractSemanticVersion(text: String?): String? {
            if (text.isNullOrBlank()) return null
            val match = Regex("""(?i)v?(\d+(?:\.\d+)+)""").find(text)
            return match?.groupValues?.getOrNull(1)
        }

        /**
         * Compares semantic version strings like "1.1.0" vs "1.0.0" or "v1.2.0" vs "1.1.9".
         * Returns >0 if remote is newer, <0 if older, 0 if equal.
         */
        fun compareSemanticVersions(remote: String, current: String): Int {
            val cleanRemote = extractSemanticVersion(remote) ?: remote.trim().removePrefix("v").removePrefix("V")
            val cleanCurrent = extractSemanticVersion(current) ?: current.trim().removePrefix("v").removePrefix("V")

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
            remoteCode: Int = 0,
            currentVersion: String = BuildConfig.VERSION_NAME,
            currentCode: Int = BuildConfig.VERSION_CODE,
            remoteSha: String = "",
            currentSha: String = "",
            remoteAssetTimeMillis: Long = 0L,
            currentBuildTimeMillis: Long = 0L
        ): Boolean {
            if (remoteCode > currentCode && remoteCode > 0) return true
            if (compareSemanticVersions(remoteVersion, currentVersion) > 0) return true
            if (remoteSha.isNotBlank() && currentSha.isNotBlank() && currentSha != "local" &&
                !remoteSha.startsWith(currentSha) && !currentSha.startsWith(remoteSha)) return true
            if (remoteAssetTimeMillis > 0 && currentBuildTimeMillis > 0 &&
                remoteAssetTimeMillis > currentBuildTimeMillis + 60_000L) return true
            return false
        }

        fun parseIsoDateToMillis(isoString: String?): Long {
            if (isoString.isNullOrBlank()) return 0L
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val clean = isoString.substringBefore("Z").substringBefore("+").substringBefore(".")
                format.parse(clean)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        _updateState.value = UpdateDownloadState.Checking

        var latestVersion = BuildConfig.VERSION_NAME
        var latestCode = BuildConfig.VERSION_CODE
        var downloadUrl = DEFAULT_APK_DOWNLOAD_URL
        var releaseNotes = ""
        var isForce = false
        var remoteSha = ""
        var remoteAssetTimeMillis = 0L

        // 1. Try Backend /app/version endpoint first
        try {
            val backendRes = apiService.getAppVersion()
            if (backendRes.isSuccessful && backendRes.body() != null) {
                val data: AppVersionDto = backendRes.body()!!
                if (!data.latestVersion.isNullOrBlank()) {
                    latestVersion = data.latestVersion
                }
                if (data.versionCode != null && data.versionCode > 0) {
                    latestCode = data.versionCode
                }
                if (!data.downloadUrl.isNullOrBlank()) {
                    downloadUrl = data.downloadUrl
                }
                if (!data.releaseNotes.isNullOrBlank()) {
                    releaseNotes = data.releaseNotes
                }
                isForce = data.forceUpdate
            }
        } catch (_: Exception) {
            // Backend endpoint fallback
        }

        // 2. Fetch directly from GitHub Releases API for real-time release details
        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "MandiExpress-Android")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val ghRelease = gson.fromJson(bodyStr, GitHubReleaseResponse::class.java)

                        // Parse tag / release name
                        val tagVersion = extractSemanticVersion(ghRelease.tagName)
                        val nameVersion = ghRelease.name?.let { extractSemanticVersion(it) }
                        val resolvedVersion = tagVersion ?: nameVersion
                        if (!resolvedVersion.isNullOrBlank()) {
                            latestVersion = resolvedVersion
                        }

                        // Parse commit sha and build code from release body
                        val bodyText = ghRelease.body ?: ""
                        val shaMatch = Regex("""Commit[:\s*`]+([0-9a-fA-F]{7,40})""").find(bodyText)
                        if (shaMatch != null) {
                            remoteSha = shaMatch.groupValues[1]
                        }

                        val buildCodeMatch = Regex("""Build[:\s*`#]+(\d+)""").find(bodyText)
                        if (buildCodeMatch != null) {
                            buildCodeMatch.groupValues[1].toIntOrNull()?.let { bCode ->
                                if (bCode > latestCode) latestCode = bCode
                            }
                        }

                        if (ghRelease.body?.isNotBlank() == true) {
                            releaseNotes = ghRelease.body
                        }

                        val apkAsset = ghRelease.assets?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                        if (apkAsset != null) {
                            downloadUrl = apkAsset.browserDownloadUrl
                            remoteAssetTimeMillis = parseIsoDateToMillis(apkAsset.updatedAt)
                        } else {
                            remoteAssetTimeMillis = parseIsoDateToMillis(ghRelease.publishedAt ?: ghRelease.updatedAt)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore network errors during GitHub release check
        }

        // 3. Multi-factor Update Detection Evaluation
        val currentSha = try { BuildConfig.GIT_SHA } catch (_: Exception) { "local" }
        val currentBuildTime = try { BuildConfig.BUILD_TIME_MILLIS } catch (_: Exception) { 0L }

        val hasUpdate = isUpdateAvailable(
            remoteVersion = latestVersion,
            remoteCode = latestCode,
            currentVersion = BuildConfig.VERSION_NAME,
            currentCode = BuildConfig.VERSION_CODE,
            remoteSha = remoteSha,
            currentSha = currentSha,
            remoteAssetTimeMillis = remoteAssetTimeMillis,
            currentBuildTimeMillis = currentBuildTime
        )

        val info = UpdateInfo(
            isUpdateAvailable = hasUpdate,
            latestVersionName = latestVersion.ifBlank { "1.1.0" },
            latestVersionCode = latestCode,
            downloadUrl = downloadUrl,
            releaseNotes = releaseNotes.ifBlank { "Exciting new wholesale features, decimal quantities, seller fast-fulfill & performance optimizations." },
            isForceUpdate = isForce,
            currentVersionName = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE
        )

        lastCheckedInfo = info

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
            if (!apkFile.exists() || apkFile.length() == 0L) {
                _updateState.value = UpdateDownloadState.Error("Downloaded APK file is empty or missing")
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                }
            }

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
