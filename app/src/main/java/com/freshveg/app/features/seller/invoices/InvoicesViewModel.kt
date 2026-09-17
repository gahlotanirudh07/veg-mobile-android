package com.freshveg.app.features.seller.invoices

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.network.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoicesUiState(
    val selectedTab: Int = 0, // 0: Generated Invoices, 1: Pending Orders
    val invoices: List<InvoiceSummaryDto> = emptyList(),
    val pendingOrders: List<OrderDto> = emptyList(),
    val selectedInvoiceDetail: InvoiceDetailDto? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredInvoices: List<InvoiceSummaryDto> get() {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) return invoices
        return invoices.filter { inv ->
            inv.invoiceNumber.lowercase().contains(q) ||
                    (inv.customer?.businessName?.lowercase()?.contains(q) == true) ||
                    (inv.customer?.mobile?.contains(q) == true) ||
                    (inv.order?.orderNumber?.lowercase()?.contains(q) == true)
        }
    }

    val filteredPendingOrders: List<OrderDto> get() {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) return pendingOrders
        return pendingOrders.filter { order ->
            order.orderNumber.lowercase().contains(q) ||
                    (order.customer?.businessName?.lowercase()?.contains(q) == true) ||
                    (order.customer?.mobile?.contains(q) == true)
        }
    }
}

@HiltViewModel
class InvoicesViewModel @Inject constructor(
    private val apiService: VegApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoicesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val invoicesRes = apiService.getInvoices()
                val pendingRes = apiService.getPendingInvoices()

                _uiState.update {
                    it.copy(
                        invoices = invoicesRes.body()?.invoices ?: emptyList(),
                        pendingOrders = pendingRes.body()?.orders ?: emptyList(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load invoices") }
            }
        }
    }

    fun generateInvoice(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
            try {
                val res = apiService.generateInvoice(orderId)
                if (res.isSuccessful && res.body() != null) {
                    val generated = res.body()!!
                    val fullInvoiceRes = try { apiService.getInvoiceById(generated.id) } catch (e: Exception) { null }
                    val invoiceDetail = fullInvoiceRes?.body()?.invoice ?: generated
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            selectedInvoiceDetail = invoiceDetail,
                            successMessage = "✅ Invoice #${generated.invoiceNumber} generated successfully!"
                        )
                    }
                    loadData()
                } else {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = res.errorBody()?.string() ?: "Failed to generate invoice"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, errorMessage = e.message ?: "Failed to generate invoice") }
            }
        }
    }

    fun viewInvoiceDetails(invoiceId: String) {
        viewModelScope.launch {
            try {
                val res = apiService.getInvoiceById(invoiceId)
                if (res.isSuccessful && res.body()?.invoice != null) {
                    _uiState.update { it.copy(selectedInvoiceDetail = res.body()!!.invoice) }
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to load invoice details") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Error loading invoice details") }
            }
        }
    }

    fun closeInvoiceDetails() {
        _uiState.update { it.copy(selectedInvoiceDetail = null) }
    }

    fun shareInvoiceOnWhatsApp(context: Context, invoice: InvoiceDetailDto) {
        val cust = invoice.customer
        val seller = invoice.seller

        val sb = StringBuilder()
        val sellerName = seller?.businessName ?: "Fresh Veg Mandi"
        sb.append("🧾 *$sellerName — TAX INVOICE*\n")
        sb.append("━━━━━━━━━━━━━━━━━━\n")
        sb.append("📋 *Invoice Number:* ${invoice.invoiceNumber}\n")
        if (invoice.order != null) {
            sb.append("📦 *Order Number:* #${invoice.order.orderNumber}\n")
        }
        sb.append("🏪 *Bill To:* ${cust?.businessName ?: "Customer"}\n")
        if (!cust?.primaryContactName.isNullOrBlank()) {
            sb.append("👤 *Contact:* ${cust?.primaryContactName}\n")
        }
        if (!cust?.mobile.isNullOrBlank()) {
            sb.append("📱 *Phone:* ${cust?.mobile}\n")
        }
        if (!cust?.address.isNullOrBlank()) {
            sb.append("📍 *Address:* ${cust?.address}\n")
        }
        sb.append("📅 *Date:* ${com.freshveg.app.core.utils.formatSafeDate(invoice.invoiceDate, invoice.createdAt)}\n")
        sb.append("━━━━━━━━━━━━━━━━━━\n\n")

        sb.append("🥦 *INVOICE ITEMS:*\n")
        val invoiceItems = invoice.items ?: emptyList()
        invoiceItems.forEachIndexed { index, item ->
            val num = index + 1
            val delivered = item.deliveredQuantity
            val rate = item.price
            val total = (delivered * rate).toInt()
            sb.append("$num. *${item.productNameSnapshot ?: "Produce"}* — $delivered ${item.unitTypeSnapshot ?: "KG"} × ₹${rate.toInt()} = *₹$total*\n")
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━\n")
        sb.append("💵 *Subtotal:* ₹${invoice.subtotal.toInt()}\n")
        sb.append("💰 *Grand Total:* ₹${invoice.totalAmount.toInt()}\n")
        if (invoice.outstandingBalance > 0) {
            sb.append("🔴 *Total Outstanding Balance:* ₹${invoice.outstandingBalance.toInt()}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━\n")
        sb.append("🙏 Thank you for your business! — $sellerName")

        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Invoice Receipt via WhatsApp")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {}
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
