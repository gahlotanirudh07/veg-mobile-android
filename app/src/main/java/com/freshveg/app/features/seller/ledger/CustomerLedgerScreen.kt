package com.freshveg.app.features.seller.ledger

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
import com.freshveg.app.core.network.OutstandingCustomerDto
import com.freshveg.app.core.network.PaymentItemDto
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.safeDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    onNavigateBack: () -> Unit,
    viewModel: CustomerLedgerViewModel = hiltViewModel()
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
                            "Customer Khata & Payments",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Market Dues, Running Balances & UPI",
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
            // 1. Overall Financial Khata Totals Banner
            uiState.overviewSummary?.let { summary ->
                Surface(
                    color = CardSurface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Billed", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("₹${summary.totalInvoiced.toInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1976D2))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Collected", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("₹${summary.totalPaid.toInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = ActionGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Market Dues", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("₹${(summary.totalOutstanding.takeIf { it != 0.0 } ?: summary.overallOutstanding).toInt()}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = Color(0xFFD32F2F))
                        }
                    }
                }
            }

            // 2. Search & Tab Selection Bar
            Surface(
                color = CardSurface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Search customer, phone or payment...") },
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
                                    "📒 Customer Khatas (${uiState.outstandingCustomers.size})",
                                    fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            text = {
                                Text(
                                    "💸 Recent Collections (${uiState.recentPayments.size})",
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
                if (uiState.isLoading && uiState.outstandingCustomers.isEmpty() && uiState.recentPayments.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ActionGreen)
                } else if (uiState.selectedTab == 0) {
                    // Tab 0: Customer Khatas List
                    if (uiState.filteredOutstandingCustomers.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "No customers match '${uiState.searchQuery}'" else "No customer accounts found",
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
                            items(uiState.filteredOutstandingCustomers, key = { it.customerId }) { customer ->
                                CustomerKhataCard(
                                    customer = customer,
                                    onRecordPayment = { viewModel.openRecordPaymentDialog(customer) },
                                    onShowUpiQr = { viewModel.openUpiQr(customer) },
                                    onViewStatement = { viewModel.viewCustomerStatement(customer.customerId) },
                                    onSendReminder = { viewModel.sendWhatsAppPaymentReminder(context, customer) }
                                )
                            }
                        }
                    }
                } else {
                    // Tab 1: Recent Collections History
                    if (uiState.filteredRecentPayments.isEmpty()) {
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
                                text = if (uiState.searchQuery.isNotEmpty()) "No payments match '${uiState.searchQuery}'" else "No payment collections recorded yet",
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
                            items(uiState.filteredRecentPayments, key = { it.id }) { payment ->
                                PaymentCollectionCard(payment = payment)
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Record Payment Modal
    uiState.selectedCustomerForPayment?.let { customer ->
        RecordPaymentBottomSheet(
            customer = customer,
            onDismiss = viewModel::closeRecordPaymentDialog,
            onSubmitPayment = viewModel::createPayment
        )
    }

    // 2. Dynamic UPI QR Modal
    uiState.selectedCustomerForUpi?.let { customer ->
        UpiQrBottomSheet(
            customer = customer,
            onDismiss = viewModel::closeUpiQr
        )
    }

    // 3. Customer Statement Modal
    uiState.selectedCustomerStatement?.let { statement ->
        CustomerStatementBottomSheet(
            ledger = statement,
            onDismiss = viewModel::closeCustomerStatement
        )
    }
}

@Composable
fun CustomerKhataCard(
    customer: OutstandingCustomerDto,
    onRecordPayment: () -> Unit,
    onShowUpiQr: () -> Unit,
    onViewStatement: () -> Unit,
    onSendReminder: () -> Unit
) {
    val dues = customer.outstandingAmount
    val hasDues = dues > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Customer Name & Dues Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.businessName,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!customer.primaryContactName.isNullOrEmpty()) {
                        Text(
                            text = "${customer.primaryContactName} • 📱 ${customer.mobile}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (hasDues) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (hasDues) "🔴 Dues: ₹${dues.toInt()}" else "🟢 All Clear",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDues) Color(0xFFD32F2F) else ActionGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Billed vs Paid Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Invoiced: ₹${customer.totalSalesAmount.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                Text(
                    text = "Total Paid: ₹${customer.totalPaidAmount.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ActionGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Record Payment
                Button(
                    onClick = onRecordPayment,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay Cash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 2. UPI QR Code
                OutlinedButton(
                    onClick = onShowUpiQr,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp), tint = ActionGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("UPI QR", fontSize = 12.sp)
                }

                // 3. Statement
                OutlinedButton(
                    onClick = onViewStatement,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1976D2))
                }

                // 4. WhatsApp Reminder
                if (hasDues) {
                    IconButton(onClick = onSendReminder, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "WhatsApp Reminder", tint = ActionGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentCollectionCard(payment: PaymentItemDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.businessName ?: payment.customerName ?: "Customer",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = payment.paymentMode,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActionGreen
                        )
                    }
                    Text(
                        text = payment.paymentDate.safeDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )
                }
                if (!payment.referenceNumber.isNullOrBlank()) {
                    Text("Ref: ${payment.referenceNumber}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                if (!payment.notes.isNullOrBlank()) {
                    Text(payment.notes, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Text(
                text = "+ ₹${payment.amount.toInt()}.00",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = ActionGreen
            )
        }
    }
}
