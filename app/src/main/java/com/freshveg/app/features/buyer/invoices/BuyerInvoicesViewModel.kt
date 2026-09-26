package com.freshveg.app.features.buyer.invoices

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.cache.LocalDataCache
import com.freshveg.app.core.lifecycle.AppForegroundMonitor
import com.freshveg.app.core.network.CustomerLedgerSummaryDto
import com.freshveg.app.core.network.FreshnessConfig
import com.freshveg.app.core.network.InvoiceDetailDto
import com.freshveg.app.core.network.InvoiceSummaryDto
import com.freshveg.app.core.network.PaymentItemDto
import com.freshveg.app.core.network.VegApiService
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
    val isWakingUp: Boolean = false,
    val errorMessage: String? = null,
    val transientError: String? = null,
    val lastUpdated: Long = 0L,
    val hasLoadedOnce: Boolean = false
) {
    val hasData: Boolean get() = invoices.isNotEmpty() || payments.isNotEmpty()
    val isGenuinelyEmpty: Boolean get() = hasLoadedOnce && invoices.isEmpty() && payments.isEmpty() && errorMessage == null
}

@HiltViewModel
class BuyerInvoicesViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val localDataCache: LocalDataCache? = null,
    private val appForegroundMonitor: AppForegroundMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuyerInvoicesUiState())
    val uiState = _uiState.asStateFlow()

    private var activeRefreshJob: Job? = null

    companion object {
        private const val CACHE_KEY_INVOICES = "buyer_invoices_cache"
        private const val CACHE_KEY_PAYMENTS = "buyer_payments_cache"
        private const val CACHE_KEY_KHATA = "buyer_khata_cache"
    }

    init {
        restoreFromCache()
        observeForegroundResume()
        loadInvoices()
    }

    private fun restoreFromCache() {
        val cache = localDataCache ?: return
        val invoicesType = object : TypeToken<List<InvoiceSummaryDto>>() {}.type
        val cachedInvoices: List<InvoiceSummaryDto>? = cache.get(CACHE_KEY_INVOICES, invoicesType)
        val paymentsType = object : TypeToken<List<PaymentItemDto>>() {}.type
        val cachedPayments: List<PaymentItemDto>? = cache.get(CACHE_KEY_PAYMENTS, paymentsType)
        val khataType = object : TypeToken<CustomerLedgerSummaryDto>() {}.type
        val cachedKhata: CustomerLedgerSummaryDto? = cache.get(CACHE_KEY_KHATA, khataType)

        if (cachedInvoices != null || cachedPayments != null) {
            _uiState.update {
                it.copy(
                    invoices = cachedInvoices ?: it.invoices,
                    payments = cachedPayments ?: it.payments,
                    khataSummary = cachedKhata ?: it.khataSummary,
                    lastUpdated = cache.getLastUpdated(CACHE_KEY_INVOICES)
                )
            }
        }
    }

    private fun observeForegroundResume() {
        val monitor = appForegroundMonitor ?: return
        viewModelScope.launch {
            monitor.foregroundResumeEvent.collect { _ ->
                val cache = localDataCache
                val isStale = cache?.isStale(CACHE_KEY_INVOICES) ?: true
                if (isStale) {
                    refresh()
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun loadInvoices() {
        fetchInvoices(isExplicitRefresh = false)
    }

    fun refresh() {
        fetchInvoices(isExplicitRefresh = true)
    }

    private fun fetchInvoices(isExplicitRefresh: Boolean) {
        if (activeRefreshJob?.isActive == true) return

        activeRefreshJob = viewModelScope.launch {
            if (isExplicitRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null, transientError = null) }
            } else {
                if (!_uiState.value.hasData) {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null, transientError = null) }
                }
            }

            var wakingTimerJob: Job? = null
            try {
                coroutineScope {
                    wakingTimerJob = launch {
                        delay(FreshnessConfig.SLOW_CONNECTION_THRESHOLD_MS)
                        _uiState.update { it.copy(isWakingUp = true) }
                    }

                    val invoicesDeferred = async { apiService.getInvoices() }
                    val paymentsDeferred = async { apiService.getMyPayments() }

                    val invoicesRes = invoicesDeferred.await()
                    val paymentsRes = paymentsDeferred.await()

                    wakingTimerJob?.cancel()

                    val invoicesList: List<InvoiceSummaryDto> = if (invoicesRes.isSuccessful) {
                        invoicesRes.body()?.invoices ?: emptyList()
                    } else {
                        emptyList()
                    }
                    val paymentsBody = if (paymentsRes.isSuccessful) paymentsRes.body() else null
                    val paymentsList = paymentsBody?.payments ?: emptyList()
                    val khata = paymentsBody?.summary ?: CustomerLedgerSummaryDto()

                    localDataCache?.put(CACHE_KEY_INVOICES, invoicesList)
                    localDataCache?.put(CACHE_KEY_PAYMENTS, paymentsList)
                    localDataCache?.put(CACHE_KEY_KHATA, khata)

                    _uiState.update {
                        it.copy(
                            invoices = invoicesList,
                            payments = paymentsList,
                            khataSummary = khata,
                            isLoading = false,
                            isRefreshing = false,
                            isWakingUp = false,
                            errorMessage = null,
                            transientError = null,
                            hasLoadedOnce = true,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                }
            } catch (e: Exception) {
                wakingTimerJob?.cancel()
                val msg = e.message ?: "Failed to reach server. Connecting to database..."
                _uiState.update { current ->
                    if (current.hasData) {
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isWakingUp = false,
                            transientError = "⚡ Connecting to live Mandi... showing cached bills"
                        )
                    } else {
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isWakingUp = false,
                            errorMessage = msg,
                            hasLoadedOnce = true
                        )
                    }
                }
            } finally {
                wakingTimerJob?.cancel()
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, isWakingUp = false) }
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

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, transientError = null) }
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
