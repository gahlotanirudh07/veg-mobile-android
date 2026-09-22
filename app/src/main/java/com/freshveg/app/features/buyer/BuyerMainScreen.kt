package com.freshveg.app.features.buyer

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
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
import com.freshveg.app.core.network.VegApiService
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.update.AppUpdateManager
import com.freshveg.app.features.buyer.account.BuyerAccountScreen
import com.freshveg.app.features.buyer.catalogue.BuyerCatalogueScreen
import com.freshveg.app.features.buyer.catalogue.BuyerCatalogueViewModel
import com.freshveg.app.features.buyer.home.BuyerHomeScreen
import com.freshveg.app.features.buyer.invoices.BuyerInvoicesScreen
import com.freshveg.app.features.buyer.orders.BuyerOrdersScreen

@Composable
fun BuyerMainScreen(
    sessionManager: SessionManager,
    apiService: VegApiService,
    updateManager: AppUpdateManager? = null,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val view = LocalView.current
    val catalogueViewModel: BuyerCatalogueViewModel = hiltViewModel()
    val invoicesViewModel: com.freshveg.app.features.buyer.invoices.BuyerInvoicesViewModel = hiltViewModel()
    val invoicesUiState by invoicesViewModel.uiState.collectAsState()

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
                            imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
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

                // Tab 1: Catalogue
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 1
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Storefront else Icons.Outlined.Storefront,
                            contentDescription = stringResource(R.string.nav_catalogue)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_catalogue),
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

                // Tab 2: Orders
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 2
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping,
                            contentDescription = stringResource(R.string.nav_orders)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_orders),
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

                // Tab 3: Khata / Invoices
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 3
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong,
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

                // Tab 4: Account
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
                0 -> BuyerHomeScreen(
                    connectedSeller = null,
                    cutoffTime = "03:00 AM",
                    activeOrdersCount = 0,
                    outstandingDues = invoicesUiState.khataSummary.outstanding,
                    onNavigateToCatalogue = {
                        triggerHaptic()
                        selectedTab = 1
                    },
                    onNavigateToOrders = {
                        triggerHaptic()
                        selectedTab = 2
                    },
                    onNavigateToInvoices = {
                        triggerHaptic()
                        selectedTab = 3
                    },
                    onLogout = onLogout
                )
                1 -> BuyerCatalogueScreen(
                    viewModel = catalogueViewModel,
                    onNavigateToOrders = { selectedTab = 2 }
                )
                2 -> BuyerOrdersScreen(
                    onNavigateBack = {
                        triggerHaptic()
                        selectedTab = 0
                    }
                )
                3 -> BuyerInvoicesScreen(
                    viewModel = invoicesViewModel,
                    onNavigateBack = {
                        triggerHaptic()
                        selectedTab = 0
                    }
                )
                4 -> BuyerAccountScreen(
                    sessionManager = sessionManager,
                    apiService = apiService,
                    updateManager = updateManager,
                    onNavigateToOrders = {
                        triggerHaptic()
                        selectedTab = 2
                    },
                    onNavigateToInvoices = {
                        triggerHaptic()
                        selectedTab = 3
                    },
                    onLogout = onLogout
                )
            }
        }
    }
}
