package com.freshveg.app.features.seller.invoices

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.network.*
import com.freshveg.app.core.utils.isThisWeek
import com.freshveg.app.core.utils.isToday
import com.freshveg.app.core.utils.isYesterday
import com.freshveg.app.core.utils.parseEpochMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.freshveg.app.core.cache.LocalDataCache
import com.freshveg.app.core.lifecycle.AppForegroundMonitor
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

data class InvoicesUiState(
    val selectedTab: Int = 0, // 0: Generated Invoices, 1: Pending Orders
    val invoices: List<InvoiceSummaryDto> = emptyList(),
    val pendingOrders: List<OrderDto> = emptyList(),
    val selectedInvoiceDetail: InvoiceDetailDto? = null,
    val searchQuery: String = "",
    val selectedCustomer: String = "ALL",
    val selectedDateFilter: String = "ALL", // "ALL", "TODAY", "YESTERDAY", "THIS_WEEK"
    val selectedSortOrder: String = "NEWEST", // "NEWEST", "OLDEST", "AMOUNT_HIGH", "AMOUNT_LOW"
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isWakingUp: Boolean = false,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val transientError: String? = null,
    val successMessage: String? = null,
    val lastUpdated: Long = 0L,
    val hasLoadedOnce: Boolean = false
) {
    val hasData: Boolean get() = invoices.isNotEmpty() || pendingOrders.isNotEmpty()
    val isGenuinelyEmpty: Boolean get() = hasLoadedOnce && invoices.isEmpty() && pendingOrders.isEmpty() && errorMessage == null

    val uniqueCustomers: List<String> get() {
        val fromInvoices = invoices.mapNotNull { inv ->
            inv.customer?.businessName?.trim()?.takeIf { it.isNotEmpty() }
                ?: inv.customer?.primaryContactName?.trim()?.takeIf { it.isNotEmpty() }
        }
        val fromPending = pendingOrders.mapNotNull { ord ->
            ord.customer?.businessName?.trim()?.takeIf { it.isNotEmpty() }
                ?: ord.customer?.primaryContactName?.trim()?.takeIf { it.isNotEmpty() }
        }
        return (fromInvoices + fromPending).filter { it.isNotEmpty() }.distinct().sorted()
    }

    val filteredInvoices: List<InvoiceSummaryDto> get() {
        val q = searchQuery.trim().lowercase()
        val filtered = invoices.filter { inv ->
            val matchesSearch = q.isEmpty() ||
                    inv.invoiceNumber.lowercase().contains(q) ||
                    (inv.customer?.businessName?.lowercase()?.contains(q) == true) ||
                    (inv.customer?.primaryContactName?.lowercase()?.contains(q) == true) ||
                    (inv.customer?.mobile?.contains(q) == true) ||
                    (inv.order?.orderNumber?.lowercase()?.contains(q) == true)

            val matchesCustomer = selectedCustomer == "ALL" ||
                    inv.customer?.businessName == selectedCustomer ||
                    inv.customer?.primaryContactName == selectedCustomer ||
                    inv.customerId == selectedCustomer

            val rawDate = inv.invoiceDate ?: inv.createdAt
            val matchesDate = when (selectedDateFilter) {
                "TODAY" -> rawDate.isToday()
                "YESTERDAY" -> rawDate.isYesterday()
                "THIS_WEEK" -> rawDate.isThisWeek()
                else -> true
            }

            matchesSearch && matchesCustomer && matchesDate
        }

        return when (selectedSortOrder) {
            "OLDEST" -> filtered.sortedBy { (it.invoiceDate ?: it.createdAt).parseEpochMs() }
            "AMOUNT_HIGH" -> filtered.sortedByDescending { it.totalAmount }
            "AMOUNT_LOW" -> filtered.sortedBy { it.totalAmount }
            else -> filtered.sortedByDescending { (it.invoiceDate ?: it.createdAt).parseEpochMs() } // "NEWEST"
        }
    }

    val filteredPendingOrders: List<OrderDto> get() {
        val q = searchQuery.trim().lowercase()
        val filtered = pendingOrders.filter { order ->
            val matchesSearch = q.isEmpty() ||
                    order.orderNumber.lowercase().contains(q) ||
                    (order.customer?.businessName?.lowercase()?.contains(q) == true) ||
                    (order.customer?.primaryContactName?.lowercase()?.contains(q) == true) ||
                    (order.customer?.mobile?.contains(q) == true)

            val matchesCustomer = selectedCustomer == "ALL" ||
                    order.customer?.businessName == selectedCustomer ||
                    order.customer?.primaryContactName == selectedCustomer ||
                    order.customer?.id == selectedCustomer

            val matchesDate = when (selectedDateFilter) {
                "TODAY" -> order.createdAt.isToday()
                "YESTERDAY" -> order.createdAt.isYesterday()
                "THIS_WEEK" -> order.createdAt.isThisWeek()
                else -> true
            }

            matchesSearch && matchesCustomer && matchesDate
        }

        return when (selectedSortOrder) {
            "OLDEST" -> filtered.sortedBy { it.createdAt.parseEpochMs() }
            "AMOUNT_HIGH" -> filtered.sortedByDescending { it.totalAmount }
            "AMOUNT_LOW" -> filtered.sortedBy { it.totalAmount }
            else -> filtered.sortedByDescending { it.createdAt.parseEpochMs() } // "NEWEST"
        }
    }
}

