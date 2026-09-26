package com.freshveg.app.features.seller.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.core.ui.animation.bounceClick
import com.freshveg.app.R
import androidx.compose.ui.res.stringResource
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerHomeScreen(
    sellerBusinessName: String = "Ultra Veggies Wholesale",
    sellerCode: String = "SEL-EFBC95C3",
    newOrdersCount: Int = 0,
    marketDues: Double = 0.0,
    totalSalesToday: Double = 0.0,
    hideMasterCatalogue: Boolean = false,
    onNavigateToOrders: () -> Unit,
    onNavigateToActiveOrders: () -> Unit = onNavigateToOrders,
    onNavigateToRates: () -> Unit,
    onNavigateToStore: () -> Unit = {},
    onNavigateToKhata: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToTally: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(ActionGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = ActionGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = sellerBusinessName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MainInk,
                                maxLines = 1
                            )
                            Text(
                                text = sellerCode,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkTertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeutralSurface)
            )
        },
        containerColor = BackgroundCanvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── 1. Today's Operational Metrics ───
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NeutralSurface,
                border = BorderStroke(1.dp, BorderSubtle),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Sales Today
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Today's Sales",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkTertiary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${totalSalesToday.toInt()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MainInk
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(1.dp)
                            .background(BorderSubtle)
                    )

                    // Active Orders
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigateToActiveOrders() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Active Orders",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkTertiary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$newOrdersCount",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (newOrdersCount > 0) ActionGreen else MainInk
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .width(1.dp)
                            .background(BorderSubtle)
                    )

                    // Market Dues
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Market Dues",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkTertiary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${marketDues.toInt()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (marketDues > 0) MutedRedError else ActionGreen
                        )
                    }
                }
            }

            // ─── 2. Featured Card: Produce Store & Inventory (Clean Zomato-grade Design) ───
            Surface(
                onClick = onNavigateToStore,
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFF1F8F4),
                border = BorderStroke(1.2.dp, Color(0xFFCCE7D3)),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ActionGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Produce Store & Inventory",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.5.sp,
                                    color = MainInk
                                )
                                Text(
                                    text = "सब्जी स्टोर · कैटलॉग व स्टॉक प्रबंधन",
                                    fontSize = 12.sp,
                                    color = InkSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = ActionGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Active Store",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ActionGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "Customize selling rates, add fresh vegetables, toggle stock availability, and manage buyer visibility.",
                        fontSize = 12.5.sp,
                        color = InkSecondary,
                        lineHeight = 18.sp
                    )

                    // Clean Zomato-grade Bottom Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ActionGreen)
                            )
                            Text(
                                text = "Live in buyer app",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MainInk
                            )
                        }

                        Surface(
                            onClick = onNavigateToStore,
                            shape = RoundedCornerShape(10.dp),
                            color = ActionGreen,
                            modifier = Modifier.bounceClick()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Manage Store",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ─── 4. Operations Hub 2x2 Grid ───
            Text(
                text = "Operations & Management Hub",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = InkSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Orders Pipeline Card
                Surface(
                    onClick = if (newOrdersCount > 0) onNavigateToActiveOrders else onNavigateToOrders,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeutralSurface,
                    border = BorderStroke(1.dp, BorderSubtle),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ActionGreen.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(20.dp))
                        }

                        Column {
                            Text("Orders Pipeline", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = MainInk)
                            Text(if (newOrdersCount > 0) "$newOrdersCount active orders" else "View all orders", fontSize = 12.sp, color = InkSecondary)
                        }

                        Text("View orders →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ActionGreen)
                    }
                }

                // Morning Rates Card
                Surface(
                    onClick = onNavigateToRates,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeutralSurface,
                    border = BorderStroke(1.dp, BorderSubtle),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ActionGreen.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(20.dp))
                        }

                        Column {
                            Text("Morning Rates", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = MainInk)
                            Text("Set 4 AM भाव", fontSize = 12.sp, color = InkSecondary)
                        }

                        Text("Set rates →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ActionGreen)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Invoices & Billing Card
                Surface(
                    onClick = onNavigateToInvoices,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeutralSurface,
                    border = BorderStroke(1.dp, BorderSubtle),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1976D2).copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                        }

                        Column {
                            Text("GST Invoices", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = MainInk)
                            Text("Bills & PDF slips", fontSize = 12.sp, color = InkSecondary)
                        }

                        Text("View bills →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1976D2))
                    }
                }

                // Khata & Collections Card
                Surface(
                    onClick = onNavigateToKhata,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeutralSurface,
                    border = BorderStroke(1.dp, BorderSubtle),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (marketDues > 0) MutedRedError.copy(alpha = 0.10f) else ActionGreen.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = if (marketDues > 0) MutedRedError else ActionGreen, modifier = Modifier.size(20.dp))
                        }

                        Column {
                            Text("Customer Khata", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = MainInk)
                            Text("UPI QR & dues", fontSize = 12.sp, color = InkSecondary)
                        }

                        Text("Ledger & QR →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (marketDues > 0) MutedRedError else ActionGreen)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer Directory Card
                Surface(
                    onClick = onNavigateToCustomers,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeutralSurface,
                    border = BorderStroke(1.dp, BorderSubtle),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF7B1FA2).copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(20.dp))
                        }

                        Column {
                            Text("Buyers Directory", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = MainInk)
                            Text("Restaurants & limits", fontSize = 12.sp, color = InkSecondary)
                        }

                        Text("Directory →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7B1FA2))
                    }
                }

                // Demand Tally Card
                Surface(
                    onClick = onNavigateToTally,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick(),
                    shape = RoundedCornerShape(16.dp),
                    color = NeutralSurface,
                    border = BorderStroke(1.dp, BorderSubtle),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF57F17).copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                        }

                        Column {
                            Text("Demand Tally", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = MainInk)
                            Text("Mandi procurement", fontSize = 12.sp, color = InkSecondary)
                        }

                        Text("View tally →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFF57F17))
                    }
                }
            }

            // ─── 6. Morning Cutoff Indicator ───
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AmberWarning.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Morning cutoff: 03:00 AM · Orders accepted until cutoff",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = AmberWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
