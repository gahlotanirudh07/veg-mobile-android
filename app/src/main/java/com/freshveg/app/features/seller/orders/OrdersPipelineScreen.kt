package com.freshveg.app.features.seller.orders

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.network.OrderDto
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.R
import androidx.compose.ui.res.stringResource
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersPipelineScreen(
    onNavigateBack: () -> Unit,
    viewModel: OrdersPipelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

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
                            "Orders & Scale Fulfillment",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MainInk
                        )
                        Text(
                            "Cutoff: ${uiState.cutoffTime ?: "02:00 AM"} • Morning Dispatch",
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
                    IconButton(onClick = viewModel::openEditCutoff) {
                        Icon(Icons.Default.Timer, contentDescription = "Cutoff Settings", tint = MainInk)
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MainInk)
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
            // 1. Search & Filter Bar
            Surface(
                color = CardSurface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Search by restaurant, #order or produce...") },
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Filter Tabs Row with standard pill styling
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.selectedStatusTab == "ALL",
                            onClick = { viewModel.onSelectStatusTab("ALL") },
                            label = { Text("All (${uiState.orders.size})", fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ActionGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF475569)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.selectedStatusTab == "ALL",
                                borderColor = if (uiState.selectedStatusTab == "ALL") ActionGreen else Color(0xFFE2E8F0),
                                selectedBorderColor = ActionGreen
                            )
                        )

                        FilterChip(
                            selected = uiState.selectedStatusTab == "PENDING",
                            onClick = { viewModel.onSelectStatusTab("PENDING") },
                            label = { Text("Awaiting Weighment (${uiState.pendingCount})", fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ActionGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF475569)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.selectedStatusTab == "PENDING",
                                borderColor = if (uiState.selectedStatusTab == "PENDING") ActionGreen else Color(0xFFE2E8F0),
                                selectedBorderColor = ActionGreen
                            )
                        )

                        FilterChip(
                            selected = uiState.selectedStatusTab == "FULFILLED",
                            onClick = { viewModel.onSelectStatusTab("FULFILLED") },
                            label = { Text("Ready to Bill (${uiState.fulfilledCount})", fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ActionGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF475569)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.selectedStatusTab == "FULFILLED",
                                borderColor = if (uiState.selectedStatusTab == "FULFILLED") ActionGreen else Color(0xFFE2E8F0),
                                selectedBorderColor = ActionGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date & Customer Filter and Sorting Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date filters
                        val dateFilters = listOf("ALL" to "All Dates", "TODAY" to "Today", "YESTERDAY" to "Yesterday", "THIS_WEEK" to "This Week")
                        dateFilters.forEach { (key, label) ->
                            FilterChip(
                                selected = uiState.selectedDateFilter == key,
                                onClick = { viewModel.onSelectDateFilter(key) },
                                label = { Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0F766E),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF334155)
                                )
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(18.dp),
                            color = Color(0xFFCBD5E1)
                        )

                        // Customer Filters
                        FilterChip(
                            selected = uiState.selectedCustomer == "ALL",
                            onClick = { viewModel.onSelectCustomer("ALL") },
                            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("All Customers", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ActionGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            )
                        )

                        uiState.uniqueCustomers.forEach { custName ->
                            FilterChip(
                                selected = uiState.selectedCustomer == custName,
                                onClick = { viewModel.onSelectCustomer(if (uiState.selectedCustomer == custName) "ALL" else custName) },
                                label = { Text(custName, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ActionGreen,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = Color(0xFF334155)
                                )
                            )
                        }

                        VerticalDivider(
                            modifier = Modifier.height(18.dp),
                            color = Color(0xFFCBD5E1)
                        )

                        // Sort Controls
                        FilterChip(
                            selected = uiState.selectedSortOrder == "NEWEST",
                            onClick = { viewModel.onSelectSortOrder("NEWEST") },
                            leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            label = { Text("Newest", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1976D2),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            )
                        )

                        FilterChip(
                            selected = uiState.selectedSortOrder == "OLDEST",
                            onClick = { viewModel.onSelectSortOrder("OLDEST") },
                            leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            label = { Text("Oldest", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1976D2),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            )
                        )

                        FilterChip(
                            selected = uiState.selectedSortOrder == "AMOUNT_HIGH",
                            onClick = { viewModel.onSelectSortOrder(if (uiState.selectedSortOrder == "AMOUNT_HIGH") "AMOUNT_LOW" else "AMOUNT_HIGH") },
                            leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            label = {
                                Text(
                                    if (uiState.selectedSortOrder == "AMOUNT_LOW") "Amount: Low → High" else "Amount: High → Low",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE65100),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            )
                        )
                    }
                }
            }

            // 2. Orders List
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                if (uiState.isLoading && uiState.orders.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ActionGreen)
                } else if (uiState.filteredOrders.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "No orders match '${uiState.searchQuery}'" else "No orders in this status",
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
                        items(uiState.filteredOrders, key = { it.id }) { order ->
                            OrderPipelineCard(
                                order = order,
                                isGeneratingInvoice = uiState.isGeneratingInvoice,
                                onFulfillClick = { viewModel.openFulfillDialog(order) },
                                onEditWeighment = { viewModel.openFulfillDialog(order) },
                                onGenerateInvoice = { viewModel.generateInvoice(order.id) },
                                onUpdateStatus = { newStatus -> viewModel.updateOrderStatus(order.id, newStatus) },
                                onCall = { mobile -> viewModel.callCustomer(context, mobile) },
                                onWhatsApp = { viewModel.sendWhatsAppDispatchNotification(context, order) }
                            )
                        }
                    }
                }

                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Color.White,
                    contentColor = ActionGreen
                )
            }
        }
    }

    // Scale Fulfillment Bottom Sheet
    uiState.selectedOrderForFulfill?.let { order ->
        FulfillOrderBottomSheet(
            order = order,
            serverError = uiState.errorMessage,
            isSubmitting = uiState.isFulfilling,
            onDismiss = viewModel::closeFulfillDialog,
            onConfirmFulfill = { items ->
                viewModel.fulfillOrder(order.id, items)
            }
        )
    }

    // Cutoff Time Setting Bottom Sheet
    if (uiState.isEditCutoffOpen) {
        EditCutoffBottomSheet(
            currentCutoffTime = uiState.cutoffTime ?: "03:00 AM",
            onDismiss = viewModel::closeEditCutoff,
            onSaveCutoffTime = viewModel::saveCutoffTime
        )
    }
}

