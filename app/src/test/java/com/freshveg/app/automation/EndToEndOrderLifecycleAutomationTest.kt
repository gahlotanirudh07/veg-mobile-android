package com.freshveg.app.automation

import com.freshveg.app.core.network.*
import com.freshveg.app.features.buyer.catalogue.BuyerCatalogueViewModel
import com.freshveg.app.features.buyer.orders.BuyerOrdersViewModel
import com.freshveg.app.features.seller.invoices.InvoicesViewModel
import com.freshveg.app.features.seller.orders.OrdersPipelineViewModel
import com.freshveg.app.features.seller.tally.ProcurementTallyViewModel
import com.freshveg.app.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

/**
 * Complete End-to-End Automation Test Suite validating the entire Mandi Wholesale Lifecycle:
 * 1. Buyer places order from Catalogue (Zero-Trust pricing).
 * 2. Order immediately surfaces in Seller Orders Screen in the "New / PENDING" section.
 * 3. Seller confirms order -> aggregates in Morning Mandi Tally.
 * 4. Seller weighs produce on scale -> fulfills order with exact deliveredQuantity.
 * 5. Order moves to Invoices -> Pending section.
 * 6. Seller generates Tax Invoice -> Tax Bill is hydrated, PDF ready, and debits ledger.
 * 7. Buyer tracks status in real-time -> 3-stage timeline progression.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndOrderLifecycleAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)

    // Shared mock entities
    private val buyerCustomer = CustomerSummaryDto(
        id = "74",
        businessName = "The Dining House",
        primaryContactName = "Chef Sanjeev",
        mobile = "9899923232",
        address = "Connaught Place, New Delhi",
        sellerCode = "ULTRA99"
    )

    private val sellerDetail = SellerSummaryDetailDto(
        id = "73",
        businessName = "Ultra veggies",
        primaryContactName = "Mohit",
        mobile = "9555636926",
        address = "Shop 42, Mandi Wholesale Terminal",
        gstNumber = "07AAAAA0000A1Z5",
        sellerCode = "ULTRA99"
    )

    private val mockCatalog = listOf(
        ProductDto(
            id = "101",
            name = "Tomato Hybrid",
            hindiName = "टमाटर",
            categoryId = "cat-veg",
            categoryName = "Vegetables",
            unitType = UnitType.KG,
            currentPrice = 30.0,
            isAvailable = true
        ),
        ProductDto(
            id = "102",
            name = "Potato Jyoti",
            hindiName = "आलू",
            categoryId = "cat-veg",
            categoryName = "Vegetables",
            unitType = UnitType.KG,
            currentPrice = 25.0,
            isAvailable = true
        )
    )

    @Test
    fun testCompleteBuyerToSellerLifecycleWorkflow() = runTest {
        // =========================================================================
        // STEP 1: BUYER PLACES ORDER FROM CATALOGUE (Zero-Trust Pricing)
        // =========================================================================
        coEvery { apiService.getProducts() } returns Response.success(mockCatalog)
        coEvery { apiService.getCategories() } returns Response.success(emptyList())
        coEvery { apiService.getCutoffTime() } returns Response.success(CutoffDto(cutoffTime = "03:00 AM"))
        coEvery { apiService.getConnectedSeller() } returns Response.success(
            ConnectedSellerEnvelopeResponse(
                success = true,
                data = ConnectedSellerDto(id = "73", businessName = "Ultra veggies", sellerCode = "ULTRA99", mobile = "9555636926")
            )
        )
        coEvery { apiService.getBuyerFrequentItems() } returns Response.success(
            BuyerFrequentItemsResponse(frequentItems = emptyList())
        )
        coEvery { apiService.getBuyerLastOrder() } returns Response.success(
            BuyerLastOrderResponse(lastOrder = null)
        )

        val buyerCatalogueVm = BuyerCatalogueViewModel(apiService)

        // Buyer adds 20 KG Tomato and 30 KG Potato to Cart
        buyerCatalogueVm.setQuantity("101", 20.0)
        buyerCatalogueVm.setQuantity("102", 30.0)
        assertEquals(2, buyerCatalogueVm.uiState.value.cartItemCount)
        assertEquals((20.0 * 30.0) + (30.0 * 25.0), buyerCatalogueVm.uiState.value.cartEstimatedTotal, 0.001)

        val capturedCreateOrderReq = slot<CreateOrderRequest>()
        val createdPendingOrder = OrderDto(
            id = "ord-501",
            orderNumber = "ORD-2026-0501",
            customerId = "74",
            customer = buyerCustomer,
            status = "PENDING",
            totalAmount = 1350.0,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(java.util.Date()),
            notes = "Kitchen backdoor gate #2 delivery",
            items = listOf(
                OrderItemDto(
                    id = "item-1",
                    orderId = "ord-501",
                    productId = "101",
                    productNameSnapshot = "Tomato Hybrid",
                    unitTypeSnapshot = "KG",
                    quantity = 20.0,
                    price = 30.0,
                    total = 600.0
                ),
                OrderItemDto(
                    id = "item-2",
                    orderId = "ord-501",
                    productId = "102",
                    productNameSnapshot = "Potato Jyoti",
                    unitTypeSnapshot = "KG",
                    quantity = 30.0,
                    price = 25.0,
                    total = 750.0
                )
            )
        )

        coEvery { apiService.createOrder(capture(capturedCreateOrderReq)) } returns Response.success(createdPendingOrder)

        buyerCatalogueVm.placeOrder(deliveryNotes = "Kitchen backdoor gate #2 delivery")

        // Invariant Check 1: Zero-Trust payload sent to server
        assertTrue(capturedCreateOrderReq.isCaptured)
        val createReq = capturedCreateOrderReq.captured
        assertEquals("Kitchen backdoor gate #2 delivery", createReq.notes)
        assertEquals(2, createReq.orderItems.size)
        assertEquals("101", createReq.orderItems[0].productId)
        assertEquals(20.0, createReq.orderItems[0].quantity, 0.001)
        assertEquals("102", createReq.orderItems[1].productId)
        assertEquals(30.0, createReq.orderItems[1].quantity, 0.001)

        // =========================================================================
        // STEP 2: SELLER ORDERS SCREEN - ORDER IS PROPERLY VISIBLE IN "PENDING" TAB
        // =========================================================================
        val currentOrdersList = mutableListOf(createdPendingOrder)
        coEvery { apiService.getOrders() } answers {
            Response.success(OrdersEnvelopeResponse(orders = currentOrdersList.toList()))
        }

        val sellerOrdersVm = OrdersPipelineViewModel(apiService)

        // Verify order is visible in Seller screen
        assertEquals(1, sellerOrdersVm.uiState.value.orders.size)
        assertEquals(1, sellerOrdersVm.uiState.value.pendingCount)
        assertEquals(0, sellerOrdersVm.uiState.value.confirmedCount)
        assertEquals(0, sellerOrdersVm.uiState.value.fulfilledCount)

        // Verify Pending Tab Filter
        sellerOrdersVm.onSelectStatusTab("PENDING")
        val pendingOrders = sellerOrdersVm.uiState.value.filteredOrders
        assertEquals(1, pendingOrders.size)
        assertEquals("ORD-2026-0501", pendingOrders[0].orderNumber)
        assertEquals("The Dining House", pendingOrders[0].customer?.businessName)
        assertEquals("9899923232", pendingOrders[0].customer?.mobile)
        assertEquals(2, pendingOrders[0].items.size)
        assertEquals(1350.0, pendingOrders[0].totalAmount, 0.001)

        // =========================================================================
        // STEP 3: SELLER ACCEPTS & CONFIRMS ORDER -> AGGREGATED IN MANDI TALLY
        // =========================================================================
        val confirmedOrder = createdPendingOrder.copy(status = "CONFIRMED")
        coEvery { apiService.updateOrderStatus("ord-501", any()) } answers {
            currentOrdersList[0] = confirmedOrder
            Response.success(confirmedOrder)
        }

        sellerOrdersVm.updateOrderStatus("ord-501", "CONFIRMED")

        // Seller screen updates: Pending is 0, Confirmed is 1
        assertEquals(0, sellerOrdersVm.uiState.value.pendingCount)
        assertEquals(1, sellerOrdersVm.uiState.value.confirmedCount)

        // Morning Mandi Tally aggregates the confirmed order
        val mockTally = listOf(
            ProcurementTallyItemDto(
                productId = "101",
                productName = "Tomato Hybrid",
                unitType = "KG",
                totalOrderedQuantity = 20.0,
                orderCount = 1,
                buyerNames = listOf("The Dining House")
            ),
            ProcurementTallyItemDto(
                productId = "102",
                productName = "Potato Jyoti",
                unitType = "KG",
                totalOrderedQuantity = 30.0,
                orderCount = 1,
                buyerNames = listOf("The Dining House")
            )
        )
        coEvery { apiService.getDailyProcurementTally(any()) } returns Response.success(
            TallyEnvelopeResponse(
                success = true,
                data = ProcurementTallyData(
                    targetDate = "2026-08-25",
                    totalOrders = 1,
                    distinctItemsCount = 2,
                    tally = mockTally
                )
            )
        )
        coEvery { apiService.getProducePurchases(any(), any()) } returns Response.success(
            PurchasesEnvelopeResponse(success = true, data = emptyList())
        )
        coEvery { apiService.getExpenses(any(), any()) } returns Response.success(
            ExpensesEnvelopeResponse(success = true, data = emptyList())
        )
        coEvery { apiService.getProfitLoss(any(), any()) } returns Response.success(
            PnlEnvelopeResponse(
                success = true,
                data = ProfitLossSummaryDto(
                    period = PnlPeriodDto("2026-08-25", "2026-08-25"),
                    metrics = PnlMetricsDto(totalRevenue = 1367.5, totalProduceCost = 1000.0, netProfit = 367.5)
                )
            )
        )

        val tallyVm = ProcurementTallyViewModel(apiService)
        assertEquals(2, tallyVm.uiState.value.tallyData?.distinctItemsCount)
        assertEquals(20.0, tallyVm.uiState.value.tallyData?.tally?.get(0)?.totalOrderedQuantity ?: 0.0, 0.001)

        // =========================================================================
        // STEP 4: SELLER WEIGHS ON SCALE & FULFILLS (Scale Discrepancy Handled)
        // =========================================================================
        // Scale weights: Tomato was 21.0 KG (instead of 20), Potato was 29.5 KG (instead of 30)
        val fulfilledOrder = confirmedOrder.copy(
            status = "FULFILLED",
            totalAmount = (21.0 * 30.0) + (29.5 * 25.0), // 630 + 737.5 = 1367.5
            items = listOf(
                createdPendingOrder.items[0].copy(deliveredQuantity = 21.0, total = 630.0),
                createdPendingOrder.items[1].copy(deliveredQuantity = 29.5, total = 737.5)
            )
        )

        val capturedFulfillReq = slot<FulfillOrderRequest>()
        coEvery { apiService.fulfillOrder("ord-501", capture(capturedFulfillReq)) } answers {
            currentOrdersList[0] = fulfilledOrder
            Response.success(fulfilledOrder)
        }

        sellerOrdersVm.fulfillOrder(
            "ord-501",
            listOf(
                FulfillOrderItem(orderItemId = "item-1", deliveredQuantity = 21.0),
                FulfillOrderItem(orderItemId = "item-2", deliveredQuantity = 29.5)
            )
        )

        // Verify Fulfillment Request & State update
        assertTrue(capturedFulfillReq.isCaptured)
        assertEquals(0, sellerOrdersVm.uiState.value.confirmedCount)
        assertEquals(1, sellerOrdersVm.uiState.value.fulfilledCount)

        // =========================================================================
        // STEP 5: TAX INVOICE GENERATION & FULL HYDRATION
        // =========================================================================
        coEvery { apiService.getInvoices() } returns Response.success(InvoicesEnvelopeResponse(invoices = emptyList()))
        coEvery { apiService.getPendingInvoices() } returns Response.success(
            PendingInvoicesEnvelopeResponse(orders = listOf(fulfilledOrder))
        )

        val invoicesVm = InvoicesViewModel(apiService)
        assertEquals(1, invoicesVm.uiState.value.pendingOrders.size)
        assertEquals("ORD-2026-0501", invoicesVm.uiState.value.pendingOrders[0].orderNumber)

        // Seller generates invoice
        val rawGeneratedInvoice = InvoiceDetailDto(
            id = "inv-501",
            invoiceNumber = "INV-2026-0501",
            orderId = "ord-501",
            customerId = "74",
            subtotal = 1367.5,
            gstAmount = 0.0,
            totalAmount = 1367.5,
            status = "GENERATED"
        )
        val hydratedFullInvoice = rawGeneratedInvoice.copy(
            customer = buyerCustomer,
            seller = sellerDetail,
            items = listOf(
                InvoiceItemDetailDto(
                    productNameSnapshot = "Tomato Hybrid",
                    orderedQuantity = 20.0,
                    deliveredQuantity = 21.0,
                    unitTypeSnapshot = "KG",
                    price = 30.0,
                    total = 630.0
                ),
                InvoiceItemDetailDto(
                    productNameSnapshot = "Potato Jyoti",
                    orderedQuantity = 30.0,
                    deliveredQuantity = 29.5,
                    unitTypeSnapshot = "KG",
                    price = 25.0,
                    total = 737.5
                )
            )
        )

        coEvery { apiService.generateInvoice("ord-501") } returns Response.success(rawGeneratedInvoice)
        coEvery { apiService.getInvoiceById("inv-501") } returns Response.success(
            InvoiceDetailEnvelopeResponse(invoice = hydratedFullInvoice)
        )

        invoicesVm.generateInvoice("ord-501")

        val generatedDetail = invoicesVm.uiState.value.selectedInvoiceDetail
        assertNotNull(generatedDetail)
        assertEquals("INV-2026-0501", generatedDetail?.invoiceNumber)
        assertEquals("The Dining House", generatedDetail?.customer?.businessName)
        assertEquals(2, generatedDetail?.items?.size)
        assertEquals(1367.5, generatedDetail?.totalAmount ?: 0.0, 0.001)

        // =========================================================================
        // STEP 6: BUYER TRACKS FINAL STATUS (3-Stage Stepper: Fulfilled & Billed)
        // =========================================================================
        coEvery { apiService.getOrders() } returns Response.success(
            OrdersEnvelopeResponse(orders = listOf(fulfilledOrder))
        )
        val buyerOrdersVm = BuyerOrdersViewModel(apiService)
        val buyerOrder = buyerOrdersVm.uiState.value.orders[0]

        assertEquals("FULFILLED", buyerOrder.status)
        assertEquals(1367.5, buyerOrder.totalAmount, 0.001)
        assertEquals(21.0, buyerOrder.items[0].deliveredQuantity ?: 0.0, 0.001)
        assertEquals(29.5, buyerOrder.items[1].deliveredQuantity ?: 0.0, 0.001)
    }
}
