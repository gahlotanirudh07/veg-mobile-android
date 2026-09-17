package com.freshveg.app.features.seller.ledger

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.core.network.CreatePaymentRequest
import com.freshveg.app.core.network.OutstandingCustomerDto
import com.freshveg.app.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentBottomSheet(
    customer: OutstandingCustomerDto,
    onDismiss: () -> Unit,
    onSubmitPayment: (CreatePaymentRequest) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("CASH") } // CASH, UPI, BANK_TRANSFER, CHEQUE
    var referenceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val maxOutstanding = customer.outstandingAmount

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
                        text = "Record Customer Payment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = "${customer.businessName} • 📱 ${customer.mobile}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Current Outstanding Info Banner
            Surface(
                color = if (maxOutstanding > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Outstanding Dues:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "₹${maxOutstanding.toInt()}.00",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (maxOutstanding > 0) Color(0xFFD32F2F) else ActionGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Amount Suggestion Pills
            Text("Quick Amount:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (maxOutstanding > 0) {
                    SuggestionChip(
                        onClick = { amountText = maxOutstanding.toInt().toString() },
                        label = { Text("Full: ₹${maxOutstanding.toInt()}", fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFE8F5E9), labelColor = ActionGreen)
                    )
                }
                listOf(500, 1000, 2000, 5000, 10000).forEach { pillAmt ->
                    SuggestionChip(
                        onClick = { amountText = pillAmt.toString() },
                        label = { Text("₹$pillAmt") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Input Field
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    validationError = null
                },
                label = { Text("Payment Amount (₹)*") },
                placeholder = { Text("e.g. 5000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Mode Chips
            Text("Payment Mode:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "CASH" to "💵 Cash",
                    "UPI" to "📱 UPI / GPay",
                    "BANK_TRANSFER" to "🏦 Net Banking / NEFT",
                    "CHEQUE" to "📑 Cheque"
                ).forEach { (modeKey, modeLabel) ->
                    FilterChip(
                        selected = selectedMode == modeKey,
                        onClick = { selectedMode = modeKey },
                        label = { Text(modeLabel, fontWeight = if (selectedMode == modeKey) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ActionGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reference Number (UTR / Cheque No)
            OutlinedTextField(
                value = referenceNumber,
                onValueChange = { referenceNumber = it },
                label = { Text(if (selectedMode == "CHEQUE") "Cheque Number (Optional)" else "UTR / Transaction Ref (Optional)") },
                placeholder = { Text("e.g. UTR-98234120938") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Notes / Remarks
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Remarks (Optional)") },
                placeholder = { Text("e.g. Received morning cash at Mandi") },
                singleLine = true,
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

            // Submit Button
            Button(
                onClick = {
                    val parsedAmt = amountText.toDoubleOrNull()
                    if (parsedAmt == null || parsedAmt <= 0) {
                        validationError = "Please enter a valid payment amount"
                        return@Button
                    }

                    onSubmitPayment(
                        CreatePaymentRequest(
                            customerId = customer.customerId,
                            amount = parsedAmt,
                            paymentDate = todayDateStr,
                            paymentMode = selectedMode,
                            referenceNumber = referenceNumber.takeIf { it.isNotBlank() },
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Credit Customer Khata", fontWeight = FontWeight.Bold)
            }
        }
    }
}