@Composable
fun OrderPipelineCard(
    order: OrderDto,
    isGeneratingInvoice: Boolean = false,
    onFulfillClick: () -> Unit,
    onEditWeighment: () -> Unit,
    onGenerateInvoice: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val cust = order.customer
    val hasInvoice = order.hasInvoice
    val activeInvoice = order.activeInvoice

    val (statusLabel, statusBg, statusColor) = when {
        order.status == "FULFILLED" && hasInvoice -> Triple("Billed (${activeInvoice?.invoiceNumber?.takeLast(8) ?: "Done"})", Color(0xFFE8F5E9), ActionGreen)
        order.status == "FULFILLED" && !hasInvoice -> Triple("Ready to Bill", Color(0xFFEFF6FF), Color(0xFF1D4ED8))
        order.status == "PENDING" -> Triple("Awaiting Weighment", Color(0xFFFFF8E1), Color(0xFFE65100))
        order.status == "CONFIRMED" -> Triple("Confirmed", Color(0xFFE3F2FD), Color(0xFF1565C0))
        order.status == "DELIVERED" -> Triple("Completed", Color(0xFFE8F5E9), ActionGreen)
        order.status == "CANCELLED" -> Triple("Cancelled", Color(0xFFFFEBEE), Color(0xFFC62828))
        else -> Triple(order.status, Color(0xFFF5F5F5), Color.DarkGray)
    }

    // Modern elevated card with subtle border definition
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8ECE8))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header: Customer Business Name, Location & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = cust?.businessName ?: "Customer",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF111827),
                        fontSize = 17.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "#${order.orderNumber.takeLast(8)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                        if (!cust?.address.isNullOrBlank()) {
                            Text(
                                text = "• ${cust!!.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Clean Status Badge Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusBg
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(statusColor, CircleShape)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Produce Items Card with High-Contrast Border
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAF8),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8E2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val displayItems = if (isExpanded) order.items else order.items.take(2)
                    displayItems.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = Color(0xFFEAEFEA),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        val emoji = ProduceVisualUtils.getProduceEmoji(item.displayName, item.hindiName)
                        val qty = item.deliveredQuantity ?: item.quantity
                        val lineTotal = (qty * item.price).toInt()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFFE2E8E2), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(emoji, fontSize = 18.sp)
                                }
                                Column {
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "₹${item.price.toInt()}/${item.unitTypeSnapshot ?: "KG"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$qty ${item.unitTypeSnapshot ?: "KG"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "₹$lineTotal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ActionGreen
                                )
                            }
                        }
                    }

                    if (order.items.size > 2) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "Show Less ▲" else "+${order.items.size - 2} more items ▼",
                                style = MaterialTheme.typography.labelSmall,
                                color = ActionGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Bottom Bar: Billed Total & Primary/Secondary CTAs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (order.status == "FULFILLED") "Billed Total" else "Order Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹${order.totalAmount.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ActionGreen,
                        fontSize = 20.sp
                    )
                }

                // Quick Customer Contact + Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Contact action (Phone Call)
                    order.customer?.mobile?.let { mobile ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onCall(mobile) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "Call Customer",
                                    tint = Color(0xFF334155),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Contextual Action Buttons
                    when (order.status) {
                        "PENDING" -> {
                            Button(
                                onClick = onFulfillClick,
                                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fulfill Order", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }
                        "CONFIRMED" -> {
                            Button(
                                onClick = onFulfillClick,
                                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fulfill Order", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            }
                        }
                        "FULFILLED" -> {
                            if (hasInvoice) {
                                // Invoice has already been generated! Billed & strictly locked from editing
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.height(42.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                        Text("Billed (Locked)", fontSize = 12.sp, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = onWhatsApp,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(42.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share Bill", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                                }
                            } else {
                                // Order is fulfilled but not yet invoiced: Allow editing fulfillment
                                OutlinedButton(
                                    onClick = onEditWeighment,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(42.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color(0xFF475569)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Edit Fulfillment",
                                        fontSize = 13.sp,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // Primary Action: Generate Bill
                                Button(
                                    onClick = onGenerateInvoice,
                                    enabled = !isGeneratingInvoice,
                                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(42.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    if (isGeneratingInvoice) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Billing...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    } else {
                                         Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generate Bill", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    }
                                }
                            }
                        }
                        "DELIVERED" -> {
                            Button(
                                onClick = onWhatsApp,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp Bill", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
