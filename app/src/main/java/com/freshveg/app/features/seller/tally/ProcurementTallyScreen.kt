package com.freshveg.app.features.seller.tally

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.freshveg.app.core.network.ProcurementTallyItemDto
import com.freshveg.app.core.network.ProducePurchaseRecordDto
import com.freshveg.app.core.network.SellerExpenseRecordDto
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcurementTallyScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProcurementTallyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isAddExpenseOpen by remember { mutableStateOf(false) }
    var isAddPurchaseOpen by remember { mutableStateOf(false) }
    var isShareSheetOpen by remember { mutableStateOf(false) }

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
                            "Morning Procurement & P&L",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Date: ${uiState.selectedDate} • Mandi Operations",
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
                    // WhatsApp Share Tally (opens 2-method sheet)
                    IconButton(onClick = { isShareSheetOpen = true }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Tally",
                            tint = ForestGreenPrimary
                        )
                    }
                    // Refresh
                    IconButton(onClick = viewModel::loadData) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ForestGreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == 1) {
                // FAB for Inward Purchases
                ExtendedFloatingActionButton(
                    onClick = { isAddPurchaseOpen = true },
                    containerColor = ForestGreenPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Inward Purchase", fontWeight = FontWeight.Bold)
                }
            } else if (uiState.selectedTab == 2) {
                // FAB for Expenses
                ExtendedFloatingActionButton(
                    onClick = { isAddExpenseOpen = true },
                    containerColor = ForestGreenPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Daily Expense", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 4-Tab Navigation Bar
            PrimaryTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = CardSurface,
                contentColor = ForestGreenPrimary
            ) {
                val tallyCount = uiState.tallyData?.tally?.size ?: 0
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("📦 Tally ($tallyCount)", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("🛒 Inward (${uiState.purchases.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("💸 Overheads (${uiState.expenses.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    text = { Text("📊 Daily P&L", fontWeight = FontWeight.Bold) }
                )
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
                        0 -> TallyTabContent(
                            tallyData = uiState.tallyData,
                            onShareWhatsApp = { isShareSheetOpen = true }
                        )
                        1 -> InwardPurchasesTabContent(
                            purchases = uiState.purchases,
                            onDelete = viewModel::deletePurchase
                        )
                        2 -> ExpensesTabContent(
                            expenses = uiState.expenses,
                            onDelete = viewModel::deleteExpense
                        )
                        3 -> ProfitLossTabContent(
                            pnl = uiState.pnlSummary
                        )
                    }
                }
            }
        }
    }

    // Bottom Sheets
    if (isShareSheetOpen) {
        ShareTallyBottomSheet(
            onDismiss = { isShareSheetOpen = false },
            onSelectMethod1 = {
                isShareSheetOpen = false
                viewModel.shareMethod1ItemBreakdown(context)
            },
            onSelectMethod2 = {
                isShareSheetOpen = false
                viewModel.shareMethod2RestaurantWise(context)
            }
        )
    }

    if (isAddExpenseOpen) {
        AddExpenseBottomSheet(
            onDismiss = { isAddExpenseOpen = false },
            onSubmit = { category, amount, description, paidTo, paymentMode ->
                viewModel.createExpense(category, amount, description, paidTo, paymentMode)
                isAddExpenseOpen = false
            }
        )
    }

    if (isAddPurchaseOpen) {
        AddProducePurchaseBottomSheet(
            products = uiState.allProducts,
            onDismiss = { isAddPurchaseOpen = false },
            onSubmit = { supplierName, supplierMobile, items ->
                viewModel.createPurchase(supplierName, supplierMobile, items)
                isAddPurchaseOpen = false
            }
        )
    }
}

// ---------------------------------------------------------------------------
// 1. TALLY TAB CONTENT
// ---------------------------------------------------------------------------

@Composable
fun TallyTabContent(
    tallyData: com.freshveg.app.core.network.ProcurementTallyData?,
    onShareWhatsApp: () -> Unit
) {
    val items = tallyData?.tally ?: emptyList()

    if (items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Orders for Today Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Procurement tally will automatically generate as restaurant buyers place morning orders.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Summary Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Morning Demand", fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                            val totalItemsCount = if ((tallyData?.totalItemsCount ?: 0) > 0) tallyData!!.totalItemsCount else items.sumOf { it.orderCount }
                            Text("${tallyData?.totalOrders ?: 0} Orders • $totalItemsCount Items • ${items.size} Varieties", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        }

                        Button(
                            onClick = onShareWhatsApp,
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Tally", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Tally Items List
            items(items, key = { it.productId }) { item ->
                TallyItemRowCard(item = item)
            }
        }
    }
}

