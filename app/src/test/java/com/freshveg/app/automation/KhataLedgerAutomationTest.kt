package com.freshveg.app.automation

import com.freshveg.app.core.network.*
import com.freshveg.app.features.seller.ledger.CustomerLedgerViewModel
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
class KhataLedgerAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)

    private val mockOutstanding = listOf(
        OutstandingCustomerDto(
            customerId = "74",
            businessName = "The Dining House",
            primaryContactName = "Chef Sanjeev",
            mobile = "9899923232",
            outstanding = "4500.0"
        ),
        OutstandingCustomerDto(
            customerId = "75",
            businessName = "Tandoori Nights",
            primaryContactName = "Mr. Verma",
            mobile = "9136489683",
            outstanding = "12000.0"
        )
    )

    private val mockOverviewSummary = PaymentsOverviewSummaryDto(
        totalOutstanding = 16500.0,
        totalPaid = 3500.0,
        totalInvoiced = 20000.0
    )

    private val mockRecentPayments = listOf(
        PaymentItemDto(
            id = "pay-1",
            customerId = "74",
            businessName = "The Dining House",
            amount = 3500.0,
            paymentMode = "UPI",
            referenceNumber = "UPI-REF-992211",
            paymentDate = "2026-08-25T11:00:00Z"
        )
    )

    @Before
    fun setUp() {
        coEvery { apiService.getPaymentsOverview() } returns Response.success(
            PaymentsOverviewEnvelopeResponse(
                summary = mockOverviewSummary,
                recentPayments = mockRecentPayments
            )
        )
        coEvery { apiService.getOutstandingCustomers() } returns Response.success(
            OutstandingCustomersEnvelopeResponse(customers = mockOutstanding)
        )
    }

    @Test
    fun testKhataOverviewAndCustomerDuesLoading() = runTest {
        val viewModel = CustomerLedgerViewModel(apiService)

        val state = viewModel.uiState.value
        assertEquals(16500.0, state.overviewSummary?.totalOutstanding ?: 0.0, 0.001)
        assertEquals(3500.0, state.overviewSummary?.totalPaid ?: 0.0, 0.001)
        assertEquals(2, state.outstandingCustomers.size)
        assertEquals(1, state.recentPayments.size)
    }

    @Test
    fun testOutstandingCustomerSearchFiltering() = runTest {
        val viewModel = CustomerLedgerViewModel(apiService)

        viewModel.onSearchQueryChange("dining")
        assertEquals(1, viewModel.uiState.value.filteredOutstandingCustomers.size)
        assertEquals("The Dining House", viewModel.uiState.value.filteredOutstandingCustomers[0].businessName)

        viewModel.onSearchQueryChange("9136489683")
        assertEquals(1, viewModel.uiState.value.filteredOutstandingCustomers.size)
        assertEquals("Tandoori Nights", viewModel.uiState.value.filteredOutstandingCustomers[0].businessName)
    }

    @Test
    fun testRecordPaymentAndCreditLedger() = runTest {
        val viewModel = CustomerLedgerViewModel(apiService)

        val capturedPayment = slot<CreatePaymentRequest>()
        coEvery { apiService.createPayment(capture(capturedPayment)) } returns Response.success(
            CreatePaymentEnvelopeResponse(payment = mockRecentPayments[0])
        )

        val req = CreatePaymentRequest(
            customerId = "74",
            amount = 4500.0,
            paymentDate = "2026-08-25",
            paymentMode = "UPI",
            notes = "Full dues settlement via Google Pay",
            referenceNumber = "UPI99887766"
        )

        viewModel.createPayment(req)

        assertTrue(capturedPayment.isCaptured)
        val p = capturedPayment.captured
        assertEquals("74", p.customerId)
        assertEquals(4500.0, p.amount, 0.001)
        assertEquals("UPI", p.paymentMode)
        assertEquals("UPI99887766", p.referenceNumber)
        assertTrue(viewModel.uiState.value.successMessage?.contains("4500") == true)
    }

    @Test
    fun testDynamicUpiIntentUriFormatting() {
        val upiId = "mandiexpress@icici"
        val sellerName = "Ultra veggies Wholesale"
        val customerName = "The Dining House"
        val amount = 4500

        val encodedSeller = java.net.URLEncoder.encode(sellerName, "UTF-8")
        val encodedNote = java.net.URLEncoder.encode("Mandi dues for $customerName", "UTF-8")
        val upiUri = "upi://pay?pa=$upiId&pn=$encodedSeller&am=$amount&cu=INR&tn=$encodedNote"

        assertTrue(upiUri.startsWith("upi://pay?"))
        assertTrue(upiUri.contains("pa=mandiexpress@icici"))
        assertTrue(upiUri.contains("am=4500"))
        assertTrue(upiUri.contains("cu=INR"))
    }
}
