package com.freshveg.app.features.seller.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.network.CategoryDto
import com.freshveg.app.core.network.ProductDto
import com.freshveg.app.core.network.BulkUpdateRatesRequest
import com.freshveg.app.core.network.RateUpdateItem
import com.freshveg.app.core.network.UnitType
import com.freshveg.app.core.network.VegApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StoreTabFilter(val label: String, val hindiLabel: String) {
    ALL("All Store", "सभी"),
    IN_STOCK("In-Stock", "उपलब्ध"),
    OUT_OF_STOCK("Out of Stock", "स्टॉक समाप्त"),
    CUSTOM("My Custom", "कस्टम")
}

data class SellerProductsUiState(
    val products: List<ProductDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val sellerCode: String? = null,
    val businessName: String? = null,
    val selectedProductForEdit: ProductDto? = null,
    val isFormBottomSheetOpen: Boolean = false,
    val searchQuery: String = "",
    val selectedCategoryId: String? = null,
    val inStockFilter: Boolean? = null, // null = All, true = In-Stock, false = Out-of-Stock
    val tabFilter: StoreTabFilter = StoreTabFilter.ALL,
    val hideMasterCatalogue: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
) {
    // Only display produce that is actually stocked in this seller's store
    val visibleProducts: List<ProductDto> get() = products.filter {
        it.isInStore || it.isCustom || it.currentPrice > 0.0
    }

    val inStockCount: Int get() = visibleProducts.count { it.isAvailable }
    val outOfStockCount: Int get() = visibleProducts.count { !it.isAvailable }
    val customCount: Int get() = visibleProducts.count { it.isCustom }
    val totalCount: Int get() = visibleProducts.size

    val filteredProducts: List<ProductDto> get() {
        var list = visibleProducts

        list = when (tabFilter) {
            StoreTabFilter.ALL -> list
            StoreTabFilter.IN_STOCK -> list.filter { it.isAvailable }
            StoreTabFilter.OUT_OF_STOCK -> list.filter { !it.isAvailable }
            StoreTabFilter.CUSTOM -> list.filter { it.isCustom }
        }

        if (inStockFilter != null) {
            list = list.filter { it.isAvailable == inStockFilter }
        }

        if (selectedCategoryId != null) {
            list = list.filter { it.categoryId == selectedCategoryId }
        }

        val q = searchQuery.trim().lowercase()
        if (q.isNotEmpty()) {
            list = list.filter { prod ->
                prod.name.lowercase().contains(q) ||
                (prod.hindiName?.lowercase()?.contains(q) == true) ||
                (prod.categoryName?.lowercase()?.contains(q) == true)
            }
        }

        return list
    }
}

