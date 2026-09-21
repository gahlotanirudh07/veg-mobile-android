package com.freshveg.app.features.seller.rates

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.R
import androidx.compose.ui.res.stringResource
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerRatesScreen(
    onNavigateBack: () -> Unit,
    viewModel: SellerRatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Morning Rate Board (मंडी भाव)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Set wholesale selling prices for buyers",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MainInk)
                    }
                },
                actions = {
                    // WhatsApp Share Rate Card
                    IconButton(onClick = { viewModel.shareRateCardOnWhatsApp(context) }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share on WhatsApp",
                            tint = ActionGreen
                        )
                    }
                    // Refresh
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ActionGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardSurface)
            )
        },
        bottomBar = {
            // Sticky Save Bar if changes exist
            AnimatedVisibility(
                visible = uiState.modifiedCount > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = CardSurface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${uiState.modifiedCount} Rate Changes Pending",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ActionGreen
                            )
                            Text(
                                text = "Tap Save to publish to all buyers",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }

                        Button(
                            onClick = viewModel::saveRates,
                            enabled = !uiState.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Rates", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        containerColor = BackgroundSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. Bulk Adjuster & WhatsApp Bar
            Surface(
                color = CardSurface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Quick Bulk Action Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Adjust:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )

                        SuggestionChip(
                            onClick = { viewModel.applyBulkAdjustmentToAll(2.0) },
                            label = { Text("+₹2 All") },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFE8F5E9))
                        )

                        SuggestionChip(
                            onClick = { viewModel.applyBulkAdjustmentToAll(-2.0) },
                            label = { Text("-₹2 All") },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFFFEBEE))
                        )

                        SuggestionChip(
                            onClick = { viewModel.applyBulkAdjustmentToAll(5.0) },
                            label = { Text("+₹5 All") },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFE8F5E9))
                        )

                        SuggestionChip(
                            onClick = { viewModel.shareRateCardOnWhatsApp(context) },
                            label = { Text("📲 Share WhatsApp") },
                            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFFE0F2FE))
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Search vegetable by name or हिंदी...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ActionGreen,
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedContainerColor = BackgroundSurface,
                            unfocusedContainerColor = BackgroundSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.selectedCategoryId == null,
                            onClick = { viewModel.onCategorySelect(null) },
                            label = { Text("All (${uiState.rateItems.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ActionGreen,
                                selectedLabelColor = Color.White
                            )
                        )

                        uiState.categories.forEach { cat ->
                            FilterChip(
                                selected = uiState.selectedCategoryId == cat.id,
                                onClick = {
                                    viewModel.onCategorySelect(if (uiState.selectedCategoryId == cat.id) null else cat.id)
                                },
                                label = { Text(cat.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ActionGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 2. Rate Items List
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                if (uiState.isLoading && uiState.rateItems.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ActionGreen)
                } else if (uiState.filteredItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "No vegetables match '${uiState.searchQuery}'" else "No vegetables available",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.filteredItems, key = { it.product.id }) { item ->
                            SellerRateRowCard(
                                item = item,
                                onAdjustPrice = { delta -> viewModel.adjustPrice(item.product.id, delta) },
                                onPriceChange = { newPrice -> viewModel.setDirectPrice(item.product.id, newPrice) },
                                onToggleAvailability = { viewModel.toggleAvailability(item.product.id) }
                            )
                        }
                    }
                }

                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Color.White,
                    contentColor = ActionGreen
                )
            }
        }
    }
}

@Composable
fun SellerRateRowCard(
    item: RateItemUiState,
    onAdjustPrice: (Double) -> Unit,
    onPriceChange: (Double) -> Unit,
    onToggleAvailability: () -> Unit
) {
    val p = item.product
    val isModified = item.isModified
    var isEditingDirectPrice by remember { mutableStateOf(false) }
    var directPriceInput by remember(item.currentPrice) { mutableStateOf(item.currentPrice.toInt().toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isModified) Color(0xFFF9FFF9) else CardSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isModified) 3.dp else 1.dp),
        border = if (isModified) androidx.compose.foundation.BorderStroke(1.5.dp, ActionGreen) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Produce Thumbnail + Title + In-Stock Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProduceThumbnailBadge(
                    name = p.name,
                    hindiName = p.hindiName,
                    imageUrl = p.imageUrl,
                    size = 52.dp,
                    cornerRadius = 10.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!p.hindiName.isNullOrEmpty()) "${p.name} (${p.hindiName})" else p.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Unit: ${p.unitType.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )

                        if (item.grossMarginPercentage != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (item.grossMarginPercentage!! >= 15) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${item.grossMarginPercentage}% margin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.grossMarginPercentage!! >= 15) ActionGreen else Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isModified) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Modified",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ActionGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // In-Stock Switch
                Switch(
                    checked = item.isAvailable,
                    onCheckedChange = { onToggleAvailability() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ActionGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 1-Thumb Steppers & Live Rate Adjuster Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Decrement Steppers: [-5], [-2]
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { onAdjustPrice(-5.0) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text("-5", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 13.sp)
                    }

                    FilledTonalButton(
                        onClick = { onAdjustPrice(-2.0) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text("-2", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 13.sp)
                    }
                }

                // Center Price Display (Tap to Edit directly)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isModified) Color(0xFFE8F5E9) else Color(0xFFF5F5F5))
                        .clickable { isEditingDirectPrice = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEditingDirectPrice) {
                        OutlinedTextField(
                            value = directPriceInput,
                            onValueChange = { input ->
                                directPriceInput = input
                                input.toDoubleOrNull()?.let { onPriceChange(it) }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(80.dp),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₹${item.currentPrice.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isModified) ActionGreen else Color.Black
                            )
                            Text(
                                text = "/${p.unitType.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                // Increment Steppers: [+2], [+5]
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { onAdjustPrice(2.0) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Text("+2", fontWeight = FontWeight.Bold, color = ActionGreen, fontSize = 13.sp)
                    }

                    FilledTonalButton(
                        onClick = { onAdjustPrice(5.0) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Text("+5", fontWeight = FontWeight.Bold, color = ActionGreen, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
