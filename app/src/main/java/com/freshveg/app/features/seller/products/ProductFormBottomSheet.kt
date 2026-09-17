package com.freshveg.app.features.seller.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.core.network.CategoryDto
import com.freshveg.app.core.network.ProductDto
import com.freshveg.app.core.network.UnitType
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormBottomSheet(
    product: ProductDto?,
    categories: List<CategoryDto>,
    existingProducts: List<ProductDto> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        hindiName: String?,
        categoryId: String?,
        unitType: UnitType,
        currentPrice: Double,
        minimumOrderQuantity: Double,
        isAvailable: Boolean,
        isFeatured: Boolean
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    // Form State
    var name by remember(product) { mutableStateOf(product?.name ?: "") }
    var hindiName by remember(product) { mutableStateOf(product?.hindiName ?: "") }
    var selectedCategoryId by remember(product) { mutableStateOf(product?.categoryId ?: categories.firstOrNull()?.id) }
    var selectedUnitType by remember(product) { mutableStateOf(product?.unitType ?: UnitType.KG) }
    var currentPrice by remember(product) { mutableStateOf(if (product != null) product.currentPrice.toString() else "") }
    var minimumOrderQuantity by remember(product) { mutableStateOf(if (product != null) product.minimumOrderQuantity.toString() else "1.0") }
    var isAvailable by remember(product) { mutableStateOf(product?.isAvailable ?: true) }
    var isFeatured by remember(product) { mutableStateOf(product?.isFeatured ?: false) }

    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isUnitDropdownExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NeutralSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (product != null) "Edit Vegetable (सब्जी एडिट करें)" else "Add Vegetable (सब्जी जोड़ें)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MainInk
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. English Name *
            OutlinedTextField(
                value = name,
                onValueChange = { input ->
                    name = input
                    validationError = null
                },
                label = { Text("Produce Name (English) *") },
                placeholder = { Text("e.g. Tomato, Potato, Spinach, Jameekand...") },
                leadingIcon = { Icon(Icons.Default.Eco, contentDescription = null, tint = ForestGreenPrimary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Duplicacy check strictly in English name if it already exists in the seller's store
            val duplicateInStore = remember(product, name, existingProducts) {
                if (product == null && name.trim().isNotEmpty()) {
                    existingProducts.find { it.name.trim().equals(name.trim(), ignoreCase = true) }
                } else null
            }

            if (duplicateInStore != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MutedRedError.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ \"${duplicateInStore.name}\" already exists in your store catalogue.",
                        color = MutedRedError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Hindi Name (Optional)
            OutlinedTextField(
                value = hindiName,
                onValueChange = { input ->
                    hindiName = input
                    validationError = null
                },
                label = { Text("Produce Name (Hindi / हिंदी - Optional)") },
                placeholder = { Text("e.g. टमाटर, आलू, पालक, जिमीकंद...") },
                leadingIcon = { Icon(Icons.Default.Translate, contentDescription = null, tint = ForestGreenPrimary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

                // 3. Category & Unit Type Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = { isCategoryDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "Select"
                        OutlinedTextField(
                            value = currentCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategoryId = category.id
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Unit Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isUnitDropdownExpanded,
                        onExpandedChange = { isUnitDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedUnitType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitDropdownExpanded) },
                            modifier = Modifier.menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isUnitDropdownExpanded,
                            onDismissRequest = { isUnitDropdownExpanded = false }
                        ) {
                            UnitType.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.name) },
                                    onClick = {
                                        selectedUnitType = unit
                                        isUnitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Daily Selling Price & Min Order Quantity
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPrice,
                        onValueChange = { currentPrice = it; validationError = null },
                        label = { Text("Price (₹) *") },
                        placeholder = { Text("e.g. 40") },
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = minimumOrderQuantity,
                        onValueChange = { minimumOrderQuantity = it },
                        label = { Text("Min Qty *") },
                        placeholder = { Text("1.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Switches (In-Stock & Featured)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("In Stock (Available for Buyers)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Switch(checked = isAvailable, onCheckedChange = { isAvailable = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Featured Produce (Highlight)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Switch(checked = isFeatured, onCheckedChange = { isFeatured = it })
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (product != null && onDelete != null) {
                        OutlinedButton(
                            onClick = { onDelete(product.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedRedError)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }

                    Button(
                        onClick = {
                            val parsedPrice = currentPrice.toDoubleOrNull()
                            val parsedMinQty = minimumOrderQuantity.toDoubleOrNull() ?: 1.0

                            if (name.trim().isEmpty()) {
                                validationError = "Produce name is required"
                                return@Button
                            }
                            if (duplicateInStore != null) {
                                validationError = "\"${duplicateInStore.name}\" already exists in your store catalogue"
                                return@Button
                            }
                            if (parsedPrice == null || parsedPrice <= 0) {
                                validationError = "Please enter a valid price"
                                return@Button
                            }

                            onSave(
                                name.trim(),
                                hindiName.trim().ifEmpty { null },
                                selectedCategoryId,
                                selectedUnitType,
                                parsedPrice,
                                parsedMinQty,
                                isAvailable,
                                isFeatured
                            )
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MainInk)
                    ) {
                        Text(
                            text = if (product != null) "Update Produce" else "Save & Add to Store",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
