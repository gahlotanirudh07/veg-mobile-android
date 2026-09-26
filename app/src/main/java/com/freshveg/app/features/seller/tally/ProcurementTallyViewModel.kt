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

    fun shareMethod1ItemBreakdown(context: Context) {
        val tallyData = _uiState.value.tallyData ?: return
        val tally = tallyData.tally
        if (tally.isEmpty()) return

        val date = _uiState.value.selectedDate
        val totalOrders = tallyData.totalOrders
        val totalItems = if (tallyData.totalItemsCount > 0) tallyData.totalItemsCount else tally.sumOf { it.orderCount }
        val varieties = tally.size

        val sb = StringBuilder()
        sb.append("📋 *MANDIEXPRESS — सुबह की मंडी खरीद पर्ची*\n")
        sb.append("📅 *तारीख:* $date\n")
        sb.append("📦 *कुल ऑर्डर्स:* $totalOrders  •  *कुल आइटम्स:* $totalItems ($varieties वैरायटी)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        tally.forEachIndexed { index, item ->
            val emoji = ProduceVisualUtils.getProduceEmoji(item.productName, item.hindiName)
            val title = if (!item.hindiName.isNullOrEmpty()) "${item.productName} (${item.hindiName})" else item.productName
            val formattedTotal = if (item.totalOrderedQuantity % 1.0 == 0.0) "${item.totalOrderedQuantity.toInt()}" else "${item.totalOrderedQuantity}"
            val totalQty = "$formattedTotal ${item.unitType}"

            val breakdownStr = if (item.buyerBreakdown.isNotEmpty()) {
                item.buyerBreakdown.joinToString(" + ") { b ->
                    val q = if (b.quantity % 1.0 == 0.0) "${b.quantity.toInt()}" else "${b.quantity}"
                    "$q ${b.unitType} (${b.buyerName})"
                }
            } else if (item.buyerNames.isNotEmpty()) {
                item.buyerNames.joinToString(" + ") { name ->
                    "${name}"
                }
            } else {
                ""
            }

            if (breakdownStr.isNotEmpty()) {
                sb.append("${index + 1}) $emoji *$title* ➜ $breakdownStr = *$totalQty*\n\n")
            } else {
                sb.append("${index + 1}) $emoji *$title* ➜ *$totalQty*\n\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🚚 MandiExpress Procurement Management System")

        dispatchShareIntent(context, sb.toString(), "Share Morning Procurement Tally (Item Breakdown)")
    }

    fun shareMethod2RestaurantWise(context: Context) {
        val tallyData = _uiState.value.tallyData ?: return
        val date = _uiState.value.selectedDate

        val sb = StringBuilder()
        sb.append("📋 *MANDIEXPRESS — रेस्टोरेंट अनुसार ऑर्डर पर्ची*\n")
        sb.append("📅 *तारीख:* $date\n")

        // Use byRestaurant from backend if present, else synthesize from tally buyerBreakdown
        val restaurants: List<RestaurantTallyDto> = if (tallyData.byRestaurant.isNotEmpty()) {
            tallyData.byRestaurant
        } else {
            // Synthesize client-side from buyerBreakdown
            val map = mutableMapOf<String, MutableList<RestaurantOrderItemDto>>()
            tallyData.tally.forEach { item ->
                item.buyerBreakdown.forEach { b ->
                    val list = map.getOrPut(b.buyerName) { mutableListOf() }
                    list.add(
                        RestaurantOrderItemDto(
                            productId = item.productId,
                            productName = item.productName,
                            hindiName = item.hindiName,
                            quantity = b.quantity,
                            unitType = b.unitType
                        )
                    )
                }
            }
            map.map { (name, items) ->
                RestaurantTallyDto(
                    buyerId = name,
                    buyerName = name,
                    itemsCount = items.size,
                    items = items
                )
            }
        }

        if (restaurants.isEmpty()) return

        val totalOrders = tallyData.totalOrders
        sb.append("📦 *कुल ऑर्डर्स:* $totalOrders  •  *कुल रेस्टोरेंट:* ${restaurants.size}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        restaurants.forEach { r ->
            sb.append("🏨 *${r.buyerName}* (${r.items.size} आइटम्स):\n")
            r.items.forEachIndexed { idx, item ->
                val emoji = ProduceVisualUtils.getProduceEmoji(item.productName, item.hindiName)
                val title = if (!item.hindiName.isNullOrEmpty()) "${item.productName} (${item.hindiName})" else item.productName
                val q = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}" else "${item.quantity}"
                sb.append("  ${idx + 1}) $emoji $title = *$q ${item.unitType}*\n")
            }
            sb.append("\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🚚 MandiExpress Procurement Management System")

        dispatchShareIntent(context, sb.toString(), "Share Restaurant Wise Summary")
    }

    private fun dispatchShareIntent(context: Context, text: String, title: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun shareTallyOnWhatsApp(context: Context) {
        shareMethod1ItemBreakdown(context)
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
