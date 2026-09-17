package com.freshveg.app.features.buyer.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.network.*
import com.freshveg.app.core.utils.MandiTranslationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuyerCatalogueUiState(
    val products: List<ProductDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val selectedCategoryId: String = "ALL",
    val connectedSeller: ConnectedSellerDto? = null,
    val frequentItems: List<BuyerFrequentItemDto> = emptyList(),
    val lastOrder: OrderDto? = null,
    val cart: Map<String, Double> = emptyMap(), // productId -> quantity
    val cutoffTime: String = "03:00 AM",
    val searchQuery: String = "",
    val isCartOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isPlacingOrder: Boolean = false,
    val errorMessage: String? = null,
    val orderSuccessDto: OrderDto? = null
) {
    val filteredProducts: List<ProductDto> get() {
        var list = products.filter { it.isAvailable }
        if (selectedCategoryId != "ALL") {
            list = list.filter { it.categoryId == selectedCategoryId }
        }
        val q = searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            list = list.filter { p ->
                val name = p.safeName.lowercase()
                val hindi = p.hindiName?.lowercase() ?: MandiTranslationUtils.translateEnglishToHindi(p.safeName)?.lowercase() ?: ""
                val cat = p.resolvedCategoryName.lowercase()
                name.contains(q) || hindi.contains(q) || cat.contains(q)
            }
        }
        return list
    }

    val cartItemsList: List<CartItemDetail> get() {
        val productMap = products.associateBy { it.id }
        return cart.mapNotNull { (productId, qty) ->
            productMap[productId]?.let { prod ->
                CartItemDetail(product = prod, quantity = qty)
            }
        }
    }

    val cartItemCount: Int get() = cart.size
    val cartEstimatedTotal: Double get() = cartItemsList.sumOf { it.lineTotal }
}

@HiltViewModel
class BuyerCatalogueViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val mandiSocketManager: MandiSocketManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuyerCatalogueUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadStorefront()
        listenToSocketEvents()
    }

    private fun listenToSocketEvents() {
        val socketManager = mandiSocketManager ?: return
        viewModelScope.launch {
            socketManager.socketEvents.collect { event ->
                when (event.event) {
                    "RATES_UPDATED", "PRODUCT_INVENTORY_CHANGED", "DISCOUNT_CHANGED" -> {
                        loadStorefront(silent = true)
                    }
                }
            }
        }
    }

    fun loadStorefront(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                val productsRes = apiService.getProducts()
                val categoriesRes = apiService.getCategories()
                val sellerRes = apiService.getConnectedSeller()
                val cutoffRes = apiService.getCutoffTime()
                val frequentRes = apiService.getBuyerFrequentItems()
                val lastOrderRes = apiService.getBuyerLastOrder()

                _uiState.update {
                    it.copy(
                        products = productsRes.body() ?: emptyList(),
                        categories = categoriesRes.body() ?: emptyList(),
                        connectedSeller = sellerRes.body()?.data,
                        cutoffTime = cutoffRes.body()?.cutoffTime ?: "03:00 AM",
                        frequentItems = frequentRes.body()?.frequentItems ?: emptyList(),
                        lastOrder = lastOrderRes.body()?.lastOrder,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                if (!silent) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load catalog") }
                }
            }
        }
    }

    fun selectCategory(categoryId: String) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setQuantity(productId: String, quantity: Double) {
        _uiState.update { state ->
            val updated = state.cart.toMutableMap()
            if (quantity <= 0.0) {
                updated.remove(productId)
            } else {
                updated[productId] = quantity
            }
            state.copy(cart = updated)
        }
    }

    fun incrementQuantity(product: ProductDto) {
        val current = _uiState.value.cart[product.id] ?: 0.0
        val step = if (product.unitType == UnitType.KG) 5.0 else 1.0
        setQuantity(product.id, if (current == 0.0) step else current + step)
    }

    fun decrementQuantity(product: ProductDto) {
        val current = _uiState.value.cart[product.id] ?: return
        val step = if (product.unitType == UnitType.KG) 5.0 else 1.0
        setQuantity(product.id, current - step)
    }

    fun removeFromCart(productId: String) {
        setQuantity(productId, 0.0)
    }

    fun clearCart() {
        _uiState.update { it.copy(cart = emptyMap(), isCartOpen = false) }
    }

    fun reorderAllFromLastOrder() {
        val last = _uiState.value.lastOrder ?: return
        val newCart = mutableMapOf<String, Double>()
        last.items.forEach { item ->
            newCart[item.productId] = item.quantity
        }
        _uiState.update { it.copy(cart = newCart, isCartOpen = true) }
    }

    fun openCart() {
        if (_uiState.value.cart.isNotEmpty()) {
            _uiState.update { it.copy(isCartOpen = true) }
        }
    }

    fun closeCart() {
        _uiState.update { it.copy(isCartOpen = false) }
    }

    fun placeOrder(deliveryNotes: String?) {
        val cartItems = _uiState.value.cartItemsList
        if (cartItems.isEmpty()) return

        val orderItemsRequest = cartItems.map {
            CreateOrderItemRequest(productId = it.product.id, quantity = it.quantity)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPlacingOrder = true, errorMessage = null) }
            try {
                val req = CreateOrderRequest(
                    notes = deliveryNotes.takeIf { !it.isNullOrBlank() },
                    orderItems = orderItemsRequest
                )
                val res = apiService.createOrder(req)
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update {
                        it.copy(
                            isPlacingOrder = false,
                            isCartOpen = false,
                            cart = emptyMap(),
                            orderSuccessDto = res.body()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isPlacingOrder = false,
                            errorMessage = res.errorBody()?.string() ?: "Failed to place order"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPlacingOrder = false, errorMessage = e.message ?: "Network error") }
            }
        }
    }

    fun clearOrderSuccess() {
        _uiState.update { it.copy(orderSuccessDto = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
