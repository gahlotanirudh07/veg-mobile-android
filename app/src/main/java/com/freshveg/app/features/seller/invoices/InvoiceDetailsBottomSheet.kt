package com.freshveg.app.features.seller.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.freshveg.app.R
import com.freshveg.app.core.i18n.AppLanguage
import com.freshveg.app.core.i18n.LanguageManager
import com.freshveg.app.core.network.InvoiceDetailDto
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.formatSafeDate
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailsBottomSheet(
    invoice: InvoiceDetailDto,
    onDismiss: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val cust = invoice.customer
    val seller = invoice.seller
    val items = invoice.items ?: emptyList()
    val currentLang by (LanguageManager.instance?.currentLanguage ?: remember { mutableStateOf(AppLanguage.ENGLISH) }).let {
        if (it is StateFlow<*>) (it as StateFlow<AppLanguage>).collectAsState() else remember { mutableStateOf(AppLanguage.ENGLISH) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardSurface
    ) {
        val context = LocalContext.current
        val locale = remember(currentLang) { Locale(currentLang.code) }
        val configuration = remember(currentLang, locale) {
            val conf = Configuration(context.resources.configuration)
            conf.setLocale(locale)
            conf.setLayoutDirection(locale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                conf.setLocales(android.os.LocaleList(locale))
            }
            conf
        }
        val localizedContext = remember(currentLang, locale, configuration) {
            val configContext = context.createConfigurationContext(configuration)
            object : android.content.ContextWrapper(context) {
                override fun getResources(): android.content.res.Resources = configContext.resources
                override fun getAssets(): android.content.res.AssetManager = configContext.assets
            }
        }

        CompositionLocalProvider(
            LocalConfiguration provides configuration,
            LocalContext provides localizedContext
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            // 1. Seller Header & Tax Invoice Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = seller?.businessName ?: "Fresh Produce Wholesale",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1976D2)
                    )
                    seller?.primaryContactName?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = stringResource(R.string.invoice_detail_prop, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                    seller?.address?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                    seller?.mobile?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = stringResource(R.string.invoice_detail_mob, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE3F2FD))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.invoice_detail_tax_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.nav_invoices),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = invoice.invoiceNumber ?: "INV",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = invoice.status ?: "GENERATED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActionGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF1976D2), thickness = 2.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // 2. Bill To & Invoice Details (2-Column Layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Column: BILL TO
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.invoice_detail_billed_to), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(cust?.businessName ?: "Customer", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                    cust?.primaryContactName?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(stringResource(R.string.invoice_detail_contact_name), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }

                    cust?.mobile?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(stringResource(R.string.invoice_detail_phone), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }

                    cust?.address?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(stringResource(R.string.invoice_detail_address), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Right Column: INVOICE DETAILS
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.invoice_detail_header), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(stringResource(R.string.invoice_detail_number), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(invoice.invoiceNumber ?: "INV", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

                    invoice.order?.orderNumber?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(stringResource(R.string.invoice_detail_order_number), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(stringResource(R.string.invoice_detail_date), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(formatSafeDate(invoice.invoiceDate, invoice.createdAt), style = MaterialTheme.typography.bodySmall)

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(stringResource(R.string.invoice_detail_status), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(invoice.status ?: "GENERATED", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ActionGreen)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Invoice Items Table Header
            Text(stringResource(R.string.invoice_detail_particulars), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
            Spacer(modifier = Modifier.height(6.dp))

            // Table Header Bar
            Surface(
                color = Color(0xFFF5F7FA),
                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                    Text(stringResource(R.string.nav_products), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.invoice_table_ord), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                    Text(stringResource(R.string.invoice_table_del), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                    Text(stringResource(R.string.invoice_table_unit), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                    Text(stringResource(R.string.common_rate), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
                    Text(stringResource(R.string.common_total), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(54.dp))
                }
            }

            // Table Rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFEEEEEE), shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            ) {
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.common_no_data), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                } else {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(20.dp), color = Color.Gray)
                            Text(ProduceVisualUtils.getProduceDisplayName(item.productNameSnapshot, isHindi = (currentLang == AppLanguage.HINDI)), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text("${item.orderedQuantity ?: item.deliveredQuantity}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp))
                            Text("${item.deliveredQuantity}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), color = ActionGreen)
                            Text(ProduceVisualUtils.getUnitDisplayName(item.unitTypeSnapshot, isHindi = (currentLang == AppLanguage.HINDI)), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(36.dp), color = Color.Gray)
                            Text("₹${item.price.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
                            Text("₹${item.total.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(54.dp))
                        }
                        if (index < items.size - 1) {
                            HorizontalDivider(color = Color(0xFFEEEEEE))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Totals Breakdown Card
            Surface(
                color = Color(0xFFFAFAFA),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.invoice_detail_subtotal), style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        Text("₹${invoice.subtotal.toInt()}.00", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (invoice.discountAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.invoice_detail_savings), style = MaterialTheme.typography.bodyMedium, color = ActionGreen, fontWeight = FontWeight.Bold)
                            Text("-₹${invoice.discountAmount.toInt()}.00", style = MaterialTheme.typography.bodyMedium, color = ActionGreen, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.invoice_detail_gst), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("₹${invoice.gstAmount.toInt()}.00", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.invoice_detail_total_due), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                        Text("₹${invoice.totalAmount.toInt()}.00", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1976D2))
                    }

                    if (invoice.outstandingBalance > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.invoices_outstanding_bal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                            Text("₹${invoice.outstandingBalance.toInt()}.00", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Footer Note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.invoice_detail_thanks), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(seller?.businessName ?: "Wholesale Trader", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Download PDF, WhatsApp Share & Close
            val context = androidx.compose.ui.platform.LocalContext.current

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { com.freshveg.app.core.utils.InvoicePdfGenerator.generateAndDownloadPdf(context, invoice) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.common_download), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.common_whatsapp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}
}
