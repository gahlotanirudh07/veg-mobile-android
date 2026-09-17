package com.freshveg.app.features.catalog.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.network.CategoryDto
import com.freshveg.app.core.network.ProductDto
import com.freshveg.app.core.network.VegApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatalogUiState(
    val products: List<ProductDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val sellerCode: String? = null,
    val cutoffTime: String = "21:00",
    val cartItems: Map<String, Int> = emptyMap(), // ProductId -> Qty
    val selectedUnitIndex: Map<String, Int> = emptyMap(), // ProductId -> UnitIndex
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val totalCartItems: Int get() = cartItems.values.sum()
    val totalCartPrice: Double get() = cartItems.entries.sumOf { (id, qty) ->
        val product = products.find { it.id == id }
        (product?.currentPrice ?: 0.0) * qty
    }
}

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val code = sessionManager.sellerCode.firstOrNull()
                _uiState.update { it.copy(sellerCode = code) }
            } catch (e: Exception) {}

            try {
                val prodRes = apiService.getProducts()
                val catRes = apiService.getCategories()
                val cutoffRes = apiService.getCutoffTime()

                _uiState.update {
                    it.copy(
                        products = prodRes.body() ?: emptyList(),
                        categories = catRes.body() ?: emptyList(),
                        cutoffTime = cutoffRes.body()?.cutoffTime ?: "21:00",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun selectCategory(catId: String?) {
        _uiState.update { it.copy(selectedCategoryId = catId) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onQuantityChange(productId: String, quantity: Int) {
        _uiState.update { state ->
            val updated = state.cartItems.toMutableMap()
            if (quantity <= 0) {
                updated.remove(productId)
            } else {
                updated[productId] = quantity
            }
            state.copy(cartItems = updated)
        }
    }

    fun onUnitChange(productId: String, unitIndex: Int) {
        _uiState.update { state ->
            val updated = state.selectedUnitIndex.toMutableMap()
            updated[productId] = unitIndex
            state.copy(selectedUnitIndex = updated)
        }
    }
}
