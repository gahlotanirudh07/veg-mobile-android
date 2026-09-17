package com.freshveg.app.automation

import com.freshveg.app.core.network.*
import com.freshveg.app.features.seller.orders.OrdersPipelineViewModel
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
class OrdersPipelineAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)

    private val mockOrders = listOf(
        OrderDto(
            id = "ord-1",
            orderNumber = "ORD-1001",
            customerId = "74",
            customer = CustomerSummaryDto(id = "74", businessName = "The Dining House", mobile = "9899923232"),
            status = "PENDING",
            totalAmount = 1500.0,
            items = listOf(
                OrderItemDto(
                    id = "item-1",
                    orderId = "ord-1",
                    productId = "101",
                    productNameSnapshot = "Tomato",
                    unitTypeSnapshot = "KG",
                    quantity = 20.0,
                    deliveredQuantity = null,
                    price = 30.0,
                    total = 600.0
                )
            )
        ),
        OrderDto(
            id = "ord-2",
            orderNumber = "ORD-1002",
            customerId = "75",
            customer = CustomerSummaryDto(id = "75", businessName = "Tandoori Nights", mobile = "9136489683"),
            status = "CONFIRMED",
            totalAmount = 2400.0,
            items = listOf(
                OrderItemDto(
                    id = "item-2",
                    orderId = "ord-2",
                    productId = "102",
                    productNameSnapshot = "Potato",
                    unitTypeSnapshot = "KG",
                    quantity = 50.0,
                    deliveredQuantity = null,
                    price = 25.0,
                    total = 1250.0
                )
            )
        ),
        OrderDto(
            id = "ord-3",
            orderNumber = "ORD-1003",
            customerId = "78",
            customer = CustomerSummaryDto(id = "78", businessName = "Bunny Bite N Sip", mobile = "8512035627"),
            status = "FULFILLED",
            totalAmount = 3100.0,
            items = listOf(
                OrderItemDto(
                    id = "item-3",
                    orderId = "ord-3",
                    productId = "101",
                    productNameSnapshot = "Tomato",
                    unitTypeSnapshot = "KG",
                    quantity = 30.0,
                    deliveredQuantity = 32.5, // Weighed on scale
                    price = 30.0,
                    total = 975.0
                )
            )
        )
    )

    @Before
    fun setUp() {
        coEvery { apiService.getOrders() } returns Response.success(
            OrdersEnvelopeResponse(orders = mockOrders)
        )
        coEvery { apiService.getCutoffTime() } returns Response.success(
            CutoffDto(cutoffTime = "03:00 AM")
        )
    }

    @Test
    fun testOrdersEnvelopeUnpackingAndStatusTabCounts() = runTest {
        val viewModel = OrdersPipelineViewModel(apiService)

        val state = viewModel.uiState.value
        assertEquals(3, state.orders.size)
        assertEquals(1, state.pendingCount)
        assertEquals(1, state.confirmedCount)
        assertEquals(1, state.fulfilledCount)
    }

    @Test
    fun testStatusTabFiltering() = runTest {
        val viewModel = OrdersPipelineViewModel(apiService)

        viewModel.onSelectStatusTab("PENDING")
        assertEquals(1, viewModel.uiState.value.filteredOrders.size)
        assertEquals("ORD-1001", viewModel.uiState.value.filteredOrders[0].orderNumber)

        viewModel.onSelectStatusTab("CONFIRMED")
        assertEquals(1, viewModel.uiState.value.filteredOrders.size)
        assertEquals("ORD-1002", viewModel.uiState.value.filteredOrders[0].orderNumber)

        viewModel.onSelectStatusTab("FULFILLED")
        assertEquals(1, viewModel.uiState.value.filteredOrders.size)
        assertEquals("ORD-1003", viewModel.uiState.value.filteredOrders[0].orderNumber)

        viewModel.onSelectStatusTab("ALL")
        assertEquals(3, viewModel.uiState.value.filteredOrders.size)
    }

    @Test
    fun testSearchFilteringAcrossRestaurantAndProduce() = runTest {
        val viewModel = OrdersPipelineViewModel(apiService)

        // Search by restaurant name
        viewModel.onSearchQueryChange("tandoori")
        assertEquals(1, viewModel.uiState.value.filteredOrders.size)
        assertEquals("Tandoori Nights", viewModel.uiState.value.filteredOrders[0].customer?.businessName)

        // Search by produce item snapshot
        viewModel.onSearchQueryChange("tomato")
        assertEquals(2, viewModel.uiState.value.filteredOrders.size) // ord-1 and ord-3
    }

    @Test
    fun testConfirmOrderStatusTransition() = runTest {
        val viewModel = OrdersPipelineViewModel(apiService)

        val capturedReq = slot<UpdateOrderStatusRequest>()
        coEvery { apiService.updateOrderStatus("ord-1", capture(capturedReq)) } returns Response.success(
            mockOrders[0].copy(status = "CONFIRMED")
        )

        viewModel.updateOrderStatus("ord-1", "CONFIRMED")

        assertTrue(capturedReq.isCaptured)
        assertEquals("CONFIRMED", capturedReq.captured.status)
    }

    @Test
    fun testWarehouseScaleFulfillmentZeroTrustPayload() = runTest {
        val viewModel = OrdersPipelineViewModel(apiService)

        val capturedReq = slot<FulfillOrderRequest>()
        coEvery { apiService.fulfillOrder("ord-2", capture(capturedReq)) } returns Response.success(
            mockOrders[1].copy(status = "FULFILLED")
        )

        // Enter scale weighment: 51.5 KG instead of 50.0 KG ordered
        val fulfillItems = listOf(
            FulfillOrderItem(orderItemId = "item-2", deliveredQuantity = 51.5)
        )

        viewModel.fulfillOrder("ord-2", fulfillItems)

        assertTrue(capturedReq.isCaptured)
        val items = capturedReq.captured.items
        assertEquals(1, items.size)
        assertEquals("item-2", items[0].orderItemId)
        assertEquals(51.5, items[0].deliveredQuantity, 0.001)
    }
}
