package com.freshveg.app.features.seller.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.network.MandiSocketManager
import com.freshveg.app.core.network.VegApiService
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isLoading: Boolean = false
)

@HiltViewModel
class SellerHomeViewModel @Inject constructor(
    private val apiService: VegApiService,
    val sessionManager: SessionManager,
    private val mandiSocketManager: MandiSocketManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
        listenToSocketEvents()
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val bName = sessionManager.businessName.firstOrNull()?.ifBlank { null } ?: "Ultra Veggies Wholesale"
                val sCode = sessionManager.sellerCode.firstOrNull()?.ifBlank { null } ?: "SEL-EFBC95C3"
                _uiState.update { it.copy(businessName = bName, sellerCode = sCode) }
            } catch (_: Exception) {}

            // 1. Fetch real orders to compute active orders count and today's sales
            try {
                val ordersRes = apiService.getOrders()
                if (ordersRes.isSuccessful && ordersRes.body() != null) {
                    val orders = ordersRes.body()?.orders ?: emptyList()
                    val activeCount = orders.count { it.status == "PENDING" || it.status == "CONFIRMED" }
                    val salesSum = orders.filter { it.status == "FULFILLED" || it.status == "DELIVERED" }
                        .sumOf { it.totalAmount }
                    _uiState.update {
                        it.copy(
                            newOrdersCount = activeCount,
                            totalSalesToday = salesSum
                        )
                    }
                }
            } catch (_: Exception) {}

            // 2. Fetch payments overview to get verified market dues / total outstanding
            try {
                val payRes = apiService.getPaymentsOverview()
                if (payRes.isSuccessful && payRes.body() != null) {
                    val dues = payRes.body()?.summary?.totalOutstanding ?: 0.0
                    _uiState.update { it.copy(marketDues = dues) }
                }
            } catch (_: Exception) {}

            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
