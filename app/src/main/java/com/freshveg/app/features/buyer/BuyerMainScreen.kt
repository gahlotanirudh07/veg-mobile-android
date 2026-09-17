package com.freshveg.app.features.buyer

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
import com.freshveg.app.core.network.VegApiService
import com.freshveg.app.core.ui.animation.rememberTactileHaptic
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.features.buyer.catalogue.BuyerCatalogueScreen
import com.freshveg.app.features.buyer.home.BuyerHomeScreen
import com.freshveg.app.features.buyer.invoices.BuyerInvoicesScreen
import com.freshveg.app.features.buyer.orders.BuyerOrdersScreen

@Composable
fun BuyerMainScreen(
    sessionManager: SessionManager,
    apiService: VegApiService,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val triggerHaptic = rememberTactileHaptic()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = NeutralSurface,
                tonalElevation = 3.dp
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
                            contentDescription = "Home"
                        )
                    },
                    label = {
                        Text(
                            text = "Home",
                            fontSize = 11.5.sp,
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
                            contentDescription = "Catalogue"
                        )
                    },
                    label = {
                        Text(
                            text = "Catalogue",
                            fontSize = 11.5.sp,
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

                // Tab 2: Orders (Tracking & History)
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 2
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping,
                            contentDescription = "Orders"
                        )
                    },
                    label = {
                        Text(
                            text = "Orders",
                            fontSize = 11.5.sp,
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

                // Tab 3: Khata (Invoices & Balance)
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        triggerHaptic()
                        selectedTab = 3
                    },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 3) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = "Khata"
                        )
                    },
                    label = {
                        Text(
                            text = "Khata",
                            fontSize = 11.5.sp,
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
                    outstandingDues = 0.0,
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
                    onNavigateToOrders = { selectedTab = 2 },
                    onNavigateToInvoices = { selectedTab = 3 },
                    onNavigateBack = {
                        triggerHaptic()
                        selectedTab = 0
                    },
                    onLogout = onLogout
                )
                2 -> BuyerOrdersScreen(
                    onNavigateBack = {
                        triggerHaptic()
                        selectedTab = 0
                    }
                )
                3 -> BuyerInvoicesScreen(
                    onNavigateBack = {
                        triggerHaptic()
                        selectedTab = 0
                    }
                )
            }
        }
    }
}
