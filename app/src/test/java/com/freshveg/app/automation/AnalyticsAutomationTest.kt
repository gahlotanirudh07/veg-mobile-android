package com.freshveg.app.automation

import com.freshveg.app.core.network.*
import com.freshveg.app.features.admin.AdminViewModel
import com.freshveg.app.features.seller.analytics.AnalyticsViewModel
import com.freshveg.app.features.seller.analytics.DateRangePeriod
import com.freshveg.app.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)

    private val mockDailySales = DailySalesReportResponse(
        summary = DailySalesSummaryDto(
            overallOrders = 42,
            overallWeight = 1250.0,
            overallRevenue = 85000.0
        ),
        rows = emptyList()
    )

    private val mockBuyerSummary = BuyerSummaryReportResponse(
        summary = BuyerSummaryTotalsDto(
            totalBuyers = 2,
            totalOrders = 24,
            totalSales = 56000.0
        ),
        rows = listOf(
            BuyerSummaryRowDto(
                customerId = "74",
                businessName = "The Dining House",
                mobile = "9899923232",
                ordersCount = 14,
                totalSales = 32000.0,
                totalWeight = 800.0,
                averageOrderValue = 2285.71
            ),
            BuyerSummaryRowDto(
                customerId = "75",
                businessName = "Tandoori Nights",
                mobile = "9136489683",
                ordersCount = 10,
                totalSales = 24000.0,
                totalWeight = 450.0,
                averageOrderValue = 2400.0
            )
        )
    )

    private val mockPlatformAnalytics = PlatformAnalyticsResponse(
        totalGmv = 540000.0,
        activeSellers = 12,
        totalBuyers = 85,
        totalOrders = 340,
        totalProduceVolumeKg = 12500.0,
        activeRateCardsCount = 10
    )

    @Before
    fun setUp() {
        coEvery { apiService.getDailySalesReport(any(), any()) } returns Response.success(mockDailySales)
        coEvery { apiService.getBuyerSummaryReport(any(), any()) } returns Response.success(mockBuyerSummary)
        coEvery { apiService.getProduceSalesReport(any(), any()) } returns Response.success(
            ProduceSalesReportResponse(
                summary = ProduceSalesSummaryDto(totalProducts = 0, totalQuantitySold = 0.0, totalRevenue = 0.0),
                rows = emptyList()
            )
        )
        coEvery { apiService.getPaymentCollectionsReport(any(), any()) } returns Response.success(
            PaymentCollectionsReportResponse(
                summary = PaymentCollectionsSummaryDto(totalCollections = 0.0, totalCash = 0.0, totalUpi = 0.0, totalBank = 0.0, totalCheque = 0.0),
                rows = emptyList()
            )
        )
        coEvery { apiService.getPlatformAnalytics() } returns Response.success(mockPlatformAnalytics)
        coEvery { apiService.getAdminSellers() } returns Response.success(emptyList())
        coEvery { apiService.getCustomProducts() } returns Response.success(emptyList())
        coEvery { apiService.getAdminUsers() } returns Response.success(emptyList())
    }

    @Test
    fun testSellerAnalyticsAovAndDatePeriods() = runTest {
        val viewModel = AnalyticsViewModel(apiService)

        val state = viewModel.uiState.value
        assertEquals(85000.0, state.dailySales?.summary?.overallRevenue ?: 0.0, 0.001)
        assertEquals(42, state.dailySales?.summary?.overallOrders)

        // Switch date period to TODAY
        viewModel.selectPeriod(DateRangePeriod.TODAY)
        assertEquals(DateRangePeriod.TODAY, viewModel.uiState.value.selectedPeriod)
    }

    @Test
    fun testBuyerParetoRanking() = runTest {
        val viewModel = AnalyticsViewModel(apiService)

        val rows = viewModel.uiState.value.buyerSummary?.rows ?: emptyList()
        assertEquals(2, rows.size)
        assertEquals("The Dining House", rows[0].businessName)
        assertEquals(32000.0, rows[0].totalSales, 0.001)
    }

    @Test
    fun testSuperAdminPlatformMetricsAndMasterProductPromotion() = runTest {
        val adminViewModel = AdminViewModel(apiService)

        val state = adminViewModel.uiState.value
        assertEquals(540000.0, state.analytics?.totalGmv ?: 0.0, 0.001)
        assertEquals(12, state.analytics?.activeSellers)
        assertEquals(85, state.analytics?.totalBuyers)

        coEvery { apiService.promoteCustomProduct("prod-custom-1") } returns Response.success(Unit)

        adminViewModel.promoteCustomProduct("prod-custom-1", "Organic Kale")

        assertFalse(adminViewModel.uiState.value.isPromoting)
        assertTrue(adminViewModel.uiState.value.successMessage?.contains("Organic Kale") == true)
    }
}
