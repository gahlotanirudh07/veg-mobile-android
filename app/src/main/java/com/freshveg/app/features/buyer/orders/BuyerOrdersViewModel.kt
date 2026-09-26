package com.freshveg.app.features.buyer.orders

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

data class BuyerOrdersUiState(
    val orders: List<OrderDto> = emptyList(),
    val selectedOrder: OrderDto? = null,
    val connectedSeller: ConnectedSellerDto? = null,
    val cutoffTime: String = "03:00 AM",
    val searchQuery: String = "",
    val selectedDateFilter: String = "TODAY", // "TODAY", "ALL", "YESTERDAY", "THIS_WEEK"
    val selectedSortOrder: String = "NEWEST", // "NEWEST", "OLDEST", "AMOUNT_HIGH", "AMOUNT_LOW"
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isWakingUp: Boolean = false,
    val errorMessage: String? = null,
    val transientError: String? = null,
    val lastUpdated: Long = 0L,
    val hasLoadedOnce: Boolean = false
) {
    val hasData: Boolean get() = orders.isNotEmpty()
    val isGenuinelyEmpty: Boolean get() = hasLoadedOnce && orders.isEmpty() && errorMessage == null

    val filteredOrders: List<OrderDto> get() {
        val q = searchQuery.trim().lowercase()
        val filtered = orders.filter { order ->
            val matchesSearch = q.isEmpty() ||
                    order.orderNumber.lowercase().contains(q) ||
                    order.items.any { it.displayName.lowercase().contains(q) || (it.productNameSnapshot?.lowercase()?.contains(q) == true) }

            val matchesDate = when (selectedDateFilter) {
                "TODAY" -> order.createdAt.isToday()
                "YESTERDAY" -> order.createdAt.isYesterday()
                "THIS_WEEK" -> order.createdAt.isThisWeek()
                else -> true
            }

            matchesSearch && matchesDate
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
class BuyerOrdersViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val mandiSocketManager: MandiSocketManager? = null,
    private val localDataCache: LocalDataCache? = null,
    private val appForegroundMonitor: AppForegroundMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuyerOrdersUiState())
    val uiState = _uiState.asStateFlow()

    private var activeRefreshJob: Job? = null

    companion object {
        private const val CACHE_KEY_ORDERS = "buyer_orders_cache"
        private const val CACHE_KEY_SELLER = "buyer_connected_seller_cache"
        private const val CACHE_KEY_CUTOFF = "buyer_cutoff_cache"
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSelectDateFilter(filter: String) {
        _uiState.update { it.copy(selectedDateFilter = filter) }
    }

    fun onSelectSortOrder(sort: String) {
        _uiState.update { it.copy(selectedSortOrder = sort) }
    }

    init {
        restoreFromCache()
        observeForegroundResume()
        loadOrders()
        listenToSocketEvents()
    }

    private fun restoreFromCache() {
        val cache = localDataCache ?: return
        val ordersType = object : TypeToken<List<OrderDto>>() {}.type
        val cachedOrders: List<OrderDto>? = cache.get(CACHE_KEY_ORDERS, ordersType)
        val sellerType = object : TypeToken<ConnectedSellerDto>() {}.type
        val cachedSeller: ConnectedSellerDto? = cache.get(CACHE_KEY_SELLER, sellerType)
        val cachedCutoff: String? = cache.get(CACHE_KEY_CUTOFF, String::class.java)

        if (cachedOrders != null) {
            _uiState.update {
                it.copy(
                    orders = cachedOrders,
                    connectedSeller = cachedSeller ?: it.connectedSeller,
                    cutoffTime = cachedCutoff ?: it.cutoffTime,
                    lastUpdated = cache.getLastUpdated(CACHE_KEY_ORDERS)
                )
            }
        }
    }

    private fun observeForegroundResume() {
        val monitor = appForegroundMonitor ?: return
        viewModelScope.launch {
            monitor.foregroundResumeEvent.collect { _ ->
                val cache = localDataCache
                val isStale = cache?.isStale(CACHE_KEY_ORDERS) ?: true
                if (isStale) {
                    refresh()
                }
            }
        }
    }

    private fun listenToSocketEvents() {
        val socketManager = mandiSocketManager ?: return
        viewModelScope.launch {
            socketManager.socketEvents.collect { event ->
                when (event.event) {
                    "ORDER_CREATED", "ORDER_UPDATED", "ORDER_FULFILLED" -> {
                        fetchOrders(isExplicitRefresh = false, silent = true)
                    }
                }
            }
        }
    }

    fun loadOrders(silent: Boolean = false) {
        fetchOrders(isExplicitRefresh = false, silent = silent)
    }

    fun refresh() {
        fetchOrders(isExplicitRefresh = true, silent = false)
    }

    private fun fetchOrders(isExplicitRefresh: Boolean, silent: Boolean) {
        if (activeRefreshJob?.isActive == true) return

        activeRefreshJob = viewModelScope.launch {
            if (isExplicitRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null, transientError = null) }
            } else if (!silent) {
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

                    val ordersDeferred = async { apiService.getOrders() }
                    val cutoffDeferred = async { apiService.getCutoffTime() }
                    val sellerDeferred = async {
                        try {
                            apiService.getConnectedSeller()
                        } catch (_: Exception) {
                            null
                        }
                    }

                    val ordersRes = ordersDeferred.await()
                    val cutoffRes = cutoffDeferred.await()
                    val sellerRes = sellerDeferred.await()

                    wakingTimerJob?.cancel()

                    val newOrders = ordersRes.body()?.orders ?: emptyList()
                    val newCutoff = cutoffRes.body()?.cutoffTime ?: "03:00 AM"
                    val newSeller = sellerRes?.body()?.data

                    localDataCache?.put(CACHE_KEY_ORDERS, newOrders)
                    localDataCache?.put(CACHE_KEY_CUTOFF, newCutoff)
                    if (newSeller != null) {
                        localDataCache?.put(CACHE_KEY_SELLER, newSeller)
                    }

                    _uiState.update {
                        it.copy(
                            orders = newOrders,
                            cutoffTime = newCutoff,
                            connectedSeller = newSeller ?: it.connectedSeller,
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
                            transientError = "⚡ Connecting to live Mandi... showing cached orders"
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

    fun openOrderDetail(order: OrderDto) {
        _uiState.update { it.copy(selectedOrder = order) }
    }

    fun closeOrderDetail() {
        _uiState.update { it.copy(selectedOrder = null) }
    }

    fun shareOrderStatus(context: Context, order: OrderDto) {
        val statusEmoji = when (order.status.uppercase()) {
            "DELIVERED" -> "🟢 DELIVERED"
            "FULFILLED" -> "📦 WEIGHED & DISPATCHED"
            "CONFIRMED" -> "🔵 CONFIRMED"
            else -> "🟡 PLACED"
        }

        val itemsSummary = order.items.joinToString("\n") { it ->
            "• ${it.productNameSnapshot}: ${it.deliveredQuantity ?: it.quantity} ${it.unitTypeSnapshot} @ ₹${it.price.toInt()}"
        }

        val text = "🥬 *MandiExpress — Order Status Update*\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "📦 *Order:* #${order.orderNumber}\n" +
                "📊 *Status:* $statusEmoji\n" +
                "💰 *Total Amount:* ₹${order.totalAmount.toInt()}\n\n" +
                "📋 *Items:*\n$itemsSummary\n\n" +
                "धन्यवाद — Mandi Wholesale Delivery"

        try {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(intent, "Share Order Status"))
        } catch (e: Exception) {}
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, transientError = null) }
    }
}