@HiltViewModel
class SellerProductsViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerProductsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.hideMasterCatalogue.collect { hide ->
                _uiState.update { it.copy(hideMasterCatalogue = hide) }
            }
        }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val code = sessionManager.sellerCode.firstOrNull()
                val business = sessionManager.businessName.firstOrNull() ?: sessionManager.role.firstOrNull()
                _uiState.update { it.copy(sellerCode = code, businessName = business) }
            } catch (_: Exception) {}

            try {
                val prodRes = apiService.getProducts()
                val catRes = apiService.getCategories()

                _uiState.update {
                    it.copy(
                        products = prodRes.body() ?: emptyList(),
                        categories = catRes.body() ?: emptyList(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load products") }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelect(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onStockFilterSelect(inStock: Boolean?) {
        _uiState.update { it.copy(inStockFilter = inStock) }
    }

    fun setTabFilter(filter: StoreTabFilter) {
        _uiState.update { it.copy(tabFilter = filter) }
    }

    fun toggleHideMasterCatalogue(hide: Boolean) {
        viewModelScope.launch {
            sessionManager.setHideMasterCatalogue(hide)
            _uiState.update {
                it.copy(
                    hideMasterCatalogue = hide,
                    snackbarMessage = if (hide) "Master catalogue hidden. Showing only custom produce." else "Master catalogue restored."
                )
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun toggleAvailability(product: ProductDto) {
        viewModelScope.launch {
            val updated = product.copy(isAvailable = !product.isAvailable)
            _uiState.update { state ->
                state.copy(products = state.products.map { if (it.id == product.id) updated else it })
            }
            try {
                val res = apiService.updateProduct(product.id, updated)
                if (!res.isSuccessful) {
                    _uiState.update { state ->
                        state.copy(products = state.products.map { if (it.id == product.id) product else it })
                    }
                }
            } catch (_: Exception) {
                _uiState.update { state ->
                    state.copy(products = state.products.map { if (it.id == product.id) product else it })
                }
            }
        }
    }

    fun openAddProduct() {
        _uiState.update {
            it.copy(
                selectedProductForEdit = null,
                isFormBottomSheetOpen = true
            )
        }
    }

    fun openEditProduct(product: ProductDto) {
        _uiState.update {
            it.copy(
                selectedProductForEdit = product,
                isFormBottomSheetOpen = true
            )
        }
    }

    fun closeFormBottomSheet() {
        _uiState.update {
            it.copy(
                selectedProductForEdit = null,
                isFormBottomSheetOpen = false
            )
        }
    }

    fun saveProduct(
        name: String,
        hindiName: String?,
        categoryId: String?,
        unitType: UnitType,
        currentPrice: Double,
        minimumOrderQuantity: Double,
        isAvailable: Boolean,
        isFeatured: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val existing = _uiState.value.selectedProductForEdit

            val payload = ProductDto(
                id = existing?.id ?: "",
                name = name,
                hindiName = hindiName,
                categoryId = categoryId,
                unitType = unitType,
                currentPrice = currentPrice,
                minimumOrderQuantity = minimumOrderQuantity,
                isAvailable = isAvailable,
                isFeatured = isFeatured,
                isCustom = existing?.isCustom ?: true // New items created by seller are custom!
            )

            try {
                if (existing != null) {
                    val res = apiService.updateProduct(existing.id, payload)
                    if (res.isSuccessful && res.body() != null) {
                        _uiState.update { state ->
                            state.copy(
                                products = state.products.map { if (it.id == existing.id) res.body()!! else it },
                                isFormBottomSheetOpen = false,
                                selectedProductForEdit = null,
                                isLoading = false,
                                snackbarMessage = "\"$name\" updated successfully."
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to update product") }
                    }
                } else {
                    val res = apiService.createProduct(payload)
                    if (res.isSuccessful && res.body() != null) {
                        _uiState.update { state ->
                            state.copy(
                                products = listOf(res.body()!!) + state.products,
                                isFormBottomSheetOpen = false,
                                selectedProductForEdit = null,
                                isLoading = false,
                                snackbarMessage = "\"$name\" added to your store catalogue!"
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to create product") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to save product") }
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val target = _uiState.value.products.find { it.id == productId }
            var success = false
            try {
                val res = apiService.deleteProduct(productId)
                if (res.isSuccessful) {
                    success = true
                }
            } catch (_: Exception) {}

            if (!success && target?.isCustom == false) {
                // For master produce on legacy backend, unlist by setting price to 0 and unavailable
                try {
                    apiService.bulkUpdateSellerRates(
                        BulkUpdateRatesRequest(
                            updates = listOf(
                                RateUpdateItem(
                                    productId = productId,
                                    sellingPrice = 0.0,
                                    isAvailable = false
                                )
                            )
                        )
                    )
                    success = true
                } catch (_: Exception) {}
            }

            _uiState.update { state ->
                val unlisted = state.products.map { prod ->
                    if (prod.id == productId) {
                        prod.copy(isInStore = false, currentPrice = 0.0, isAvailable = false)
                    } else prod
                }.filter {
                    if (it.id == productId && target?.isCustom == true) false else true
                }

                state.copy(
                    products = unlisted,
                    isFormBottomSheetOpen = false,
                    selectedProductForEdit = null,
                    isLoading = false,
                    snackbarMessage = "\"${target?.name ?: "Vegetable"}\" deleted from store."
                )
            }
        }
    }
}
