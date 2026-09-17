package com.freshveg.app.features.seller.analytics

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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class DateRangePeriod(val label: String, val days: Int) {
    TODAY("Today", 0),
    LAST_7_DAYS("7 Days", 7),
    LAST_30_DAYS("30 Days", 30),
    ALL_TIME("All Time", -1)
}

data class AnalyticsUiState(
    val selectedTab: Int = 0, // 0: Daily Sales, 1: Buyer Summary, 2: Produce Sales, 3: Collections
    val selectedPeriod: DateRangePeriod = DateRangePeriod.LAST_7_DAYS,
    val dailySales: DailySalesReportResponse? = null,
    val buyerSummary: BuyerSummaryReportResponse? = null,
    val produceSales: ProduceSalesReportResponse? = null,
    val collections: PaymentCollectionsReportResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val apiService: VegApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadReportData()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun selectPeriod(period: DateRangePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadReportData()
    }

    fun loadReportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val (startDate, endDate) = calculateDateRange(_uiState.value.selectedPeriod)

                val dailyRes = apiService.getDailySalesReport(startDate, endDate)
                val buyerRes = apiService.getBuyerSummaryReport(startDate, endDate)
                val produceRes = apiService.getProduceSalesReport(startDate, endDate)
                val collectionsRes = apiService.getPaymentCollectionsReport(startDate, endDate)

                _uiState.update {
                    it.copy(
                        dailySales = dailyRes.body(),
                        buyerSummary = buyerRes.body(),
                        produceSales = produceRes.body(),
                        collections = collectionsRes.body(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load reports") }
            }
        }
    }

    private fun calculateDateRange(period: DateRangePeriod): Pair<String?, String?> {
        if (period == DateRangePeriod.ALL_TIME) return Pair(null, null)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)

        if (period == DateRangePeriod.TODAY) {
            return Pair(endDate, endDate)
        }

        cal.add(Calendar.DAY_OF_YEAR, -period.days)
        val startDate = sdf.format(cal.time)
        return Pair(startDate, endDate)
    }

    fun shareWhatsAppReport(context: Context) {
        val state = _uiState.value
        val daily = state.dailySales?.summary
        val collections = state.collections?.summary

        val text = "📊 *MandiExpress — Executive Analytics Summary*\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "📅 *Period:* ${state.selectedPeriod.label}\n\n" +
                "💰 *Total Revenue:* ₹${daily?.overallRevenue?.toInt() ?: 0}\n" +
                "📦 *Total Orders:* ${daily?.overallOrders ?: 0}\n" +
                "⚖️ *Volume Dispatched:* ${daily?.overallWeight?.toInt() ?: 0} KG\n\n" +
                "💸 *Collections Breakdown:*\n" +
                "• 💵 Cash: ₹${collections?.totalCash?.toInt() ?: 0}\n" +
                "• 📱 UPI: ₹${collections?.totalUpi?.toInt() ?: 0}\n" +
                "• 🏦 Bank: ₹${collections?.totalBank?.toInt() ?: 0}\n" +
                "━━━━━━━━━━━━━━━━━━━━\n" +
                "Generated via MandiExpress Wholesale"

        try {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(intent, "Share Analytics Summary"))
        } catch (e: Exception) {}
    }
}
