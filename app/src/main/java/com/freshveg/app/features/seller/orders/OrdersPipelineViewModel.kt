package com.freshveg.app.features.seller.orders

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.network.*
import com.freshveg.app.core.ui.ProduceVisualUtils
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

data class OrdersPipelineUiState(
    val orders: List<OrderDto> = emptyList(),
    val selectedStatusTab: String = "ALL", // "ALL", "PENDING", "CONFIRMED", "FULFILLED"
    val searchQuery: String = "",
    val selectedCustomer: String = "ALL",
    val selectedDateFilter: String = "ALL", // "ALL", "TODAY", "YESTERDAY", "THIS_WEEK"
    val selectedSortOrder: String = "NEWEST", // "NEWEST", "OLDEST", "AMOUNT_HIGH", "AMOUNT_LOW"
    val cutoffTime: String? = null,
    val selectedOrderForFulfill: OrderDto? = null,
    val isEditCutoffOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isFulfilling: Boolean = false,
    val isGeneratingInvoice: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val pendingCount: Int get() = orders.count { it.status == "PENDING" }
    val confirmedCount: Int get() = orders.count { it.status == "CONFIRMED" }
    val fulfilledCount: Int get() = orders.count { it.status == "FULFILLED" }

    val uniqueCustomers: List<String> get() =
        orders.mapNotNull { it.customer?.businessName?.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()

    val filteredOrders: List<OrderDto> get() {
        val q = searchQuery.trim().lowercase()
        val filtered = orders.filter { order ->
            val matchesStatus = when (selectedStatusTab) {
                "ALL" -> true
                else -> order.status.equals(selectedStatusTab, ignoreCase = true)
            }

            val matchesSearch = q.isEmpty() ||
                    order.orderNumber.lowercase().contains(q) ||
                    (order.customer?.businessName?.lowercase()?.contains(q) == true) ||
                    (order.customer?.mobile?.contains(q) == true) ||
                    order.items.any { it.displayName.lowercase().contains(q) }

            val matchesCustomer = selectedCustomer == "ALL" ||
                    order.customer?.businessName == selectedCustomer ||
                    order.customer?.id == selectedCustomer

            val matchesDate = when (selectedDateFilter) {
                "TODAY" -> order.createdAt.isToday()
                "YESTERDAY" -> order.createdAt.isYesterday()
                "THIS_WEEK" -> order.createdAt.isThisWeek()
                else -> true
            }

            matchesStatus && matchesSearch && matchesCustomer && matchesDate
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
class OrdersPipelineViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val mandiSocketManager: MandiSocketManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersPipelineUiState())
    val uiState = _uiState.asStateFlow()

    fun onSelectCustomer(customer: String) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun onSelectDateFilter(filter: String) {
        _uiState.update { it.copy(selectedDateFilter = filter) }
    }

    fun onSelectSortOrder(sort: String) {
        _uiState.update { it.copy(selectedSortOrder = sort) }
    }

    init {
        loadOrders()
        loadCutoff()
        listenToSocketEvents()
    }

    private fun listenToSocketEvents() {
        val socketManager = mandiSocketManager ?: return
        viewModelScope.launch {
            socketManager.socketEvents.collect { event ->
                when (event.event) {
                    "ORDER_CREATED", "ORDER_UPDATED", "ORDER_FULFILLED" -> {
                        loadOrders(silent = true)
                    }
                }
            }
        }
    }

    fun loadOrders(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            }
            try {
                val res = apiService.getOrders()
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update { it.copy(orders = res.body()?.orders ?: emptyList(), isLoading = false) }
                } else {
                    if (!silent) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load orders") }
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Network error") }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            try {
                apiService.warmUpDatabase()
            } catch (_: Exception) {}
            try {
                val res = apiService.getOrders()
                val cutoffRes = try { apiService.getCutoffTime() } catch (_: Exception) { null }
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update {
                        it.copy(
                            orders = res.body()?.orders ?: emptyList(),
                            cutoffTime = cutoffRes?.body()?.cutoffTime ?: it.cutoffTime,
                            isRefreshing = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, errorMessage = e.message ?: "Failed to refresh orders") }
            }
        }
    }

    fun loadCutoff() {
        viewModelScope.launch {
            try {
                val res = apiService.getCutoffTime()
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update { it.copy(cutoffTime = res.body()!!.cutoffTime) }
                }
            } catch (e: Exception) {}
        }
    }

    fun onSelectStatusTab(status: String) {
        _uiState.update { it.copy(selectedStatusTab = status) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openFulfillDialog(order: OrderDto) {
        if (order.hasInvoice) {
            _uiState.update { it.copy(errorMessage = "⚠️ This order has already been invoiced (#${order.activeInvoice?.invoiceNumber ?: ""}). Weights cannot be edited once billed.") }
            return
        }
        _uiState.update { it.copy(selectedOrderForFulfill = order) }
    }

    fun closeFulfillDialog() {
        _uiState.update { it.copy(selectedOrderForFulfill = null) }
    }

    fun openEditCutoff() {
        _uiState.update { it.copy(isEditCutoffOpen = true) }
    }

    fun closeEditCutoff() {
        _uiState.update { it.copy(isEditCutoffOpen = false) }
    }

    fun saveCutoffTime(newCutoff: String) {
        viewModelScope.launch {
            try {
                val res = apiService.updateCutoffTime(CutoffDto(cutoffTime = newCutoff))
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update {
                        it.copy(
                            cutoffTime = res.body()!!.cutoffTime,
                            isEditCutoffOpen = false,
                            successMessage = "✅ Cutoff deadline updated to $newCutoff"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            cutoffTime = newCutoff,
                            isEditCutoffOpen = false,
                            successMessage = "✅ Cutoff deadline updated to $newCutoff"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cutoffTime = newCutoff,
                        isEditCutoffOpen = false,
                        successMessage = "✅ Cutoff deadline updated to $newCutoff"
                    )
                }
            }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                val req = UpdateOrderStatusRequest(status = newStatus)
                val res = apiService.updateOrderStatus(orderId, req)
                if (res.isSuccessful) {
                    _uiState.update { it.copy(successMessage = "Order status updated to $newStatus") }
                    loadOrders()
                } else {
                    _uiState.update { it.copy(errorMessage = res.errorBody()?.string() ?: "Failed to update order status") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to update order") }
            }
        }
    }

    fun fulfillOrder(orderId: String, items: List<FulfillOrderItem>) {
        val currentOrder = _uiState.value.orders.find { it.id == orderId }
        if (currentOrder?.hasInvoice == true) {
            _uiState.update { it.copy(errorMessage = "⚠️ This order has already been invoiced. Weights cannot be modified after billing.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isFulfilling = true, errorMessage = null) }
            try {
                val req = FulfillOrderRequest(items = items)
                val res = apiService.fulfillOrder(orderId, req)
                if (res.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isFulfilling = false,
                            selectedOrderForFulfill = null,
                            successMessage = "✅ Order weighed & marked packed!"
                        )
                    }
                    loadOrders()
                } else {
                    val rawError = res.errorBody()?.string()
                    _uiState.update {
                        it.copy(
                            isFulfilling = false,
                            errorMessage = extractErrorMessage(rawError)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFulfilling = false, errorMessage = e.message ?: "Failed to fulfill order") }
            }
        }
    }

    private fun extractErrorMessage(rawJson: String?): String {
        if (rawJson.isNullOrBlank()) return "An error occurred. Please try again."
        return try {
            val json = org.json.JSONObject(rawJson)
            if (json.has("error")) json.getString("error")
            else if (json.has("message")) json.getString("message")
            else rawJson
        } catch (_: Exception) {
            rawJson
        }
    }

    fun generateInvoice(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingInvoice = true, errorMessage = null) }
            try {
                val res = apiService.generateInvoice(orderId)
                if (res.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isGeneratingInvoice = false,
                            successMessage = "✅ Invoice generated and posted to Khata!"
                        )
                    }
                    loadOrders()
                } else {
                    val rawError = res.errorBody()?.string()
                    _uiState.update {
                        it.copy(
                            isGeneratingInvoice = false,
                            errorMessage = extractErrorMessage(rawError)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeneratingInvoice = false,
                        errorMessage = e.message ?: "Failed to generate invoice"
                    )
                }
            }
        }
    }

    fun callCustomer(context: Context, mobile: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$mobile")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    fun sendWhatsAppDispatchNotification(context: Context, order: OrderDto) {
        val cust = order.customer ?: return
        val sb = StringBuilder()
        sb.append("🚚 *MANDIEXPRESS — आपका ऑर्डर पैक और तैयार है!*\n")
        sb.append("━━━━━━━━━━━━━━━━━━\n")
        sb.append("📋 *ऑर्डर नंबर:* #${order.orderNumber}\n")
        sb.append("🏨 *ग्राहक:* ${cust.businessName}\n")
        sb.append("💰 *अंतिम बिल राशि:* ₹${order.totalAmount.toInt()}\n\n")

        sb.append("📦 *वजन व सामान विवरण:*\n")
        order.items.forEach { item ->
            val emoji = ProduceVisualUtils.getProduceEmoji(item.productNameSnapshot, null)
            val qty = item.deliveredQuantity ?: item.quantity
            val total = (qty * item.price).toInt()
            sb.append("$emoji ${item.productNameSnapshot} — $qty ${item.unitTypeSnapshot} (₹$total)\n")
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━\n")
        sb.append("⚡ *गाड़ी सुबह 05:00 AM डिलीवरी के लिए निकल चुकी है!*\n")
        sb.append("धन्यवाद — MandiExpress B2B Portal")

        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Send WhatsApp Order Update")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {}
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
