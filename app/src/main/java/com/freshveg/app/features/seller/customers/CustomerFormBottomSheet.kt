package com.freshveg.app.features.seller.customers

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
import com.freshveg.app.core.network.CreateCustomerRequest
import com.freshveg.app.core.network.CustomerDto
import com.freshveg.app.core.network.UpdateCustomerRequest
import com.freshveg.app.core.ui.theme.CardSurface
import com.freshveg.app.core.ui.theme.ForestGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFormBottomSheet(
    customer: CustomerDto? = null, // null for Add, non-null for Edit
    onDismiss: () -> Unit,
    onSaveAdd: (CreateCustomerRequest) -> Unit,
    onSaveEdit: (String, UpdateCustomerRequest) -> Unit
) {
    val isEdit = customer != null

    var businessName by remember { mutableStateOf(customer?.businessName ?: "") }
    var primaryContactName by remember { mutableStateOf(customer?.primaryContactName ?: "") }
    var mobile by remember { mutableStateOf(customer?.mobile ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var gstNumber by remember { mutableStateOf(customer?.gstNumber ?: "") }
    var creditLimitText by remember { mutableStateOf(customer?.creditLimit ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var discountType by remember { mutableStateOf(customer?.discountType ?: "NONE") }
    var discountValueText by remember { mutableStateOf(customer?.discountValue?.let { if (it > 0) it.toString() else "" } ?: "") }
    var discountNotes by remember { mutableStateOf(customer?.discountNotes ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEdit) "Edit Restaurant Profile" else "Add New Restaurant / Buyer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )
                    Text(
                        text = if (isEdit) "Update contact, discount and credit limit" else "Onboard a new restaurant to your Mandi portal",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Icon(
                    imageVector = if (isEdit) Icons.Default.Edit else Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = ForestGreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Business Name
            OutlinedTextField(
                value = businessName,
                onValueChange = {
                    businessName = it
                    validationError = null
                },
                label = { Text("Restaurant / Business Name*") },
                placeholder = { Text("e.g. Hotel Royal Spice") },
                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = ForestGreenPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Primary Contact Name (Owner / Chef)
            OutlinedTextField(
                value = primaryContactName,
                onValueChange = {
                    primaryContactName = it
                    validationError = null
                },
                label = { Text("Contact Person (Owner / Head Chef)*") },
                placeholder = { Text("e.g. Amit Sharma") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ForestGreenPrimary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Mobile Number
            OutlinedTextField(
                value = mobile,
                onValueChange = {
                    mobile = it
                    validationError = null
                },
                label = { Text("Mobile Number (10 digits)*") },
                placeholder = { Text("e.g. 9876543210") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ForestGreenPrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Special Customer Discount Policy Section
            Text(
                text = "🏷️ Special Pricing & Customer Discount",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Discount Type Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = discountType == "NONE",
                    onClick = { discountType = "NONE" },
                    label = { Text("Standard", fontSize = 12.sp, color = if (discountType == "NONE") Color.White else ForestGreenPrimary) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = discountType == "PERCENTAGE",
                    onClick = { discountType = "PERCENTAGE" },
                    label = { Text("% Discount", fontSize = 12.sp, color = if (discountType == "PERCENTAGE") Color.White else ForestGreenPrimary) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = discountType == "RUPEES_PER_UNIT",
                    onClick = { discountType = "RUPEES_PER_UNIT" },
                    label = { Text("₹/KG Flat", fontSize = 12.sp, color = if (discountType == "RUPEES_PER_UNIT") Color.White else ForestGreenPrimary) },
                    shape = RoundedCornerShape(8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ForestGreenPrimary,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (discountType != "NONE") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = discountValueText,
                    onValueChange = { discountValueText = it },
                    label = { Text(if (discountType == "PERCENTAGE") "Discount Percentage (%)" else "Discount Amount (₹/KG)") },
                    placeholder = { Text(if (discountType == "PERCENTAGE") "e.g. 5 for 5% off" else "e.g. 2 for ₹2/kg off") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = discountNotes,
                    onValueChange = { discountNotes = it },
                    label = { Text("Discount Notes / Agreement") },
                    placeholder = { Text("e.g. Institutional buyer tier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                val valNum = discountValueText.toDoubleOrNull() ?: 0.0
                val sampleBase = 50.0
                val discounted = if (discountType == "PERCENTAGE") {
                    Math.max(0.0, sampleBase - (sampleBase * valNum / 100.0))
                } else {
                    Math.max(0.0, sampleBase - valNum)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Preview: ₹${sampleBase.toInt()}/kg produce will be priced at ₹${discounted.toInt()}/kg for this buyer.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = ForestGreenPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Delivery / Kitchen Address
            OutlinedTextField(
                value = address,
                onValueChange = {
                    address = it
                    validationError = null
                },
                label = { Text("Delivery / Kitchen Address*") },
                placeholder = { Text("e.g. Plot 14, Main Market, Sector 18") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = ForestGreenPrimary) },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Credit Limit (₹) & GSTIN (2-column row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = creditLimitText,
                    onValueChange = { creditLimitText = it },
                    label = { Text("Credit Limit (₹)") },
                    placeholder = { Text("50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = gstNumber,
                    onValueChange = { gstNumber = it },
                    label = { Text("GSTIN (Optional)") },
                    placeholder = { Text("07AAAAA0000A1Z5") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. Notes / Kitchen Delivery Instructions
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Kitchen Delivery Notes / Instructions (Optional)") },
                placeholder = { Text("e.g. Deliver before 5:00 AM to rear kitchen gate") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    if (businessName.trim().isEmpty()) {
                        validationError = "Please enter business/restaurant name"
                        return@Button
                    }
                    if (primaryContactName.trim().isEmpty()) {
                        validationError = "Please enter contact person name"
                        return@Button
                    }
                    val cleanMobile = mobile.trim().replace("+91", "").replace(" ", "")
                    if (cleanMobile.length < 10) {
                        validationError = "Please enter a valid 10-digit mobile number"
                        return@Button
                    }
                    if (address.trim().isEmpty()) {
                        validationError = "Please enter delivery address"
                        return@Button
                    }

                    val creditLimitVal = creditLimitText.toDoubleOrNull()
                    val discountVal = discountValueText.toDoubleOrNull() ?: 0.0

                    if (isEdit) {
                        onSaveEdit(
                            customer!!.id,
                            UpdateCustomerRequest(
                                businessName = businessName.trim(),
                                primaryContactName = primaryContactName.trim(),
                                mobile = cleanMobile,
                                address = address.trim(),
                                gstNumber = gstNumber.takeIf { it.isNotBlank() },
                                creditLimit = creditLimitVal,
                                notes = notes.takeIf { it.isNotBlank() },
                                discountType = discountType,
                                discountValue = discountVal,
                                discountNotes = discountNotes.takeIf { it.isNotBlank() }
                            )
                        )
                    } else {
                        onSaveAdd(
                            CreateCustomerRequest(
                                businessName = businessName.trim(),
                                primaryContactName = primaryContactName.trim(),
                                mobile = cleanMobile,
                                address = address.trim(),
                                gstNumber = gstNumber.takeIf { it.isNotBlank() },
                                creditLimit = creditLimitVal,
                                notes = notes.takeIf { it.isNotBlank() },
                                discountType = discountType,
                                discountValue = discountVal,
                                discountNotes = discountNotes.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEdit) "Update Profile" else "Save & Add Customer", fontWeight = FontWeight.Bold)
            }
        }
    }
}
