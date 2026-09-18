package com.freshveg.app.features.seller.orders

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.R
import com.freshveg.app.core.network.FulfillOrderItem
import com.freshveg.app.core.network.OrderDto
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.ui.animation.bounceClick
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.MandiTranslationUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FulfillOrderBottomSheet(
    order: OrderDto,
    isSubmitting: Boolean,
    serverError: String? = null,
    onConfirmFulfill: (List<FulfillOrderItem>) -> Unit,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val focusManager = LocalFocusManager.current

    fun triggerHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    // Pre-fill 100% of weights with ordered weights or existing weighed quantities
    val weightsMap = remember(order) {
        mutableStateMapOf<String, String>().apply {
            order.items.forEach { item ->
                val initVal = if ((item.weighedQuantity ?: 0.0) > 0.0) {
                    ProduceVisualUtils.formatQuantityValue(item.weighedQuantity!!)
                } else {
                    ProduceVisualUtils.formatQuantityValue(item.quantity)
                }
                put(item.id, initVal)
            }
        }
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    // Live calculated gross and net billed amounts
    val calculatedTotal = remember(weightsMap.toMap()) {
        var sum = 0.0
        order.items.forEach { item ->
            val w = weightsMap[item.id]?.toDoubleOrNull() ?: item.quantity
            val eff = item.effectivePrice
            sum += (eff * w)
        }
        sum
    }

    val orderedTotal = remember(order) {
        order.totalAmount
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.seller_scale_fulfill_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = "Order #${order.id.takeLast(6)} • ${order.customer?.businessName ?: order.customer?.primaryContactName ?: "Buyer"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ActionGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${order.items.size} Items",
                        fontWeight = FontWeight.Bold,
                        color = ActionGreen,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ⚡ 1-Tap Fast Fulfill Button (All Weights Match)
            ElevatedCard(
                onClick = {
                    triggerHaptic()
                    val fulfillmentList = order.items.map { item ->
                        FulfillOrderItem(
                            orderItemId = item.id,
                            deliveredQuantity = item.quantity
                        )
                    }
                    onConfirmFulfill(fulfillmentList)
                },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = ActionGreen),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚡", fontSize = 18.sp)
                        Column {
                            Text(
                                text = stringResource(R.string.seller_fast_fulfill_all),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                color = Color.White
                            )
                            Text(
                                text = "1-Tap fast track • No weight edits needed",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Produce Items List with Exception Steppers
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(order.items, key = { it.id }) { item ->
                    val unit = item.unitSnapshot.lowercase()
                    val currentValStr = weightsMap[item.id] ?: ProduceVisualUtils.formatQuantityValue(item.quantity)
                    val currentVal = currentValStr.toDoubleOrNull() ?: item.quantity
                    val variance = currentVal - item.quantity
                    val itemName = item.displayName
                    val hindiName = item.hindiName ?: MandiTranslationUtils.translateEnglishToHindi(itemName)
                    val bringIntoViewRequester = remember { BringIntoViewRequester() }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NeutralSurface,
                        border = BorderStroke(0.75.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(bringIntoViewRequester)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProduceThumbnailBadge(
                                    name = itemName,
                                    hindiName = hindiName,
                                    size = 46.dp,
                                    cornerRadius = 8.dp
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = itemName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MainInk
                                    )
                                    Text(
                                        text = "Ordered: ${ProduceVisualUtils.formatQuantity(item.quantity, unit)} @ ₹${item.effectivePrice.toInt()}/$unit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = InkSecondary
                                    )
                                }

                                // Scale Weight Text Input Box
                                OutlinedTextField(
                                    value = currentValStr,
                                    onValueChange = { newVal ->
                                        val filtered = newVal.filter { it.isDigit() || it == '.' }
                                        weightsMap[item.id] = filtered
                                        validationError = null
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Done
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MainInk
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ActionGreen,
                                        unfocusedBorderColor = BorderSubtle,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.width(80.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Variance Indicator & Exception Quick Steppers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Real-time Tare Variance Badge
                                if (Math.abs(variance) < 0.001) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = ActionGreen.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.seller_tare_zero),
                                            color = ActionGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    val sign = if (variance > 0) "+" else ""
                                    val varianceStr = String.format(Locale.US, "%s%.2f", sign, variance).trimEnd('0').trimEnd('.')
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (variance > 0) Color(0xFFFFF3E0) else Color(0xFFF3E5F5)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.seller_tare_diff, varianceStr),
                                            color = if (variance > 0) Color(0xFFE65100) else Color(0xFF7B1FA2),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // Quick Stepper Chips ([-0.5], [-0.1], [Reset], [+0.1], [+0.5])
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(-0.5, -0.1).forEach { delta ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SecondarySurface,
                                            border = BorderStroke(0.5.dp, BorderSubtle),
                                            modifier = Modifier.bounceClick {
                                                triggerHaptic()
                                                val updated = (currentVal + delta).coerceAtLeast(0.1)
                                                weightsMap[item.id] = ProduceVisualUtils.formatQuantityValue(updated)
                                            }
                                        ) {
                                            Text(
                                                text = "${delta}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MainInk,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ActionGreen.copy(alpha = 0.1f),
                                        modifier = Modifier.bounceClick {
                                            triggerHaptic()
                                            weightsMap[item.id] = ProduceVisualUtils.formatQuantityValue(item.quantity)
                                        }
                                    ) {
                                        Text(
                                            text = "Reset",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ActionGreen,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    listOf(0.1, 0.5).forEach { delta ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SecondarySurface,
                                            border = BorderStroke(0.5.dp, BorderSubtle),
                                            modifier = Modifier.bounceClick {
                                                triggerHaptic()
                                                val updated = currentVal + delta
                                                weightsMap[item.id] = ProduceVisualUtils.formatQuantityValue(updated)
                                            }
                                        ) {
                                            Text(
                                                text = "+${delta}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MainInk,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calculated Billed Total Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SecondarySurface,
                border = BorderStroke(0.75.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Original Ordered: ${ProduceVisualUtils.formatCurrency(orderedTotal)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkTertiary
                        )
                        Text(
                            text = "Final Weighed Bill Amount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MainInk
                        )
                    }

                    Text(
                        text = ProduceVisualUtils.formatCurrency(calculatedTotal),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ActionGreen
                    )
                }
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = validationError!!,
                    color = MutedRedError,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (serverError != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠️ $serverError",
                    color = MutedRedError,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Confirm & Dispatch Button
            Button(
                onClick = {
                    triggerHaptic()
                    val fulfillmentList = mutableListOf<FulfillOrderItem>()
                    for (item in order.items) {
                        val textVal = weightsMap[item.id]
                        val parsedVal = textVal?.toDoubleOrNull()
                        if (parsedVal == null || parsedVal <= 0) {
                            validationError = "Please enter valid scale weight for ${item.displayName}"
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
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.seller_fulfill_confirm),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
