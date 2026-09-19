package com.freshveg.app.features.buyer.catalogue

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.R
import com.freshveg.app.core.i18n.AppLanguage
import com.freshveg.app.core.i18n.LanguageManager
import com.freshveg.app.core.network.CategoryDto
import com.freshveg.app.core.network.ProductDto
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.ui.animation.bounceClick
import com.freshveg.app.core.ui.animation.rememberShimmerBrush
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.MandiTranslationUtils
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerCatalogueScreen(
    viewModel: BuyerCatalogueViewModel,
    onNavigateToOrders: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val currentLang by (LanguageManager.instance?.currentLanguage ?: remember { mutableStateOf(AppLanguage.ENGLISH) }).let {
        if (it is StateFlow<*>) (it as StateFlow<AppLanguage>).collectAsState() else remember { mutableStateOf(AppLanguage.ENGLISH) }
    }

    LaunchedEffect(state.orderSuccessDto) {
        state.orderSuccessDto?.let {
            snackbarHostState.showSnackbar(
                message = "Morning Order #${it.id.takeLast(6)} Placed Successfully! Supplier Notified.",
                duration = SnackbarDuration.Short
            )
            viewModel.clearOrderSuccess()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SecondarySurface,
        bottomBar = {
            AnimatedVisibility(
                visible = state.cartItemCount > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    ElevatedCard(
                        onClick = { viewModel.openCart() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = ActionGreen),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingCart,
                                        contentDescription = "Cart",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = stringResource(R.string.buyer_cart_items_count, state.cartItemCount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Estimated: ${ProduceVisualUtils.formatCurrency(state.cartEstimatedTotal)}",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.buyer_cart_bar_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(bottom = if (state.cartItemCount > 0) 80.dp else 16.dp)
            ) {
                // Top Header with Supplier & Language Switch
                item {
                    Surface(
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.nav_catalogue),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MainInk
                                    )
                                    state.connectedSeller?.let { seller ->
                                        Text(
                                            text = stringResource(R.string.buyer_supplier_info, seller.businessName.ifBlank { seller.primaryContactName ?: "Supplier" }),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ActionGreen
                                        )
                                    }
                                }

                                // Language Toggle Chip
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = SecondarySurface,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.clickable {
                                        LanguageManager.instance?.toggleLanguage()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (currentLang == AppLanguage.HINDI) "🇮🇳 हिन्दी" else "🇬🇧 EN",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MainInk
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Cutoff Warning Banner
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFF8E1),
                                border = BorderStroke(0.5.dp, Color(0xFFFFE082)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.buyer_order_cutoff, state.cutoffTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Search Field
                            OutlinedTextField(
                                value = state.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.buyer_search_produce),
                                        fontSize = 13.5.sp,
                                        color = InkTertiary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search",
                                        tint = InkSecondary
                                    )
                                },
                                trailingIcon = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = InkSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ActionGreen,
                                    unfocusedBorderColor = BorderSubtle,
                                    focusedContainerColor = SecondarySurface,
                                    unfocusedContainerColor = SecondarySurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Category Chips Row
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            CategoryFilterChip(
                                title = stringResource(R.string.buyer_category_all),
                                isSelected = state.selectedCategoryId == "ALL",
                                onClick = { viewModel.selectCategory("ALL") }
                            )
                        }
                        items(state.categories, key = { it.id }) { cat ->
                            CategoryFilterChip(
                                title = cat.name,
                                isSelected = state.selectedCategoryId == cat.id,
                                onClick = { viewModel.selectCategory(cat.id) }
                            )
                        }
                    }
                }

                // Frequent Staples Banner (1-Tap Reorder from Last Order)
                if (state.lastOrder != null && state.cartItemCount == 0) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFE8F5E9),
                            border = BorderStroke(1.dp, ActionGreen.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.buyer_frequent_reorder),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MainInk
                                    )
                                    Text(
                                        text = "${state.lastOrder?.items?.size ?: 0} items from previous delivery",
                                        fontSize = 11.5.sp,
                                        color = InkSecondary
                                    )
                                }

                                Button(
                                    onClick = { viewModel.reorderAllFromLastOrder() },
                                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Reorder All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Loading Shimmer Skeletons
                if (state.isLoading && state.products.isEmpty()) {
                    items(6) {
                        ProduceRowSkeleton(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                } else if (state.filteredProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🥬", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.common_no_data),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MainInk
                                )
                            }
                        }
                    }
                } else {
                    items(state.filteredProducts, key = { it.id }) { product ->
                        val qtyInCart = state.cart[product.id] ?: 0.0
                        ModernWholesaleProduceCard(
                            product = product,
                            quantityInCart = qtyInCart,
                            onQuantityChanged = { newQty -> viewModel.setQuantity(product.id, newQty) },
                            onIncrement = { viewModel.incrementQuantity(product) },
                            onDecrement = { viewModel.decrementQuantity(product) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    if (state.isCartOpen) {
        BuyerCartBottomSheet(
            cartItems = state.cartItemsList,
            estimatedTotal = state.cartEstimatedTotal,
            isPlacingOrder = state.isPlacingOrder,
            onUpdateQuantity = { id, qty -> viewModel.setQuantity(id, qty) },
            onRemoveItem = { id -> viewModel.removeFromCart(id) },
            onClearCart = { viewModel.clearCart() },
            onPlaceOrder = { notes -> viewModel.placeOrder(notes) },
            onDismiss = { viewModel.closeCart() }
        )
    }
}

@Composable
fun CategoryFilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) ActionGreen else NeutralSurface,
        border = BorderStroke(1.dp, if (isSelected) ActionGreen else BorderSubtle),
        modifier = Modifier.bounceClick(onClick = onClick)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MainInk,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/**
 * Modern Wholesale Produce Card (Blinkit / Zepto Layout)
 * Right-aligned SVG badge, direct numeric typing, quick wholesale steppers (+0.5, +1, +5, +10 kg).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernWholesaleProduceCard(
    product: ProductDto,
    quantityInCart: Double,
    onQuantityChanged: (Double) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unitName = product.unitType.name.lowercase()
    val hindiName = product.hindiName ?: MandiTranslationUtils.translateEnglishToHindi(product.safeName)
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    var textInput by remember(quantityInCart) {
        mutableStateOf(if (quantityInCart > 0.0) ProduceVisualUtils.formatQuantityValue(quantityInCart) else "")
    }

    fun triggerHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(0.75.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Produce Info & Pricing
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val displayName = ProduceVisualUtils.getProduceDisplayName(product.safeName, hindiName)
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MainInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!hindiName.isNullOrBlank()) {
                        Text(
                            text = hindiName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = InkSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pricing Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "₹${product.effectivePrice.toInt()}/$unitName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ActionGreen
                        )

                        val origBasePrice = product.basePrice
                        if (origBasePrice != null && origBasePrice > product.effectivePrice) {
                            Text(
                                text = "₹${origBasePrice.toInt()}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.LineThrough
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

                // Right Column: Artwork Badge + Stepper Action
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProduceThumbnailBadge(
                        name = product.safeName,
                        hindiName = hindiName,
                        imageUrl = product.imageUrl,
                        size = 64.dp,
                        cornerRadius = 12.dp
                    )

                    // Add Button or Numeric Input Stepper
                    if (quantityInCart == 0.0) {
                        Button(
                            onClick = {
                                triggerHaptic()
                                onIncrement()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .bounceClick {
                                    triggerHaptic()
                                    onIncrement()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(stringResource(R.string.buyer_add_to_cart), fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NeutralSurface,
                            border = BorderStroke(1.dp, ActionGreen.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        triggerHaptic()
                                        onDecrement()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Decrease",
                                        tint = MainInk,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Direct Numeric Editable Text Box
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White,
                                    border = BorderStroke(0.75.dp, BorderSubtle),
                                    modifier = Modifier
                                        .width(52.dp)
                                        .height(28.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        BasicTextField(
                                            value = textInput,
                                            onValueChange = { newVal ->
                                                val filtered = newVal.filter { it.isDigit() || it == '.' }
                                                textInput = filtered
                                                val parsed = filtered.toDoubleOrNull()
                                                if (parsed != null && parsed >= 0) {
                                                    onQuantityChanged(parsed)
                                                }
                                            },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = MainInk
                                            ),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                                imeAction = ImeAction.Done
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    focusManager.clearFocus()
                                                    val parsed = textInput.toDoubleOrNull() ?: 0.0
                                                    onQuantityChanged(parsed)
                                                }
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusEvent { focusState ->
                                                    if (focusState.isFocused) {
                                                        coroutineScope.launch {
                                                            bringIntoViewRequester.bringIntoView()
                                                        }
                                                    }
                                                }
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        triggerHaptic()
                                        onIncrement()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase",
                                        tint = ActionGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Wholesale Increment Steppers (+0.5 kg, +1 kg, +5 kg, +10 kg)
            if (quantityInCart > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0.5, 1.0, 5.0, 10.0).forEach { quickQty ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SecondarySurface,
                            border = BorderStroke(0.5.dp, BorderSubtle),
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .bounceClick {
                                    triggerHaptic()
                                    onQuantityChanged(quantityInCart + quickQty)
                                }
                        ) {
                            Text(
                                text = "+${ProduceVisualUtils.formatQuantityValue(quickQty)} $unitName",
                                color = ActionGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProduceRowSkeleton(modifier: Modifier = Modifier) {
    val shimmerBrush = rememberShimmerBrush()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
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
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmerBrush)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
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
                    .width(70.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
        }
    }
}
