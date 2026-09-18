package com.freshveg.app.features.buyer.account

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
import androidx.compose.material.icons.outlined.*
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
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.network.ConnectedSellerDto
import com.freshveg.app.core.network.VegApiService
import com.freshveg.app.core.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerAccountScreen(
    sessionManager: SessionManager,
    apiService: VegApiService,
    onNavigateToOrders: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var restaurantName by remember { mutableStateOf("Restaurant Buyer") }
    var mobile by remember { mutableStateOf("") }
    var connectedSeller by remember { mutableStateOf<ConnectedSellerDto?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val bizName = sessionManager.businessName.firstOrNull()
        val uName = sessionManager.userName.firstOrNull()
        val mob = sessionManager.savedMobile.firstOrNull() ?: ""
        
        if (!bizName.isNullOrBlank()) {
            restaurantName = bizName
        } else if (!uName.isNullOrBlank()) {
            restaurantName = uName
        }
        mobile = mob

        try {
            val sellerRes = apiService.getConnectedSeller()
            if (sellerRes.isSuccessful) {
                connectedSeller = sellerRes.body()?.data
            }
        } catch (e: Exception) {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Restaurant Account",
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
            // 1. Restaurant Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
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
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(restaurantName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("📱 $mobile", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE8F5E9))
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "VERIFIED RESTAURANT BUYER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Connected Wholesale Supplier Card
            connectedSeller?.let { seller ->
                item {
                    Text("Connected Wholesale Supplier", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(seller.businessName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("Seller Code: ${seller.sellerCode ?: "N/A"}", style = MaterialTheme.typography.labelSmall, color = ForestGreenPrimary)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${seller.mobile}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = ForestGreenPrimary)
                                    }

                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91${seller.mobile}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "WhatsApp", tint = ForestGreenPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Quick Navigation Hub
            item {
                Text("Orders & Billing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        BuyerAccountActionRow(
                            icon = Icons.Default.LocalShipping,
                            title = "My Orders & Deliveries",
                            subtitle = "Check active orders, status & delivered weighments",
                            iconTint = ForestGreenPrimary,
                            onClick = onNavigateToOrders
                        )
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        BuyerAccountActionRow(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = "My Tax Bills & Invoices",
                            subtitle = "View GST invoices, download slips & check dues",
                            iconTint = Color(0xFF1976D2),
                            onClick = onNavigateToInvoices
                        )
                    }
                }
            }

            // 3. Section: Language & Preferences
            item {
                val langManager = com.freshveg.app.core.i18n.LanguageManager.instance
                val currentLang = langManager?.currentLanguage?.collectAsState()?.value ?: com.freshveg.app.core.i18n.AppLanguage.ENGLISH

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BuyerAccountActionRow(
                        icon = Icons.Default.Language,
                        title = if (currentLang == com.freshveg.app.core.i18n.AppLanguage.HINDI) "भाषा: हिन्दी (🇮🇳)" else "Language: English (🇬🇧)",
                        subtitle = if (currentLang == com.freshveg.app.core.i18n.AppLanguage.HINDI) "Switch to English" else "हिन्दी में बदलें",
                        iconTint = Color(0xFF1E88E5),
                        onClick = {
                            langManager?.toggleLanguage()
                        }
                    )
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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out?") },
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
fun BuyerAccountActionRow(
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
