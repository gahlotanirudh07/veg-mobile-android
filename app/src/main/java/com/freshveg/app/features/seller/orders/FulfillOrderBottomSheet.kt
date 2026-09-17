package com.freshveg.app.features.seller.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.core.network.FulfillOrderItem
import com.freshveg.app.core.network.OrderDto
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulfillOrderBottomSheet(
    order: OrderDto,
    serverError: String? = null,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmFulfill: (List<FulfillOrderItem>) -> Unit
) {
    // Map of orderItemId to current weighed text
    val weightsMap = remember(order) {
        mutableStateMapOf<String, String>().apply {
            order.items.forEach { item ->
                put(item.id, (item.deliveredQuantity ?: item.quantity).toString())
            }
        }
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    val orderedTotal = remember(order) {
        order.items.sumOf { it.quantity * it.price }
    }

    val deliveredGross = order.items.sumOf { item ->
        val weight = weightsMap[item.id]?.toDoubleOrNull() ?: item.quantity
        val base = item.basePriceSnapshot ?: item.price
        weight * base
    }

    val discountSaved = order.items.sumOf { item ->
        val weight = weightsMap[item.id]?.toDoubleOrNull() ?: item.quantity
        val base = item.basePriceSnapshot ?: item.price
        val disc = item.discountAmount ?: maxOf(0.0, base - item.price)
        weight * disc
    }

    val calculatedTotal = order.items.sumOf { item ->
        val weight = weightsMap[item.id]?.toDoubleOrNull() ?: item.quantity
        weight * item.price
    }

    val varianceItems = order.items.mapNotNull { item ->
        val weight = weightsMap[item.id]?.toDoubleOrNull()
        if (weight != null && Math.abs(weight - item.quantity) > 0.001) {
            item to (weight - item.quantity)
        } else null
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (order.status == "FULFILLED") "Edit Fulfillment (Adjust Weights)" else "Fulfill Order (Delivered Weights)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = "Order #${order.orderNumber} • ${order.customer?.businessName ?: "Customer"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkSecondary
                    )
                }
                Icon(Icons.Default.Scale, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Live Quantity Variance Alert Banner
            if (varianceItems.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF8E1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scale Weight Variance Detected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Supplied scale weights differ from ordered quantity for ${varianceItems.size} item(s). Live bill totals have been adjusted:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D4037)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            varianceItems.take(3).forEach { (item, diff) ->
                                val w = weightsMap[item.id] ?: ""
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (diff > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ) {
                                    Text(
                                        text = "${item.displayName}: ${item.quantity} → $w ${item.unitTypeSnapshot} (${if (diff > 0) "+$diff" else "$diff"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (diff > 0) ActionGreen else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = "Enter actual weight delivered for each item:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Vegetable Weighed Input List
            order.items.forEach { item ->
                val itemName = item.displayName
                val unit = item.unitTypeSnapshot ?: "KG"
                val currentWeightText = weightsMap[item.id] ?: ""
                val lineTotal = (currentWeightText.toDoubleOrNull() ?: 0.0) * item.price
                val enteredWeight = currentWeightText.toDoubleOrNull()
                
                // Smart Typo Detection: e.g. entered 262 for ordered 25 -> suggested 26.2
                val suggestedDecimal = remember(currentWeightText, item.quantity) {
                    if (enteredWeight != null && enteredWeight >= item.quantity * 4 && currentWeightText.length >= 2 && !currentWeightText.contains('.')) {
                        // Insert decimal before last digit: e.g. "262" -> "26.2"
                        val candidate = "${currentWeightText.dropLast(1)}.${currentWeightText.takeLast(1)}".toDoubleOrNull()
                        if (candidate != null && candidate >= item.quantity * 0.5 && candidate <= item.quantity * 2.0) {
                            "${currentWeightText.dropLast(1)}.${currentWeightText.takeLast(1)}"
                        } else null
                    } else null
                }
                
                val hasLargeDiscrepancy = enteredWeight != null && (enteredWeight > item.quantity * 2.0 || (enteredWeight < item.quantity * 0.3 && enteredWeight > 0))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SecondarySurface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ProduceThumbnailBadge(
                                name = itemName,
                                hindiName = item.hindiName,
                                imageUrl = null,
                                size = 44.dp,
                                cornerRadius = 8.dp
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MainInk
                                )
                                Text(
                                    text = "Ordered: ${item.quantity} $unit • ₹${item.price.toInt()}/$unit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSecondary
                                )
                                if (lineTotal > 0) {
                                    Text(
                                        text = "Line Total: ₹${lineTotal.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ActionGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Delivered Quantity Input Field
                            OutlinedTextField(
                                value = currentWeightText,
                                onValueChange = { input ->
                                    weightsMap[item.id] = input
                                    validationError = null
                                },
                                label = { Text("Delivered (${item.unitTypeSnapshot})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.width(110.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // Typo Auto-Fix Alert Banner
                        if (suggestedDecimal != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF3E0),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Typo? Ordered ${item.quantity} $unit. Mean $suggestedDecimal $unit?",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE65100),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            weightsMap[item.id] = suggestedDecimal
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "Auto-Fix",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ActionGreen
                                        )
                                    }
                                }
                            }
                        } else if (hasLargeDiscrepancy) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFEBEE),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "High variance: ${enteredWeight} $unit vs ordered ${item.quantity} $unit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFC62828),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live 4-Tier Calculated Billed Total Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Original Ordered Total:", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                        Text("₹${orderedTotal.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = InkSecondary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Delivered Total (Actual):", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        Text("₹${deliveredGross.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }

                    if (discountSaved > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discount Saved:", style = MaterialTheme.typography.bodySmall, color = ActionGreen, fontWeight = FontWeight.Bold)
                            Text("-₹${discountSaved.toInt()}", style = MaterialTheme.typography.bodySmall, color = ActionGreen, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Final Billed Total:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MainInk
                        )
                        Text(
                            text = "₹${calculatedTotal.toInt()}",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            color = ActionGreen
                        )
                    }
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (serverError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ $serverError",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    val fulfillmentList = mutableListOf<FulfillOrderItem>()
                    for (item in order.items) {
                        val textVal = weightsMap[item.id]
                        val parsedVal = textVal?.toDoubleOrNull()
                        if (parsedVal == null || parsedVal <= 0) {
                            validationError = "Please enter valid delivered quantity for ${item.productNameSnapshot}"
                            return@Button
                        }
                        fulfillmentList.add(
                            FulfillOrderItem(
                                orderItemId = item.id,
                                deliveredQuantity = parsedVal
                            )
                        )
                    }
                    onConfirmFulfill(fulfillmentList)
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Updating...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (order.status == "FULFILLED") "Save Updated Weights" else "Complete Order & Fulfill",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
