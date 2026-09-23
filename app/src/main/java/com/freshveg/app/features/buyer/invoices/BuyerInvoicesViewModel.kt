package com.freshveg.app.features.buyer.invoices

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.network.CustomerLedgerSummaryDto
import com.freshveg.app.core.network.InvoiceDetailDto
import com.freshveg.app.core.network.InvoiceSummaryDto
import com.freshveg.app.core.network.PaymentItemDto
import com.freshveg.app.core.network.VegApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuyerInvoicesUiState(
    val selectedTab: Int = 0, // 0: Invoices, 1: Payments Recorded by Seller
    val invoices: List<InvoiceSummaryDto> = emptyList(),
    val payments: List<PaymentItemDto> = emptyList(),
    val khataSummary: CustomerLedgerSummaryDto = CustomerLedgerSummaryDto(),
    val selectedInvoiceDetail: InvoiceDetailDto? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class BuyerInvoicesViewModel @Inject constructor(
    private val apiService: VegApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuyerInvoicesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInvoices()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun loadInvoices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                coroutineScope {
                    val invoicesDeferred = async(Dispatchers.IO) { runCatching { apiService.getInvoices() }.getOrNull() }
                    val paymentsDeferred = async(Dispatchers.IO) { runCatching { apiService.getMyPayments() }.getOrNull() }

                    val invoicesRes = invoicesDeferred.await()
                    val paymentsRes = paymentsDeferred.await()

                    val invoicesList: List<InvoiceSummaryDto> = if (invoicesRes != null && invoicesRes.isSuccessful) {
                        invoicesRes.body()?.invoices ?: emptyList()
                    } else {
                        emptyList()
                    }
                    val paymentsBody = if (paymentsRes != null && paymentsRes.isSuccessful) paymentsRes.body() else null

                    _uiState.update {
                        it.copy(
                            invoices = invoicesList,
                            payments = paymentsBody?.payments ?: emptyList(),
                            khataSummary = paymentsBody?.summary ?: CustomerLedgerSummaryDto(),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load invoices") }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                coroutineScope {
                    val invoicesDeferred = async(Dispatchers.IO) { runCatching { apiService.getInvoices() }.getOrNull() }
                    val paymentsDeferred = async(Dispatchers.IO) { runCatching { apiService.getMyPayments() }.getOrNull() }

                    val invoicesRes = invoicesDeferred.await()
                    val paymentsRes = paymentsDeferred.await()

                    val invoicesBody = if (invoicesRes != null && invoicesRes.isSuccessful) invoicesRes.body()?.invoices else null
                    val paymentsBody = if (paymentsRes != null && paymentsRes.isSuccessful) paymentsRes.body() else null

                    _uiState.update { current ->
                        current.copy(
                            invoices = invoicesBody ?: current.invoices,
                            payments = paymentsBody?.payments ?: current.payments,
                            khataSummary = paymentsBody?.summary ?: current.khataSummary
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to refresh invoices") }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun viewInvoiceDetail(invoiceId: String) {
        viewModelScope.launch {
            try {
                val res = apiService.getInvoiceById(invoiceId)
                if (res.isSuccessful && res.body()?.invoice != null) {
                    _uiState.update { it.copy(selectedInvoiceDetail = res.body()!!.invoice) }
                }
            } catch (e: Exception) {}
        }
    }

    fun closeInvoiceDetail() {
        _uiState.update { it.copy(selectedInvoiceDetail = null) }
    }

    fun shareWhatsAppInvoice(context: Context, invoice: InvoiceSummaryDto) {
        val text = "🧾 *MandiExpress — Tax Invoice Bill*\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "📄 *Invoice No:* ${invoice.invoiceNumber}\n" +
                "📅 *Date:* ${com.freshveg.app.core.utils.formatSafeDate(invoice.invoiceDate, invoice.createdAt)}\n" +
                "💰 *Total Billed:* ₹${invoice.totalAmount.toInt()}.00\n" +
                "🔴 *Outstanding Balance:* ₹${invoice.outstandingBalance.toInt()}.00\n\n" +
                "धन्यवाद — Mandi Wholesale Operations"

        try {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(intent, "Share Invoice on WhatsApp"))
        } catch (e: Exception) {}
    }
}
