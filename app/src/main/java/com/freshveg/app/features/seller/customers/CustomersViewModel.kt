package com.freshveg.app.features.seller.customers

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
import javax.inject.Inject

data class CustomersUiState(
    val customers: List<CustomerDto> = emptyList(),
    val outstandingMap: Map<String, Double> = emptyMap(),
    val sellerCode: String? = null,
    val connectedSeller: ConnectedSellerDto? = null,
    val searchQuery: String = "",
    val selectedCustomerForEdit: CustomerDto? = null,
    val selectedCustomerForView: CustomerDto? = null,
    val isAddCustomerOpen: Boolean = false,
    val isShareCodeOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredCustomers: List<CustomerDto> get() {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) return customers
        return customers.filter {
            it.businessName.lowercase().contains(q) ||
                    it.primaryContactName.lowercase().contains(q) ||
                    it.mobile.contains(q) ||
                    it.address.lowercase().contains(q)
        }
    }
}

@HiltViewModel
class CustomersViewModel @Inject constructor(
    private val apiService: VegApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomersUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val savedCode = sessionManager.sellerCode.firstOrNull()
                val customersRes = apiService.getCustomers()
                val outstandingRes = apiService.getOutstandingCustomers()
                val sellerRes = apiService.getConnectedSeller()

                val duesMap = mutableMapOf<String, Double>()
                outstandingRes.body()?.customers?.forEach { c ->
                    duesMap[c.customerId] = c.outstandingAmount
                }

                val sellerData = sellerRes.body()?.data

                _uiState.update {
                    it.copy(
                        customers = customersRes.body() ?: emptyList(),
                        outstandingMap = duesMap,
                        sellerCode = sellerData?.sellerCode ?: savedCode,
                        connectedSeller = sellerData,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load customers") }
            }
        }
    }

    fun openAddCustomer() {
        _uiState.update { it.copy(isAddCustomerOpen = true) }
    }

    fun closeAddCustomer() {
        _uiState.update { it.copy(isAddCustomerOpen = false) }
    }

    fun openEditCustomer(customer: CustomerDto) {
        _uiState.update { it.copy(selectedCustomerForEdit = customer, selectedCustomerForView = null) }
    }

    fun closeEditCustomer() {
        _uiState.update { it.copy(selectedCustomerForEdit = null) }
    }

    fun openViewCustomer(customer: CustomerDto) {
        _uiState.update { it.copy(selectedCustomerForView = customer) }
    }

    fun closeViewCustomer() {
        _uiState.update { it.copy(selectedCustomerForView = null) }
    }

    fun openShareCode() {
        _uiState.update { it.copy(isShareCodeOpen = true) }
    }

    fun closeShareCode() {
        _uiState.update { it.copy(isShareCodeOpen = false) }
    }

    fun createCustomer(request: CreateCustomerRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val res = apiService.createCustomer(request)
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isAddCustomerOpen = false,
                            successMessage = "✅ Restaurant '${request.businessName}' added successfully!"
                        )
                    }
                    loadData()
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = res.errorBody()?.string() ?: "Failed to add customer"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to create customer") }
            }
        }
    }

    fun updateCustomer(id: String, request: UpdateCustomerRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val res = apiService.updateCustomer(id, request)
                if (res.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            selectedCustomerForEdit = null,
                            successMessage = "✅ Restaurant profile updated successfully!"
                        )
                    }
                    loadData()
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = res.errorBody()?.string() ?: "Failed to update customer"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to update customer") }
            }
        }
    }

    fun deleteCustomer(id: String) {
        viewModelScope.launch {
            try {
                val res = apiService.deleteCustomer(id)
                if (res.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            selectedCustomerForView = null,
                            successMessage = "Customer removed successfully"
                        )
                    }
                    loadData()
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to delete customer") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to delete customer") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
