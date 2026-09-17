package com.freshveg.app.features.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.network.*
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlCenterScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Super Admin Control Center",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Platform Governance & Master Catalog",
                            style = MaterialTheme.typography.labelMedium,
                            color = FarmGreenSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ForestGreenPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadAdminData) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ForestGreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs Row
            Surface(
                color = CardSurface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = CardSurface,
                    contentColor = ForestGreenPrimary
                ) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        text = { Text("Network", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        text = { Text("Sellers (${uiState.sellers.size})", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        text = { Text("Master Review (${uiState.customProducts.size})", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = uiState.selectedTab == 3,
                        onClick = { viewModel.selectTab(3) },
                        text = { Text("Users (${uiState.users.size})", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ForestGreenPrimary)
                } else {
                    when (uiState.selectedTab) {
                        0 -> AdminNetworkAnalyticsTab(analytics = uiState.analytics)
                        1 -> AdminSellersTab(sellers = uiState.sellers)
                        2 -> AdminMasterReviewTab(
                            customProducts = uiState.customProducts,
                            isPromoting = uiState.isPromoting,
                            onPromote = viewModel::promoteCustomProduct
                        )
                        3 -> AdminUsersTab(users = uiState.users)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Tab 0: Network Analytics
// ----------------------------------------------------
@Composable
fun AdminNetworkAnalyticsTab(analytics: PlatformAnalyticsResponse?) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("PLATFORM GROSS MERCHANDISE VALUE (GMV)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE8F5E9), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${analytics?.totalGmv?.toInt() ?: 0}.00", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Active Sellers", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${analytics?.activeSellers ?: 0}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = ForestGreenPrimary)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Total Buyers", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${analytics?.totalBuyers ?: 0}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = Color(0xFF1976D2))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Total Orders", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${analytics?.totalOrders ?: 0}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Volume Dispatched", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${analytics?.totalProduceVolumeKg?.toInt() ?: 0} KG", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = Color(0xFFD32F2F))
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Tab 1: Sellers Directory
// ----------------------------------------------------
@Composable
fun AdminSellersTab(sellers: List<AdminSellerDto>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (sellers.isEmpty()) {
            item {
                Text("No sellers registered in platform", color = Color.Gray)
            }
        } else {
            items(sellers, key = { it.id }) { seller ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(seller.businessName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Code: ${seller.sellerCode} • 📱 ${seller.mobile}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                            Text("${seller.buyerCount} connected buyers • GMV: ₹${seller.totalGmv.toInt()}", style = MaterialTheme.typography.labelSmall, color = ForestGreenPrimary)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (seller.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (seller.isActive) "ACTIVE" else "DISABLED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (seller.isActive) ForestGreenPrimary else Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Tab 2: Custom Produce Review & 1-Tap Master Promotion
// ----------------------------------------------------
@Composable
fun AdminMasterReviewTab(
    customProducts: List<CustomProductDto>,
    isPromoting: Boolean,
    onPromote: (String, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Unverified Seller Produce Awaiting Review",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Promoting custom produce makes it globally available in the Master Catalog for all vendors across the platform.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        if (customProducts.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All produce items are verified in Master Catalog!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(customProducts, key = { it.id }) { product ->
                val emoji = ProduceVisualUtils.getProduceEmoji(product.name, null)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(emoji, fontSize = 28.sp)
                            Column {
                                Text(product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Seller: ${product.sellerBusinessName ?: "Custom"} • ${product.unitType}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text("Suggested Price: ₹${product.sellingPrice.toInt()}", style = MaterialTheme.typography.labelSmall, color = ForestGreenPrimary)
                            }
                        }

                        Button(
                            onClick = { onPromote(product.id, product.name) },
                            enabled = !isPromoting,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Promote to Master", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Tab 3: Users Governance
// ----------------------------------------------------
@Composable
fun AdminUsersTab(users: List<AdminUserDto>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (users.isEmpty()) {
            item {
                Text("No users found", color = Color.Gray)
            }
        } else {
            items(users, key = { it.id }) { user ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("📱 ${user.mobile} • Role: ${user.role}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                            if (!user.customerName.isNullOrBlank()) {
                                Text("Org: ${user.customerName}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (user.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (user.isActive) "ACTIVE" else "BLOCKED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (user.isActive) ForestGreenPrimary else Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }
        }
    }
}
