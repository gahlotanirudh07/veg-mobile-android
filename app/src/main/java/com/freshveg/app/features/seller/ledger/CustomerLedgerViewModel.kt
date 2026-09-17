package com.freshveg.app.features.seller.ledger

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

data class CustomerLedgerUiState(
    val selectedTab: Int = 0, // 0: Customer Khatas (Dues), 1: Recent Payments
    val overviewSummary: PaymentsOverviewSummaryDto? = null,
    val outstandingCustomers: List<OutstandingCustomerDto> = emptyList(),
    val recentPayments: List<PaymentItemDto> = emptyList(),
    val selectedCustomerForPayment: OutstandingCustomerDto? = null,
    val selectedCustomerForUpi: OutstandingCustomerDto? = null,
    val selectedCustomerStatement: CustomerLedgerResponse? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredOutstandingCustomers: List<OutstandingCustomerDto> get() {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) return outstandingCustomers
        return outstandingCustomers.filter {
            it.businessName.lowercase().contains(q) ||
                    (it.primaryContactName?.lowercase()?.contains(q) == true) ||
                    it.mobile.contains(q)
        }
    }

    val filteredRecentPayments: List<PaymentItemDto> get() {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) return recentPayments
        return recentPayments.filter {
            (it.businessName?.lowercase()?.contains(q) == true) ||
                    (it.customerName?.lowercase()?.contains(q) == true) ||
                    (it.referenceNumber?.lowercase()?.contains(q) == true) ||
                    (it.paymentMode.lowercase().contains(q))
        }
    }
}

@HiltViewModel
class CustomerLedgerViewModel @Inject constructor(
    private val apiService: VegApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerLedgerUiState())
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
                val overviewRes = apiService.getPaymentsOverview()
                val outstandingRes = apiService.getOutstandingCustomers()

                val overviewBody = overviewRes.body()
                val outstandingBody = outstandingRes.body()

                _uiState.update {
                    it.copy(
                        overviewSummary = overviewBody?.summary,
                        recentPayments = overviewBody?.recentPayments ?: overviewBody?.payments ?: emptyList(),
                        outstandingCustomers = outstandingBody?.customers ?: emptyList(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load ledger data") }
            }
        }
    }

    fun openRecordPaymentDialog(customer: OutstandingCustomerDto) {
        _uiState.update { it.copy(selectedCustomerForPayment = customer) }
    }

    fun closeRecordPaymentDialog() {
        _uiState.update { it.copy(selectedCustomerForPayment = null) }
    }

    fun openUpiQr(customer: OutstandingCustomerDto) {
        _uiState.update { it.copy(selectedCustomerForUpi = customer) }
    }

    fun closeUpiQr() {
        _uiState.update { it.copy(selectedCustomerForUpi = null) }
    }

    fun viewCustomerStatement(customerId: String) {
        viewModelScope.launch {
            try {
                val res = apiService.getCustomerLedger(customerId)
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update { it.copy(selectedCustomerStatement = res.body()!!) }
                }
            } catch (e: Exception) {}
        }
    }

    fun closeCustomerStatement() {
        _uiState.update { it.copy(selectedCustomerStatement = null) }
    }

    fun createPayment(request: CreatePaymentRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val res = apiService.createPayment(request)
                if (res.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            selectedCustomerForPayment = null,
                            successMessage = "✅ Payment of ₹${request.amount.toInt()} recorded and credited!"
                        )
                    }
                    loadData()
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = res.errorBody()?.string() ?: "Failed to record payment"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to record payment") }
            }
        }
    }

    fun sendWhatsAppPaymentReminder(context: Context, customer: OutstandingCustomerDto, upiId: String = "mandiexpress@upi") {
        val amount = customer.outstandingAmount.toInt()
        val text = "🙏 *MandiExpress — विनम्र भुगतान स्मरण (Payment Reminder)*\n" +
                "━━━━━━━━━━━━━━━━━━\n" +
                "🏪 *ग्राहक:* ${customer.businessName}\n" +
                "💰 *वर्तमान बकाया राशि (Dues):* ₹$amount.00\n\n" +
                "💳 *कृपया नीचे दिए गए UPI ID पर भुगतान करें:*\n" +
                "`$upiId`\n\n" +
                "यदि आपने हाल ही में भुगतान कर दिया है तो कृपया इस संदेश को अनदेखा करें।\n" +
                "धन्यवाद — Mandi Wholesale Operations"

        try {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(intent, "Send WhatsApp Payment Reminder"))
        } catch (e: Exception) {}
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
