package com.freshveg.app.features.seller.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.network.ProductDto
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.animation.bounceClick
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProductsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToRates: () -> Unit = {},
    onNavigateToTally: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: SellerProductsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var productToDelete by remember { mutableStateOf<ProductDto?>(null) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ActionGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = ActionGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                "My Store (सब्जी स्टोर)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MainInk
                            )
                            Text(
                                "${uiState.totalCount} Vegetables in Store",
                                style = MaterialTheme.typography.labelSmall,
                                color = ActionGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainInk)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::loadData) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ActionGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeutralSurface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::openAddProduct,
                containerColor = MainInk,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Produce", tint = HarvestLime) },
                text = { Text("+ Add Vegetable (सब्जी जोड़ें)", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundCanvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ─── 1. Quick Operations Row ───
            Surface(
                color = NeutralSurface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = viewModel::openAddProduct,
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.weight(1.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("+ Add Vegetable", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = ActionGreen)
                                    Text("सब्जी जोड़ें", fontSize = 10.sp, color = InkSecondary)
                                }
                            }
                        }

                        Surface(
                            onClick = onNavigateToRates,
                            shape = RoundedCornerShape(10.dp),
                            color = ActionGreen.copy(alpha = 0.08f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("04:00 AM Rates", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = ActionGreen)
                                    Text("Set morning भाव", fontSize = 10.sp, color = InkSecondary)
                                }
                            }
                        }

                        Surface(
                            onClick = onNavigateToTally,
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1976D2).copy(alpha = 0.08f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Demand Tally", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF1976D2))
                                    Text("Procurement", fontSize = 10.sp, color = InkSecondary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ─── 2. Search Bar ───
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Search store produce (Tomato, आलू)...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = InkTertiary)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = InkTertiary)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ActionGreen,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SecondarySurface,
                            unfocusedContainerColor = SecondarySurface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // ─── 3. Store Tabs Row (Web Parity) ───
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All Store Produce Tab
                        FilterChip(
                            selected = uiState.tabFilter == StoreTabFilter.ALL,
                            onClick = { viewModel.setTabFilter(StoreTabFilter.ALL) },
                            label = {
                                Text(
                                    text = "All Store (${uiState.totalCount})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (uiState.tabFilter == StoreTabFilter.ALL) Color.White else MainInk
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MainInk,
                                containerColor = Color.Transparent,
                                labelColor = MainInk,
                                selectedLabelColor = Color.White
                            )
                        )

                        // In-Stock Tab
                        FilterChip(
                            selected = uiState.tabFilter == StoreTabFilter.IN_STOCK,
                            onClick = { viewModel.setTabFilter(StoreTabFilter.IN_STOCK) },
                            label = {
                                Text(
                                    text = "In-Stock (${uiState.inStockCount})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (uiState.tabFilter == StoreTabFilter.IN_STOCK) Color.White else MainInk
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.tabFilter == StoreTabFilter.IN_STOCK) Color.White else ActionGreen)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ActionGreen,
                                containerColor = Color.Transparent,
                                labelColor = MainInk,
                                selectedLabelColor = Color.White
                            )
                        )

                        // Out of Stock Tab
                        FilterChip(
                            selected = uiState.tabFilter == StoreTabFilter.OUT_OF_STOCK,
                            onClick = { viewModel.setTabFilter(StoreTabFilter.OUT_OF_STOCK) },
                            label = {
                                Text(
                                    text = "Out of Stock (${uiState.outOfStockCount})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (uiState.tabFilter == StoreTabFilter.OUT_OF_STOCK) Color.White else MainInk
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MainInk,
                                containerColor = Color.Transparent,
                                labelColor = MainInk,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ─── 4. Category Chips ───
                    if (uiState.categories.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = uiState.selectedCategoryId == null,
                                onClick = { viewModel.onCategorySelect(null) },
                                label = { Text("All Categories", fontSize = 11.sp) }
                            )

                            uiState.categories.forEach { cat ->
                                FilterChip(
                                    selected = uiState.selectedCategoryId == cat.id,
                                    onClick = {
                                        viewModel.onCategorySelect(if (uiState.selectedCategoryId == cat.id) null else cat.id)
                                    },
                                    label = { Text(cat.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ActionGreen,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ─── 5. Products List ───
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading && uiState.products.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ActionGreen)
                } else if (uiState.filteredProducts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = InkTertiary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (uiState.products.isEmpty())
                                "Your Store is Empty"
                            else if (uiState.searchQuery.isNotEmpty())
                                "No vegetables match '${uiState.searchQuery}'"
                            else
                                "No vegetables in this section",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MainInk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.products.isEmpty())
                                "Tap '+ Add Vegetable' below to add fresh vegetables (Potato, Onion, Tomato...) to your store catalog."
                            else if (uiState.searchQuery.isNotEmpty())
                                "Try searching with a different name or Hindi term (e.g. आलू, टमाटर)"
                            else
                                "Switch tabs or tap '+ Add Vegetable' to list fresh produce.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (uiState.products.isEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = viewModel::openAddProduct,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ Add Vegetable", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredProducts, key = { it.id }) { product ->
                            SellerProductItemCard(
                                product = product,
                                onToggleAvailability = { viewModel.toggleAvailability(product) },
                                onEditClick = { viewModel.openEditProduct(product) },
                                onDeleteClick = { productToDelete = product }
                            )
                        }
                    }
                }
            }
        }

        // Add/Edit Bottom Sheet Dialog
        if (uiState.isFormBottomSheetOpen) {
            ProductFormBottomSheet(
                product = uiState.selectedProductForEdit,
                categories = uiState.categories,
                existingProducts = uiState.products,
                onDismiss = viewModel::closeFormBottomSheet,
                onSave = { name, hindiName, categoryId, unitType, currentPrice, minQty, isAvailable, isFeatured ->
                    viewModel.saveProduct(name, hindiName, categoryId, unitType, currentPrice, minQty, isAvailable, isFeatured)
                },
                onDelete = { id -> viewModel.deleteProduct(id) }
            )
        }

        // Delete Confirmation Dialog
        if (productToDelete != null) {
            val target = productToDelete!!
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = {
                    Text(
                        "Delete Vegetable from Store",
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                },
                text = {
                    Text(
                        "Are you sure you want to delete \"${target.name}\"? It will be removed from your store catalog and pricing."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = target.id
                            productToDelete = null
                            viewModel.deleteProduct(id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MutedRedError)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) {
                        Text("Cancel", color = MainInk)
                    }
                }
            )
        }
    }
}

