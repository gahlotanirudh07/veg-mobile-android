package com.freshveg.app.features.buyer.orders

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.network.ConnectedSellerDto
import com.freshveg.app.core.network.OrderDto
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.ui.animation.bounceClick
import com.freshveg.app.core.ui.animation.rememberTactileHaptic
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.safeDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrderHubScreen(
    onCreateNewOrder: () -> Unit,
    onViewPastOrders: () -> Unit,
    onLogout: () -> Unit,
    viewModel: BuyerOrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val triggerHaptic = rememberTactileHaptic()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🥬", fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = "MandiExpress Wholesale",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = ForestGreenPrimary
                            )
                            Text(
                                text = "Daily Early Morning Produce Orders",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadOrders) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ForestGreenPrimary)
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Gray)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connected Supplier & Cutoff Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardSurface,
                shadowElevation = 1.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(ForestGreenPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = ForestGreenPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = uiState.connectedSeller?.businessName ?: "Ultra Veggies Wholesale",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Seller Code: ${uiState.connectedSeller?.sellerCode ?: "SEL-EFBC95C3"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Call Vendor Button
                        IconButton(
                            onClick = {
                                uiState.connectedSeller?.mobile?.let { phone ->
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9))
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Call Seller",
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Cutoff Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "⏰ Order Cutoff: ${uiState.cutoffTime} • Dispatched at 05:00 AM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }
            }

            // 2. Section Title
            Text(
                text = "📦 Orders & Re-ordering",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            // 3. Card 1: CREATE NEW ORDER (Primary Green Hero Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick {
                        triggerHaptic()
                        onCreateNewOrder()
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(ForestGreenPrimary, FarmGreenSecondary)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🛒", fontSize = 24.sp)
                                }
                                Column {
                                    Text(
                                        text = "Create New Order",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp
                                    )
                                    Text(
                                        text = "Order Today's Fresh Produce",
                                        color = Color(0xFFE8F5E9),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Feature Badges Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OrderFeatureBadge(text = "Wholesale Chips (+5/10/25 kg)", bg = Color.White.copy(alpha = 0.18f), textColor = Color.White)
                            OrderFeatureBadge(text = "Mandi Rates Live", bg = Color.White.copy(alpha = 0.18f), textColor = Color.White)
                        }

                        // Prominent Action Button inside Card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHaptic()
                                    onCreateNewOrder()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Start Today's Order →",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = ForestGreenPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Card 2: VIEW PAST ORDERS (Clean Elevated Card)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick {
                        triggerHaptic()
                        onViewPastOrders()
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = ForestGreenPrimary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "View Past Orders",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Track Deliveries & Mandi Slips",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ForestGreenPrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Feature Badges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrderFeatureBadge(text = "Live Scale Weighment", bg = Color(0xFFE8F5E9), textColor = ForestGreenPrimary)
                        OrderFeatureBadge(text = "Itemized Slips", bg = Color(0xFFF5F5F5), textColor = Color.DarkGray)
                    }

                    // Button inside Card
                    OutlinedButton(
                        onClick = {
                            triggerHaptic()
                            onViewPastOrders()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreenPrimary)
                    ) {
                        Text(
                            text = "View Order History →",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 5. Section 3: Recent Orders & Quick Re-order
            Text(
                text = "⚡ Recent Orders & Quick Re-order",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            if (uiState.isLoading && uiState.orders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ForestGreenPrimary)
                }
            } else if (uiState.orders.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No past orders yet",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap 'Create New Order' above to place your first wholesale produce order!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Show latest up to 3 orders
                uiState.orders.take(3).forEach { order ->
                    RecentOrderPreviewCard(
                        order = order,
                        onClick = { viewModel.openOrderDetail(order) },
                        onRepeat = {
                            triggerHaptic()
                            onCreateNewOrder()
                        }
                    )
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
fun OrderFeatureBadge(text: String, bg: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
fun RecentOrderPreviewCard(
    order: OrderDto,
    onClick: () -> Unit,
    onRepeat: () -> Unit
) {
    val statusUpper = order.status.uppercase()
    val (statusLabel, statusBg, statusColor) = when (statusUpper) {
        "DELIVERED" -> Triple("🟢 DELIVERED", Color(0xFFE8F5E9), ForestGreenPrimary)
        "FULFILLED" -> Triple("📦 DISPATCHED", Color(0xFFE3F2FD), Color(0xFF1976D2))
        "CONFIRMED" -> Triple("🔵 CONFIRMED", Color(0xFFFFF3E0), Color(0xFFE65100))
        "CANCELLED" -> Triple("🔴 CANCELLED", Color(0xFFFFEBEE), Color(0xFFD32F2F))
        else -> Triple("🟡 PLACED", Color(0xFFFFFDE7), Color(0xFFF57F17))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Order #${order.orderNumber}",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Black
                    )
                    Text(
                        text = order.createdAt.safeDate(),
                        style = MaterialTheme.typography.labelSmall,
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

            HorizontalDivider(color = Color(0xFFF0F0F0))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.items.size} Produce Items",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
                Text(
                    text = "₹${order.totalAmount.toInt()}.00",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ForestGreenPrimary
                )
            }

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
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp), tint = ForestGreenPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Slip", fontSize = 12.sp)
                }

                Button(
                    onClick = onRepeat,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Re-order", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