@Composable
fun TallyItemRowCard(item: ProcurementTallyItemDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProduceThumbnailBadge(
                    name = item.productName,
                    hindiName = item.hindiName,
                    size = 48.dp,
                    cornerRadius = 8.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!item.hindiName.isNullOrEmpty()) "${item.productName} (${item.hindiName})" else item.productName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Category: ${item.categoryName}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        if (item.currentSellingPrice > 0) {
                            Text("• Sell: ₹${item.currentSellingPrice.toInt()}/${item.unitType}", style = MaterialTheme.typography.labelSmall, color = ForestGreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Total Ordered Qty Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${item.totalOrderedQuantity} ${item.unitType}",
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestGreenPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${item.orderCount} buyers",
                            style = MaterialTheme.typography.labelSmall,
                            color = FarmGreenSecondary
                        )
                    }
                }
            }

            // Buyer Breakdown Chips
            val hasBreakdown = item.buyerBreakdown.isNotEmpty()
            if (hasBreakdown || item.buyerNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(6.dp))

                val breakdownStr = if (hasBreakdown) {
                    item.buyerBreakdown.joinToString(" + ") { b ->
                        val q = if (b.quantity % 1.0 == 0.0) "${b.quantity.toInt()}" else "${b.quantity}"
                        "$q ${b.unitType} (${b.buyerName})"
                    }
                } else {
                    item.buyerNames.joinToString(", ")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        if (hasBreakdown) "Breakdown: " else "Ordered by: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = breakdownStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. INWARD PURCHASES TAB CONTENT
// ---------------------------------------------------------------------------

@Composable
fun InwardPurchasesTabContent(
    purchases: List<ProducePurchaseRecordDto>,
    onDelete: (String) -> Unit
) {
    if (purchases.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Inward Purchases Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Tap '+ Log Inward Purchase' below to record produce bought at the Mandi auction.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(purchases, key = { it.id }) { purchase ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(purchase.supplierName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Bill: ${purchase.purchaseNumber} • ${purchase.paymentMode}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹${purchase.totalAmount.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = ForestGreenPrimary
                                )
                                IconButton(onClick = { onDelete(purchase.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }

                        if (purchase.items.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFFEEEEEE))
                            Spacer(modifier = Modifier.height(6.dp))

                            purchase.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${item.productNameSnapshot} (${item.quantity} ${item.unitTypeSnapshot})", style = MaterialTheme.typography.bodySmall)
                                    Text("₹${item.lineTotal.toInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. EXPENSES TAB CONTENT
// ---------------------------------------------------------------------------

@Composable
fun ExpensesTabContent(
    expenses: List<SellerExpenseRecordDto>,
    onDelete: (String) -> Unit
) {
    if (expenses.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Expenses Recorded Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Tap '+ Add Daily Expense' below to track labour, transport, ice, or mandi cess.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
        }
    } else {
        val totalAmount = expenses.sumOf { it.amount }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Daily Overheads:", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Text("₹${totalAmount.toInt()}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = Color(0xFFE65100))
                    }
                }
            }

            items(expenses, key = { it.id }) { exp ->
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(exp.description, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(exp.category, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                if (!exp.paidTo.isNullOrEmpty()) {
                                    Text("• Paid to: ${exp.paidTo}", style = MaterialTheme.typography.labelSmall, color = ForestGreenPrimary)
                                }
                            }
                        }

                        Text("₹${exp.amount.toInt()}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = Color(0xFFD32F2F))

                        IconButton(onClick = { onDelete(exp.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 4. P&L TAB CONTENT
// ---------------------------------------------------------------------------

@Composable
fun ProfitLossTabContent(
    pnl: com.freshveg.app.core.network.ProfitLossSummaryDto?
) {
    if (pnl == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ForestGreenPrimary)
        }
        return
    }

    val m = pnl.metrics
    val isNetProfit = m.netProfit >= 0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Net Profit / Loss Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNetProfit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isNetProfit) "💰 Net Profit Today" else "🔻 Net Loss Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isNetProfit) ForestGreenPrimary else Color(0xFFC62828)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "₹${m.netProfit.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isNetProfit) ForestGreenPrimary else Color(0xFFC62828)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Net Margin: ${m.netMarginPercentage}% • Gross Margin: ${m.grossMarginPercentage}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.DarkGray
                    )
                }
            }
        }

        // 2. 3-Column Financial Breakdown
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Revenue
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Sales Revenue", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${m.totalRevenue.toInt()}", fontWeight = FontWeight.Bold, color = ForestGreenPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("${m.invoiceCount} invoices", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    }
                }

                // Procurement Cost
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Produce Cost", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${m.totalProduceCost.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), style = MaterialTheme.typography.titleMedium)
                        Text("${m.purchaseCount} inward bills", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    }
                }

                // Expenses
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Overheads", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${m.totalExpenses.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), style = MaterialTheme.typography.titleMedium)
                        Text("${m.expenseCount} entries", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                    }
                }
            }
        }

        // 3. Category Expenses Breakdown
        if (pnl.categoryBreakdown.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Expense Breakdown by Category:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(10.dp))

                        pnl.categoryBreakdown.forEach { cat ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cat.category, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text("₹${cat.amount.toInt()} (${cat.percentage}%)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (cat.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = ForestGreenPrimary,
                                    trackColor = Color(0xFFEEEEEE)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 5. SHARE TALLY BOTTOM SHEET (2 METHODS)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareTallyBottomSheet(
    onDismiss: () -> Unit,
    onSelectMethod1: () -> Unit,
    onSelectMethod2: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(
                    text = "Share Morning Order Slip",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MainInk
                )
                Text(
                    text = "Choose WhatsApp slip format to send to mandi / suppliers",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }

            // Option 1: Item Breakdown Slip
            Card(
                onClick = onSelectMethod1,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F4)),
                border = BorderStroke(1.dp, Color(0xFFCCE7D3))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ForestGreenPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Calculate,
                            contentDescription = null,
                            tint = ForestGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Method 1: Produce Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MainInk
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "आइटम अनुसार जोड़ (जैसे: आलू ➜ 1kg Rest A + 2kg Rest B = 3kg)",
                            fontSize = 12.sp,
                            color = InkSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ForestGreenPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Option 2: Restaurant-Wise Summary Slip
            Card(
                onClick = onSelectMethod2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F766E).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Method 2: Restaurant-Wise Summary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MainInk
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "रेस्टोरेंट अनुसार सूची (जैसे: Hotel A ➜ 1) आलू = 2kg, 2) भिंडी = 3kg)",
                            fontSize = 12.sp,
                            color = InkSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
