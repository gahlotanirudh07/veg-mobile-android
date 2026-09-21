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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
        }
    }
    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            pullToRefreshState.startRefresh()
        } else {
            pullToRefreshState.endRefresh()
        }
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
        containerColor = CatalogueCanvas,
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
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = BrandGreenPrimary),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ShoppingCart,
                                        contentDescription = "Cart",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = stringResource(R.string.buyer_cart_items_count, state.cartItemCount),
                                        style = CatalogueTypography.buttonChip,
                                        color = Color.White
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.buyer_cart_total,
                                            ProduceVisualUtils.formatCurrency(state.cartEstimatedTotal).replace("₹", "").trim()
                                        ),
                                        style = CatalogueTypography.buttonChip.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.buyer_cart_bar_title),
                                    style = CatalogueTypography.buttonChip,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(bottom = if (state.cartItemCount > 0) 80.dp else 16.dp)
            ) {
                // Top Header Card: Title, Supplier Subtitle, Language Switch, Subtle Delivery Strip & Compact Search Bar
                item {
                    Surface(
                        color = ProductCardSurface,
                        border = BorderStroke(1.dp, ProductCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // Row: Screen Title & Language Switcher
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.nav_catalogue),
                                        style = CatalogueTypography.screenTitle
                                    )
                                    state.connectedSeller?.let { seller ->
                                        Text(
                                            text = stringResource(R.string.buyer_supplier_info, seller.businessName.ifBlank { seller.primaryContactName ?: "Supplier" }),
                                            style = CatalogueTypography.secondaryInfo,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Language Toggle Pill (No emoji, Material vector icon, compact 30dp height)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SurfaceMuted,
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    modifier = Modifier.clickable {
                                        LanguageManager.instance?.toggleLanguage()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Translate,
                                            contentDescription = "Language",
                                            tint = InkSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (currentLang == AppLanguage.HINDI) "हिन्दी" else "English",
                                            style = CatalogueTypography.buttonCompact,
                                            color = InkPrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Delivery Information Banner: Subtle informational strip with soft sage tint
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandGreenSurface,
                                border = BorderStroke(0.75.dp, BrandGreenBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = BrandGreenPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.buyer_order_cutoff, state.cutoffTime),
                                        style = CatalogueTypography.secondaryInfo.copy(
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = InkPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Compact Search Bar: 42dp height, soft surface, restrained border
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceMuted,
                                border = BorderStroke(1.dp, BorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search",
                                        tint = InkSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (state.searchQuery.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.buyer_search_produce),
                                                style = CatalogueTypography.searchPlaceholder
                                            )
                                        }
                                        BasicTextField(
                                            value = state.searchQuery,
                                            onValueChange = { viewModel.onSearchQueryChange(it) },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontFamily = AppFontFamily,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = InkPrimary
                                            ),
                                            cursorBrush = SolidColor(BrandGreenPrimary),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (state.searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.onSearchQueryChange("") },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Clear",
                                                tint = InkSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Category Chips Row: 36dp pill shape, intentional partial-next-chip peek
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CatalogueCanvas)
                            .padding(vertical = 10.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 24.dp),
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
                                title = ProduceVisualUtils.getCategoryDisplayName(cat.name, isHindi = (currentLang == AppLanguage.HINDI)),
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
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = BrandGreenSurface,
                            border = BorderStroke(1.dp, BrandGreenBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.buyer_frequent_reorder),
                                        style = CatalogueTypography.buttonChip,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = stringResource(R.string.buyer_frequent_items_count, state.lastOrder?.items?.size ?: 0),
                                        style = CatalogueTypography.secondaryInfo
                                    )
                                }

                                Button(
                                    onClick = { viewModel.reorderAllFromLastOrder() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.buyer_reorder_all),
                                        style = CatalogueTypography.buttonCompact,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Loading Shimmer Skeletons or Product List
                if (state.isLoading && state.products.isEmpty()) {
                    items(6) {
                        ProduceRowSkeleton(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                } else if (state.filteredProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Spa,
                                    contentDescription = null,
                                    tint = InkTertiary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.common_no_data),
                                    style = CatalogueTypography.sectionTitle,
                                    color = InkPrimary
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
                            isHindi = (currentLang == AppLanguage.HINDI),
                            onQuantityChanged = { newQty -> viewModel.setQuantity(product.id, newQty) },
                            onIncrement = { viewModel.incrementQuantity(product) },
                            onDecrement = { viewModel.decrementQuantity(product) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color.White,
                contentColor = BrandGreenPrimary
            )
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

/**
 * Compact 36dp pill shape category filter chip.
 * Selected: BrandGreenPrimary with white text.
 * Unselected: White surface with subtle 1dp border and charcoal/grey-green text.
 */
@Composable
fun CategoryFilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (isSelected) BrandGreenPrimary else ProductCardSurface,
        border = BorderStroke(1.dp, if (isSelected) BrandGreenPrimary else BorderSubtle),
        modifier = Modifier
            .height(36.dp)
            .bounceClick(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = CatalogueTypography.buttonChip,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color.White else InkPrimary
            )
        }
    }
}

/**
 * Compact Modern Wholesale Produce Card
 * - 14dp radius, white product surface, 1dp subtle border, 0dp shadow.
 * - Standardized 60dp uniform imagery badge with minimal inner padding.
 * - Clean typography hierarchy: 16sp semibold title, 12.5sp secondary, 18sp bold price.
 * - Compact 34dp Add button and Stepper with 48dp minimum touch target area.
 * - Quick wholesale increment steppers (+0.5 kg, +1 kg, +5 kg, +10 kg) in 26dp pill chips.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernWholesaleProduceCard(
    product: ProductDto,
    quantityInCart: Double,
    isHindi: Boolean = false,
    onQuantityChanged: (Double) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unitName = product.unitType.name.lowercase()
    val displayUnit = ProduceVisualUtils.getUnitDisplayName(unitName, isHindi = isHindi)
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ProductCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ProductCardBorder)
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
                // Left Column: Produce Titles & Pricing
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val displayName = ProduceVisualUtils.getProduceDisplayName(product.safeName, hindiName, isHindi = isHindi)
                    val secondaryName = ProduceVisualUtils.getProduceSecondaryName(product.safeName, hindiName, isHindi = isHindi)

                    Text(
                        text = displayName,
                        style = CatalogueTypography.productTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!secondaryName.isNullOrBlank()) {
                        Text(
                            text = secondaryName,
                            style = CatalogueTypography.secondaryInfo,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pricing Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₹${product.effectivePrice.toInt()}",
                                style = CatalogueTypography.priceLarge
                            )
                            Text(
                                text = "/$displayUnit",
                                style = CatalogueTypography.priceUnit
                            )
                        }

                        val origBasePrice = product.basePrice
                        if (origBasePrice != null && origBasePrice > product.effectivePrice) {
                            Text(
                                text = "₹${origBasePrice.toInt()}",
                                style = CatalogueTypography.secondaryInfo.copy(
                                    textDecoration = TextDecoration.LineThrough,
                                    fontSize = 12.sp
                                ),
                                color = InkTertiary
                            )
                        }

                        if ((product.discountAmountPerUnit ?: 0.0) > 0.0) {
                            Surface(
                                color = BrandGreenSurface,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, BrandGreenBorder)
                            ) {
                                val discountText = if (product.discountType == "PERCENTAGE") "${product.discountValue?.toInt()}%" else "₹${product.discountValue?.toInt() ?: product.discountAmountPerUnit?.toInt()}"
                                Text(
                                    text = stringResource(R.string.buyer_discount_off, discountText),
                                    style = CatalogueTypography.microBadge,
                                    color = BrandGreenPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Column: Standardized 60dp Produce Artwork Badge + Compact Stepper Action
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProduceThumbnailBadge(
                        name = product.safeName,
                        hindiName = hindiName,
                        imageUrl = product.imageUrl,
                        size = 60.dp,
                        cornerRadius = 10.dp
                    )

                    // Add Button or Numeric Input Stepper (with 48dp minimum touch target bounding)
                    if (quantityInCart == 0.0) {
                        Button(
                            onClick = {
                                triggerHaptic()
                                onIncrement()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .bounceClick {
                                    triggerHaptic()
                                    onIncrement()
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.buyer_add_to_cart),
                                style = CatalogueTypography.buttonCompact,
                                color = Color.White
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = ProductCardSurface,
                                border = BorderStroke(1.dp, BrandGreenBorder)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .height(32.dp)
                                        .padding(horizontal = 2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            triggerHaptic()
                                            onDecrement()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Remove,
                                            contentDescription = "Decrease",
                                            tint = InkPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    // Direct Numeric Editable Box
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SurfaceMuted,
                                        border = BorderStroke(0.75.dp, BorderSubtle),
                                        modifier = Modifier
                                            .width(44.dp)
                                            .height(26.dp)
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
                                                    fontFamily = AppFontFamily,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    color = InkPrimary
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
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Add,
                                            contentDescription = "Increase",
                                            tint = BrandGreenPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Wholesale Increment Steppers (+0.5 kg, +1 kg, +5 kg, +10 kg)
            if (quantityInCart > 0.0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0.5, 1.0, 5.0, 10.0).forEach { quickQty ->
                        Surface(
                            onClick = {
                                triggerHaptic()
                                onQuantityChanged(quantityInCart + quickQty)
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceMuted,
                            border = BorderStroke(0.5.dp, BorderSubtle),
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "+${ProduceVisualUtils.formatQuantityValue(quickQty)} $displayUnit",
                                color = BrandGreenPrimary,
                                style = CatalogueTypography.microBadge,
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
            .height(74.dp),
        shape = RoundedCornerShape(14.dp),
        color = ProductCardSurface,
        border = BorderStroke(1.dp, ProductCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(shimmerBrush)
            )
        }
    }
}
