package com.freshveg.app.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// ----------------------------------------------------
// DTOs mapped 1:1 to veg-app/backend
// ----------------------------------------------------

enum class AuthModeType { PASSWORD, OTP, BOTH }

data class AuthConfigResponse(val authMode: AuthModeType)

data class LoginRequest(
    val mobile: String,
    val password: String? = null,
    val otp: String? = null
)

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: UserDto,
    val customer: CustomerDto,
    val accessToken: String,
    val refreshToken: String
)

data class PermissionsDto(
    val canManageRates: Boolean = true,
    val canManageOrders: Boolean = true,
    val canManageBilling: Boolean = true,
    val canManageStaff: Boolean = true
)

data class UserDto(
    val id: String,
    val name: String,
    val mobile: String,
    val role: String,
    val isMobileVerified: Boolean? = false,
    val isStaff: Boolean? = false,
    val designation: String? = null,
    val permissions: PermissionsDto? = null
)

data class CustomerDto(
    val id: String,
    val businessName: String,
    val primaryContactName: String = "",
    val mobile: String = "",
    val address: String = "",
    val gstNumber: String? = null,
    val creditLimit: String = "0",
    val notes: String? = null,
    val isBuyer: Boolean = true,
    val isSeller: Boolean = false,
    val sellerCode: String? = null,
    val isActive: Boolean = true,
    val discountType: String? = "NONE",
    val discountValue: Double? = 0.0,
    val discountNotes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    val creditLimitAmount: Double get() = creditLimit.toDoubleOrNull() ?: 0.0
}

data class RegisterSellerRequest(
    val businessName: String,
    val primaryContactName: String,
    val address: String,
    val mobile: String?,
    val password: String?
)

data class RegisterSellerResponse(
    val success: Boolean,
    val message: String,
    val seller: SellerSummaryDto,
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String
)

data class RegisterBuyerRequest(
    val businessName: String,
    val primaryContactName: String,
    val address: String,
    val sellerCode: String,
    val mobile: String?,
    val password: String?
)

data class RegisterBuyerResponse(
    val success: Boolean,
    val message: String,
    val buyer: BuyerDetailsDto,
    val seller: SellerSummaryDto,
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String
)

data class BuyerDetailsDto(
    val id: String,
    val businessName: String,
    val primaryContactName: String,
    val mobile: String,
    val sellerCode: String
)

data class SellerSummaryDto(
    val id: String,
    val businessName: String,
    val sellerCode: String
)

enum class UnitType { KG, GRAM, PACKET, CRATE, PIECE, BUNDLE }

data class WeightOption(
    val label: String,
    val unitType: UnitType,
    val weightInGrams: Int,
    val price: Double
)

data class CategorySummaryDto(
    val id: String? = null,
    val name: String? = null
)

data class CartItemDetail(
    val product: ProductDto,
    val quantity: Double
) {
    val lineTotal: Double get() = product.effectivePrice * quantity
}

data class ProductDto(
    val id: String = "",
    val name: String = "Produce",
    val hindiName: String? = null,
    val masterCode: String? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val category: CategorySummaryDto? = null,
    val unitType: UnitType = UnitType.KG,
    val currentPrice: Double = 0.0,
    val basePrice: Double? = null,
    val hasCustomDiscount: Boolean = false,
    val discountType: String? = null,
    val discountValue: Double? = null,
    val discountAmountPerUnit: Double? = null,
    val minimumOrderQuantity: Double = 1.0,
    val isAvailable: Boolean = true,
    val isFeatured: Boolean = false,
    val description: String? = null,
    val imageUrl: String? = null,
    val isCustom: Boolean = false,
    val isInStore: Boolean = true
) {
    val safeName: String get() = if (name.isNullOrBlank()) "Produce" else name
    val resolvedCategoryName: String get() = categoryName ?: category?.name ?: "All"
    val effectivePrice: Double get() = currentPrice
    val marketPrice: Double? get() = basePrice
}

