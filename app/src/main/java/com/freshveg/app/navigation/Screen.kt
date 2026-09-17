package com.freshveg.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object RegisterBuyer : Screen("register_buyer")
    object RegisterSeller : Screen("register_seller")
    object Home : Screen("home") // Buyer Fresh Market
    object SellerProducts : Screen("seller_products") // Seller Inventory Manager
    object MyStore : Screen("my_store") // Seller My Store & Inventory
    object SellerRates : Screen("seller_rates") // Morning 04:00 AM Rate Board
    object ProcurementTally : Screen("procurement_tally") // Morning Demand Tally & P&L
    object Checkout : Screen("checkout")
    object Orders : Screen("orders")
    object Invoices : Screen("invoices")
    object Ledger : Screen("ledger")
    object Customers : Screen("customers")
    object Analytics : Screen("analytics")
    object Admin : Screen("admin")
    object BuyerOrders : Screen("buyer_orders")
    object BuyerInvoices : Screen("buyer_invoices")
    object OrderTracking : Screen("order_tracking/{orderId}") {
        fun createRoute(orderId: String) = "order_tracking/$orderId"
    }
    object Profile : Screen("profile")
}
