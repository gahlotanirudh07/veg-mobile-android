package com.freshveg.app.features.seller.tally

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
import com.freshveg.app.core.network.CreateProducePurchaseItemDto
import com.freshveg.app.core.network.ProductDto
import com.freshveg.app.core.ui.theme.CardSurface
import com.freshveg.app.core.ui.theme.FarmGreenSecondary
import com.freshveg.app.core.ui.theme.ForestGreenPrimary

data class InwardItemRow(
    val productId: String,
    val productName: String,
    val quantity: String,
    val buyingRate: String,
    val unitType: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProducePurchaseBottomSheet(
    products: List<ProductDto>,
    onDismiss: () -> Unit,
    onSubmit: (supplierName: String, supplierMobile: String?, items: List<CreateProducePurchaseItemDto>) -> Unit
) {
    var supplierName by remember { mutableStateOf("") }
    var supplierMobile by remember { mutableStateOf("") }

    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var isProductDropdownExpanded by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf("") }
    var rateText by remember { mutableStateOf("") }

    var addedItems by remember { mutableStateOf(listOf<InwardItemRow>()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val grandTotal = addedItems.sumOf { (it.quantity.toDoubleOrNull() ?: 0.0) * (it.buyingRate.toDoubleOrNull() ?: 0.0) }

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
            Text(
                text = "Log Inward Produce Purchase",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary
            )
            Text(
                text = "Record bulk mandi purchase from farmer or commission agent",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Supplier / Farmer Name
            OutlinedTextField(
                value = supplierName,
                onValueChange = { supplierName = it; validationError = null },
                label = { Text("Farmer / Supplier Name *") },
                placeholder = { Text("e.g. Kisan Suresh Patel, Arhat No. 42") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Supplier Mobile
            OutlinedTextField(
                value = supplierMobile,
                onValueChange = { supplierMobile = it },
                label = { Text("Supplier Mobile (Optional)") },
                placeholder = { Text("10-digit mobile number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Add Purchased Vegetables:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Product Dropdown
            ExposedDropdownMenuBox(
                expanded = isProductDropdownExpanded,
                onExpandedChange = { isProductDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentTitle = selectedProduct?.let {
                    if (!it.hindiName.isNullOrEmpty()) "${it.name} (${it.hindiName})" else it.name
                } ?: "Select Produce"

                OutlinedTextField(
                    value = currentTitle,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Vegetable") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProductDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = isProductDropdownExpanded,
                    onDismissRequest = { isProductDropdownExpanded = false }
                ) {
                    products.forEach { prod ->
                        val title = if (!prod.hindiName.isNullOrEmpty()) "${prod.name} (${prod.hindiName})" else prod.name
                        DropdownMenuItem(
                            text = { Text(title) },
                            onClick = {
                                selectedProduct = prod
                                isProductDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quantity & Buying Rate Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it; validationError = null },
                    label = { Text("Quantity (${selectedProduct?.unitType?.name ?: "KG"}) *") },
                    placeholder = { Text("e.g. 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it; validationError = null },
                    label = { Text("Buying Rate (₹) *") },
                    placeholder = { Text("e.g. 24") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add Item Button
            OutlinedButton(
                onClick = {
                    val p = selectedProduct
                    if (p == null) {
                        validationError = "Please select a product"
                        return@OutlinedButton
                    }
                    val q = qtyText.toDoubleOrNull()
                    if (q == null || q <= 0) {
                        validationError = "Please enter a valid quantity"
                        return@OutlinedButton
                    }
                    val r = rateText.toDoubleOrNull()
                    if (r == null || r <= 0) {
                        validationError = "Please enter a valid buying rate"
                        return@OutlinedButton
                    }

                    addedItems = addedItems + InwardItemRow(
                        productId = p.id,
                        productName = if (!p.hindiName.isNullOrEmpty()) "${p.name} (${p.hindiName})" else p.name,
                        quantity = q.toString(),
                        buyingRate = r.toString(),
                        unitType = p.unitType.name
                    )

                    qtyText = ""
                    rateText = ""
                    validationError = null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add to Inward Bill")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display Added Items
            if (addedItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FBF8), shape = RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Items in this Purchase (${addedItems.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )

                    addedItems.forEachIndexed { index, item ->
                        val lineTotal = (item.quantity.toDoubleOrNull() ?: 0.0) * (item.buyingRate.toDoubleOrNull() ?: 0.0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${item.quantity} ${item.unitType} × ₹${item.buyingRate} = ₹${lineTotal.toInt()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                            IconButton(onClick = {
                                addedItems = addedItems.filterIndexed { i, _ -> i != index }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }

                    Divider(color = Color(0xFFE0E0E0))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grand Total:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("₹${grandTotal.toInt()}", fontWeight = FontWeight.ExtraBold, color = ForestGreenPrimary, style = MaterialTheme.typography.titleLarge)
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

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    if (supplierName.isBlank()) {
                        validationError = "Please enter supplier or farmer name"
                        return@Button
                    }
                    if (addedItems.isEmpty()) {
                        validationError = "Please add at least one vegetable item"
                        return@Button
                    }
                    val dtos = addedItems.map {
                        CreateProducePurchaseItemDto(
                            productId = it.productId,
                            quantity = it.quantity.toDouble(),
                            unitType = it.unitType,
                            buyingPricePerUnit = it.buyingRate.toDouble()
                        )
                    }
                    onSubmit(supplierName.trim(), supplierMobile.trim().ifEmpty { null }, dtos)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Inward Purchase", fontWeight = FontWeight.Bold)
            }
        }
    }
}