data class AddMasterProductRequest(
    val productId: String,
    val sellingPrice: Double,
    val minimumOrderQuantity: Double = 1.0,
    val isAvailable: Boolean = true
)

data class CategoryDto(
    val id: String = "",
    val name: String = "Category",
    val description: String? = null
) {
    val safeName: String get() = if (name.isNullOrBlank()) "Category" else name
}

data class CutoffDto(
    val cutoffTime: String = "03:00 AM"
)

data class OrderDto(
    val id: String = "",
    val orderNumber: String = "",
    val customerId: String? = null,
    val status: String = "PENDING", // PENDING, CONFIRMED, FULFILLED, CANCELLED
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val notes: String? = null,
    val processingDate: String? = null,
    val placedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val customer: CustomerSummaryDto? = null,
    val items: List<OrderItemDto> = emptyList(),
    val invoices: List<InvoiceSummaryDto> = emptyList()
) {
    val hasInvoice: Boolean get() = invoices.isNotEmpty()
    val activeInvoice: InvoiceSummaryDto? get() = invoices.firstOrNull()
    val buyer: CustomerSummaryDto? get() = customer
}

data class OrderItemDto(
    val id: String = "",
    val orderId: String? = null,
    val productId: String = "",
    @SerializedName(value = "productNameSnapshot", alternate = ["productName"])
    val productNameSnapshot: String? = null,
    @SerializedName(value = "unitTypeSnapshot", alternate = ["unitType"])
    val unitTypeSnapshot: String? = "KG",
    val hindiName: String? = null,
    val basePriceSnapshot: Double? = null,
    val discountAmount: Double? = null,
    val quantity: Double = 1.0,
    val deliveredQuantity: Double? = null,
    val price: Double = 0.0,
    val currentPrice: Double? = null,
    val total: Double = 0.0,
    val isAvailable: Boolean = true
) {
    val displayName: String get() = if (productNameSnapshot.isNullOrBlank()) "Produce Item" else productNameSnapshot
    val unitSnapshot: String get() = unitTypeSnapshot ?: "KG"
    val safeUnit: String get() = unitTypeSnapshot ?: "KG"
    val effectivePrice: Double get() = price
    val weighedQuantity: Double? get() = deliveredQuantity
    val unitPriceSnapshot: Double get() = price
}

data class CustomerSummaryDto(
    val id: String = "",
    val businessName: String = "",
    val primaryContactName: String? = null,
    val mobile: String = "",
    val address: String? = null,
    val sellerCode: String? = null
)

data class UpdateOrderStatusRequest(
    val status: String, // PENDING, CONFIRMED, FULFILLED, CANCELLED
    val notes: String? = null
)

data class FulfillOrderRequest(
    val items: List<FulfillOrderItem>
)

data class FulfillOrderItem(
    val orderItemId: String,
    val deliveredQuantity: Double
)

data class CreateOrderRequest(
    val deliverySlotId: String? = null,
    val deliveryAddress: String? = null,
    val notes: String? = null,
    val orderItems: List<CreateOrderItemRequest>
)

data class CreateOrderItemRequest(
    val productId: String,
    val quantity: Double
)

data class OrderResponse(
    val success: Boolean = true,
    val message: String = "",
    val orderId: String = ""
)

data class AccountSummaryResponse(
    val customerId: String = "",
    val businessName: String = "",
    val outstandingBalance: Double = 0.0,
    val creditLimit: Double = 0.0,
    val lastPaymentDate: String? = null
)

data class FrequentProductDetailsDto(
    val id: String? = null,
    val name: String? = null,
    val hindiName: String? = null,
    val currentPrice: Double? = null,
    val unitType: String? = null,
    val category: String? = null,
    val isAvailable: Boolean? = null
)

