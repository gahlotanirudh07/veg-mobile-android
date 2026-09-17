package com.freshveg.app.features.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.network.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val selectedTab: Int = 0, // 0: Platform Analytics, 1: Sellers, 2: Custom Produce Review, 3: Users
    val analytics: PlatformAnalyticsResponse? = null,
    val sellers: List<AdminSellerDto> = emptyList(),
    val customProducts: List<CustomProductDto> = emptyList(),
    val users: List<AdminUserDto> = emptyList(),
    val isLoading: Boolean = false,
    val isPromoting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val apiService: VegApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAdminData()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun loadAdminData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val analyticsRes = apiService.getPlatformAnalytics()
                val sellersRes = apiService.getAdminSellers()
                val customProductsRes = apiService.getCustomProducts()
                val usersRes = apiService.getAdminUsers()

                _uiState.update {
                    it.copy(
                        analytics = analyticsRes.body(),
                        sellers = sellersRes.body() ?: emptyList(),
                        customProducts = customProductsRes.body() ?: emptyList(),
                        users = usersRes.body() ?: emptyList(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load admin data") }
            }
        }
    }

    fun promoteCustomProduct(productId: String, productName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPromoting = true, errorMessage = null) }
            try {
                val res = apiService.promoteCustomProduct(productId)
                if (res.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isPromoting = false,
                            successMessage = "🌟 '$productName' promoted to Global Master Catalog!"
                        )
                    }
                    loadAdminData()
                } else {
                    _uiState.update {
                        it.copy(
                            isPromoting = false,
                            errorMessage = res.errorBody()?.string() ?: "Failed to promote product"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPromoting = false, errorMessage = e.message ?: "Network error") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
