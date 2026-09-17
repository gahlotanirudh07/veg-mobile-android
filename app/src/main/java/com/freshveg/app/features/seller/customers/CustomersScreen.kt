package com.freshveg.app.features.seller.customers

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.network.CustomerDto
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    onNavigateBack: () -> Unit,
    viewModel: CustomersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
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
                            "Customer Directory",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "${uiState.customers.size} Connected Restaurants & Buyers",
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
                    IconButton(onClick = viewModel::loadData) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ForestGreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openAddCustomer,
                containerColor = ForestGreenPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Restaurant", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Seller Code Onboarding Invite Banner
            if (!uiState.sellerCode.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Seller Code for New Buyers:", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                            Text(
                                text = uiState.sellerCode!!,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = ForestGreenPrimary,
                                letterSpacing = 1.sp
                            )
                        }

                        Button(
                            onClick = viewModel::openShareCode,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invite Buyer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Search Bar
            Surface(
                color = CardSurface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Search restaurant, contact person, phone...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreenPrimary,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = BackgroundSurface,
                            unfocusedContainerColor = BackgroundSurface
                        )
                    )
                }
            }

            // 3. Customers List
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading && uiState.customers.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ForestGreenPrimary)
                } else if (uiState.filteredCustomers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "No restaurants match '${uiState.searchQuery}'" else "No connected restaurants found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap 'Invite Buyer' to share your Seller Code or tap '+ Add Restaurant' to onboard manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredCustomers, key = { it.id }) { customer ->
                            val dues = uiState.outstandingMap[customer.id] ?: 0.0
                            CustomerDirectoryCard(
                                customer = customer,
                                outstandingDues = dues,
                                onClick = { viewModel.openViewCustomer(customer) },
                                onCall = {
                                    try {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile}"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {}
                                },
                                onWhatsApp = {
                                    try {
                                        val clean = customer.mobile.replace("+91", "").replace(" ", "")
                                        val url = "https://wa.me/91$clean"
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (e: Exception) {}
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal 1: Add or Edit Customer
    if (uiState.isAddCustomerOpen || uiState.selectedCustomerForEdit != null) {
        CustomerFormBottomSheet(
            customer = uiState.selectedCustomerForEdit,
            onDismiss = {
                viewModel.closeAddCustomer()
                viewModel.closeEditCustomer()
            },
            onSaveAdd = viewModel::createCustomer,
            onSaveEdit = viewModel::updateCustomer
        )
    }

    // Modal 2: View Customer Profile
    uiState.selectedCustomerForView?.let { customer ->
        val dues = uiState.outstandingMap[customer.id] ?: 0.0
        CustomerProfileBottomSheet(
            customer = customer,
            outstandingDues = dues,
            onDismiss = viewModel::closeViewCustomer,
            onEdit = { viewModel.openEditCustomer(customer) },
            onDelete = { viewModel.deleteCustomer(customer.id) }
        )
    }

    // Modal 3: Share Seller Code
    if (uiState.isShareCodeOpen) {
        ShareSellerCodeBottomSheet(
            seller = uiState.connectedSeller,
            fallbackSellerCode = uiState.sellerCode,
            onDismiss = viewModel::closeShareCode
        )
    }
}

@Composable
fun CustomerDirectoryCard(
    customer: CustomerDto,
    outstandingDues: Double,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Business Name & Status/Dues Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.businessName,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                    Text(
                        text = "👤 ${customer.primaryContactName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }

                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (outstandingDues > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔴 Dues: ₹${outstandingDues.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🟢 Active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        }
                    }

                    if (customer.discountType == "PERCENTAGE" && (customer.discountValue ?: 0.0) > 0.0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🏷️ ${customer.discountValue?.toInt()}% OFF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreenPrimary
                            )
                        }
                    } else if (customer.discountType == "RUPEES_PER_UNIT" && (customer.discountValue ?: 0.0) > 0.0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE0F2FE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🏷️ ₹${customer.discountValue?.toInt()}/KG OFF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address & Phone Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📍 ${customer.address}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Text(
                        text = "📱 +91 ${customer.mobile}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Quick Call & WhatsApp Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onWhatsApp, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
