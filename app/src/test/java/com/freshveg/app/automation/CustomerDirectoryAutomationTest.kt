package com.freshveg.app.automation

import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.network.*
import com.freshveg.app.features.seller.customers.CustomersViewModel
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
class CustomerDirectoryAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)

    private val mockCustomers = listOf(
        CustomerDto(
            id = "74",
            businessName = "The Dining House",
            primaryContactName = "Chef Sanjeev",
            mobile = "9899923232",
            address = "Connaught Place, New Delhi",
            isBuyer = true,
            isSeller = false
        ),
        CustomerDto(
            id = "75",
            businessName = "Tandoori Nights",
            primaryContactName = "Mr. Verma",
            mobile = "9136489683",
            address = "Sector 18, Noida",
            isBuyer = true,
            isSeller = false
        )
    )

    private val mockOutstanding = listOf(
        OutstandingCustomerDto(
            customerId = "74",
            businessName = "The Dining House",
            primaryContactName = "Chef Sanjeev",
            mobile = "9899923232",
            outstanding = "4500.0"
        )
    )

    @Before
    fun setUp() {
        every { sessionManager.sellerCode } returns flowOf("ULTRA99")
        coEvery { apiService.getCustomers() } returns Response.success(mockCustomers)
        coEvery { apiService.getOutstandingCustomers() } returns Response.success(
            OutstandingCustomersEnvelopeResponse(customers = mockOutstanding)
        )
        coEvery { apiService.getConnectedSeller() } returns Response.success(
            ConnectedSellerEnvelopeResponse(
                success = true,
                data = ConnectedSellerDto(
                    id = "73",
                    businessName = "Ultra veggies",
                    sellerCode = "ULTRA99",
                    mobile = "9555636926"
                )
            )
        )
    }

    @Test
    fun testCustomersAndDuesLoading() = runTest {
        val viewModel = CustomersViewModel(apiService, sessionManager)

        val state = viewModel.uiState.value
        assertEquals(2, state.customers.size)
        assertEquals(1, state.outstandingMap.size)
        assertEquals(4500.0, state.outstandingMap["74"] ?: 0.0, 0.001)
        assertEquals("ULTRA99", state.sellerCode)
    }

    @Test
    fun testCustomerSearchByMultiFields() = runTest {
        val viewModel = CustomersViewModel(apiService, sessionManager)

        viewModel.onSearchQueryChange("noida")
        assertEquals(1, viewModel.uiState.value.filteredCustomers.size)
        assertEquals("Tandoori Nights", viewModel.uiState.value.filteredCustomers[0].businessName)

        viewModel.onSearchQueryChange("Sanjeev")
        assertEquals(1, viewModel.uiState.value.filteredCustomers.size)
        assertEquals("The Dining House", viewModel.uiState.value.filteredCustomers[0].businessName)
    }

    @Test
    fun testCreateNewRestaurantCustomer() = runTest {
        val viewModel = CustomersViewModel(apiService, sessionManager)

        val capturedReq = slot<CreateCustomerRequest>()
        coEvery { apiService.createCustomer(capture(capturedReq)) } returns Response.success(
            CustomerDto(id = "76", businessName = "Royal Dhaba", mobile = "9811223344")
        )

        val newCustomer = CreateCustomerRequest(
            businessName = "Royal Dhaba",
            primaryContactName = "Balwant Singh",
            mobile = "9811223344",
            address = "GT Road Murthal"
        )

        viewModel.createCustomer(newCustomer)

        assertTrue(capturedReq.isCaptured)
        val req = capturedReq.captured
        assertEquals("Royal Dhaba", req.businessName)
        assertEquals("9811223344", req.mobile)
        assertTrue(viewModel.uiState.value.successMessage?.contains("Royal Dhaba") == true)
    }
}