@Composable
fun SellerProductItemCard(
    product: ProductDto,
    onToggleAvailability: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        onClick = onEditClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NeutralSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Produce Thumbnail Badge with SVG support
            ProduceThumbnailBadge(
                name = product.name,
                hindiName = product.hindiName,
                imageUrl = product.imageUrl,
                size = 58.dp,
                cornerRadius = 12.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MainInk,
                        maxLines = 1
                    )
                }

                if (!product.hindiName.isNullOrEmpty()) {
                    Text(
                        text = product.hindiName,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unit pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SecondarySurface)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = product.unitType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = InkSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Price display with clickable edit affordance
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onEditClick() }
                    ) {
                        Text(
                            text = if (product.currentPrice > 0) "₹${product.currentPrice.toInt()} / ${product.unitType.name}" else "Not Priced",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (product.currentPrice > 0) ActionGreen else InkTertiary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Price",
                            tint = ActionGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // In-Stock Switch & Delete Action
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Switch(
                    checked = product.isAvailable,
                    onCheckedChange = { onToggleAvailability() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ActionGreen
                    )
                )
                Text(
                    text = if (product.isAvailable) "In-Stock" else "Out of Stock",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (product.isAvailable) ActionGreen else MutedRedError,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete or unlist produce",
                    tint = InkTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

