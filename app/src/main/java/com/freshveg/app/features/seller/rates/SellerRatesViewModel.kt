package com.freshveg.app.features.seller.rates

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.network.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class RateItemUiState(
    val product: SellerRateDto,
    val originalPrice: Double,
    val currentPrice: Double,
    val originalAvailability: Boolean,
    val isAvailable: Boolean
) {
    val isModified: Boolean get() = Math.abs(currentPrice - originalPrice) > 0.01 || isAvailable != originalAvailability

    val grossMarginPercentage: Double? get() {
        val buying = product.lastBuyingPrice
        return if (currentPrice > 0 && buying != null && buying > 0) {
            val margin = ((currentPrice - buying) / currentPrice) * 100
            Math.round(margin * 10.0) / 10.0
        } else null
    }
}

data class SellerRatesUiState(
    val rateItems: List<RateItemUiState> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val sellerCode: String? = null,
    val businessName: String? = null,
    val hideMasterCatalogue: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredItems: List<RateItemUiState> get() = rateItems.filter { item ->
        val isInActiveStore = item.product.isInStore || item.product.isCustom || item.originalPrice > 0 || item.currentPrice > 0
        if (!isInActiveStore) return@filter false

        val prod = item.product
        val matchesSearch = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                (prod.hindiName?.contains(searchQuery, ignoreCase = true) == true) ||
                (prod.category?.name?.contains(searchQuery, ignoreCase = true) == true)

        val matchesCat = selectedCategoryId == null || prod.categoryId == selectedCategoryId
        matchesSearch && matchesCat
    }

    val modifiedCount: Int get() = rateItems.count { it.isModified }
    val totalPricedCount: Int get() = filteredItems.count { it.currentPrice > 0 }
}

@HiltViewModel
class SellerRatesViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerRatesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionManager.hideMasterCatalogue.collect { hide ->
                _uiState.update { it.copy(hideMasterCatalogue = hide) }
            }
        }
        loadRates()
    }

    fun loadRates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                val code = sessionManager.sellerCode.firstOrNull()
                val business = sessionManager.role.firstOrNull()
                _uiState.update { it.copy(sellerCode = code, businessName = business) }
            } catch (e: Exception) {}

            try {
                val ratesRes = apiService.getSellerRateCard()
                val catRes = apiService.getCategories()

                if (ratesRes.isSuccessful && ratesRes.body() != null) {
                    val items = ratesRes.body()!!.map { prod ->
                        RateItemUiState(
                            product = prod,
                            originalPrice = prod.sellingPrice,
                            currentPrice = prod.sellingPrice,
                            originalAvailability = prod.isAvailable,
                            isAvailable = prod.isAvailable
                        )
                    }
                    _uiState.update {
                        it.copy(
                            rateItems = items,
                            categories = catRes.body() ?: emptyList(),
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load rate card") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Network error") }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelect(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun adjustPrice(productId: String, delta: Double) {
        _uiState.update { state ->
            state.copy(
                rateItems = state.rateItems.map { item ->
                    if (item.product.id == productId) {
                        val newPrice = (item.currentPrice + delta).coerceAtLeast(0.0)
                        item.copy(currentPrice = newPrice)
                    } else item
                }
            )
        }
    }

    fun setDirectPrice(productId: String, newPrice: Double) {
        _uiState.update { state ->
            state.copy(
                rateItems = state.rateItems.map { item ->
                    if (item.product.id == productId) {
                        item.copy(currentPrice = newPrice.coerceAtLeast(0.0))
                    } else item
                }
            )
        }
    }

    fun toggleAvailability(productId: String) {
        _uiState.update { state ->
            state.copy(
                rateItems = state.rateItems.map { item ->
                    if (item.product.id == productId) {
                        item.copy(isAvailable = !item.isAvailable)
                    } else item
                }
            )
        }
    }

    fun applyBulkAdjustmentToAll(delta: Double) {
        _uiState.update { state ->
            state.copy(
                rateItems = state.rateItems.map { item ->
                    if (item.currentPrice > 0) {
                        val newPrice = (item.currentPrice + delta).coerceAtLeast(0.0)
                        item.copy(currentPrice = newPrice)
                    } else item
                }
            )
        }
    }

    fun resetAllChanges() {
        _uiState.update { state ->
            state.copy(
                rateItems = state.rateItems.map { item ->
                    item.copy(
                        currentPrice = item.originalPrice,
                        isAvailable = item.originalAvailability
                    )
                }
            )
        }
    }

    fun saveRates() {
        val modified = _uiState.value.rateItems.filter { it.isModified }
        if (modified.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }
            try {
                val updates = modified.map { item ->
                    RateUpdateItem(
                        productId = item.product.id,
                        sellingPrice = item.currentPrice,
                        isAvailable = item.isAvailable,
                        minimumOrderQuantity = item.product.minimumOrderQuantity
                    )
                }

                val res = apiService.bulkUpdateSellerRates(BulkUpdateRatesRequest(updates))
                if (res.isSuccessful) {
                    _uiState.update { state ->
                        state.copy(
                            rateItems = state.rateItems.map { item ->
                                item.copy(
                                    originalPrice = item.currentPrice,
                                    originalAvailability = item.isAvailable
                                )
                            },
                            isSaving = false,
                            successMessage = "✅ Successfully published ${modified.size} updated vegetable rates!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to update rates: ${res.errorBody()?.string()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save rates") }
            }
        }
    }

    fun shareRateCardOnWhatsApp(context: Context) {
        val state = _uiState.value
        val activePriced = state.rateItems.filter { it.currentPrice > 0 && it.isAvailable }
        if (activePriced.isEmpty()) return

        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("🌅 *MANDIEXPRESS — आज के ताजा मंडी भाव*\n")
        sb.append("📅 *तारीख:* $dateStr\n")
        if (!state.sellerCode.isNullOrBlank()) {
            sb.append("🏪 *विक्रेता कोड:* ${state.sellerCode}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━\n\n")

        activePriced.forEach { item ->
            val p = item.product
            val emoji = com.freshveg.app.core.ui.ProduceVisualUtils.getProduceEmoji(p.name, p.hindiName)
            val title = if (!p.hindiName.isNullOrEmpty()) "${p.name} (${p.hindiName})" else p.name
            val price = "₹${item.currentPrice.toInt()}/${p.unitType.name}"
            sb.append("$emoji *$title* — $price\n")
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━\n")
        sb.append("⚡ *सुबह 05:00 AM डिलीवरी के लिए रात 02:00 AM से पहले ऑर्डर करें!*\n")
        sb.append("📱 MandiExpress App से सीधा ऑर्डर करें।")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Mandi Rate Card via WhatsApp")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
