package com.freshveg.app.features.seller.customers

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.freshveg.app.core.network.CustomerDto
import com.freshveg.app.core.ui.theme.CardSurface
import com.freshveg.app.core.ui.theme.ForestGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileBottomSheet(
    customer: CustomerDto,
    outstandingDues: Double = 0.0,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Business Name + Active Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.businessName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    Text(
                        text = "Contact: ${customer.primaryContactName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (customer.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (customer.isActive) "ACTIVE" else "INACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (customer.isActive) ForestGreenPrimary else Color(0xFFD32F2F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Contact Action Bar (Call, WhatsApp, Maps, Edit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Call
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.mobile}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 12.sp)
                }

                // WhatsApp
                OutlinedButton(
                    onClick = {
                        try {
                            val cleanMobile = customer.mobile.replace("+91", "").replace(" ", "")
                            val url = "https://wa.me/91$cleanMobile"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat", fontSize = 12.sp)
                }

                // Edit
                Button(
                    onClick = onEdit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(14.dp))

            // Details Card
            Surface(
                color = Color(0xFFF9FBF9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Mobile
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Phone Number", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text("+91 ${customer.mobile}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Address
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(20.dp))
                        Column {
                            Text("Delivery Address", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(customer.address, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    // Credit Limit & Outstanding Dues
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Credit Limit", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = if (customer.creditLimitAmount > 0) "₹${customer.creditLimitAmount.toInt()}" else "No limit",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Current Dues", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = if (outstandingDues > 0) "₹${outstandingDues.toInt()}" else "₹0 (Clear)",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (outstandingDues > 0) Color(0xFFD32F2F) else ForestGreenPrimary
                            )
                        }
                    }

                    // Discount Policy
                    if (customer.discountType != null && customer.discountType != "NONE" && (customer.discountValue ?: 0.0) > 0.0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Sell, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Customer Discount Policy", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(
                                    text = if (customer.discountType == "PERCENTAGE") "${customer.discountValue?.toInt()}% OFF on all produce" else "₹${customer.discountValue?.toInt()}/KG Flat OFF on base rates",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreenPrimary
                                )
                                if (!customer.discountNotes.isNullOrBlank()) {
                                    Text(
                                        text = "Note: ${customer.discountNotes}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    // GSTIN
                    if (!customer.gstNumber.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Column {
                                Text("GSTIN", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(customer.gstNumber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Notes
                    if (!customer.notes.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Kitchen Delivery Notes", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(customer.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Delete Customer (with Confirmation)
            if (!showDeleteConfirm) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Customer Account", fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Are you sure you want to delete ${customer.businessName}?",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDelete,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm Delete")
                            }
                            OutlinedButton(
                                onClick = { showDeleteConfirm = false },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}
