package com.freshveg.app.automation

import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.network.*
import com.freshveg.app.features.seller.rates.SellerRatesViewModel
import com.freshveg.app.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class RateBoardAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)

    private val mockRateItems = listOf(
        SellerRateDto(
            id = "1",
            name = "Tomato Hybrid",
            hindiName = "टमाटर",
            categoryId = "cat-veg",
            sellingPrice = 30.0,
            lastBuyingPrice = 24.0,
            isAvailable = true,
            unitType = UnitType.KG
        ),
        SellerRateDto(
            id = "2",
            name = "Onion Red",
            hindiName = "प्याज",
            categoryId = "cat-veg",
            sellingPrice = 40.0,
            lastBuyingPrice = 32.0,
            isAvailable = true,
            unitType = UnitType.KG
        )
    )

    @Before
    fun setUp() {
        every { sessionManager.sellerCode } returns flowOf("ULTRA99")
        every { sessionManager.role } returns flowOf("Ultra veggies")
        coEvery { apiService.getSellerRateCard() } returns Response.success(mockRateItems)
        coEvery { apiService.getCategories() } returns Response.success(emptyList())
    }

    @Test
    fun testRateLoadingAndGrossMarginCalculation() = runTest {
        val viewModel = SellerRatesViewModel(apiService, sessionManager)

        val items = viewModel.uiState.value.rateItems
        assertEquals(2, items.size)

        // Margin formula: ((Selling - Buying) / Selling) * 100
        // Tomato: ((30 - 24) / 30) * 100 = 20.0%
        assertEquals(20.0, items[0].grossMarginPercentage ?: 0.0, 0.001)

        // Onion: ((40 - 32) / 40) * 100 = 20.0%
        assertEquals(20.0, items[1].grossMarginPercentage ?: 0.0, 0.001)
    }

    @Test
    fun testRapidPriceSteppersAndFloorClamping() = runTest {
        val viewModel = SellerRatesViewModel(apiService, sessionManager)

        // Stepper: +₹2.0 on Tomato (30 + 2 = 32.0)
        viewModel.adjustPrice("1", 2.0)
        assertEquals(32.0, viewModel.uiState.value.rateItems[0].currentPrice, 0.001)
        assertTrue(viewModel.uiState.value.rateItems[0].isModified)
        assertEquals(1, viewModel.uiState.value.modifiedCount)

        // Stepper: -₹50.0 on Tomato (must clamp at 0.0, never negative)
        viewModel.adjustPrice("1", -50.0)
        assertEquals(0.0, viewModel.uiState.value.rateItems[0].currentPrice, 0.001)
    }

    @Test
    fun testBulkAdjustmentAndReset() = runTest {
        val viewModel = SellerRatesViewModel(apiService, sessionManager)

        // Apply +₹5.0 across all active produce
        viewModel.applyBulkAdjustmentToAll(5.0)

        // Tomato: 30 + 5 = 35.0, Onion: 40 + 5 = 45.0
        assertEquals(35.0, viewModel.uiState.value.rateItems[0].currentPrice, 0.001)
        assertEquals(45.0, viewModel.uiState.value.rateItems[1].currentPrice, 0.001)
        assertEquals(2, viewModel.uiState.value.modifiedCount)

        // Reset all changes back to original
        viewModel.resetAllChanges()
        assertEquals(30.0, viewModel.uiState.value.rateItems[0].currentPrice, 0.001)
        assertEquals(40.0, viewModel.uiState.value.rateItems[1].currentPrice, 0.001)
        assertEquals(0, viewModel.uiState.value.modifiedCount)
    }

    @Test
    fun testBatchSaveModifiedRates() = runTest {
        val viewModel = SellerRatesViewModel(apiService, sessionManager)

        viewModel.adjustPrice("1", 4.0) // Tomato 30 -> 34.0

        val capturedUpdates = slot<BulkUpdateRatesRequest>()
        coEvery { apiService.bulkUpdateSellerRates(capture(capturedUpdates)) } returns Response.success(
            BulkUpdateResponse(message = "Updated", count = 1)
        )

        viewModel.saveRates()

        assertTrue(capturedUpdates.isCaptured)
        val items = capturedUpdates.captured.updates
        assertEquals(1, items.size)
        assertEquals("1", items[0].productId)
        assertEquals(34.0, items[0].sellingPrice, 0.001)
    }
}
