package com.freshveg.app.features.seller.ledger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.core.network.OutstandingCustomerDto
import com.freshveg.app.core.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiQrBottomSheet(
    customer: OutstandingCustomerDto,
    upiId: String = "mandiexpress@upi",
    sellerName: String = "Mandi Wholesale",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val amount = customer.outstandingAmount
    val roundedAmount = amount.toInt()

    // Generate UPI Intent String: upi://pay?pa=...&pn=...&am=...&cu=INR&tn=...
    val upiUri = remember(customer, upiId) {
        "upi://pay?pa=$upiId&pn=${Uri.encode(sellerName)}&am=$roundedAmount&cu=INR&tn=${Uri.encode("MandiExpress dues for ${customer.businessName}")}"
    }

    val qrBitmap = remember(upiUri) {
        generateQrBitmap(upiUri, 512)
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
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Scan & Pay via UPI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = customer.businessName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Card
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Outstanding Dues", style = MaterialTheme.typography.labelMedium, color = Color.DarkGray)
                    Text(
                        text = "₹$roundedAmount.00",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ActionGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code Display Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 3.dp,
                modifier = Modifier.size(240.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "UPI QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = ActionGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // UPI ID Pill with Copy Button
            Surface(
                color = Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(18.dp))
                        Text(
                            text = "UPI ID: $upiId",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", upiId))
                            Toast.makeText(context, "UPI ID copied!", Toast.LENGTH_SHORT).show()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Open in UPI App & WhatsApp Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // WhatsApp Share Link
                OutlinedButton(
                    onClick = {
                        val shareText = "🙏 *MandiExpress Payment Request*\n\n" +
                                "🏪 *Customer:* ${customer.businessName}\n" +
                                "💰 *Outstanding Balance:* ₹$roundedAmount.00\n" +
                                "💳 *Pay via UPI ID:* $upiId\n\n" +
                                "🔗 *Direct UPI Payment Link:*\n$upiUri\n\n" +
                                "Thank you! — $sellerName"

                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(intent, "Share UPI Link via WhatsApp"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share Link", fontSize = 13.sp)
                }

                // Launch UPI App
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
                            context.startActivity(Intent.createChooser(intent, "Pay using UPI App"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No UPI App found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay via App", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