data class BuyerFrequentItemDto(
    val productId: String = "",
    val product: FrequentProductDetailsDto? = null,
    val productName: String? = null,
    val unitType: String? = "KG",
    val currentPrice: Double = 0.0,
    val orderCount: Int = 0,
    val totalQuantityOrdered: Double? = null,
    val lastQuantity: Double? = null,
    val lastOrderedQuantity: Double = 1.0,
    val isAvailable: Boolean = true
) {
    val displayName: String get() = product?.name ?: productName ?: "Produce"
    val displayHindiName: String? get() = product?.hindiName
    val displayPrice: Double get() = product?.currentPrice ?: currentPrice
    val displayUnit: String get() = product?.unitType ?: unitType ?: "KG"
    val resolvedLastQty: Double get() = lastQuantity ?: lastOrderedQuantity
}

data class BuyerFrequentItemsResponse(
    val frequentItems: List<BuyerFrequentItemDto> = emptyList()
)

data class BuyerLastOrderResponse(
    val lastOrder: OrderDto? = null
)

data class AppVersionDto(
    val latestVersion: String = "1.0.0",
    val versionCode: Int? = 1,
    val downloadUrl: String? = null,
    val releaseNotes: String? = null,
    val forceUpdate: Boolean = false,
    val publishedAt: String? = null
)

interface VegApiService {
    // 1. Auth (/auth)
    @GET("auth/config")
    suspend fun getAuthConfig(): Response<AuthConfigResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register-seller")
    suspend fun registerSeller(@Body request: RegisterSellerRequest): Response<RegisterSellerResponse>

    @POST("auth/register-buyer")
    suspend fun registerBuyer(@Body request: RegisterBuyerRequest): Response<RegisterBuyerResponse>

    @GET("health/db-warmup")
    suspend fun warmUpDatabase(): Response<Unit>

    @GET("app/version")
    suspend fun getAppVersion(): Response<AppVersionDto>

    @GET("products")
    suspend fun getProducts(@Query("categoryId") categoryId: String? = null): Response<List<ProductDto>>

    @GET("products/master-library")
    suspend fun getMasterLibrary(): Response<List<ProductDto>>

    @POST("products/add-from-master")
    suspend fun addMasterProductToStore(@Body request: AddMasterProductRequest): Response<ProductDto>

