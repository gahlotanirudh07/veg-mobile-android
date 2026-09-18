package com.freshveg.app.features.buyer.orders

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.R
import com.freshveg.app.core.network.OrderDto
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.safeDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrdersScreen(
    onNavigateBack: () -> Unit,
    onReorder: (OrderDto) -> Unit = {},
    viewModel: BuyerOrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.orders_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MainInk
                        )
                        Text(
                            text = "Live Morning Dispatch & Weighment",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = MainInk)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondarySurface)
            )
        },
        containerColor = SecondarySurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to stringResource(R.string.orders_tab_all),
                    "TODAY" to "Today",
                    "YESTERDAY" to "Yesterday",
                    "THIS_WEEK" to "This Week"
                ).forEach { (filter, label) ->
                    val isSelected = uiState.selectedDateFilter == filter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) ActionGreen else NeutralSurface,
                        modifier = Modifier.clickable { viewModel.onSelectDateFilter(filter) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MainInk,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            if (uiState.isLoading && uiState.orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ActionGreen)
                }
            } else if (uiState.filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📦", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.orders_empty),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MainInk
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredOrders, key = { it.id }) { order ->
                        BuyerOrderCard(
                            order = order,
                            onClick = { viewModel.openOrderDetail(order) },
                            onShare = { viewModel.shareOrderStatus(context, order) }
                        )
                    }
                }
            }
        }
    }

    uiState.selectedOrder?.let { order ->
        BuyerOrderDetailBottomSheet(
            order = order,
            onDismiss = { viewModel.closeOrderDetail() },
            onShare = { viewModel.shareOrderStatus(context, order) }
        )
    }
}

@Composable
fun BuyerOrderCard(
    order: OrderDto,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.orderNumber.ifBlank { order.id.takeLast(6) }}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MainInk
                    )
                    Text(
                        text = (order.placedAt ?: order.createdAt ?: "").safeDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (order.status) {
                        "FULFILLED", "DELIVERED" -> ActionGreen.copy(alpha = 0.12f)
                        "CONFIRMED" -> Color(0xFF1976D2).copy(alpha = 0.12f)
                        "CANCELLED" -> MutedRedError.copy(alpha = 0.12f)
                        else -> AmberWarning.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = order.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (order.status) {
                            "FULFILLED", "DELIVERED" -> ActionGreen
                            "CONFIRMED" -> Color(0xFF1976D2)
                            "CANCELLED" -> MutedRedError
                            else -> AmberWarning
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${order.items.size} Produce Items • ${order.items.take(3).joinToString { it.displayName }}",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ${ProduceVisualUtils.formatCurrency(order.totalAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = ActionGreen
                )

                TextButton(onClick = onClick) {
                    Text("View Weighment →", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ActionGreen)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrderDetailBottomSheet(
    order: OrderDto,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.orderNumber.ifBlank { order.id.takeLast(6) }}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = "Placed on ${(order.placedAt ?: order.createdAt ?: "").safeDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = ActionGreen)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(10.dp))

            Text("Itemized Produce & Scale Weighment:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(order.items, key = { it.id }) { item ->
                    val unit = item.unitSnapshot.lowercase()
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeutralSurface,
                        border = androidx.compose.foundation.BorderStroke(0.75.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ProduceThumbnailBadge(
                                    name = item.displayName,
                                    hindiName = item.hindiName,
                                    size = 42.dp,
                                    cornerRadius = 8.dp
                                )
                                Column {
                                    Text(item.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MainInk)
                                    Text(
                                        text = if (item.deliveredQuantity != null && item.deliveredQuantity!! > 0) {
                                            "Weighed: ${ProduceVisualUtils.formatQuantity(item.deliveredQuantity!!, unit)} (Ordered: ${ProduceVisualUtils.formatQuantity(item.quantity, unit)})"
                                        } else {
                                            "Ordered: ${ProduceVisualUtils.formatQuantity(item.quantity, unit)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.deliveredQuantity != null) ActionGreen else InkSecondary
                                    )
                                }
                            }

                            Text(ProduceVisualUtils.formatCurrency(item.total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MainInk)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.common_total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(ProduceVisualUtils.formatCurrency(order.totalAmount), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = ActionGreen)
                }
            }
        }
    }
}
