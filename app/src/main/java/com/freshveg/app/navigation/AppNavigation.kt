package com.freshveg.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.freshveg.app.features.auth.screens.LoginScreen
import com.freshveg.app.features.auth.screens.RegisterBuyerScreen
import com.freshveg.app.features.auth.screens.RegisterSellerScreen
import com.freshveg.app.features.catalog.screens.CatalogScreen
import com.freshveg.app.features.seller.products.SellerProductsScreen

@Composable
fun AppNavigation(
    sessionManager: com.freshveg.app.core.datastore.SessionManager,
    apiService: com.freshveg.app.core.network.VegApiService,
    updateManager: com.freshveg.app.core.update.AppUpdateManager? = null,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // Module 1: Auth & Role Selection
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegisterBuyer = { navController.navigate(Screen.RegisterBuyer.route) },
                onNavigateToRegisterSeller = { navController.navigate(Screen.RegisterSeller.route) },
                onLoginSuccess = { role, isSeller, _isBuyer ->
                    val targetRoute = when {
                        role.uppercase() == "SUPER_ADMIN" || role.uppercase() == "ADMIN" -> Screen.Admin.route
                        isSeller -> Screen.SellerProducts.route
                        else -> Screen.Home.route
                    }
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RegisterBuyer.route) {
            RegisterBuyerScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RegisterSeller.route) {
            RegisterSellerScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.SellerProducts.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Buyer Main Experience (4-Tab Bottom Navigation: Market, My Orders, Invoices, Account)
        composable(Screen.Home.route) {
            com.freshveg.app.features.buyer.BuyerMainScreen(
                sessionManager = sessionManager,
                apiService = apiService,
                updateManager = updateManager,
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Seller Main Experience
        composable(Screen.SellerProducts.route) {
            com.freshveg.app.features.seller.SellerMainScreen(
                sessionManager = sessionManager,
                updateManager = updateManager,
                onNavigateToStore = {
                    navController.navigate(Screen.MyStore.route)
                },
                onNavigateToRates = {
                    navController.navigate(Screen.SellerRates.route)
                },
                onNavigateToTally = {
                    navController.navigate(Screen.ProcurementTally.route)
                },
                onNavigateToCustomers = {
                    navController.navigate(Screen.Customers.route)
                },
                onNavigateToInvoices = {
                    navController.navigate(Screen.Invoices.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToAdmin = {
                    navController.navigate(Screen.Admin.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Seller My Store (Produce Inventory, Stock & Add/Edit/Delete Produce)
        composable(Screen.MyStore.route) {
            com.freshveg.app.features.seller.products.SellerProductsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRates = {
                    navController.navigate(Screen.SellerRates.route)
                },
                onNavigateToTally = {
                    navController.navigate(Screen.ProcurementTally.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Module 3 (Part 2): 1-Thumb Morning Rate Board
        composable(Screen.SellerRates.route) {
            com.freshveg.app.features.seller.rates.SellerRatesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Module 4: Morning Procurement Demand Tally & Daily P&L
        composable(Screen.ProcurementTally.route) {
            com.freshveg.app.features.seller.tally.ProcurementTallyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Module 5: Orders Pipeline & Scale Weighment Fulfillment
        composable(Screen.Orders.route) {
            com.freshveg.app.features.seller.orders.OrdersPipelineScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Module 6: Tax Invoices & Billing Management
        composable(Screen.Invoices.route) {
            com.freshveg.app.features.seller.invoices.InvoicesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Module 7: Customer Khata, Running Ledger & UPI
        composable(Screen.Ledger.route) {
            com.freshveg.app.features.seller.ledger.CustomerLedgerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Module 8: Customer Directory & Restaurant Profiles
        composable(Screen.Customers.route) {
            com.freshveg.app.features.seller.customers.CustomersScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Module 11: Executive Analytics & Reports
        composable(Screen.Analytics.route) {
            com.freshveg.app.features.seller.analytics.AnalyticsReportsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Module 12: Super Admin Control Center
        composable(Screen.Admin.route) {
            com.freshveg.app.features.admin.AdminControlCenterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Buyer Orders & Delivery Tracking
        composable(Screen.BuyerOrders.route) {
            com.freshveg.app.features.buyer.orders.BuyerOrdersScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Buyer Tax Invoices & Billing
        composable(Screen.BuyerInvoices.route) {
            com.freshveg.app.features.buyer.invoices.BuyerInvoicesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