@HiltViewModel
class InvoicesViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val localDataCache: LocalDataCache? = null,
    private val appForegroundMonitor: AppForegroundMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoicesUiState())
    val uiState = _uiState.asStateFlow()

    private var activeRefreshJob: Job? = null

    companion object {
        private const val CACHE_KEY_INVOICES = "seller_invoices_cache"
        private const val CACHE_KEY_PENDING = "seller_pending_invoices_cache"
    }

    init {
        restoreFromCache()
        observeForegroundResume()
        loadData()
    }

    private fun restoreFromCache() {
        val cache = localDataCache ?: return
        val invoicesType = object : TypeToken<List<InvoiceSummaryDto>>() {}.type
        val cachedInvoices: List<InvoiceSummaryDto>? = cache.get(CACHE_KEY_INVOICES, invoicesType)

        val pendingType = object : TypeToken<List<OrderDto>>() {}.type
        val cachedPending: List<OrderDto>? = cache.get(CACHE_KEY_PENDING, pendingType)

        if (cachedInvoices != null || cachedPending != null) {
            _uiState.update {
                it.copy(
                    invoices = cachedInvoices ?: it.invoices,
                    pendingOrders = cachedPending ?: it.pendingOrders,
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

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSelectCustomer(customer: String) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun onSelectDateFilter(filter: String) {
        _uiState.update { it.copy(selectedDateFilter = filter) }
    }

    fun onSelectSortOrder(sort: String) {
        _uiState.update { it.copy(selectedSortOrder = sort) }
    }

    fun loadData() {
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
                    val pendingDeferred = async { apiService.getPendingInvoices() }

                    val invoicesRes = invoicesDeferred.await()
                    val pendingRes = pendingDeferred.await()

                    wakingTimerJob?.cancel()

                    val newInvoices = invoicesRes.body()?.invoices ?: emptyList()
                    val newPending = pendingRes.body()?.orders ?: emptyList()

                    localDataCache?.put(CACHE_KEY_INVOICES, newInvoices)
                    localDataCache?.put(CACHE_KEY_PENDING, newPending)

                    _uiState.update {
                        it.copy(
                            invoices = newInvoices,
                            pendingOrders = newPending,
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
                            transientError = "⚡ Connecting to live Mandi... showing cached data"
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
        _uiState.update { it.copy(errorMessage = null, successMessage = null, transientError = null) }
    }
}
