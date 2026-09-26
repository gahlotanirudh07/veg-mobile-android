package com.freshveg.app.features.seller

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.R
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.update.AppUpdateManager
import com.freshveg.app.features.seller.account.SellerAccountScreen
import com.freshveg.app.features.seller.home.SellerHomeScreen
import com.freshveg.app.features.seller.home.SellerHomeViewModel
import com.freshveg.app.features.seller.ledger.CustomerLedgerScreen
import com.freshveg.app.features.seller.orders.OrdersPipelineScreen
import com.freshveg.app.features.seller.rates.SellerRatesScreen

@Composable
fun SellerMainScreen(
    sessionManager: SessionManager,
    updateManager: AppUpdateManager? = null,
    hideMasterCatalogue: Boolean = false,
    onNavigateToStore: () -> Unit = {},
    onNavigateToRates: () -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToInvoices: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToTally: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var ordersInitialStatus by remember { mutableStateOf<String?>(null) }
    var ordersInitialDateFilter by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current
    val homeViewModel: SellerHomeViewModel = hiltViewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()

    fun triggerHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                // Tab 0: Home
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 0
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Storefront else Icons.Outlined.Storefront,
                            contentDescription = stringResource(R.string.nav_home)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_home),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ActionGreen,
                        selectedTextColor = ActionGreen,
                        unselectedIconColor = InkSecondary,
                        unselectedTextColor = InkSecondary,
                        indicatorColor = ActionGreen.copy(alpha = 0.12f)
                    )
                )

                // Tab 1: Orders Pipeline
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        triggerHaptic()
                        ordersInitialStatus = null
                        ordersInitialDateFilter = null
                        selectedTab = 1
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping,
                            contentDescription = stringResource(R.string.nav_orders)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_orders),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ActionGreen,
                        selectedTextColor = ActionGreen,
                        unselectedIconColor = InkSecondary,
                        unselectedTextColor = InkSecondary,
                        indicatorColor = ActionGreen.copy(alpha = 0.12f)
                    )
                )

                // Tab 2: Rates
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 2
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = stringResource(R.string.nav_rates)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_rates),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ActionGreen,
                        selectedTextColor = ActionGreen,
                        unselectedIconColor = InkSecondary,
                        unselectedTextColor = InkSecondary,
                        indicatorColor = ActionGreen.copy(alpha = 0.12f)
                    )
                )

                // Tab 3: Khata
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 3
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = stringResource(R.string.nav_khata)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_khata),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ActionGreen,
                        selectedTextColor = ActionGreen,
                        unselectedIconColor = InkSecondary,
                        unselectedTextColor = InkSecondary,
                        indicatorColor = ActionGreen.copy(alpha = 0.12f)
                    )
                )

                // Tab 4: Account / Profile
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 4
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 4) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = stringResource(R.string.nav_account)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_account),
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ActionGreen,
                        selectedTextColor = ActionGreen,
                        unselectedIconColor = InkSecondary,
                        unselectedTextColor = InkSecondary,
                        indicatorColor = ActionGreen.copy(alpha = 0.12f)
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> SellerHomeScreen(
                    sellerBusinessName = homeUiState.businessName,
                    sellerCode = homeUiState.sellerCode,
                    newOrdersCount = homeUiState.newOrdersCount,
                    marketDues = homeUiState.marketDues,
                    totalSalesToday = homeUiState.totalSalesToday,
                    hideMasterCatalogue = hideMasterCatalogue,
                    onNavigateToOrders = {
                        triggerHaptic()
                        ordersInitialStatus = null
                        ordersInitialDateFilter = null
                        selectedTab = 1
                    },
                    onNavigateToActiveOrders = {
                        triggerHaptic()
                        ordersInitialStatus = "PENDING"
                        ordersInitialDateFilter = "ALL"
                        selectedTab = 1
                    },
                    onNavigateToRates = {
                        triggerHaptic()
                        selectedTab = 2
                    },
                    onNavigateToStore = onNavigateToStore,
                    onNavigateToKhata = {
                        triggerHaptic()
                        selectedTab = 3
                    },
                    onNavigateToInvoices = onNavigateToInvoices,
                    onNavigateToCustomers = onNavigateToCustomers,
                    onNavigateToTally = onNavigateToTally,
                    onLogout = onLogout
                )
                1 -> OrdersPipelineScreen(
                    onNavigateBack = {
                        triggerHaptic()
                        selectedTab = 0
                    },
                    initialStatus = ordersInitialStatus,
                    initialDateFilter = ordersInitialDateFilter
                )
                2 -> SellerRatesScreen(
                    onNavigateBack = {
                        triggerHaptic()
                        selectedTab = 0
                    }
                )
                3 -> CustomerLedgerScreen(
                    onNavigateBack = {
                        triggerHaptic()
                        selectedTab = 0
                    }
                )
                4 -> SellerAccountScreen(
                    sessionManager = sessionManager,
                    updateManager = updateManager,
                    onNavigateToStore = onNavigateToStore,
                    onNavigateToCustomers = onNavigateToCustomers,
                    onNavigateToInvoices = onNavigateToInvoices,
                    onNavigateToAnalytics = onNavigateToAnalytics,
                    onNavigateToAdmin = onNavigateToAdmin,
                    onLogout = onLogout
                )
            }
        }
    }
}
