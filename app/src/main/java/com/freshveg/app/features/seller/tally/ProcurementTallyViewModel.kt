package com.freshveg.app.features.seller.tally

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.freshveg.app.core.network.*
import com.freshveg.app.core.ui.ProduceVisualUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ProcurementTallyUiState(
    val selectedTab: Int = 0, // 0: Tally, 1: Inward Purchases, 2: Overheads/Expenses, 3: P&L
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val tallyData: ProcurementTallyData? = null,
    val purchases: List<ProducePurchaseRecordDto> = emptyList(),
    val expenses: List<SellerExpenseRecordDto> = emptyList(),
    val pnlSummary: ProfitLossSummaryDto? = null,
    val allProducts: List<ProductDto> = emptyList(),
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ProcurementTallyViewModel @Inject constructor(
    private val apiService: VegApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProcurementTallyUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
        loadProducts()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun onDateChange(newDate: String) {
        _uiState.update { it.copy(selectedDate = newDate) }
        loadData()
    }

    fun loadProducts() {
        viewModelScope.launch {
            try {
                val res = apiService.getProducts()
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update { it.copy(allProducts = res.body()!!) }
                }
            } catch (e: Exception) {}
        }
    }

    fun loadData() {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            try {
                // Parallel fetches
                val tallyRes = apiService.getDailyProcurementTally(date)
                val purchasesRes = apiService.getProducePurchases(startDate = date, endDate = date)
                val expensesRes = apiService.getExpenses(startDate = date, endDate = date)
                val pnlRes = apiService.getProfitLoss(startDate = date, endDate = date)

                _uiState.update {
                    it.copy(
                        tallyData = tallyRes.body()?.data,
                        purchases = purchasesRes.body()?.data ?: emptyList(),
                        expenses = expensesRes.body()?.data ?: emptyList(),
                        pnlSummary = pnlRes.body()?.data,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load tally") }
            }
        }
    }

    fun createExpense(
        category: String,
        amount: Double,
        description: String,
        paidTo: String?,
        paymentMode: String
    ) {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val req = CreateExpenseRequest(
                    category = category,
                    amount = amount,
                    description = description,
                    paidTo = paidTo,
                    paymentMode = paymentMode,
                    expenseDate = date
                )
                val res = apiService.createExpense(req)
                if (res.isSuccessful) {
                    _uiState.update { it.copy(isSubmitting = false, successMessage = "Expense recorded successfully!") }
                    loadData()
                } else {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = res.errorBody()?.string() ?: "Failed to record expense") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to record expense") }
            }
        }
    }

    fun createPurchase(
        supplierName: String,
        supplierMobile: String?,
        items: List<CreateProducePurchaseItemDto>
    ) {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val req = CreateProducePurchaseRequest(
                    supplierName = supplierName,
                    supplierMobile = supplierMobile,
                    purchaseDate = date,
                    items = items
                )
                val res = apiService.createProducePurchase(req)
                if (res.isSuccessful) {
                    _uiState.update { it.copy(isSubmitting = false, successMessage = "Inward purchase recorded successfully!") }
                    loadData()
                } else {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = res.errorBody()?.string() ?: "Failed to record purchase") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to record purchase") }
            }
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            try {
                val res = apiService.deleteExpense(id)
                if (res.isSuccessful) {
                    _uiState.update { it.copy(successMessage = "Expense deleted") }
                    loadData()
                }
            } catch (e: Exception) {}
        }
    }

    fun deletePurchase(id: String) {
        viewModelScope.launch {
            try {
                val res = apiService.deleteProducePurchase(id)
                if (res.isSuccessful) {
                    _uiState.update { it.copy(successMessage = "Purchase deleted") }
                    loadData()
                }
            } catch (e: Exception) {}
        }
    }

    fun shareTallyOnWhatsApp(context: Context) {
        val tally = _uiState.value.tallyData?.tally ?: return
        if (tally.isEmpty()) return

        val date = _uiState.value.selectedDate
        val sb = StringBuilder()
        sb.append("📋 *MANDIEXPRESS — सुबह की मंडी खरीद पर्ची*\n")
        sb.append("📅 *तारीख:* $date\n")
        sb.append("📦 *कुल सब्जियां:* ${tally.size} आइटम्स\n")
        sb.append("━━━━━━━━━━━━━━━━━━\n\n")

        tally.forEach { item ->
            val emoji = ProduceVisualUtils.getProduceEmoji(item.productName, item.hindiName)
            val title = if (!item.hindiName.isNullOrEmpty()) "${item.productName} (${item.hindiName})" else item.productName
            val qty = "${item.totalOrderedQuantity} ${item.unitType}"
            val buyers = item.buyerNames.joinToString(", ")
            sb.append("$emoji *$title* ➜ *$qty*\n")
            if (buyers.isNotEmpty()) {
                sb.append("   ↳ 🏨 _${buyers}_\n")
            }
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━\n")
        sb.append("🚚 MandiExpress Procurement Management System")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Morning Procurement Tally")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
