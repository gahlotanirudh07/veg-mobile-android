package com.freshveg.app.automation

import com.freshveg.app.core.network.*
import com.freshveg.app.features.seller.tally.ProcurementTallyViewModel
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
class ProcurementAndPnLAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)

    private val mockTallyItem = ProcurementTallyItemDto(
        productId = "101",
        productName = "Tomato Hybrid",
        hindiName = "टमाटर",
        unitType = "KG",
        totalOrderedQuantity = 150.0,
        orderCount = 4,
        buyerNames = listOf("TDH", "tandoori", "Bunny Bite", "Green Chilly")
    )

    private val mockPnl = ProfitLossSummaryDto(
        period = PnlPeriodDto(startDate = "2026-08-25", endDate = "2026-08-25"),
        metrics = PnlMetricsDto(
            totalRevenue = 15000.0,
            totalProduceCost = 11000.0,
            totalExpenses = 1200.0,
            grossProfit = 4000.0,
            grossMarginPercentage = 26.67,
            netProfit = 2800.0,
            netMarginPercentage = 18.67,
            purchaseCount = 3,
            expenseCount = 2,
            invoiceCount = 6
        )
    )

    @Before
    fun setUp() {
        coEvery { apiService.getDailyProcurementTally(any()) } returns Response.success(
            TallyEnvelopeResponse(
                success = true,
                data = ProcurementTallyData(
                    targetDate = "2026-08-25",
                    totalOrders = 4,
                    distinctItemsCount = 1,
                    tally = listOf(mockTallyItem)
                )
            )
        )
        coEvery { apiService.getProfitLoss(any(), any()) } returns Response.success(
            PnlEnvelopeResponse(success = true, data = mockPnl)
        )
        coEvery { apiService.getProducePurchases(any(), any()) } returns Response.success(
            PurchasesEnvelopeResponse(success = true, data = emptyList())
        )
        coEvery { apiService.getExpenses(any(), any()) } returns Response.success(
            ExpensesEnvelopeResponse(success = true, data = emptyList())
        )
        coEvery { apiService.getProducts() } returns Response.success(emptyList())
    }

    @Test
    fun testMorningDemandTallyAggregation() = runTest {
        val viewModel = ProcurementTallyViewModel(apiService)

        val state = viewModel.uiState.value
        assertNotNull(state.tallyData)
        assertEquals(1, state.tallyData?.distinctItemsCount)
        assertEquals(4, state.tallyData?.totalOrders)

        val item = state.tallyData!!.tally[0]
        assertEquals("Tomato Hybrid", item.productName)
        assertEquals(4, item.orderCount)
        assertEquals(4, item.buyerNames.size)
        assertTrue(item.buyerNames.contains("TDH"))
    }

    @Test
    fun testDailyPnLMathVerification() = runTest {
        val viewModel = ProcurementTallyViewModel(apiService)

        val pnl = viewModel.uiState.value.pnlSummary
        assertNotNull(pnl)

        val metrics = pnl!!.metrics
        // Mathematical invariant: Net Profit = Gross Revenue - (Produce Cost + Operating Expenses)
        val expectedTotalCost = metrics.totalProduceCost + metrics.totalExpenses
        assertEquals(12200.0, expectedTotalCost, 0.001)

        val expectedNetProfit = metrics.totalRevenue - expectedTotalCost
        assertEquals(expectedNetProfit, metrics.netProfit, 0.001)
        assertEquals(2800.0, metrics.netProfit, 0.001)

        // Net Margin %: (Net Profit / Total Revenue) * 100
        val expectedMargin = (metrics.netProfit / metrics.totalRevenue) * 100
        assertEquals(expectedMargin, metrics.netMarginPercentage, 0.01)
    }

    @Test
    fun testCreateExpenseSubmission() = runTest {
        val viewModel = ProcurementTallyViewModel(apiService)

        val capturedReq = slot<CreateExpenseRequest>()
        coEvery { apiService.createExpense(capture(capturedReq)) } returns Response.success(
            CreateExpenseEnvelopeResponse(success = true, message = "Expense created")
        )

        viewModel.createExpense(
            category = "TRANSPORT",
            amount = 450.0,
            description = "Mandi Tempo Auto Fare",
            paidTo = "Ramesh Driver",
            paymentMode = "CASH"
        )

        assertTrue(capturedReq.isCaptured)
        val req = capturedReq.captured
        assertEquals("TRANSPORT", req.category)
        assertEquals(450.0, req.amount, 0.001)
        assertEquals("Mandi Tempo Auto Fare", req.description)
        assertEquals("CASH", req.paymentMode)
    }

    @Test
    fun testCreateInwardPurchaseSubmission() = runTest {
        val viewModel = ProcurementTallyViewModel(apiService)

        val capturedReq = slot<CreateProducePurchaseRequest>()
        coEvery { apiService.createProducePurchase(capture(capturedReq)) } returns Response.success(
            CreatePurchaseEnvelopeResponse(success = true, message = "Purchase recorded")
        )

        val purchaseItems = listOf(
            CreateProducePurchaseItemDto(
                productId = "101",
                quantity = 150.0,
                unitType = "KG",
                buyingPricePerUnit = 22.0
            )
        )

        viewModel.createPurchase(
            supplierName = "Kishan Trader Azadpur",
            supplierMobile = "9810011223",
            items = purchaseItems
        )

        assertTrue(capturedReq.isCaptured)
        val req = capturedReq.captured
        assertEquals("Kishan Trader Azadpur", req.supplierName)
        assertEquals(1, req.items.size)
        assertEquals(150.0, req.items[0].quantity, 0.001)
        assertEquals(22.0, req.items[0].buyingPricePerUnit, 0.001)
    }
}
