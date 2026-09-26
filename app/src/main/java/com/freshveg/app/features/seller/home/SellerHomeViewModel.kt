package com.freshveg.app.features.seller.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.cache.LocalDataCache
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.lifecycle.AppForegroundMonitor
import com.freshveg.app.core.network.MandiSocketManager
import com.freshveg.app.core.network.VegApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SellerHomeUiState(
    val businessName: String = "FreshVeg Seller",
    val sellerCode: String = "SEL-0000",
    val newOrdersCount: Int = 0,
    val marketDues: Double = 0.0,
    val totalSalesToday: Double = 0.0,
    val isLoading: Boolean = false,
    val lastUpdated: Long = 0L
)

@HiltViewModel
class SellerHomeViewModel @Inject constructor(
    private val apiService: VegApiService,
    val sessionManager: SessionManager,
    private val mandiSocketManager: MandiSocketManager? = null,
    private val localDataCache: LocalDataCache? = null,
    private val appForegroundMonitor: AppForegroundMonitor? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerHomeUiState())
    val uiState = _uiState.asStateFlow()

    private var activeJob: Job? = null

    companion object {
        private const val CACHE_KEY_STATS = "seller_home_dashboard_stats"
    }

    init {
        restoreFromCache()
        observeForegroundResume()
        loadData()
        listenToSocketEvents()
    }

    private fun restoreFromCache() {
        val cache = localDataCache ?: return
        val cached: SellerHomeUiState? = cache.get(CACHE_KEY_STATS, SellerHomeUiState::class.java)
        if (cached != null) {
            _uiState.update {
                it.copy(
                    businessName = cached.businessName.ifBlank { it.businessName },
                    sellerCode = cached.sellerCode.ifBlank { it.sellerCode },
                    newOrdersCount = cached.newOrdersCount,
                    marketDues = cached.marketDues,
                    totalSalesToday = cached.totalSalesToday,
                    lastUpdated = cache.getLastUpdated(CACHE_KEY_STATS)
                )
            }
        }
    }

    private fun observeForegroundResume() {
        val monitor = appForegroundMonitor ?: return
        viewModelScope.launch {
            monitor.foregroundResumeEvent.collect { _ ->
                val cache = localDataCache
                val isStale = cache?.isStale(CACHE_KEY_STATS) ?: true
                if (isStale) {
                    loadData()
                }
            }
        }
    }

    private fun listenToSocketEvents() {
        val socketManager = mandiSocketManager ?: return
        viewModelScope.launch {
            socketManager.socketEvents.collect { event ->
                when (event.event) {
                    "ORDER_CREATED", "ORDER_UPDATED", "ORDER_FULFILLED", "PAYMENT_RECORDED" -> {
                        loadData()
                    }
                }
            }
        }
    }

    fun loadData() {
        if (activeJob?.isActive == true) return

        activeJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val bName = sessionManager.businessName.firstOrNull()?.ifBlank { null } ?: "Ultra Veggies Wholesale"
                val sCode = sessionManager.sellerCode.firstOrNull()?.ifBlank { null } ?: "SEL-EFBC95C3"
                _uiState.update { it.copy(businessName = bName, sellerCode = sCode) }
            } catch (_: Exception) {}

            try {
                coroutineScope {
                    val ordersDeferred = async { apiService.getOrders() }
                    val payDeferred = async { apiService.getPaymentsOverview() }

                    val ordersRes = ordersDeferred.await()
                    val payRes = payDeferred.await()

                    var activeCount = _uiState.value.newOrdersCount
                    var salesSum = _uiState.value.totalSalesToday
                    var dues = _uiState.value.marketDues

                    if (ordersRes.isSuccessful && ordersRes.body() != null) {
                        val orders = ordersRes.body()?.orders ?: emptyList()
                        activeCount = orders.count { it.status == "PENDING" || it.status == "CONFIRMED" }
                        salesSum = orders.filter { it.status == "FULFILLED" || it.status == "DELIVERED" }
                            .sumOf { it.totalAmount }
                    }

                    if (payRes.isSuccessful && payRes.body() != null) {
                        dues = payRes.body()?.summary?.totalOutstanding ?: 0.0
                    }

                    val updatedState = _uiState.value.copy(
                        newOrdersCount = activeCount,
                        totalSalesToday = salesSum,
                        marketDues = dues,
                        isLoading = false,
                        lastUpdated = System.currentTimeMillis()
                    )

                    localDataCache?.put(CACHE_KEY_STATS, updatedState)
                    _uiState.value = updatedState
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
