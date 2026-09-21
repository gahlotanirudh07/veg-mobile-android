package com.freshveg.app.features.buyer.invoices

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.R
import com.freshveg.app.core.network.InvoiceSummaryDto
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.features.seller.invoices.InvoiceDetailsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerInvoicesScreen(
    onNavigateBack: () -> Unit,
    viewModel: BuyerInvoicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.invoices_screen_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MainInk
                        )
                        Text(
                            text = stringResource(R.string.invoices_screen_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainInk)
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MainInk)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                if (uiState.isLoading && uiState.invoices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ForestGreenPrimary)
                    }
                } else if (uiState.invoices.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.invoices_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.invoices_empty_desc), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.invoices, key = { it.id }) { invoice ->
                            BuyerInvoiceCard(
                                invoice = invoice,
                                onClick = { viewModel.viewInvoiceDetail(invoice.id) },
                                onShare = { viewModel.shareWhatsAppInvoice(context, invoice) }
                            )
                        }
                    }
                }

                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Color.White,
                    contentColor = ActionGreen
                )
            }
        }
    }

    // Modal: Detailed Sample PDF Tax Invoice View
    uiState.selectedInvoiceDetail?.let { detail ->
        InvoiceDetailsBottomSheet(
            invoice = detail,
            onDismiss = viewModel::closeInvoiceDetail,
            onShareWhatsApp = {
                viewModel.shareWhatsAppInvoice(
                    context,
                    InvoiceSummaryDto(
                        id = detail.id,
                        invoiceNumber = detail.invoiceNumber,
                        orderId = detail.orderId,
                        customerId = detail.customerId,
                        subtotal = detail.subtotal,
                        gstAmount = detail.gstAmount,
                        totalAmount = detail.totalAmount,
                        previousBalance = detail.previousBalance,
                        outstandingBalance = detail.outstandingBalance,
                        createdAt = detail.createdAt
                    )
                )
            }
        )
    }
}

@Composable
fun BuyerInvoiceCard(
    invoice: InvoiceSummaryDto,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.invoices_card_number, invoice.invoiceNumber),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MainInk
                    )
                    Text(
                        text = com.freshveg.app.core.utils.formatSafeDate(invoice.invoiceDate, invoice.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.invoices_tax_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ActionGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.invoices_total_billed), style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                    Text(
                        text = "₹${invoice.totalAmount.toInt()}.00",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.invoices_outstanding_bal), style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                    Text(
                        text = "₹${invoice.outstandingBalance.toInt()}.00",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (invoice.outstandingBalance > 0) Color(0xFFD32F2F) else ActionGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.invoices_view_tax_bill), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = ActionGreen)
                }
            }
        }
    }
}
