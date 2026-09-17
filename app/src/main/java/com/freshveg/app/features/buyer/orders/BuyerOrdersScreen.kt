package com.freshveg.app.features.buyer.orders

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
                            "My Orders & Deliveries",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MainInk
                        )
                        Text(
                            "Live Scale Weighment & Status",
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
                    IconButton(onClick = viewModel::loadOrders) {
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
            if (uiState.isLoading && uiState.orders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ForestGreenPrimary)
                }
            } else if (uiState.orders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No orders placed yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Explore today's fresh produce catalog to place your early morning order.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.orders, key = { it.id }) { order ->
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

    // Modal: Itemized Order Details Bottom Sheet
    uiState.selectedOrder?.let { order ->
        BuyerOrderDetailBottomSheet(
            order = order,
            onDismiss = viewModel::closeOrderDetail,
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
    val statusUpper = order.status.uppercase()
    val (statusLabel, statusBg, statusColor) = when (statusUpper) {
        "DELIVERED" -> Triple("🟢 DELIVERED", Color(0xFFE8F5E9), ForestGreenPrimary)
        "FULFILLED" -> Triple("📦 WEIGHED & DISPATCHED", Color(0xFFE3F2FD), Color(0xFF1976D2))
        "CONFIRMED" -> Triple("🔵 CONFIRMED", Color(0xFFFFF3E0), Color(0xFFE65100))
        "CANCELLED" -> Triple("🔴 CANCELLED", Color(0xFFFFEBEE), Color(0xFFD32F2F))
        else -> Triple("🟡 PLACED", Color(0xFFFFFDE7), Color(0xFFF57F17))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Order Number & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.orderNumber}",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black
                    )
                    Text(
                        text = order.createdAt.safeDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Stage Progress Tracker
            OrderProgressTracker(status = statusUpper)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(10.dp))

            // Items Summary & Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.items.size} Produce Items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "₹${order.totalAmount.toInt()}.00",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ForestGreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp), tint = ForestGreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Items", fontSize = 12.sp)
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = ForestGreenPrimary)
                }
            }
        }
    }
}

@Composable
fun OrderProgressTracker(status: String) {
    val step = when (status) {
        "CONFIRMED" -> 2
        "FULFILLED" -> 3
        else -> 1
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackingPill(title = "1. Placed", active = step >= 1, isCurrent = step == 1)
        TrackingConnector(active = step >= 2)
        TrackingPill(title = "2. Confirmed", active = step >= 2, isCurrent = step == 2)
        TrackingConnector(active = step >= 3)
        TrackingPill(title = "3. Fulfilled & Billed", active = step >= 3, isCurrent = step == 3)
    }
}

@Composable
fun TrackingPill(title: String, active: Boolean, isCurrent: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) ForestGreenPrimary else Color(0xFFEEEEEE))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (active) Color.White else Color.Gray
        )
    }
}

@Composable
fun TrackingConnector(active: Boolean) {
    Box(
        modifier = Modifier
            .width(8.dp)
            .height(2.dp)
            .background(if (active) ForestGreenPrimary else Color(0xFFEEEEEE))
    )
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
        containerColor = CardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.orderNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = "Placed on ${order.createdAt.safeDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = ActionGreen)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(10.dp))

            Text("Itemized Produce & Scale Weighment:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(order.items, key = { it.id }) { item ->
                    val itemName = item.displayName
                    val unit = item.unitTypeSnapshot ?: "KG"
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SecondarySurface),
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
                                    name = itemName,
                                    hindiName = item.hindiName,
                                    imageUrl = null,
                                    size = 40.dp,
                                    cornerRadius = 8.dp
                                )
                                Column {
                                    Text(itemName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MainInk)
                                    Text(
                                        text = if (item.deliveredQuantity != null) "Weighed: ${item.deliveredQuantity} $unit (Ordered: ${item.quantity})"
                                        else "Ordered: ${item.quantity} $unit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.deliveredQuantity != null) ActionGreen else InkSecondary
                                    )
                                }
                            }

                            Text("₹${item.total.toInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MainInk)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Total Card
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
                    Text("Total Billed Amount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text("₹${order.totalAmount.toInt()}.00", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = ActionGreen)
                }
            }
        }
    }
}
