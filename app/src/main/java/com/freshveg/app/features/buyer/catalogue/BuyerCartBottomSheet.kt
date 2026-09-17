package com.freshveg.app.features.buyer.catalogue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.core.network.ProductDto
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.animation.bounceClick
import com.freshveg.app.core.ui.animation.rememberTactileHaptic
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.MandiTranslationUtils

data class CartItemDetail(
    val product: ProductDto,
    val quantity: Double
) {
    val lineTotal: Double get() = product.currentPrice * quantity
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerCartBottomSheet(
    cartItems: List<CartItemDetail>,
    onUpdateQuantity: (String, Double) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onDismiss: () -> Unit,
    onPlaceOrder: (String) -> Unit, // deliveryNotes
    isPlacingOrder: Boolean = false
) {
    var deliveryNotes by remember { mutableStateOf("") }
    val estimatedTotal = cartItems.sumOf { it.lineTotal }
    val totalSavings = cartItems.sumOf { (it.product.discountAmountPerUnit ?: 0.0) * it.quantity }
    val triggerHaptic = rememberTactileHaptic()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NeutralSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: Title & Clear All Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Review Produce Cart",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = "${cartItems.size} produce items for morning delivery",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }

                TextButton(
                    onClick = {
                        triggerHaptic()
                        onClearCart()
                    }
                ) {
                    Text(
                        text = "Clear All",
                        color = MutedRedError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(10.dp))

            // Cart Items Scrollable List
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cartItems, key = { it.product.id }) { item ->
                    val product = item.product
                    val safeName = product.safeName
                    val hindiName = product.hindiName ?: MandiTranslationUtils.translateEnglishToHindi(safeName)
                    val unitName = product.unitType?.name ?: "KG"

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SecondarySurface,
                        border = BorderStroke(0.5.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Produce Vector Thumbnail & Bilingual Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                ProduceThumbnailBadge(
                                    name = safeName,
                                    hindiName = hindiName,
                                    imageUrl = product.imageUrl,
                                    size = 46.dp,
                                    cornerRadius = 10.dp
                                )
                                Column {
                                    Text(
                                        text = safeName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MainInk
                                    )
                                    if (hindiName != null) {
                                        Text(
                                            text = hindiName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InkSecondary
                                        )
                                    }
                                    Text(
                                        text = "₹${product.currentPrice.toInt()} / $unitName",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ActionGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Stepper (- [Qty] +) & Line Total
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Decrement / Delete
                                IconButton(
                                    onClick = {
                                        triggerHaptic()
                                        if (item.quantity <= 1.0) {
                                            onRemoveItem(product.id)
                                        } else {
                                            onUpdateQuantity(product.id, item.quantity - 1.0)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeutralSurface)
                                ) {
                                    Icon(
                                        imageVector = if (item.quantity <= 1.0) Icons.Outlined.Delete else Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = if (item.quantity <= 1.0) MutedRedError else MainInk,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = "${item.quantity.toInt()} $unitName",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MainInk,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )

                                // Increment
                                IconButton(
                                    onClick = {
                                        triggerHaptic()
                                        onUpdateQuantity(product.id, item.quantity + 1.0)
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ActionGreen)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = "₹${item.lineTotal.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MainInk
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Zero-Trust Wholesale Rate Card & Schedule Trust Box
            Surface(
                color = ActionGreen.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = ActionGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Verified Wholesale Rate Card",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MainInk
                        )
                        Text(
                            text = "Standard delivery tomorrow 05:00 AM – 07:00 AM. Final invoice calculated upon physical scale weighment.",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kitchen Delivery Notes TextField
            OutlinedTextField(
                value = deliveryNotes,
                onValueChange = { deliveryNotes = it },
                label = { Text("Kitchen Delivery Instructions (Optional)") },
                placeholder = { Text("e.g. Call receiving chef, gate #2") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ActionGreen,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = SecondarySurface,
                    unfocusedContainerColor = SecondarySurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Bill Breakdown & Primary Submit CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (totalSavings > 0.0) {
                        Text(
                            text = "🎉 Total Savings: -₹${totalSavings.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActionGreen
                        )
                    }
                    Text(
                        text = "Estimated Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkTertiary
                    )
                    Text(
                        text = "₹${estimatedTotal.toInt()}.00",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                }

                // Place Order Button (Strictly transmits only products and quantities)
                Button(
                    onClick = {
                        triggerHaptic()
                        onPlaceOrder(deliveryNotes)
                    },
                    enabled = cartItems.isNotEmpty() && !isPlacingOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .padding(start = 16.dp)
                ) {
                    if (isPlacingOrder) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Place Morning Order",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