    @POST("products")
    suspend fun createProduct(@Body product: ProductDto): Response<ProductDto>

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body product: ProductDto): Response<ProductDto>

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: String): Response<Unit>

    @GET("categories")
    suspend fun getCategories(): Response<List<CategoryDto>>

    // 3. Cutoff (/cutoff)
    @GET("cutoff")
    suspend fun getCutoffTime(): Response<CutoffDto>

    @PUT("cutoff")
    suspend fun updateCutoffTime(@Body request: CutoffDto): Response<CutoffDto>

    // 4. Orders (/orders)
    @GET("orders")
    suspend fun getOrders(@Query("status") status: String? = null): Response<OrdersEnvelopeResponse>

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") id: String): Response<OrderDto>

    @GET("orders/buyer/frequent-items")
    suspend fun getBuyerFrequentItems(): Response<BuyerFrequentItemsResponse>

    @GET("orders/buyer/last-order")
    suspend fun getBuyerLastOrder(): Response<BuyerLastOrderResponse>

    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderDto>

    @PUT("orders/{id}")
    suspend fun updateOrderStatus(
        @Path("id") id: String,
        @Body request: UpdateOrderStatusRequest
    ): Response<OrderDto>

    @PUT("orders/{id}/fulfill")
    suspend fun fulfillOrder(
        @Path("id") id: String,
        @Body request: FulfillOrderRequest
    ): Response<OrderDto>

    // 5. Customer Directory & Connected Seller (/customers)
    @GET("customers")
    suspend fun getCustomers(): Response<List<CustomerDto>>

    @POST("customers")
    suspend fun createCustomer(@Body request: CreateCustomerRequest): Response<CustomerDto>

    @PUT("customers/{id}")
    suspend fun updateCustomer(
        @Path("id") id: String,
        @Body request: UpdateCustomerRequest
    ): Response<CustomerDto>

    @DELETE("customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: String): Response<CustomerDto>

    @GET("customers/connected-seller")
    suspend fun getConnectedSeller(): Response<ConnectedSellerEnvelopeResponse>

    @GET("customers/{customerId}/account-summary")
    suspend fun getAccountSummary(@Path("customerId") customerId: String): Response<AccountSummaryResponse>

    // 6. Seller Rates Board (/seller-rates)
    @GET("seller-rates")
    suspend fun getSellerRateCard(): Response<List<SellerRateDto>>

    @PUT("seller-rates/bulk")
    suspend fun bulkUpdateSellerRates(@Body request: BulkUpdateRatesRequest): Response<BulkUpdateResponse>

    // 7. Seller Procurement Tally, Purchases & Expenses (/seller)
    @GET("seller/procurement-tally")
    suspend fun getDailyProcurementTally(@Query("date") date: String? = null): Response<TallyEnvelopeResponse>

    @GET("seller/purchases")
    suspend fun getProducePurchases(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<PurchasesEnvelopeResponse>

    @POST("seller/purchases")
    suspend fun createProducePurchase(@Body request: CreateProducePurchaseRequest): Response<CreatePurchaseEnvelopeResponse>

    @DELETE("seller/purchases/{id}")
    suspend fun deleteProducePurchase(@Path("id") id: String): Response<Unit>

    @GET("seller/expenses")
    suspend fun getExpenses(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("category") category: String? = null
    ): Response<ExpensesEnvelopeResponse>

    @POST("seller/expenses")
    suspend fun createExpense(@Body request: CreateExpenseRequest): Response<CreateExpenseEnvelopeResponse>

    @DELETE("seller/expenses/{id}")
    suspend fun deleteExpense(@Path("id") id: String): Response<Unit>

    @GET("seller/pnl")
    suspend fun getProfitLoss(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<PnlEnvelopeResponse>

    // 8. Invoices (/invoices)
    @GET("invoices")
    suspend fun getInvoices(): Response<InvoicesEnvelopeResponse>

    @GET("invoices/pending")
    suspend fun getPendingInvoices(): Response<PendingInvoicesEnvelopeResponse>

    @POST("invoices/{orderId}")
    suspend fun generateInvoice(@Path("orderId") orderId: String): Response<InvoiceDetailDto>

    @GET("invoices/{invoiceId}")
    suspend fun getInvoiceById(@Path("invoiceId") invoiceId: String): Response<InvoiceDetailEnvelopeResponse>

    // 9. Payments & Khata Ledger (/payments, /customers)
    @GET("payments/my-payments")
    suspend fun getMyPayments(): Response<CustomerLedgerResponse>

    @GET("payments/overview")
    suspend fun getPaymentsOverview(): Response<PaymentsOverviewEnvelopeResponse>

    @GET("customers/outstanding")
    suspend fun getOutstandingCustomers(): Response<OutstandingCustomersEnvelopeResponse>

    @GET("customers/{customerId}/ledger")
    suspend fun getCustomerLedger(@Path("customerId") customerId: String): Response<CustomerLedgerResponse>

    @POST("payments")
    suspend fun createPayment(@Body request: CreatePaymentRequest): Response<CreatePaymentEnvelopeResponse>

    @DELETE("payments/{paymentId}")
    suspend fun deletePayment(@Path("paymentId") paymentId: String): Response<Unit>

    // 10. Reports & Executive Analytics (/reports)
    @GET("reports/daily-sales")
    suspend fun getDailySalesReport(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<DailySalesReportResponse>

    @GET("reports/buyer-summary")
    suspend fun getBuyerSummaryReport(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<BuyerSummaryReportResponse>

    @GET("reports/produce-sales")
    suspend fun getProduceSalesReport(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ProduceSalesReportResponse>

    @GET("reports/payment-collections")
    suspend fun getPaymentCollectionsReport(
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<PaymentCollectionsReportResponse>

    // 11. Super Admin Control Center (/admin)
    @GET("admin/analytics")
    suspend fun getPlatformAnalytics(): Response<PlatformAnalyticsResponse>

    @GET("admin/users")
    suspend fun getAdminUsers(): Response<List<AdminUserDto>>

    @GET("admin/sellers")
    suspend fun getAdminSellers(): Response<List<AdminSellerDto>>

    @GET("admin/custom-products")
    suspend fun getCustomProducts(): Response<List<CustomProductDto>>

    @POST("admin/custom-products/{id}/promote")
    suspend fun promoteCustomProduct(@Path("id") id: String): Response<Unit>
}

data class SellerRateDto(
    val id: String,
    val masterCode: String? = null,
    val name: String,
    val hindiName: String? = null,
    val categoryId: String? = null,
    val category: CategoryDto? = null,
    val unitType: UnitType = UnitType.KG,
    val imageUrl: String? = null,
    val minimumOrderQuantity: Double = 1.0,
    val description: String? = null,
    val isCustom: Boolean = false,
    val sellingPrice: Double = 0.0,
    val lastBuyingPrice: Double? = null,
    val grossMarginPercentage: Double? = null,
    val isAvailable: Boolean = true,
    val isPriced: Boolean = false,
    val isInStore: Boolean = false
)

data class BulkUpdateRatesRequest(
    val updates: List<RateUpdateItem>
)

data class RateUpdateItem(
    val productId: String,
    val sellingPrice: Double,
    val isAvailable: Boolean,
    val minimumOrderQuantity: Double? = null
)

data class BulkUpdateResponse(
    val message: String,
    val count: Int
)

// --- Module 4: Procurement Tally & Expenses DTOs ---

data class TallyEnvelopeResponse(
    val success: Boolean,
    val data: ProcurementTallyData
)

data class BuyerItemBreakdownDto(
    val buyerId: String = "",
    val buyerName: String = "",
    val quantity: Double = 0.0,
    val unitType: String = "KG"
)

data class RestaurantOrderItemDto(
    val productId: String = "",
    val productName: String = "",
    val hindiName: String? = null,
    val quantity: Double = 0.0,
    val unitType: String = "KG"
)

data class RestaurantTallyDto(
    val buyerId: String = "",
    val buyerName: String = "",
    val itemsCount: Int = 0,
    val items: List<RestaurantOrderItemDto> = emptyList()
)

data class ProcurementTallyData(
    val targetDate: String,
    val totalOrders: Int,
    val distinctItemsCount: Int,
    val totalItemsCount: Int = 0,
    val tally: List<ProcurementTallyItemDto>,
    val byRestaurant: List<RestaurantTallyDto> = emptyList()
)

data class ProcurementTallyItemDto(
    val productId: String,
    val productName: String,
    val hindiName: String? = null,
    val categoryName: String = "General",
    val unitType: String = "KG",
    val currentSellingPrice: Double = 0.0,
    val lastBuyingPrice: Double? = null,
    val totalOrderedQuantity: Double = 0.0,
    val orderCount: Int = 0,
    val buyerNames: List<String> = emptyList(),
    val buyerBreakdown: List<BuyerItemBreakdownDto> = emptyList()
)

data class PurchasesEnvelopeResponse(
    val success: Boolean,
    val data: List<ProducePurchaseRecordDto>
)

data class CreatePurchaseEnvelopeResponse(
    val success: Boolean,
    val message: String,
    val data: ProducePurchaseRecordDto? = null
)

data class ProducePurchaseRecordDto(
    val id: String,
    val purchaseNumber: String,
    val supplierName: String,
    val supplierMobile: String? = null,
    val purchaseDate: String,
    val totalAmount: Double,
    val paymentStatus: String = "PAID",
    val paymentMode: String = "CASH",
    val notes: String? = null,
    val items: List<ProducePurchaseItemDto> = emptyList()
)

data class ProducePurchaseItemDto(
    val id: String,
    val productId: String,
    val productNameSnapshot: String,
    val quantity: Double,
    val unitTypeSnapshot: String,
    val buyingPricePerUnit: Double,
    val lineTotal: Double
)

data class CreateProducePurchaseRequest(
    val supplierName: String,
    val supplierMobile: String? = null,
    val purchaseDate: String? = null,
    val paymentStatus: String = "PAID",
    val paymentMode: String = "CASH",
    val notes: String? = null,
    val items: List<CreateProducePurchaseItemDto>
)

data class CreateProducePurchaseItemDto(
    val productId: String,
    val quantity: Double,
    val unitType: String = "KG",
    val buyingPricePerUnit: Double
)

data class ExpensesEnvelopeResponse(
    val success: Boolean,
    val data: List<SellerExpenseRecordDto>
)

data class CreateExpenseEnvelopeResponse(
    val success: Boolean,
    val message: String,
    val data: SellerExpenseRecordDto? = null
)

data class SellerExpenseRecordDto(
    val id: String,
    val category: String,
    val amount: Double,
    val description: String,
    val paidTo: String? = null,
    val paymentMode: String = "CASH",
    val expenseDate: String,
    val receiptUrl: String? = null,
    val createdAt: String
)

data class CreateExpenseRequest(
    val category: String,
    val amount: Double,
    val description: String,
    val paidTo: String? = null,
    val paymentMode: String = "CASH",
    val expenseDate: String? = null,
    val receiptUrl: String? = null
)

data class PnlEnvelopeResponse(
    val success: Boolean,
    val data: ProfitLossSummaryDto
)

data class ProfitLossSummaryDto(
    val period: PnlPeriodDto,
    val metrics: PnlMetricsDto,
    val categoryBreakdown: List<PnlCategoryBreakdownDto> = emptyList()
)

data class PnlPeriodDto(
    val startDate: String,
    val endDate: String
)

data class PnlMetricsDto(
    val totalRevenue: Double = 0.0,
    val totalProduceCost: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossMarginPercentage: Double = 0.0,
    val netProfit: Double = 0.0,
    val netMarginPercentage: Double = 0.0,
    val invoiceCount: Int = 0,
    val purchaseCount: Int = 0,
    val expenseCount: Int = 0
)

data class PnlCategoryBreakdownDto(
    val category: String,
    val amount: Double,
    val percentage: Double
)

// --- Module 6: Invoicing & Billing DTOs ---

data class InvoiceSummaryDto(
    val id: String = "",
    val invoiceNumber: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val previousBalance: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val status: String = "GENERATED",
    val invoiceDate: String? = null,
    val createdAt: String? = null,
    val customer: CustomerSummaryDto? = null,
    val order: OrderDto? = null
)

data class InvoiceDetailDto(
    val id: String = "",
    val invoiceNumber: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val previousBalance: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val status: String = "GENERATED",
    val invoiceDate: String? = null,
    val createdAt: String? = null,
    val customer: CustomerSummaryDto? = null,
    val seller: SellerSummaryDetailDto? = null,
    val order: OrderDto? = null,
    val items: List<InvoiceItemDetailDto> = emptyList()
)

data class SellerSummaryDetailDto(
    val id: String = "",
    val businessName: String = "Fresh Produce Wholesale",
    val primaryContactName: String? = null,
    val mobile: String = "",
    val address: String? = null,
    val gstNumber: String? = null,
    val sellerCode: String? = null
)

data class InvoiceItemDetailDto(
    val productNameSnapshot: String? = null,
    val orderedQuantity: Double? = null,
    val deliveredQuantity: Double = 0.0,
    val unitTypeSnapshot: String? = "KG",
    val basePriceSnapshot: Double? = null,
    val discountAmount: Double? = null,
    val price: Double = 0.0,
    val total: Double = 0.0
) {
    val displayName: String get() = productNameSnapshot ?: "Produce"
    val safeUnit: String get() = unitTypeSnapshot ?: "KG"
}

// --- Module 7: Payments & Khata Ledger DTOs ---

data class PaymentsOverviewEnvelopeResponse(
    val summary: PaymentsOverviewSummaryDto? = null,
    val recentPayments: List<PaymentItemDto> = emptyList(),
    val payments: List<PaymentItemDto> = emptyList()
)

data class PaymentsOverviewSummaryDto(
    val totalOutstanding: Double = 0.0,
    val overallOutstanding: Double = 0.0,
    val outstanding: Double = 0.0,
    val totalInvoiced: Double = 0.0,
    val totalPaid: Double = 0.0
)

data class PaymentItemDto(
    val id: String = "",
    val customerName: String? = null,
    val businessName: String? = null,
    val customerId: String = "",
    val paymentDate: String? = null,
    val amount: Double = 0.0,
    val paymentMode: String = "CASH", // CASH, UPI, BANK_TRANSFER, CHEQUE
    val referenceNumber: String? = null,
    val notes: String? = null,
    val recordedBy: String? = null
)

data class OutstandingCustomersEnvelopeResponse(
    val customers: List<OutstandingCustomerDto> = emptyList()
)

data class OutstandingCustomerDto(
    val customerId: String,
    val businessName: String,
    val primaryContactName: String? = null,
    val mobile: String,
    val totalSales: String = "0",
    val totalPayments: String = "0",
    val outstanding: String = "0"
) {
    val totalSalesAmount: Double get() = totalSales.toDoubleOrNull() ?: 0.0
    val totalPaidAmount: Double get() = totalPayments.toDoubleOrNull() ?: 0.0
    val outstandingAmount: Double get() = outstanding.toDoubleOrNull() ?: 0.0
}

data class CustomerLedgerResponse(
    val customer: CustomerSummaryDto,
    val summary: CustomerLedgerSummaryDto,
    val payments: List<PaymentItemDto> = emptyList()
)

data class CustomerLedgerSummaryDto(
    val totalInvoiced: Double = 0.0,
    val totalPaid: Double = 0.0,
    val outstanding: Double = 0.0
)

data class CreatePaymentRequest(
    val customerId: String,
    val amount: Double,
    val paymentDate: String,
    val paymentMode: String = "CASH",
    val referenceNumber: String? = null,
    val notes: String? = null
)

data class CreatePaymentEnvelopeResponse(
    val payment: PaymentItemDto
)

// --- Module 8: Customer Directory & Connected Supplier DTOs ---

data class CreateCustomerRequest(
    val businessName: String,
    val primaryContactName: String,
    val mobile: String,
    val address: String,
    val gstNumber: String? = null,
    val creditLimit: Double? = null,
    val notes: String? = null,
    val discountType: String? = "NONE",
    val discountValue: Double? = 0.0,
    val discountNotes: String? = null
)

data class UpdateCustomerRequest(
    val businessName: String? = null,
    val primaryContactName: String? = null,
    val mobile: String? = null,
    val address: String? = null,
    val gstNumber: String? = null,
    val creditLimit: Double? = null,
    val notes: String? = null,
    val isActive: Boolean? = null,
    val discountType: String? = null,
    val discountValue: Double? = null,
    val discountNotes: String? = null
)

data class ConnectedSellerEnvelopeResponse(
    val success: Boolean,
    val data: ConnectedSellerDto? = null
)

data class ConnectedSellerDto(
    val id: String,
    val businessName: String,
    val primaryContactName: String? = null,
    val mobile: String,
    val address: String? = null,
    val gstNumber: String? = null,
    val sellerCode: String? = null
)

// --- Module 11: Executive Analytics DTOs ---

data class DailySalesReportResponse(
    val summary: DailySalesSummaryDto,
    val rows: List<DailySalesRowDto> = emptyList()
)

data class DailySalesSummaryDto(
    val overallOrders: Int = 0,
    val overallWeight: Double = 0.0,
    val overallRevenue: Double = 0.0
)

data class DailySalesRowDto(
    val date: String,
    val ordersCount: Int,
    val uniqueBuyersCount: Int,
    val totalWeight: Double,
    val totalRevenue: Double
)

data class BuyerSummaryReportResponse(
    val summary: BuyerSummaryTotalsDto,
    val rows: List<BuyerSummaryRowDto> = emptyList()
)

data class BuyerSummaryTotalsDto(
    val totalBuyers: Int = 0,
    val totalOrders: Int = 0,
    val totalSales: Double = 0.0
)

data class BuyerSummaryRowDto(
    val customerId: String,
    val businessName: String,
    val mobile: String,
    val ordersCount: Int,
    val totalSales: Double,
    val totalWeight: Double,
    val averageOrderValue: Double
)

data class ProduceSalesReportResponse(
    val summary: ProduceSalesSummaryDto,
    val rows: List<ProduceSalesRowDto> = emptyList()
)

data class ProduceSalesSummaryDto(
    val totalProducts: Int = 0,
    val totalQuantitySold: Double = 0.0,
    val totalRevenue: Double = 0.0
)

data class ProduceSalesRowDto(
    val productId: String,
    val productName: String,
    val categoryName: String? = null,
    val unitType: String = "KG",
    val totalQuantity: Double = 0.0,
    val totalRevenue: Double = 0.0,
    val averagePrice: Double = 0.0
)

data class PaymentCollectionsReportResponse(
    val summary: PaymentCollectionsSummaryDto,
    val rows: List<PaymentCollectionRowDto> = emptyList()
)

data class PaymentCollectionsSummaryDto(
    val totalCollections: Double = 0.0,
    val totalCash: Double = 0.0,
    val totalUpi: Double = 0.0,
    val totalBank: Double = 0.0,
    val totalCheque: Double = 0.0
)

data class PaymentCollectionRowDto(
    val date: String,
    val paymentMode: String,
    val amount: Double,
    val customerName: String? = null,
    val referenceNumber: String? = null
)

// --- Module 12: Super Admin DTOs ---

data class PlatformAnalyticsResponse(
    val totalGmv: Double = 0.0,
    val activeSellers: Int = 0,
    val totalBuyers: Int = 0,
    val totalOrders: Int = 0,
    val totalProduceVolumeKg: Double = 0.0,
    val activeRateCardsCount: Int = 0
)

data class AdminUserDto(
    val id: String,
    val name: String,
    val mobile: String,
    val role: String,
    val isActive: Boolean = true,
    val customerName: String? = null,
    val createdAt: String
)

data class AdminSellerDto(
    val id: String,
    val businessName: String,
    val sellerCode: String,
    val mobile: String,
    val buyerCount: Int = 0,
    val totalGmv: Double = 0.0,
    val isActive: Boolean = true
)

data class CustomProductDto(
    val id: String,
    val name: String,
    val categoryName: String? = null,
    val unitType: String = "KG",
    val sellerBusinessName: String? = null,
    val sellingPrice: Double = 0.0,
    val isVerified: Boolean = false
)

data class OrdersEnvelopeResponse(
    val orders: List<OrderDto> = emptyList()
)

data class InvoicesEnvelopeResponse(
    val invoices: List<InvoiceSummaryDto> = emptyList()
)

data class PendingInvoicesEnvelopeResponse(
    val orders: List<OrderDto> = emptyList()
)

data class InvoiceDetailEnvelopeResponse(
    val invoice: InvoiceDetailDto? = null
)


