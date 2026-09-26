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

import com.freshveg.app.core.cache.LocalDataCache
import com.freshveg.app.core.lifecycle.AppForegroundMonitor
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

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
    val isRefreshing: Boolean = false,
    val isWakingUp: Boolean = false,
    val isPlacingOrder: Boolean = false,
    val errorMessage: String? = null,
    val transientError: String? = null,
    val lastUpdated: Long = 0L,
    val hasLoadedOnce: Boolean = false,
    val orderSuccessDto: OrderDto? = null
) {
    val hasData: Boolean get() = products.isNotEmpty()
    val isGenuinelyEmpty: Boolean get() = hasLoadedOnce && products.isEmpty() && errorMessage == null

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
    private val localDataCache: LocalDataCache? = null,
    private val appForegroundMonitor: AppForegroundMonitor? = null,
    private val mandiSocketManager: MandiSocketManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuyerCatalogueUiState())
    val uiState = _uiState.asStateFlow()

    private var activeRefreshJob: Job? = null

    init {
        restoreFromLocalCache()
        loadStorefront(silent = _uiState.value.hasData)
        listenToSocketEvents()
        listenToAppForegroundResume()
    }

    private fun restoreFromLocalCache() {
        val cache = localDataCache ?: return
        val cachedProducts: List<ProductDto>? = cache.get(
            LocalDataCache.KEY_BUYER_PRODUCTS,
            object : TypeToken<List<ProductDto>>() {}.type
        )
        val cachedCategories: List<CategoryDto>? = cache.get(
            LocalDataCache.KEY_BUYER_CATEGORIES,
            object : TypeToken<List<CategoryDto>>() {}.type
        )
        val cachedSeller: ConnectedSellerDto? = cache.get(
            LocalDataCache.KEY_BUYER_CONNECTED_SELLER,
            ConnectedSellerDto::class.java
        )
        val cachedCutoff: String? = cache.get(
            LocalDataCache.KEY_BUYER_CUTOFF,
            String::class.java
        )

        if (!cachedProducts.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    products = cachedProducts,
                    categories = cachedCategories ?: it.categories,
                    connectedSeller = cachedSeller ?: it.connectedSeller,
                    cutoffTime = cachedCutoff ?: it.cutoffTime,
                    lastUpdated = cache.getLastUpdated(LocalDataCache.KEY_BUYER_PRODUCTS),
                    hasLoadedOnce = true
                )
            }
        }
    }

    private fun listenToAppForegroundResume() {
        val monitor = appForegroundMonitor ?: return
        viewModelScope.launch {
            monitor.appResumedEvents.collect {
                // If data is older than stale threshold (5 min), trigger background refresh
                val cache = localDataCache
                val isStale = cache?.isStale(LocalDataCache.KEY_BUYER_PRODUCTS) ?: true
                if (isStale) {
                    refresh(isManualPull = false, silent = true)
                }
            }
        }
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
        refresh(isManualPull = false, silent = silent)
    }

    fun refresh(isManualPull: Boolean = true, silent: Boolean = false): Job {
        val existing = activeRefreshJob
        if (existing != null && existing.isActive) {
            return existing
        }

        val job = viewModelScope.launch {
            val hasData = _uiState.value.hasData
            if (!hasData && !silent) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, isWakingUp = false) }
            } else {
                _uiState.update { it.copy(isRefreshing = true, transientError = null) }
            }

            // Detect database waking (> 2.5s) to provide responsive feedback
            val wakingDetectionJob = launch {
                delay(FreshnessConfig.slowConnectionThresholdMs)
                _uiState.update { it.copy(isWakingUp = true) }
            }

            try {
                coroutineScope {
                    val productsDeferred = async { runCatching { apiService.getProducts() }.getOrNull() }
                    val categoriesDeferred = async { runCatching { apiService.getCategories() }.getOrNull() }
                    val sellerDeferred = async { runCatching { apiService.getConnectedSeller() }.getOrNull() }
                    val cutoffDeferred = async { runCatching { apiService.getCutoffTime() }.getOrNull() }
                    val frequentDeferred = async { runCatching { apiService.getBuyerFrequentItems() }.getOrNull() }
                    val lastOrderDeferred = async { runCatching { apiService.getBuyerLastOrder() }.getOrNull() }

                    val productsRes = productsDeferred.await()
                    val categoriesRes = categoriesDeferred.await()
                    val sellerRes = sellerDeferred.await()
                    val cutoffRes = cutoffDeferred.await()
                    val frequentRes = frequentDeferred.await()
                    val lastOrderRes = lastOrderDeferred.await()

                    wakingDetectionJob.cancel()

                    if (productsRes != null && productsRes.isSuccessful) {
                        val newProducts = productsRes.body() ?: emptyList()
                        val newCategories = categoriesRes?.body() ?: _uiState.value.categories
                        val newSeller = sellerRes?.body()?.data ?: _uiState.value.connectedSeller
                        val newCutoff = cutoffRes?.body()?.cutoffTime ?: _uiState.value.cutoffTime
                        val newFrequent = frequentRes?.body()?.frequentItems ?: _uiState.value.frequentItems
                        val newLastOrder = lastOrderRes?.body()?.lastOrder ?: _uiState.value.lastOrder
                        val now = System.currentTimeMillis()

                        // Update local cache
                        localDataCache?.put(LocalDataCache.KEY_BUYER_PRODUCTS, newProducts)
                        localDataCache?.put(LocalDataCache.KEY_BUYER_CATEGORIES, newCategories)
                        if (newSeller != null) localDataCache?.put(LocalDataCache.KEY_BUYER_CONNECTED_SELLER, newSeller)
                        localDataCache?.put(LocalDataCache.KEY_BUYER_CUTOFF, newCutoff)

                        _uiState.update {
                            it.copy(
                                products = newProducts,
                                categories = newCategories,
                                connectedSeller = newSeller,
                                cutoffTime = newCutoff,
                                frequentItems = newFrequent,
                                lastOrder = newLastOrder,
                                isLoading = false,
                                isRefreshing = false,
                                isWakingUp = false,
                                errorMessage = null,
                                transientError = null,
                                lastUpdated = now,
                                hasLoadedOnce = true
                            )
                        }
                    } else {
                        val err = productsRes?.errorBody()?.string() ?: "Failed to refresh catalogue"
                        _uiState.update { current ->
                            if (current.hasData) {
                                // PRESERVE CACHED DATA! Never clear user screen.
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    isWakingUp = false,
                                    transientError = "Couldn't refresh data. Showing previously loaded data."
                                )
                            } else {
                                current.copy(
                                    isLoading = false,
                                    isRefreshing = false,
                                    isWakingUp = false,
                                    errorMessage = err,
                                    hasLoadedOnce = true
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                wakingDetectionJob.cancel()
                _uiState.update { current ->
                    if (current.hasData) {
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isWakingUp = false,
                            transientError = "Couldn't refresh data. Showing previously loaded data."
                        )
                    } else {
                        current.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isWakingUp = false,
                            errorMessage = e.message ?: "Network error",
                            hasLoadedOnce = true
                        )
                    }
                }
            } finally {
                wakingDetectionJob.cancel()
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, isWakingUp = false) }
            }
        }
        activeRefreshJob = job
        return job
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
        val step = 1.0
        setQuantity(product.id, if (current == 0.0) step else current + step)
    }

    fun decrementQuantity(product: ProductDto) {
        val current = _uiState.value.cart[product.id] ?: return
        val step = 1.0
        if (current <= step) {
            setQuantity(product.id, 0.0)
        } else {
            setQuantity(product.id, current - step)
        }
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
