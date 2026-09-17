package com.freshveg.app.features.seller.tally

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
import com.freshveg.app.core.ui.theme.CardSurface
import com.freshveg.app.core.ui.theme.ForestGreenPrimary

val EXPENSE_CATEGORIES = listOf(
    "LABOUR_HAMALI" to "👷‍♂️ Labour & Hamali (हम्माली)",
    "FREIGHT_TRANSPORT" to "🚚 Freight & Transport (भाड़ा)",
    "PACKAGING_CRATES" to "📦 Crates & Bags (बारदाना/क्रेट्स)",
    "MANDI_APMC_TAX" to "🏬 Mandi APMC Tax (मंडी टैक्स)",
    "FUEL_MAINTENANCE" to "⛽ Fuel & Vehicle (गाड़ी/डीजल)",
    "STAFF_MISC" to "☕ Staff Tea & Food (चाय-नाश्ता)",
    "OTHER" to "⚙️ Other Overheads (अन्य)"
)

val PAYMENT_MODES = listOf("CASH", "UPI", "BANK_TRANSFER", "CHEQUE")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (category: String, amount: Double, description: String, paidTo: String?, paymentMode: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(EXPENSE_CATEGORIES.first().first) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var paidTo by remember { mutableStateOf("") }
    var selectedPaymentMode by remember { mutableStateOf(PAYMENT_MODES.first()) }
    var isPaymentModeDropdownExpanded by remember { mutableStateOf(false) }
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
            Text(
                text = "Log Daily Mandi Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ForestGreenPrimary
            )
            Text(
                text = "Record labour, transport, cess, or packaging overheads",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Expense Category Dropdown
            ExposedDropdownMenuBox(
                expanded = isCategoryDropdownExpanded,
                onExpandedChange = { isCategoryDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentLabel = EXPENSE_CATEGORIES.find { it.first == selectedCategory }?.second ?: "Select Category"
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Expense Category *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false }
                ) {
                    EXPENSE_CATEGORIES.forEach { (catCode, catLabel) ->
                        DropdownMenuItem(
                            text = { Text(catLabel) },
                            onClick = {
                                selectedCategory = catCode
                                isCategoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Amount
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it; validationError = null },
                label = { Text("Amount (₹) *") },
                placeholder = { Text("e.g. 450") },
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; validationError = null },
                label = { Text("Description / Notes *") },
                placeholder = { Text("e.g. 5 Crates unloading charges") },
                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Paid To (Optional)
            OutlinedTextField(
                value = paidTo,
                onValueChange = { paidTo = it },
                label = { Text("Paid To / Payee Name (Optional)") },
                placeholder = { Text("e.g. Ramesh Hamal, Tempo Driver") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Payment Mode Dropdown
            ExposedDropdownMenuBox(
                expanded = isPaymentModeDropdownExpanded,
                onExpandedChange = { isPaymentModeDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPaymentMode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Mode") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaymentModeDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                ExposedDropdownMenu(
                    expanded = isPaymentModeDropdownExpanded,
                    onDismissRequest = { isPaymentModeDropdownExpanded = false }
                ) {
                    PAYMENT_MODES.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode) },
                            onClick = {
                                selectedPaymentMode = mode
                                isPaymentModeDropdownExpanded = false
                            }
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

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        validationError = "Please enter a valid amount greater than 0"
                        return@Button
                    }
                    if (description.isBlank()) {
                        validationError = "Please enter a short description"
                        return@Button
                    }
                    onSubmit(
                        selectedCategory,
                        amount,
                        description.trim(),
                        paidTo.trim().ifEmpty { null },
                        selectedPaymentMode
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Expense", fontWeight = FontWeight.Bold)
            }
        }
    }
}
