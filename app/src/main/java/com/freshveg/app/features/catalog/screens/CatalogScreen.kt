package com.freshveg.app.features.catalog.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.features.catalog.components.VegetableCard
import com.freshveg.app.features.catalog.viewmodel.CatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onNavigateToCheckout: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Delivery Address & Seller Pin
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = FarmGreenSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Deliver to: Shop #14, Main Market", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Seller: ${uiState.sellerCode ?: "Green Valley"} (Connected)",
                            style = MaterialTheme.typography.labelMedium,
                            color = FarmGreenSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToOrders) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Orders", tint = ForestGreenPrimary)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile", tint = ForestGreenPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Search spinach, tomatoes, organic...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { Icon(Icons.Default.Mic, contentDescription = "Voice search") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BackgroundSurface,
                        unfocusedContainerColor = BackgroundSurface
                    ),
                    singleLine = true
                )
            }
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cutoff Alert Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Order before ${uiState.cutoffTime} for 6:00 AM Delivery",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE65100)
                    )
                }

                // Category Chips Row
                val sampleCategories = listOf("All", "Leafy Greens", "Root Veggies", "Fruity", "Herbs", "Organic")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sampleCategories) { category ->
                        val isSelected = (category == "All" && uiState.selectedCategoryId == null)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(if (category == "All") null else category) },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ForestGreenPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // 2-Column Product Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.products) { product ->
                        val qty = uiState.cartItems[product.id] ?: 0
                        val unitIndex = uiState.selectedUnitIndex[product.id] ?: 0

                        VegetableCard(
                            product = product,
                            quantity = qty,
                            selectedUnitIndex = unitIndex,
                            onUnitSelected = { viewModel.onUnitChange(product.id, it) },
                            onQuantityChanged = { viewModel.onQuantityChange(product.id, it) }
                        )
                    }
                }
            }

            // Floating Persistent Cart Bar
            AnimatedVisibility(
                visible = uiState.totalCartItems > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = ForestGreenPrimary,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${uiState.totalCartItems} Items in Cart",
                                style = MaterialTheme.typography.labelMedium,
                                color = LightMintAccent
                            )
                            Text(
                                "₹${uiState.totalCartPrice.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onNavigateToCheckout,
                            colors = ButtonDefaults.buttonColors(containerColor = CardSurface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "View Cart ➔",
                                color = ForestGreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
