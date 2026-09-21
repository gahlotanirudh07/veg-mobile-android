package com.freshveg.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.i18n.LanguageManager
import com.freshveg.app.core.network.MandiSocketManager
import com.freshveg.app.core.network.VegApiService
import com.freshveg.app.core.ui.theme.BackgroundSurface
import com.freshveg.app.core.ui.theme.FreshVegTheme
import com.freshveg.app.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var apiService: VegApiService

    @Inject
    lateinit var mandiSocketManager: MandiSocketManager

    @Inject
    lateinit var languageManager: LanguageManager

    @Inject
    lateinit var updateManager: com.freshveg.app.core.update.AppUpdateManager

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun attachBaseContext(newBase: android.content.Context) {
        val prefs = newBase.getSharedPreferences("language_prefs", android.content.Context.MODE_PRIVATE)
        val langCode = prefs.getString("app_language_code", "en") ?: "en"
        val locale = java.util.Locale(langCode)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        }
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Maintain WebSocket connection based on auth token
        lifecycleScope.launch {
            sessionManager.accessToken.collectLatest { token ->
                if (!token.isNullOrBlank()) {
                    mandiSocketManager.connect(token)
                } else {
                    mandiSocketManager.disconnect()
                }
            }
        }

        setContent {
            val currentLang by languageManager.currentLanguage.collectAsState()
            val context = LocalContext.current
            val locale = remember(currentLang) { Locale(currentLang.code) }
            val configuration = remember(currentLang, locale) {
                val conf = Configuration(context.resources.configuration)
                conf.setLocale(locale)
                conf.setLayoutDirection(locale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val localeList = android.os.LocaleList(locale)
                    conf.setLocales(localeList)
                }
                conf
            }
            val localizedContext = remember(currentLang, locale, configuration) {
                val configContext = context.createConfigurationContext(configuration)
                object : android.content.ContextWrapper(context) {
                    override fun getResources(): android.content.res.Resources = configContext.resources
                    override fun getAssets(): android.content.res.AssetManager = configContext.assets
                }
            }

            androidx.compose.runtime.LaunchedEffect(configuration) {
                @Suppress("DEPRECATION")
                resources.updateConfiguration(configuration, resources.displayMetrics)
            }

            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalContext provides localizedContext
            ) {
                FreshVegTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = BackgroundSurface
                    ) {
                        AppNavigation(
                            sessionManager = sessionManager,
                            apiService = apiService,
                            updateManager = updateManager
                        )
                    }
                }
            }
        }
    }

    private var lastWarmupTime: Long = 0L

    private fun triggerDatabaseWarmup() {
        val now = System.currentTimeMillis()
        // Pre-warm if first launch OR if app was in Recent Apps for > 4 minutes
        if (now - lastWarmupTime > 4 * 60 * 1000L) {
            lastWarmupTime = now
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    apiService.warmUpDatabase()
                } catch (_: Exception) {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        triggerDatabaseWarmup()
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                updateManager.checkForUpdates()
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mandiSocketManager.disconnect()
    }
}
