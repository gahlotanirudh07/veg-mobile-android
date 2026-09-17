package com.freshveg.app.features.seller.customers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.core.network.ConnectedSellerDto
import com.freshveg.app.core.ui.theme.CardSurface
import com.freshveg.app.core.ui.theme.ForestGreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSellerCodeBottomSheet(
    seller: ConnectedSellerDto?,
    fallbackSellerCode: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sellerCode = seller?.sellerCode ?: fallbackSellerCode ?: "SEL-MANDI"
    val businessName = seller?.businessName ?: "Fresh Produce Wholesale"

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
                        text = "Onboard New Restaurants",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreenPrimary
                    )
                    Text(
                        text = "Share your unique Seller Code with buyers",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Seller Code Big Card
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("YOUR SELLER CODE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sellerCode,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ForestGreenPrimary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(businessName, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "When new restaurant owners install MandiExpress, they enter this Seller Code during registration to link directly with your store and see your daily rates.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons (Copy & WhatsApp Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Copy Code
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Seller Code", sellerCode))
                        Toast.makeText(context, "Seller Code copied: $sellerCode", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Code")
                }

                // WhatsApp Invite Share
                Button(
                    onClick = {
                        val inviteText = "🥬 *नमस्ते! $businessName से सीधे ताज़ा सब्ज़ियाँ थोक भाव में ऑर्डर करें!*\n\n" +
                                "📲 MandiExpress B2B App डाउनलोड करें और रजिस्ट्रेशन के समय हमारा सेलर कोड दर्ज करें:\n\n" +
                                "👉 *Seller Code:* `$sellerCode`\n\n" +
                                "रोज़ाना सुबह 04:00 AM के ताज़ा भाव देखें और 1-क्लिक में ऑर्डर प्लेस करें।\n" +
                                "धन्यवाद — $businessName"

                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, inviteText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Seller Code via WhatsApp"))
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Invite on WhatsApp", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
