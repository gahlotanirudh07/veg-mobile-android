package com.freshveg.app.automation

import com.freshveg.app.core.network.*
import com.freshveg.app.features.seller.invoices.InvoicesViewModel
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
class InvoicesAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)

    private val mockCustomerSummary = CustomerSummaryDto(
        id = "74",
        businessName = "The Dining House",
        primaryContactName = "Chef Sanjeev",
        mobile = "9899923232",
        address = "Connaught Place, New Delhi",
        sellerCode = "ULTRA99"
    )

    private val mockSellerDetail = SellerSummaryDetailDto(
        id = "73",
        businessName = "Ultra veggies",
        primaryContactName = "Mohit",
        mobile = "9555636926",
        address = "Shop 42, Mandi Wholesale",
        gstNumber = "07AAAAA0000A1Z5",
        sellerCode = "ULTRA99"
    )

    private val mockInvoices = listOf(
        InvoiceSummaryDto(
            id = "inv-1",
            invoiceNumber = "INV-2026-001",
            orderId = "ord-1",
            customerId = "74",
            customer = mockCustomerSummary,
            order = OrderDto(id = "ord-1", orderNumber = "ORD-1001", customerId = "74", status = "FULFILLED", totalAmount = 1500.0),
            subtotal = 1500.0,
            gstAmount = 0.0,
            totalAmount = 1500.0,
            previousBalance = 500.0,
            status = "GENERATED",
            invoiceDate = "2026-08-25T05:30:00.000Z",
            createdAt = "2026-08-25T05:30:00.000Z"
        )
    )

    private val mockPendingOrders = listOf(
        OrderDto(
            id = "ord-2",
            orderNumber = "ORD-1002",
            customerId = "75",
            customer = CustomerSummaryDto(id = "75", businessName = "Tandoori Nights", mobile = "9136489683"),
            status = "FULFILLED",
            totalAmount = 2400.0
        )
    )

    private val mockFullInvoiceDetail = InvoiceDetailDto(
        id = "inv-1",
        invoiceNumber = "INV-2026-001",
        orderId = "ord-1",
        customerId = "74",
        customer = mockCustomerSummary,
        seller = mockSellerDetail,
        items = listOf(
            InvoiceItemDetailDto(
                productNameSnapshot = "Tomato Hybrid",
                orderedQuantity = 50.0,
                deliveredQuantity = 50.0,
                unitTypeSnapshot = "KG",
                price = 30.0,
                total = 1500.0
            )
        ),
        subtotal = 1500.0,
        gstAmount = 0.0,
        totalAmount = 1500.0,
        previousBalance = 0.0,
        status = "GENERATED"
    )

    @Before
    fun setUp() {
        coEvery { apiService.getInvoices() } returns Response.success(
            InvoicesEnvelopeResponse(invoices = mockInvoices)
        )
        coEvery { apiService.getPendingInvoices() } returns Response.success(
            PendingInvoicesEnvelopeResponse(orders = mockPendingOrders)
        )
        coEvery { apiService.getInvoiceById("inv-1") } returns Response.success(
            InvoiceDetailEnvelopeResponse(invoice = mockFullInvoiceDetail)
        )
    }

    @Test
    fun testInvoicesEnvelopeAndPendingOrdersLoading() = runTest {
        val viewModel = InvoicesViewModel(apiService)

        val state = viewModel.uiState.value
        assertEquals(1, state.invoices.size)
        assertEquals("INV-2026-001", state.invoices[0].invoiceNumber)
        assertEquals(1, state.pendingOrders.size)
        assertEquals("ORD-1002", state.pendingOrders[0].orderNumber)
    }

    @Test
    fun testViewInvoiceDetailsEnvelopeUnpacking() = runTest {
        val viewModel = InvoicesViewModel(apiService)

        viewModel.viewInvoiceDetails("inv-1")

        val detail = viewModel.uiState.value.selectedInvoiceDetail
        assertNotNull(detail)
        assertEquals("INV-2026-001", detail?.invoiceNumber)
        assertEquals("The Dining House", detail?.customer?.businessName)
        assertEquals("Chef Sanjeev", detail?.customer?.primaryContactName)
        assertEquals("Ultra veggies", detail?.seller?.businessName)
        assertEquals(1, detail?.items?.size)
        assertEquals("Tomato Hybrid", detail?.items?.get(0)?.productNameSnapshot)
        assertEquals(1500.0, detail?.totalAmount ?: 0.0, 0.001)
    }

    @Test
    fun testGenerateInvoiceHydratesFullDetails() = runTest {
        val generatedRaw = mockFullInvoiceDetail.copy(id = "inv-new", invoiceNumber = "INV-2026-002", items = emptyList())
        val hydratedFull = mockFullInvoiceDetail.copy(id = "inv-new", invoiceNumber = "INV-2026-002")

        coEvery { apiService.generateInvoice("ord-2") } returns Response.success(generatedRaw)
        coEvery { apiService.getInvoiceById("inv-new") } returns Response.success(
            InvoiceDetailEnvelopeResponse(invoice = hydratedFull)
        )

        val viewModel = InvoicesViewModel(apiService)
        viewModel.generateInvoice("ord-2")

        val detail = viewModel.uiState.value.selectedInvoiceDetail
        assertNotNull(detail)
        assertEquals("INV-2026-002", detail?.invoiceNumber)
        assertEquals(1, detail?.items?.size) // Full items hydrated!
        assertTrue(viewModel.uiState.value.successMessage?.contains("INV-2026-002") == true)
    }

    @Test
    fun testCustomerFilteringAndSortingOnSellerInvoices() = runTest {
        val extraInvoices = listOf(
            mockInvoices[0],
            InvoiceSummaryDto(
                id = "inv-2",
                invoiceNumber = "INV-2026-002",
                orderId = "ord-2",
                customerId = "75",
                customer = CustomerSummaryDto(id = "75", businessName = "Tandoori Nights", mobile = "9136489683"),
                totalAmount = 3500.0,
                status = "GENERATED",
                invoiceDate = "2026-09-18T05:30:00.000Z",
                createdAt = "2026-09-18T05:30:00.000Z"
            )
        )
        coEvery { apiService.getInvoices() } returns Response.success(InvoicesEnvelopeResponse(invoices = extraInvoices))

        val viewModel = InvoicesViewModel(apiService)
        assertEquals(2, viewModel.uiState.value.filteredInvoices.size)

        // Verify unique customers
        val customers = viewModel.uiState.value.uniqueCustomers
        assertTrue(customers.contains("The Dining House"))
        assertTrue(customers.contains("Tandoori Nights"))

        // Filter by Customer
        viewModel.onSelectCustomer("Tandoori Nights")
        assertEquals(1, viewModel.uiState.value.filteredInvoices.size)
        assertEquals("INV-2026-002", viewModel.uiState.value.filteredInvoices[0].invoiceNumber)

        // Reset customer filter and sort by amount
        viewModel.onSelectCustomer("ALL")
        viewModel.onSelectSortOrder("AMOUNT_HIGH")
        assertEquals("INV-2026-002", viewModel.uiState.value.filteredInvoices[0].invoiceNumber) // 3500.0
        assertEquals("INV-2026-001", viewModel.uiState.value.filteredInvoices[1].invoiceNumber) // 1500.0
    }
}
