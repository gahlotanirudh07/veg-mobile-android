package com.freshveg.app.features.seller.invoices

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.network.InvoiceSummaryDto
import com.freshveg.app.core.network.OrderDto
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    onNavigateBack: () -> Unit,
    viewModel: InvoicesViewModel = hiltViewModel()
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
                            "Tax Invoices & Billing",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "GST Bills, Thermal Slips & WhatsApp Dispatch",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainInk)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadData) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ActionGreen)
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
            // 1. Search Bar
            Surface(
                color = CardSurface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Search by invoice #, order # or customer...") },
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
                            focusedBorderColor = ActionGreen,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = BackgroundSurface,
                            unfocusedContainerColor = BackgroundSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Dual Tab Selector (Generated Invoices vs Pending Orders)
                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = CardSurface,
                        contentColor = ActionGreen
                    ) {
                        Tab(
                            selected = uiState.selectedTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            text = {
                                Text(
                                    "🧾 Generated Bills (${uiState.invoices.size})",
                                    fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            text = {
                                Text(
                                    "⏳ Pending Orders (${uiState.pendingOrders.size})",
                                    fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // 3. Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading && uiState.invoices.isEmpty() && uiState.pendingOrders.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ActionGreen)
                } else if (uiState.selectedTab == 0) {
                    // Tab 0: Generated Invoices List
                    if (uiState.filteredInvoices.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "No invoices match '${uiState.searchQuery}'" else "No invoices generated yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.filteredInvoices, key = { it.id }) { invoice ->
                                GeneratedInvoiceCard(
                                    invoice = invoice,
                                    onViewDetails = { viewModel.viewInvoiceDetails(invoice.id) }
                                )
                            }
                        }
                    }
                } else {
                    // Tab 1: Pending Orders Ready for Invoicing
                    if (uiState.filteredPendingOrders.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "All delivered orders have been invoiced! 🎉",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.filteredPendingOrders, key = { it.id }) { order ->
                                PendingOrderInvoiceCard(
                                    order = order,
                                    isGenerating = uiState.isGenerating,
                                    onGenerateInvoice = { viewModel.generateInvoice(order.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Invoice Detail Bottom Sheet
    uiState.selectedInvoiceDetail?.let { detail ->
        InvoiceDetailsBottomSheet(
            invoice = detail,
            onDismiss = viewModel::closeInvoiceDetails,
            onShareWhatsApp = { viewModel.shareInvoiceOnWhatsApp(context, detail) }
        )
    }
}

@Composable
fun GeneratedInvoiceCard(
    invoice: InvoiceSummaryDto,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Invoice Number & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                    Text(
                        text = invoice.invoiceNumber,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1976D2)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = invoice.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ActionGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Customer Name & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = invoice.customer?.businessName ?: "Customer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    val addr = invoice.customer?.address
                    if (!addr.isNullOrEmpty()) {
                        Text(
                            text = addr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                Text(
                    text = com.freshveg.app.core.utils.formatSafeDate(invoice.invoiceDate, invoice.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))

            // Amount & View Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Grand Total:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = "₹${invoice.totalAmount.toInt()}.00",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1976D2)
                    )
                }

                OutlinedButton(
                    onClick = onViewDetails,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("View Tax Bill", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun PendingOrderInvoiceCard(
    order: OrderDto,
    isGenerating: Boolean,
    onGenerateInvoice: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Order Number & Ready to Bill Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${order.orderNumber}",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "READY TO BILL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = order.customer?.businessName ?: "Customer",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Produce Emojis Summary
            val emojis = order.items.joinToString(" ") { ProduceVisualUtils.getProduceEmoji(it.productNameSnapshot, null) }
            Text(text = "$emojis (${order.items.size} items delivered)", fontSize = 14.sp)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))

            // Amount & Generate Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Billed Amount:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = "₹${order.totalAmount.toInt()}.00",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ActionGreen
                    )
                }

                Button(
                    onClick = onGenerateInvoice,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isGenerating,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Tax Bill", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
