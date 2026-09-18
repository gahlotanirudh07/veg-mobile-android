package com.freshveg.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

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
            FreshVegTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundSurface
                ) {
                    AppNavigation(
                        sessionManager = sessionManager,
                        apiService = apiService
                    )
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
    }

    override fun onDestroy() {
        super.onDestroy()
        mandiSocketManager.disconnect()
    }
}
