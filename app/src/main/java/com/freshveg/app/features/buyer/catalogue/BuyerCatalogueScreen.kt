package com.freshveg.app.features.buyer.catalogue

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.network.ProductDto
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.animation.bounceClick
import com.freshveg.app.core.ui.animation.rememberShimmerBrush
import com.freshveg.app.core.ui.animation.rememberTactileHaptic
import com.freshveg.app.core.ui.components.AnimatedOdometerText
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.MandiTranslationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerCatalogueScreen(
    onNavigateToOrders: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onLogout: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: BuyerCatalogueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val triggerHaptic = rememberTactileHaptic()

    var selectedCategory by remember { mutableStateOf("Frequent") }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Filter products by selected category
    val categoryFilteredProducts = remember(uiState.filteredProducts, selectedCategory) {
        when (selectedCategory) {
            "Frequent" -> {
                // Return frequently ordered wholesale staples first
                val frequentKeys = listOf("tomato", "potato", "onion", "chilli", "ginger", "garlic", "coriander", "palak")
                val matched = uiState.filteredProducts.filter { prod ->
                    val name = prod.safeName.lowercase()
                    frequentKeys.any { name.contains(it) }
                }
                if (matched.isNotEmpty()) matched else uiState.filteredProducts
            }
            "All" -> uiState.filteredProducts
            "Roots" -> uiState.filteredProducts.filter { prod ->
                val name = prod.safeName.lowercase()
                name.contains("potato") || name.contains("aloo") || name.contains("ginger") || name.contains("adrak") || name.contains("garlic") || name.contains("lahsun") || name.contains("carrot") || name.contains("gajar") || name.contains("radish") || name.contains("mooli") || name.contains("beetroot")
            }
            "Greens" -> uiState.filteredProducts.filter { prod ->
                val name = prod.safeName.lowercase()
                name.contains("palak") || name.contains("spinach") || name.contains("methi") || name.contains("coriander") || name.contains("dhaniya") || name.contains("pudina") || name.contains("mint") || name.contains("cabbage") || name.contains("gobhi")
            }
            "Essentials" -> uiState.filteredProducts.filter { prod ->
                val name = prod.safeName.lowercase()
                name.contains("tomato") || name.contains("tamatar") || name.contains("onion") || name.contains("pyaz") || name.contains("chili") || name.contains("mirch")
            }
            "Gourds" -> uiState.filteredProducts.filter { prod ->
                val name = prod.safeName.lowercase()
                name.contains("kheera") || name.contains("cucumber") || name.contains("lauki") || name.contains("karela") || name.contains("torai") || name.contains("gourd")
            }
            else -> uiState.filteredProducts
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MainInk
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Text(
                            text = uiState.connectedSeller?.businessName ?: "Wholesale Yard",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MainInk
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⏰ Order cutoff: ${uiState.cutoffTime}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberWarning,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (uiState.connectedSeller?.sellerCode != null) {
                                Text(
                                    text = "• ${uiState.connectedSeller?.sellerCode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkTertiary
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Call Seller
                    uiState.connectedSeller?.mobile?.let { phone ->
                        IconButton(onClick = {
                            triggerHaptic()
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = "Call Seller",
                                tint = ActionGreen
                            )
                        }
                    }

                    // Refresh
                    IconButton(onClick = {
                        triggerHaptic()
                        viewModel.loadStorefront()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = InkSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeutralSurface)
            )
        },
        bottomBar = {
            // High-Trust Animated Sticky Bottom Cart Dock
            AnimatedVisibility(
                visible = uiState.cartItemCount > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = MainInk,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .bounceClick {
                            triggerHaptic()
                            viewModel.openCart()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(HarvestLime),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingCart,
                                    contentDescription = null,
                                    tint = MainInk,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "${uiState.cartItemCount} Items Selected",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                AnimatedOdometerText(
                                    value = uiState.cartEstimatedTotal.toInt(),
                                    prefix = "Est: ₹",
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        color = HarvestLime,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                triggerHaptic()
                                viewModel.openCart()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ActionGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Review Order →", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        containerColor = BackgroundCanvas
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Search Bar & Category Filter Bar
            Surface(
                color = NeutralSurface,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = {
                            Text(
                                "Search produce (e.g. Potato, tamatar, pyaz)...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = InkTertiary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = InkSecondary
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = InkSecondary
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ActionGreen,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = SecondarySurface,
                            unfocusedContainerColor = SecondarySurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Category Filter Chips (Frequent as first option)
                    val categories = listOf("Frequent", "All", "Roots", "Greens", "Essentials", "Gourds")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    triggerHaptic()
                                    selectedCategory = cat
                                },
                                label = {
                                    Text(
                                        text = if (cat == "All") "All (${uiState.filteredProducts.size})" else cat,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.5.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ActionGreen,
                                    selectedLabelColor = Color.White,
                                    containerColor = SecondarySurface,
                                    labelColor = MainInk
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = null
                            )
                        }
                    }
                }
            }

            // 3. Dense Produce List with Botanical Vector Badges
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Shimmer Loading Skeleton State
                if (uiState.isLoading && uiState.products.isEmpty()) {
                    items(6) {
                        ProduceRowSkeleton()
                    }
                } else if (categoryFilteredProducts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = InkTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No produce found matching '${uiState.searchQuery}'",
                                fontWeight = FontWeight.Bold,
                                color = MainInk
                            )
                            Text(
                                text = "Try searching by English or Hindi produce name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                        }
                    }
                } else {
                    items(categoryFilteredProducts, key = { it.id }) { product ->
                        val qtyInCart = uiState.cart[product.id] ?: 0.0
                        CompactBuyerProduceRow(
                            product = product,
                            quantityInCart = qtyInCart,
                            onIncrement = {
                                triggerHaptic()
                                viewModel.incrementQuantity(product)
                            },
                            onDecrement = {
                                triggerHaptic()
                                viewModel.decrementQuantity(product)
                            },
                            onAddQuickQuantity = { addQty ->
                                triggerHaptic()
                                val current = uiState.cart[product.id] ?: 0.0
                                viewModel.setQuantity(product.id, current + addQty)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal 1: Buyer Cart Drawer with Zero-Trust Transparency
    if (uiState.isCartOpen) {
        BuyerCartBottomSheet(
            cartItems = uiState.cartItemsList,
            onUpdateQuantity = viewModel::setQuantity,
            onRemoveItem = viewModel::removeFromCart,
            onClearCart = viewModel::clearCart,
            onDismiss = viewModel::closeCart,
            onPlaceOrder = viewModel::placeOrder,
            isPlacingOrder = uiState.isPlacingOrder
        )
    }

    // Modal 2: Order Placed Congratulatory Dialog
    uiState.orderSuccessDto?.let { order ->
        AlertDialog(
            onDismissRequest = viewModel::clearOrderSuccess,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ActionGreen,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Order Placed Successfully",
                    fontWeight = FontWeight.Bold,
                    color = MainInk
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Order Number: #${order.orderNumber}",
                        fontWeight = FontWeight.Bold,
                        color = ActionGreen
                    )
                    Text(
                        text = "Your early morning produce requirement has been submitted to ${uiState.connectedSeller?.businessName ?: "Wholesale Vendor"}.",
                        color = MainInk
                    )
                    Text(
                        text = "Expected delivery: Tomorrow morning between 05:00 AM – 07:00 AM.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearOrderSuccess()
                        onNavigateToOrders()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Track Order Status", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = viewModel::clearOrderSuccess,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue Browsing", color = MainInk)
                }
            }
        )
    }
}

/**
 * High-density compact produce row.
 * Features botanical vector art badge, clear English/Hindi typography, and wholesale 1-tap chips (+5, +10, +25 kg).
 */
@Composable
fun CompactBuyerProduceRow(
    product: ProductDto,
    quantityInCart: Double,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onAddQuickQuantity: (Double) -> Unit
) {
    val safeName = product.safeName
    val hindiName = product.hindiName ?: MandiTranslationUtils.translateEnglishToHindi(safeName)
    val unitName = product.unitType?.name ?: "KG"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NeutralSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botanical Artwork Badge & Bilingual Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ProduceThumbnailBadge(
                        name = safeName,
                        hindiName = hindiName,
                        imageUrl = product.imageUrl,
                        size = 52.dp,
                        cornerRadius = 12.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = safeName,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MainInk
                        )
                        if (hindiName != null) {
                            Text(
                                text = hindiName,
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSecondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "₹${product.currentPrice.toInt()} / $unitName",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ActionGreen,
                                fontWeight = FontWeight.Bold
                            )

                            if (product.basePrice != null && product.basePrice > product.currentPrice) {
                                Text(
                                    text = "₹${product.basePrice.toInt()}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                    ),
                                    color = InkTertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if ((product.discountAmountPerUnit ?: 0.0) > 0.0) {
                                Surface(
                                    color = ActionGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (product.discountType == "PERCENTAGE") "${product.discountValue?.toInt()}% OFF" else "₹${product.discountValue?.toInt() ?: product.discountAmountPerUnit?.toInt()} OFF",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = ActionGreen,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Stepper (- [Qty] +) or (+ ADD) with 48dp Touch Targets
                if (quantityInCart == 0.0) {
                    Button(
                        onClick = onIncrement,
                        colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .bounceClick(onClick = onIncrement)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ADD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onDecrement,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SecondarySurface)
                                .bounceClick(onClick = onDecrement)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = MainInk,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "${quantityInCart.toInt()} $unitName",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MainInk,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = onIncrement,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ActionGreen)
                                .bounceClick(onClick = onIncrement)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Wholesale Quick-Add Chips (+5, +10, +25 kg) - Visible when item in cart
            if (quantityInCart > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick add: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkTertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    listOf(5.0, 10.0, 25.0).forEach { quickQty ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SecondarySurface,
                            border = BorderStroke(0.5.dp, BorderSubtle),
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .bounceClick { onAddQuickQuantity(quickQty) }
                        ) {
                            Text(
                                text = "+${quickQty.toInt()} $unitName",
                                color = ActionGreen,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shimmer placeholder skeleton for zero-wait visual feedback.
 */
@Composable
fun ProduceRowSkeleton() {
    val shimmerBrush = rememberShimmerBrush()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = NeutralSurface,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

