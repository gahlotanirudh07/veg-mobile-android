package com.freshveg.app.features.buyer.orders

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

data class BuyerOrdersUiState(
    val orders: List<OrderDto> = emptyList(),
    val selectedOrder: OrderDto? = null,
    val connectedSeller: ConnectedSellerDto? = null,
    val cutoffTime: String = "03:00 AM",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class BuyerOrdersViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val mandiSocketManager: MandiSocketManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuyerOrdersUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadOrders()
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
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                val ordersRes = apiService.getOrders()
                val cutoffRes = apiService.getCutoffTime()
                val sellerRes = try { apiService.getConnectedSeller() } catch (_: Exception) { null }

                _uiState.update {
                    it.copy(
                        orders = ordersRes.body()?.orders ?: emptyList(),
                        cutoffTime = cutoffRes.body()?.cutoffTime ?: "03:00 AM",
                        connectedSeller = sellerRes?.body()?.data,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                if (!silent) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load orders") }
                }
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
}
