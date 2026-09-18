package com.freshveg.app.automation

import com.freshveg.app.core.network.*
import com.freshveg.app.features.buyer.orders.BuyerOrdersViewModel
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
class BuyerOrdersAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)

    private val mockBuyerOrders = listOf(
        OrderDto(
            id = "ord-1",
            orderNumber = "ORD-2026-001",
            customerId = "74",
            status = "FULFILLED",
            totalAmount = 850.0,
            items = listOf(
                OrderItemDto(
                    id = "item-1",
                    orderId = "ord-1",
                    productId = "101",
                    productNameSnapshot = "Tomato Hybrid",
                    unitTypeSnapshot = "KG",
                    quantity = 25.0,
                    deliveredQuantity = 26.2, // Discrepancy on scale
                    price = 30.0,
                    total = 786.0
                )
            )
        )
    )

    @Before
    fun setUp() {
        coEvery { apiService.getOrders() } returns Response.success(
            OrdersEnvelopeResponse(orders = mockBuyerOrders)
        )
        coEvery { apiService.getCutoffTime() } returns Response.success(
            CutoffDto(cutoffTime = "03:00 AM")
        )
        coEvery { apiService.getConnectedSeller() } returns Response.success(
            ConnectedSellerEnvelopeResponse(
                success = true,
                data = ConnectedSellerDto(id = "73", businessName = "Ultra veggies", sellerCode = "ULTRA99", mobile = "9555636926")
            )
        )
    }

    @Test
    fun testBuyerOrdersEnvelopeUnpacking() = runTest {
        val viewModel = BuyerOrdersViewModel(apiService)

        val state = viewModel.uiState.value
        assertEquals(1, state.orders.size)
        assertEquals("ORD-2026-001", state.orders[0].orderNumber)
        assertEquals("03:00 AM", state.cutoffTime)
    }

    @Test
    fun testScaleDiscrepancyCalculation() = runTest {
        val viewModel = BuyerOrdersViewModel(apiService)
        val order = viewModel.uiState.value.orders[0]
        val item = order.items[0]

        // Ordered vs Delivered check
        assertEquals(25.0, item.quantity, 0.001)
        assertEquals(26.2, item.deliveredQuantity ?: 0.0, 0.001)
        val diffKg = (item.deliveredQuantity ?: item.quantity) - item.quantity
        assertEquals(1.2, diffKg, 0.001)
    }

    @Test
    fun testOpenAndCloseOrderDetailModal() = runTest {
        val viewModel = BuyerOrdersViewModel(apiService)

        assertNull(viewModel.uiState.value.selectedOrder)

        val order = viewModel.uiState.value.orders[0]
        viewModel.openOrderDetail(order)
        assertEquals("ORD-2026-001", viewModel.uiState.value.selectedOrder?.orderNumber)

        viewModel.closeOrderDetail()
        assertNull(viewModel.uiState.value.selectedOrder)
    }

    @Test
    fun testBuyerOrdersDateSortingAndSearchFiltering() = runTest {
        val multiOrders = listOf(
            OrderDto(id = "ord-1", orderNumber = "ORD-001", totalAmount = 500.0, createdAt = "2026-09-15T03:00:00.000Z"),
            OrderDto(id = "ord-2", orderNumber = "ORD-002", totalAmount = 1500.0, createdAt = "2026-09-18T03:00:00.000Z"),
            OrderDto(id = "ord-3", orderNumber = "ORD-003", totalAmount = 800.0, createdAt = "2026-09-17T03:00:00.000Z")
        )
        coEvery { apiService.getOrders() } returns Response.success(OrdersEnvelopeResponse(orders = multiOrders))

        val viewModel = BuyerOrdersViewModel(apiService)
        assertEquals(3, viewModel.uiState.value.filteredOrders.size)

        // Newest First sort check
        viewModel.onSelectSortOrder("NEWEST")
        assertEquals("ORD-002", viewModel.uiState.value.filteredOrders[0].orderNumber)

        // Oldest First sort check
        viewModel.onSelectSortOrder("OLDEST")
        assertEquals("ORD-001", viewModel.uiState.value.filteredOrders[0].orderNumber)

        // Amount High -> Low
        viewModel.onSelectSortOrder("AMOUNT_HIGH")
        assertEquals(1500.0, viewModel.uiState.value.filteredOrders[0].totalAmount, 0.001)

        // Search Filter
        viewModel.onSearchQueryChange("003")
        assertEquals(1, viewModel.uiState.value.filteredOrders.size)
        assertEquals("ORD-003", viewModel.uiState.value.filteredOrders[0].orderNumber)
    }
}
