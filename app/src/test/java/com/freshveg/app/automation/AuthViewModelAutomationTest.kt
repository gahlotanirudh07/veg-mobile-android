package com.freshveg.app.automation

import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.network.*
import com.freshveg.app.features.auth.viewmodel.AuthViewModel
import com.freshveg.app.util.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)
    private val sessionManager: SessionManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { sessionManager.rememberMe } returns flowOf(false)
        every { sessionManager.savedMobile } returns flowOf(null)
        every { sessionManager.savedPassword } returns flowOf(null)
        coEvery { apiService.getAuthConfig() } returns Response.success(
            AuthConfigResponse(authMode = AuthModeType.PASSWORD)
        )
    }

    @Test
    fun testMobileNumberSanitization() {
        val viewModel = AuthViewModel(apiService, sessionManager)

        // User enters formatted or noisy input "+91 955-563 6926 EXTRA"
        viewModel.onMobileChange("+91 955-563 6926 EXTRA")

        // Should strip all non-digits and cap at 10 digits
        assertEquals("9195556369", viewModel.uiState.value.mobile)

        // Normal 10-digit entry
        viewModel.onMobileChange("9555636926")
        assertEquals("9555636926", viewModel.uiState.value.mobile)
    }

    @Test
    fun testPasswordVisibilityToggles() {
        val viewModel = AuthViewModel(apiService, sessionManager)

        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun testSellerCodeUppercaseAndValidation() {
        val viewModel = AuthViewModel(apiService, sessionManager)

        viewModel.onSellerCodeChange(" mandi123 ")
        assertEquals("MANDI123", viewModel.uiState.value.sellerCode)
        assertTrue(viewModel.uiState.value.isSellerCodeValid)

        viewModel.onSellerCodeChange("abc")
        assertFalse(viewModel.uiState.value.isSellerCodeValid)
    }

    @Test
    fun testLoginValidationFailures() {
        val viewModel = AuthViewModel(apiService, sessionManager)

        var navigated = false
        viewModel.onMobileChange("123") // Too short
        viewModel.login { _, _, _ -> navigated = true }

        assertFalse(navigated)
        assertEquals("Please enter a valid 10-digit mobile number", viewModel.uiState.value.errorMessage)

        viewModel.onMobileChange("9555636926")
        viewModel.onPasswordChange("") // Missing password
        viewModel.login { _, _, _ -> navigated = true }

        assertFalse(navigated)
        assertEquals("Please enter your password", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun testSuccessfulSellerLoginAndSessionPersistence() = runTest {
        val mockUser = UserDto(
            id = "1",
            mobile = "9555636926",
            name = "Mohit Seller",
            role = "OWNER"
        )
        val mockCustomer = CustomerDto(
            id = "73",
            businessName = "Ultra veggies",
            sellerCode = "ULTRA99",
            isSeller = true,
            isBuyer = false
        )
        val mockResponse = LoginResponse(
            success = true,
            message = "Success",
            accessToken = "jwt-mock-token-abc",
            refreshToken = "refresh-token-xyz",
            user = mockUser,
            customer = mockCustomer
        )

        coEvery { apiService.login(any()) } returns Response.success(mockResponse)

        val viewModel = AuthViewModel(apiService, sessionManager)
        viewModel.onMobileChange("9555636926")
        viewModel.onPasswordChange("Pass@1234")

        var routedRole = ""
        var isSellerTarget = false
        viewModel.login { role, isSeller, _ ->
            routedRole = role
            isSellerTarget = isSeller
        }

        assertTrue(viewModel.uiState.value.isSuccess)
        assertEquals("OWNER", routedRole)
        assertTrue(isSellerTarget)

        // Verify session was persisted
        coVerify(exactly = 1) {
            sessionManager.saveSession(
                accessToken = "jwt-mock-token-abc",
                refreshToken = "refresh-token-xyz",
                userId = "1",
                name = "Mohit Seller",
                mobile = "9555636926",
                role = "OWNER",
                sellerCode = "ULTRA99",
                businessName = "Ultra veggies",
                isSeller = true,
                isBuyer = false,
                isStaff = false,
                canManageRates = true,
                canManageOrders = true,
                canManageBilling = true,
                canManageStaff = true
            )
        }
    }

    @Test
    fun testSuccessfulBuyerLoginRouting() = runTest {
        val mockUser = UserDto(
            id = "2",
            mobile = "9899923232",
            name = "TDH Restaurant",
            role = "CUSTOMER"
        )
        val mockCustomer = CustomerDto(
            id = "74",
            businessName = "The Dining House",
            sellerCode = null,
            isSeller = false,
            isBuyer = true
        )
        val mockResponse = LoginResponse(
            success = true,
            message = "Success",
            accessToken = "jwt-mock-token-buyer",
            refreshToken = "refresh-buyer",
            user = mockUser,
            customer = mockCustomer
        )

        coEvery { apiService.login(any()) } returns Response.success(mockResponse)

        val viewModel = AuthViewModel(apiService, sessionManager)
        viewModel.onMobileChange("9899923232")
        viewModel.onPasswordChange("password123")

        var routedRole = ""
        var isBuyerTarget = false
        viewModel.login { role, _, isBuyer ->
            routedRole = role
            isBuyerTarget = isBuyer
        }

        assertTrue(viewModel.uiState.value.isSuccess)
        assertEquals("CUSTOMER", routedRole)
        assertTrue(isBuyerTarget)
    }

    @Test
    fun testLoginApiErrorHandling() = runTest {
        coEvery { apiService.login(any()) } returns Response.error(
            401,
            "{\"error\":\"Invalid mobile or password\"}".toResponseBody()
        )

        val viewModel = AuthViewModel(apiService, sessionManager)
        viewModel.onMobileChange("9555636926")
        viewModel.onPasswordChange("WrongPass")

        viewModel.login { _, _, _ -> }

        assertFalse(viewModel.uiState.value.isSuccess)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }
}
