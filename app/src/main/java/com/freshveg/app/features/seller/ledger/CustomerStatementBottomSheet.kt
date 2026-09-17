package com.freshveg.app.features.seller.ledger

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.core.network.CustomerLedgerResponse
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.safeDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerStatementBottomSheet(
    ledger: CustomerLedgerResponse,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cust = ledger.customer
    val summary = ledger.summary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Khata Account Statement",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = "${cust.businessName} • 📱 ${cust.mobile}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary 3-Column Card
            Surface(
                color = Color(0xFFF9FBF9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Billed", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${summary.totalInvoiced.toInt()}", fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Paid", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("₹${summary.totalPaid.toInt()}", fontWeight = FontWeight.Bold, color = ActionGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Balance Dues", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            text = "₹${summary.outstanding.toInt()}",
                            fontWeight = FontWeight.ExtraBold,
                            color = if (summary.outstanding > 0) Color(0xFFD32F2F) else ActionGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Payment History:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (ledger.payments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No payment transactions recorded yet.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ledger.payments, key = { it.id }) { payment ->
                        Surface(
                            color = Color(0xFFFAFAFA),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("💵 ${payment.paymentMode}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        if (!payment.referenceNumber.isNullOrBlank()) {
                                            Text("• ${payment.referenceNumber}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                    }
                                    Text(
                                        text = payment.paymentDate.safeDate(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.DarkGray
                                    )
                                    if (!payment.notes.isNullOrBlank()) {
                                        Text(payment.notes, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }

                                Text(
                                    text = "+ ₹${payment.amount.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ActionGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // WhatsApp Share Statement Button
            Button(
                onClick = {
                    val sb = StringBuilder()
                    sb.append("📜 *${cust.businessName} — KHATA ACCOUNT STATEMENT*\n")
                    sb.append("━━━━━━━━━━━━━━━━━━\n")
                    sb.append("💰 *Total Invoiced Bills:* ₹${summary.totalInvoiced.toInt()}\n")
                    sb.append("💵 *Total Payments Received:* ₹${summary.totalPaid.toInt()}\n")
                    sb.append("🔴 *Current Outstanding Dues:* ₹${summary.outstanding.toInt()}\n\n")

                    if (ledger.payments.isNotEmpty()) {
                        sb.append("📋 *Recent Payments:*\n")
                        ledger.payments.take(5).forEach { p ->
                            sb.append("• ${p.paymentDate.safeDate()}: ₹${p.amount.toInt()} (${p.paymentMode})\n")
                        }
                        sb.append("\n")
                    }

                    sb.append("━━━━━━━━━━━━━━━━━━\n")
                    sb.append("Thank you for your business! — MandiExpress")

                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, sb.toString())
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Statement via WhatsApp"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Statement via WhatsApp", fontWeight = FontWeight.Bold)
            }
        }
    }
}
