package com.freshveg.app.features.seller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.ui.animation.rememberTactileHaptic
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.features.seller.account.SellerAccountScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.features.seller.home.SellerHomeScreen
import com.freshveg.app.features.seller.home.SellerHomeViewModel
import com.freshveg.app.features.seller.ledger.CustomerLedgerScreen
import com.freshveg.app.features.seller.orders.OrdersPipelineScreen
import com.freshveg.app.features.seller.rates.SellerRatesScreen

@Composable
fun SellerMainScreen(
    sessionManager: SessionManager,
    homeViewModel: SellerHomeViewModel = hiltViewModel(),
    onNavigateToStore: () -> Unit = {},
    onNavigateToRates: () -> Unit,
    onNavigateToTally: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val hideMasterCatalogue by sessionManager.hideMasterCatalogue.collectAsState(initial = false)
    val homeUiState by homeViewModel.uiState.collectAsState()
    val triggerHaptic = rememberTactileHaptic()

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            homeViewModel.loadData()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NeutralSurface,
                tonalElevation = 3.dp
            ) {
                // Tab 0: Home (Operations Command Hub)
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 0
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = {
                        Text(
                            text = "Home",
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

                // Tab 1: Orders (5-Stage Pipeline & Weighment)
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 1
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping,
                            contentDescription = "Orders"
                        )
                    },
                    label = {
                        Text(
                            text = "Orders",
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

                // Tab 2: Rates (4:00 AM Rate Board)
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 2
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Outlined.TrendingUp,
                            contentDescription = "Rates"
                        )
                    },
                    label = {
                        Text(
                            text = "Rates",
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

                // Tab 3: Khata (Customer Ledger & Aging Dues)
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 3
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                            contentDescription = "Khata"
                        )
                    },
                    label = {
                        Text(
                            text = "Khata",
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

                // Tab 4: Profile & Business Settings
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 4
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 4) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = {
                        Text(
                            text = "Profile",
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
                    }
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

