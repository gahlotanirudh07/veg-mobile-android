package com.freshveg.app.features.buyer.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.BuildConfig
import com.freshveg.app.R
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.i18n.AppLanguage
import com.freshveg.app.core.i18n.LanguageManager
import com.freshveg.app.core.network.ConnectedSellerDto
import com.freshveg.app.core.network.VegApiService
import com.freshveg.app.core.ui.components.UpdatePromptDialog
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.update.AppUpdateManager
import com.freshveg.app.core.update.UpdateDownloadState
import com.freshveg.app.core.update.UpdateInfo
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerAccountScreen(
    sessionManager: SessionManager,
    apiService: VegApiService,
    updateManager: AppUpdateManager? = null,
    onNavigateToOrders: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var connectedSeller by remember { mutableStateOf<ConnectedSellerDto?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val currentLang by (LanguageManager.instance?.currentLanguage ?: remember { mutableStateOf(AppLanguage.ENGLISH) }).let {
        if (it is StateFlow<*>) (it as StateFlow<AppLanguage>).collectAsState() else remember { mutableStateOf(AppLanguage.ENGLISH) }
    }

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val downloadState by (updateManager?.updateState ?: remember { mutableStateOf(UpdateDownloadState.Idle) }).let {
        if (it is StateFlow<*>) (it as StateFlow<UpdateDownloadState>).collectAsState() else remember { mutableStateOf(UpdateDownloadState.Idle) }
    }

    LaunchedEffect(Unit) {
        buyerName = sessionManager.userName.firstOrNull() ?: "Restaurant Buyer"
        buyerPhone = sessionManager.userMobile.firstOrNull() ?: ""
        shopName = sessionManager.businessName.firstOrNull() ?: "Fresh Veg Direct"
        try {
            val sellerRes = apiService.getConnectedSeller()
            if (sellerRes.isSuccessful) {
                connectedSeller = sellerRes.body()?.data
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.account_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MainInk
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondarySurface)
            )
        },
        containerColor = SecondarySurface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Profile Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(0.75.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ActionGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏪", fontSize = 28.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shopName.ifBlank { buyerName },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MainInk
                            )
                            Text(
                                text = if (buyerPhone.isNotBlank()) "+91 $buyerPhone" else stringResource(R.string.account_role_buyer),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ActionGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = stringResource(R.string.account_role_buyer),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ActionGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Language Switcher Tile
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(0.75.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { LanguageManager.instance?.toggleLanguage() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.account_change_language),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MainInk
                                )
                                Text(
                                    text = if (currentLang == AppLanguage.HINDI) "वर्तमान भाषा: हिन्दी" else "Current: English",
                                    fontSize = 12.sp,
                                    color = InkSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ActionGreen.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = if (currentLang == AppLanguage.HINDI) "🇮🇳 हिन्दी" else "🇬🇧 EN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = ActionGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Operations Quick Links
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(0.75.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AccountActionTile(
                            icon = Icons.Filled.LocalShipping,
                            title = stringResource(R.string.orders_title),
                            subtitle = "Track orders & physical scale weighments",
                            onClick = onNavigateToOrders
                        )
                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(horizontal = 14.dp))
                        AccountActionTile(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = stringResource(R.string.invoices_title),
                            subtitle = "GST invoices & account khata ledger",
                            onClick = onNavigateToInvoices
                        )
                    }
                }
            }

            // Support & Helplines
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(0.75.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AccountActionTile(
                            icon = Icons.Default.HeadsetMic,
                            title = stringResource(R.string.account_support),
                            subtitle = "Call or message MandiExpress helpline",
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:18001234567"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            // App Version & OTA Updates
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(0.75.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.account_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MainInk
                                )
                                Text(
                                    text = "Automated OTA Updater Enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkSecondary
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    updateManager?.let { mgr ->
                                        coroutineScope.launch {
                                            isCheckingUpdates = true
                                            val update = mgr.checkForUpdates()
                                            isCheckingUpdates = false
                                            if (update.isUpdateAvailable) {
                                                availableUpdate = update
                                                showUpdateDialog = true
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.update_already_latest, BuildConfig.VERSION_NAME),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isCheckingUpdates,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                if (isCheckingUpdates) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                                } else {
                                    Text("Check Update", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Logout Tile
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(0.75.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MutedRedError.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MutedRedError, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = stringResource(R.string.auth_logout),
                            fontWeight = FontWeight.Bold,
                            color = MutedRedError,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.auth_logout)) },
            text = { Text(stringResource(R.string.auth_logout_confirm)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedRedError)
                ) {
                    Text(stringResource(R.string.auth_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showUpdateDialog && availableUpdate != null) {
        UpdatePromptDialog(
            updateInfo = availableUpdate!!,
            downloadState = downloadState,
            onStartDownload = {
                updateManager?.let { mgr ->
                    coroutineScope.launch {
                        mgr.downloadAndInstall(availableUpdate!!.downloadUrl)
                    }
                }
            },
            onInstall = {
                if (downloadState is UpdateDownloadState.ReadyToInstall) {
                    updateManager?.installApk((downloadState as UpdateDownloadState.ReadyToInstall).apkFile)
                } else if (downloadState is UpdateDownloadState.Installing) {
                    updateManager?.installApk((downloadState as UpdateDownloadState.Installing).apkFile)
                }
            },
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
fun AccountActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SecondarySurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MainInk, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MainInk)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = InkSecondary, modifier = Modifier.size(20.dp))
    }
}
