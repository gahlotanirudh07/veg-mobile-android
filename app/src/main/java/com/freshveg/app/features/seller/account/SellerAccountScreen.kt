package com.freshveg.app.features.seller.account

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.BuildConfig
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.ui.components.UpdatePromptDialog
import com.freshveg.app.core.update.AppUpdateManager
import com.freshveg.app.core.update.UpdateDownloadState
import com.freshveg.app.core.update.UpdateInfo
import com.freshveg.app.core.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerAccountScreen(
    sessionManager: SessionManager,
    updateManager: AppUpdateManager? = null,
    onNavigateToStore: () -> Unit = {},
    onNavigateToCustomers: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var businessName by remember { mutableStateOf("Mandi Wholesale Trader") }
    var sellerCode by remember { mutableStateOf("SEL-000") }
    var mobile by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("SELLER") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val updateState by (updateManager?.updateState?.collectAsState() ?: remember { mutableStateOf(UpdateDownloadState.Idle) })
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var activeUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        businessName = sessionManager.businessName.firstOrNull() ?: sessionManager.dataStoreBusinessName() ?: "Mandi Wholesale Trader"
        sellerCode = sessionManager.sellerCode.firstOrNull() ?: "SEL-000"
        mobile = sessionManager.savedMobile.firstOrNull() ?: ""
        role = sessionManager.role.firstOrNull() ?: "SELLER"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Account & Management",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Business Profile Header Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MainInk),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(businessName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("📱 $mobile", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE8F5E9))
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "SELLER CODE: $sellerCode",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Section: Store & Catalogue Preferences
            item {
                Text("Store & Catalogue Preferences", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MainInk)
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AccountActionRow(
                            icon = Icons.Default.Storefront,
                            title = "My Store & Produce Inventory (सब्जी स्टोर)",
                            subtitle = "Add vegetables, edit prices, manage stock & custom items",
                            iconTint = ActionGreen,
                            onClick = onNavigateToStore
                        )
                    }
                }
            }

            // 3. Section: Operations & Hub
            item {
                Text("Operations & Management", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MainInk)
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AccountActionRow(
                            icon = Icons.Default.People,
                            title = "Customer Directory",
                            subtitle = "Restaurant buyer profiles, credit limits & invites",
                            iconTint = ActionGreen,
                            onClick = onNavigateToCustomers
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        AccountActionRow(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = "GST Tax Invoices & Billing",
                            subtitle = "Generated tax invoices, payment slips & PDF bills",
                            iconTint = Color(0xFF1976D2),
                            onClick = onNavigateToInvoices
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        AccountActionRow(
                            icon = Icons.Default.BarChart,
                            title = "Executive Analytics & Reports",
                            subtitle = "Sales trends, top produce volume & collections",
                            iconTint = Color(0xFFF57F17),
                            onClick = onNavigateToAnalytics
                        )

                        if (role.uppercase() == "SUPER_ADMIN" || role.uppercase() == "ADMIN") {
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                            AccountActionRow(
                                icon = Icons.Default.AdminPanelSettings,
                                title = "Super Admin Control Center",
                                subtitle = "Platform GMV, master catalog review & users",
                                iconTint = Color(0xFF7B1FA2),
                                onClick = onNavigateToAdmin
                            )
                        }
                    }
                }
            }

            // 3. Section: Support & Preferences
            item {
                Text("Preferences & Support", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            item {
                val langManager = com.freshveg.app.core.i18n.LanguageManager.instance
                val currentLang = langManager?.currentLanguage?.collectAsState()?.value ?: com.freshveg.app.core.i18n.AppLanguage.ENGLISH

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AccountActionRow(
                            icon = Icons.Default.Language,
                            title = if (currentLang == com.freshveg.app.core.i18n.AppLanguage.HINDI) "भाषा: हिन्दी (🇮🇳)" else "Language: English (🇬🇧)",
                            subtitle = if (currentLang == com.freshveg.app.core.i18n.AppLanguage.HINDI) "Switch to English" else "हिन्दी में बदलें",
                            iconTint = Color(0xFF1E88E5),
                            onClick = {
                                langManager?.toggleLanguage()
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        AccountActionRow(
                            icon = Icons.AutoMirrored.Filled.HelpOutline,
                            title = "WhatsApp Support Desk",
                            subtitle = "Connect directly with MandiExpress technical support",
                            iconTint = ActionGreen,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919876543210?text=Hello%20MandiExpress%20Support"))
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        AccountActionRow(
                            icon = Icons.Default.SystemUpdate,
                            title = "Check for Updates (ऐप अपडेट)",
                            subtitle = if (isCheckingUpdate) "Checking latest version..." else "Version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                            iconTint = ActionGreen,
                            onClick = {
                                if (!isCheckingUpdate && updateManager != null) {
                                    coroutineScope.launch {
                                        isCheckingUpdate = true
                                        val info = updateManager.checkForUpdates()
                                        isCheckingUpdate = false
                                        if (info.isUpdateAvailable) {
                                            activeUpdateInfo = info
                                            showUpdateDialog = true
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "MandiExpress is up to date (v${BuildConfig.VERSION_NAME})",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 4. Logout Button
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out from Account", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showUpdateDialog && activeUpdateInfo != null && updateManager != null) {
        UpdatePromptDialog(
            updateInfo = activeUpdateInfo!!,
            downloadState = updateState,
            onStartDownload = {
                coroutineScope.launch {
                    updateManager.downloadAndInstall(activeUpdateInfo!!.downloadUrl)
                }
            },
            onInstall = {
                if (updateState is UpdateDownloadState.ReadyToInstall) {
                    updateManager.installApk((updateState as UpdateDownloadState.ReadyToInstall).apkFile)
                }
            },
            onDismiss = {
                showUpdateDialog = false
                updateManager.resetState()
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out from your wholesale account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        coroutineScope.launch {
                            sessionManager.clearSession()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Log Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AccountActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }

        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
    }
}

private suspend fun SessionManager.dataStoreBusinessName(): String? {
    return savedMobile.firstOrNull() // fallback accessor
}
