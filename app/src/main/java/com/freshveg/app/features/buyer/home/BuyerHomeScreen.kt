package com.freshveg.app.features.buyer.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.freshveg.app.R
import androidx.compose.ui.res.stringResource
import com.freshveg.app.core.network.ConnectedSellerDto
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.animation.bounceClick
import com.freshveg.app.core.ui.animation.rememberTactileHaptic
import com.freshveg.app.core.ui.theme.*

data class QuickProduceItem(
    val name: String,
    val hindiName: String,
    val unit: String = "KG",
    val defaultQty: String = "10"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerHomeScreen(
    connectedSeller: ConnectedSellerDto?,
    cutoffTime: String = "03:00 AM",
    activeOrdersCount: Int = 0,
    outstandingDues: Double = 0.0,
    onNavigateToCatalogue: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val triggerHaptic = rememberTactileHaptic()

    val quickReorderItems = remember {
        listOf(
            QuickProduceItem("Tomato Hybrid", "टमाटर हाइब्रिड", "KG", "25"),
            QuickProduceItem("Potato Agra", "आलू आगरा", "KG", "50"),
            QuickProduceItem("Onion Nashik", "प्याज नासिक", "KG", "50"),
            QuickProduceItem("Green Chilli", "हरी मिर्च", "KG", "5"),
            QuickProduceItem("Ginger Special", "अदरक स्पेशल", "KG", "5"),
            QuickProduceItem("Coriander", "धनिया हरा", "KG", "3")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProduceThumbnailBadge(
                            name = "spinach",
                            size = 38.dp,
                            cornerRadius = 10.dp
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.buyer_home_header_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MainInk
                            )
                            Text(
                                text = stringResource(R.string.buyer_home_header_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeutralSurface
                )
            )
        },
        containerColor = BackgroundCanvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connected Seller & Dynamic Cutoff Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NeutralSurface,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ActionGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = ActionGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = connectedSeller?.businessName ?: "Ultra Veggies Wholesale",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MainInk
                                )
                                Text(
                                    text = stringResource(R.string.buyer_supplier_code, connectedSeller?.sellerCode ?: "SEL-EFBC95C3"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkTertiary
                                )
                            }
                        }

                        // Call Vendor CTA
                        IconButton(
                            onClick = {
                                triggerHaptic()
                                connectedSeller?.mobile?.let { phone ->
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                    } catch (_: Exception) {}
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SecondarySurface)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = "Call Seller",
                                tint = ActionGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Cutoff Pill with live status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberWarning.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.buyer_order_cutoff, cutoffTime),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberWarning
                        )
                    }
                }
            }

            // 2. Dominant Hero Action: "Place Today's Order"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick {
                        triggerHaptic()
                        onNavigateToCatalogue()
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MainInk),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Trust Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(HarvestLime)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.buyer_rates_verified),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MainInk
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = HarvestLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.buyer_hero_order_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.buyer_hero_order_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        // CTA Button Inside Hero
                        Button(
                            onClick = {
                                triggerHaptic()
                                onNavigateToCatalogue()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ActionGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.buyer_browse_full_catalogue),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            // 3. Fast Re-Order Carousel ("Buy Again")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.buyer_frequent_reorder),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = stringResource(R.string.common_view_all),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ActionGreen,
                        modifier = Modifier.clickable {
                            triggerHaptic()
                            onNavigateToCatalogue()
                        }
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(quickReorderItems) { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = NeutralSurface,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .width(135.dp)
                                .bounceClick {
                                    triggerHaptic()
                                    onNavigateToCatalogue()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ProduceThumbnailBadge(
                                    name = item.name,
                                    hindiName = item.hindiName,
                                    size = 54.dp,
                                    cornerRadius = 12.dp
                                )
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MainInk,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.hindiName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Freq: ${item.defaultQty} ${item.unit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ActionGreen
                                )
                                Button(
                                    onClick = {
                                        triggerHaptic()
                                        onNavigateToCatalogue()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SecondarySurface,
                                        contentColor = MainInk
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add",
                                        modifier = Modifier.size(14.dp),
                                        tint = ActionGreen
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Live Orders Tracking Snapshot (Single Glance Progress)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NeutralSurface,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick {
                        triggerHaptic()
                        onNavigateToOrders()
                    }
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ActionGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalShipping,
                                    contentDescription = null,
                                    tint = ActionGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.buyer_active_orders),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MainInk
                                )
                                Text(
                                    text = if (activeOrdersCount > 0) "$activeOrdersCount active order in progress" else "No pending dispatch right now",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = InkTertiary
                        )
                    }

                    if (activeOrdersCount > 0) {
                        // Linear Step Indicator: Received -> Weighed -> Invoiced -> Dispatched
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SecondarySurface)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Received", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ActionGreen)
                            Text("→", fontSize = 11.sp, color = InkTertiary)
                            Text("Weighed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ActionGreen)
                            Text("→", fontSize = 11.sp, color = InkTertiary)
                            Text("Invoiced", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = InkSecondary)
                            Text("→", fontSize = 11.sp, color = InkTertiary)
                            Text("Dispatched", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = InkSecondary)
                        }
                    }
                }
            }

            // 5. Khata & Ledger Snapshot
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = NeutralSurface,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick {
                        triggerHaptic()
                        onNavigateToInvoices()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (outstandingDues > 0) MutedRedError.copy(alpha = 0.12f) else ActionGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = if (outstandingDues > 0) MutedRedError else ActionGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Khata & Running Balance",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MainInk
                            )
                            Text(
                                text = if (outstandingDues > 0) "Outstanding Due: ₹${outstandingDues.toInt()}" else "All Invoices Settled (₹0.00)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (outstandingDues > 0) MutedRedError else ActionGreen
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = InkTertiary
                    )
                }
            }

            // 6. Wholesale Services & App Capabilities Directory
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Wholesale Services & Capabilities (मंडी सेवाएं)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MainInk
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Fresh Catalogue Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick {
                                triggerHaptic()
                                onNavigateToCatalogue()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = NeutralSurface,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ActionGreen.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Storefront, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(20.dp))
                            }
                            Text("Fresh Mandi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MainInk)
                            Text("60+ daily wholesale produce directly from mandi", fontSize = 11.5.sp, color = InkSecondary, lineHeight = 16.sp)
                            Text("Browse →", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = ActionGreen)
                        }
                    }

                    // Live Scale Weighment Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick {
                                triggerHaptic()
                                onNavigateToOrders()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = NeutralSurface,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(HarvestLime.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Scale, contentDescription = null, tint = MainInk, modifier = Modifier.size(20.dp))
                            }
                            Text("Live Weighment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MainInk)
                            Text("Scale weights verified before crate dispatch", fontSize = 11.5.sp, color = InkSecondary, lineHeight = 16.sp)
                            Text("Track orders →", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = ActionGreen)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // GST Tax Invoices Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick {
                                triggerHaptic()
                                onNavigateToInvoices()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = NeutralSurface,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1976D2).copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                            }
                            Text("GST Tax Bills", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MainInk)
                            Text("Compliant tax invoices & instant PDF slips", fontSize = 11.5.sp, color = InkSecondary, lineHeight = 16.sp)
                            Text("View bills →", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1976D2))
                        }
                    }

                    // Khata & Ledger Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .bounceClick {
                                triggerHaptic()
                                onNavigateToInvoices()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = NeutralSurface,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AmberWarning.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                            }
                            Text("Khata & Ledger", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MainInk)
                            Text("Running balance, payments & statement PDF", fontSize = 11.5.sp, color = InkSecondary, lineHeight = 16.sp)
                            Text("View khata →", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = AmberWarning)
                        }
                    }
                }
            }

            // 7. Direct Supplier Desk (WhatsApp & Call)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SecondarySurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Direct Wholesale Helpdesk",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MainInk
                        )
                        Text(
                            text = "Need special crates or customized grading?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    }

                    IconButton(
                        onClick = {
                            triggerHaptic()
                            connectedSeller?.mobile?.let { phone ->
                                try {
                                    val cleanPhone = phone.replace("+", "").replace(" ", "").trim()
                                    val url = "https://api.whatsapp.com/send?phone=91$cleanPhone&text=Hello%20MandiExpress%20Wholesale"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NeutralSurface)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "WhatsApp",
                            tint = ActionGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

