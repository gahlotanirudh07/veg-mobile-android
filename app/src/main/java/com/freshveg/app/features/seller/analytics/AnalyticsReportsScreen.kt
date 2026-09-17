package com.freshveg.app.features.seller.analytics

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.freshveg.app.core.network.*
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.safeDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Executive Analytics",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Sales, Volume & Profit Insights",
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
                    IconButton(onClick = { viewModel.shareWhatsAppReport(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share WhatsApp Summary", tint = ForestGreenPrimary)
                    }
                    IconButton(onClick = viewModel::loadReportData) {
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
            // 1. Period Selector (Today, 7 Days, 30 Days, All Time)
            Surface(
                color = CardSurface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DateRangePeriod.values()) { period ->
                            FilterChip(
                                selected = uiState.selectedPeriod == period,
                                onClick = { viewModel.selectPeriod(period) },
                                label = { Text(period.label, fontWeight = if (uiState.selectedPeriod == period) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ForestGreenPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = CardSurface,
                        contentColor = ForestGreenPrimary
                    ) {
                        Tab(
                            selected = uiState.selectedTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            text = { Text("Daily Sales", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            text = { Text("Buyers", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = uiState.selectedTab == 2,
                            onClick = { viewModel.selectTab(2) },
                            text = { Text("Produce", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = uiState.selectedTab == 3,
                            onClick = { viewModel.selectTab(3) },
                            text = { Text("Collections", fontSize = 12.sp, fontWeight = if (uiState.selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // 2. Tab Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ForestGreenPrimary)
                } else {
                    when (uiState.selectedTab) {
                        0 -> DailySalesTabContent(dailySales = uiState.dailySales)
                        1 -> BuyerSummaryTabContent(buyerSummary = uiState.buyerSummary)
                        2 -> ProduceSalesTabContent(produceSales = uiState.produceSales)
                        3 -> CollectionsTabContent(collections = uiState.collections)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Tab 0: Daily Sales
// ----------------------------------------------------
@Composable
fun DailySalesTabContent(dailySales: DailySalesReportResponse?) {
    val summary = dailySales?.summary
    val rows = dailySales?.rows ?: emptyList()
    val maxRevenue = rows.maxOfOrNull { it.totalRevenue } ?: 1.0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Summary Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Revenue", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${summary?.overallRevenue?.toInt() ?: 0}", fontWeight = FontWeight.Bold, color = ForestGreenPrimary, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Dispatched Volume", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${summary?.overallWeight?.toInt() ?: 0} KG", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2), style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Orders", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${summary?.overallOrders ?: 0}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item {
            Text("Day-by-Day Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        if (rows.isEmpty()) {
            item {
                Text("No sales data recorded for this period.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(rows, key = { it.date }) { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(row.date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("₹${row.totalRevenue.toInt()}", fontWeight = FontWeight.ExtraBold, color = ForestGreenPrimary, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${row.ordersCount} orders • ${row.uniqueBuyersCount} buyers", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                            Text("${row.totalWeight.toInt()} KG dispatched", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        // Progress visualizer bar
                        val fraction = (row.totalRevenue / maxRevenue).toFloat().coerceIn(0.05f, 1f)
                        LinearProgressIndicator(
                            progress = { fraction },
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

// ----------------------------------------------------
// Tab 1: Buyer Summary
// ----------------------------------------------------
@Composable
fun BuyerSummaryTabContent(buyerSummary: BuyerSummaryReportResponse?) {
    val summary = buyerSummary?.summary
    val rows = buyerSummary?.rows ?: emptyList()
    val totalSales = summary?.totalSales ?: 1.0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Active Buyers", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${summary?.totalBuyers ?: 0}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Sales", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${summary?.totalSales?.toInt() ?: 0}", fontWeight = FontWeight.Bold, color = ForestGreenPrimary, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Orders", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${summary?.totalOrders ?: 0}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item {
            Text("Top Restaurant Buyers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        if (rows.isEmpty()) {
            item {
                Text("No buyer summary data recorded for this period.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(rows, key = { it.customerId }) { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(row.businessName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("₹${row.totalSales.toInt()}", fontWeight = FontWeight.ExtraBold, color = ForestGreenPrimary, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${row.ordersCount} orders • 📱 ${row.mobile}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                            Text("Avg Order: ₹${row.averageOrderValue.toInt()}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val pct = ((row.totalSales / totalSales) * 100).toInt().coerceIn(1, 100)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(
                                progress = { (row.totalSales / totalSales).toFloat().coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF1976D2),
                                trackColor = Color(0xFFEEEEEE)
                            )
                            Text("$pct%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Tab 2: Produce Sales
// ----------------------------------------------------
@Composable
fun ProduceSalesTabContent(produceSales: ProduceSalesReportResponse?) {
    val summary = produceSales?.summary
    val rows = produceSales?.rows ?: emptyList()
    val totalVolume = summary?.totalQuantitySold ?: 1.0

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Active SKUs", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${summary?.totalProducts ?: 0}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Volume Sold", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${summary?.totalQuantitySold?.toInt() ?: 0} KG", fontWeight = FontWeight.Bold, color = ForestGreenPrimary, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Produce Revenue", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${summary?.totalRevenue?.toInt() ?: 0}", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        item {
            Text("Top Dispatched Produce", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        if (rows.isEmpty()) {
            item {
                Text("No produce sales data recorded for this period.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(rows, key = { it.productId }) { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(row.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("₹${row.totalRevenue.toInt()}", fontWeight = FontWeight.ExtraBold, color = ForestGreenPrimary, style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${row.totalQuantity.toInt()} ${row.unitType} sold", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                            Text("Avg Price: ₹${row.averagePrice.toInt()}/${row.unitType}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val pct = ((row.totalQuantity / totalVolume) * 100).toInt().coerceIn(1, 100)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(
                                progress = { (row.totalQuantity / totalVolume).toFloat().coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = ForestGreenPrimary,
                                trackColor = Color(0xFFEEEEEE)
                            )
                            Text("$pct%", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// Tab 3: Payment Collections
// ----------------------------------------------------
@Composable
fun CollectionsTabContent(collections: PaymentCollectionsReportResponse?) {
    val summary = collections?.summary
    val rows = collections?.rows ?: emptyList()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Collections: ₹${summary?.totalCollections?.toInt() ?: 0}", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, color = ForestGreenPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("💵 Cash", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("₹${summary?.totalCash?.toInt() ?: 0}", fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("📱 UPI", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("₹${summary?.totalUpi?.toInt() ?: 0}", fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                        }
                        Column {
                            Text("🏦 Bank", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("₹${summary?.totalBank?.toInt() ?: 0}", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                        }
                        Column {
                            Text("📑 Cheque", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("₹${summary?.totalCheque?.toInt() ?: 0}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("Collection Receipts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        if (rows.isEmpty()) {
            item {
                Text("No payment collections recorded for this period.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(rows) { row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(10.dp),
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
                            Text(row.customerName ?: "Customer Payment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("${row.date.safeDate()} • ${row.paymentMode}", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                            if (!row.referenceNumber.isNullOrBlank()) {
                                Text("Ref: ${row.referenceNumber}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }

                        Text("+ ₹${row.amount.toInt()}", fontWeight = FontWeight.ExtraBold, color = ForestGreenPrimary, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
