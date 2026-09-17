package com.freshveg.app.automation

import com.freshveg.app.core.network.*
import com.freshveg.app.features.buyer.catalogue.BuyerCatalogueViewModel
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
class BuyerCatalogueAutomationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val apiService: VegApiService = mockk(relaxed = true)

    private val mockProducts = listOf(
        ProductDto(
            id = "101",
            name = "Tomato",
            categoryId = "cat-veg",
            categoryName = "Vegetables",
            unitType = UnitType.KG,
            currentPrice = 35.0,
            isAvailable = true
        ),
        ProductDto(
            id = "102",
            name = "Potato",
            categoryId = "cat-veg",
            categoryName = "Vegetables",
            unitType = UnitType.KG,
            currentPrice = 28.0,
            isAvailable = true
        ),
        ProductDto(
            id = "103",
            name = "Coriander",
            categoryId = "cat-leafy",
            categoryName = "Leafy Greens",
            unitType = UnitType.BUNDLE,
            currentPrice = 15.0,
            isAvailable = true
        ),
        ProductDto(
            id = "104",
            name = "Dragon Fruit",
            categoryId = "cat-exotic",
            categoryName = "Exotics",
            unitType = UnitType.PIECE,
            currentPrice = 120.0,
            isAvailable = false // Out of stock
        )
    )

    private val mockCategories = listOf(
        CategoryDto(id = "cat-veg", name = "Vegetables"),
        CategoryDto(id = "cat-leafy", name = "Leafy Greens"),
        CategoryDto(id = "cat-exotic", name = "Exotics")
    )

    @Before
    fun setUp() {
        coEvery { apiService.getProducts() } returns Response.success(mockProducts)
        coEvery { apiService.getCategories() } returns Response.success(mockCategories)
        coEvery { apiService.getCutoffTime() } returns Response.success(CutoffDto(cutoffTime = "03:30 AM"))
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
    }

    @Test
    fun testStorefrontProductLoadingAndAvailabilityFilter() = runTest {
        val viewModel = BuyerCatalogueViewModel(apiService)

        val state = viewModel.uiState.value
        assertEquals(4, state.products.size)
        // Out of stock product (Dragon Fruit) must be excluded from filtered list
        assertEquals(3, state.filteredProducts.size)
        assertFalse(state.filteredProducts.any { it.name == "Dragon Fruit" })
    }

    @Test
    fun testCategoryFiltering() = runTest {
        val viewModel = BuyerCatalogueViewModel(apiService)

        viewModel.selectCategory("cat-leafy")
        val leafyList = viewModel.uiState.value.filteredProducts
        assertEquals(1, leafyList.size)
        assertEquals("Coriander", leafyList[0].name)

        viewModel.selectCategory("cat-veg")
        val vegList = viewModel.uiState.value.filteredProducts
        assertEquals(2, vegList.size)

        viewModel.selectCategory("ALL")
        assertEquals(3, viewModel.uiState.value.filteredProducts.size)
    }

    @Test
    fun testMultilingualSearchResolution() = runTest {
        val viewModel = BuyerCatalogueViewModel(apiService)

        // English search
        viewModel.onSearchQueryChange("tomato")
        assertEquals(1, viewModel.uiState.value.filteredProducts.size)
        assertEquals("Tomato", viewModel.uiState.value.filteredProducts[0].name)

        // Hindi search for Potato (आलू)
        viewModel.onSearchQueryChange("आलू")
        val potatoResults = viewModel.uiState.value.filteredProducts
        assertEquals(1, potatoResults.size)
        assertEquals("Potato", potatoResults[0].name)
    }

    @Test
    fun testCartSteppingAndRemovalLogic() = runTest {
        val viewModel = BuyerCatalogueViewModel(apiService)
        val tomato = mockProducts[0] // KG unit -> 5.0 kg step
        val coriander = mockProducts[2] // BUNDLE unit -> 1.0 step

        // Increment Tomato (first add -> 5.0 kg)
        viewModel.incrementQuantity(tomato)
        assertEquals(5.0, viewModel.uiState.value.cart[tomato.id])

        // Increment again (+5.0 kg -> 10.0 kg)
        viewModel.incrementQuantity(tomato)
        assertEquals(10.0, viewModel.uiState.value.cart[tomato.id])

        // Add Coriander (BUNDLE -> 1.0 step)
        viewModel.incrementQuantity(coriander)
        assertEquals(1.0, viewModel.uiState.value.cart[coriander.id])

        // Verify cart calculations
        assertEquals(2, viewModel.uiState.value.cartItemCount)
        // (10 kg * ₹35) + (1 bundle * ₹15) = 350 + 15 = ₹365
        assertEquals(365.0, viewModel.uiState.value.cartEstimatedTotal, 0.001)

        // Decrement Coriander (1.0 - 1.0 = 0.0 -> removed)
        viewModel.decrementQuantity(coriander)
        assertNull(viewModel.uiState.value.cart[coriander.id])
        assertEquals(1, viewModel.uiState.value.cartItemCount)

        // Clear cart
        viewModel.clearCart()
        assertEquals(0, viewModel.uiState.value.cartItemCount)
        assertEquals(0.0, viewModel.uiState.value.cartEstimatedTotal, 0.001)
    }

    @Test
    fun testZeroTrustOrderPlacementPayload() = runTest {
        val viewModel = BuyerCatalogueViewModel(apiService)
        val tomato = mockProducts[0]
        val potato = mockProducts[1]

        viewModel.setQuantity(tomato.id, 20.0)
        viewModel.setQuantity(potato.id, 15.0)

        val capturedRequest = slot<CreateOrderRequest>()
        coEvery { apiService.createOrder(capture(capturedRequest)) } returns Response.success(
            OrderDto(
                id = "ord-999",
                orderNumber = "ORD-2026-001",
                customerId = "cust-74",
                status = "PENDING",
                totalAmount = 1120.0
            )
        )

        viewModel.placeOrder(deliveryNotes = "Early morning 6 AM kitchen gate delivery")

        // Invariant: Verify CreateOrderRequest payload ONLY contains productId and quantity
        assertTrue(capturedRequest.isCaptured)
        val req = capturedRequest.captured
        assertEquals("Early morning 6 AM kitchen gate delivery", req.notes)
        assertEquals(2, req.orderItems.size)
        assertEquals("101", req.orderItems[0].productId)
        assertEquals(20.0, req.orderItems[0].quantity, 0.001)
        assertEquals("102", req.orderItems[1].productId)
        assertEquals(15.0, req.orderItems[1].quantity, 0.001)

        // Cart must be cleared after success
        assertEquals(0, viewModel.uiState.value.cartItemCount)
        assertNotNull(viewModel.uiState.value.orderSuccessDto)
    }
}
