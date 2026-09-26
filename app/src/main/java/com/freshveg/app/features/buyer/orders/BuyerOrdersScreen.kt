package com.freshveg.app.features.buyer.orders

import android.content.Context
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.freshveg.app.R
import com.freshveg.app.core.network.OrderDto
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.safeDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrdersScreen(
    onNavigateBack: () -> Unit,
    onReorder: (OrderDto) -> Unit = {},
    viewModel: BuyerOrdersViewModel = hiltViewModel()
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

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.transientError) {
        uiState.transientError?.let {
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
                            text = stringResource(R.string.orders_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MainInk
                        )
                        Text(
                            text = stringResource(R.string.orders_dispatch_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = MainInk)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondarySurface)
            )
        },
        containerColor = SecondarySurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date Filter Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "TODAY" to stringResource(R.string.orders_filter_today),
                    "ALL" to stringResource(R.string.orders_tab_all),
                    "YESTERDAY" to stringResource(R.string.orders_filter_yesterday),
                    "THIS_WEEK" to stringResource(R.string.orders_filter_this_week)
                ).forEach { (filter, label) ->
                    val isSelected = uiState.selectedDateFilter == filter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) ActionGreen else NeutralSurface,
                        modifier = Modifier.clickable { viewModel.onSelectDateFilter(filter) }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MainInk,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // 1.5 Neon DB Waking Up Indicator
            AnimatedVisibility(
                visible = uiState.isWakingUp,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFD97706)
                        )
                        Text(
                            text = "⚡ Connecting to live Mandi database...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF92400E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                if (uiState.isLoading && uiState.orders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ActionGreen)
                    }
                } else if (uiState.orders.isEmpty() && uiState.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Could not load orders",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MainInk
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.errorMessage ?: "The database is starting up or network was interrupted.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::refresh,
                                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Connection", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (uiState.filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (uiState.selectedDateFilter == "TODAY") "No orders placed today" else stringResource(R.string.orders_empty),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MainInk
                            )
                            if (uiState.selectedDateFilter == "TODAY" && uiState.orders.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.onSelectDateFilter("ALL") },
                                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("View Past Orders (${uiState.orders.size})", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredOrders, key = { it.id }) { order ->
                            BuyerOrderCard(
                                order = order,
                                onClick = { viewModel.openOrderDetail(order) },
                                onShare = { viewModel.shareOrderStatus(context, order) }
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

    uiState.selectedOrder?.let { order ->
        BuyerOrderDetailBottomSheet(
            order = order,
            onDismiss = { viewModel.closeOrderDetail() },
            onShare = { viewModel.shareOrderStatus(context, order) }
        )
    }
}

@Composable
fun BuyerOrderCard(
    order: OrderDto,
    onClick: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.orders_card_number, order.orderNumber.ifBlank { order.id.takeLast(6) }),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MainInk
                    )
                    Text(
                        text = (order.placedAt ?: order.createdAt ?: "").safeDate(),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (order.status) {
                        "FULFILLED", "DELIVERED" -> ActionGreen.copy(alpha = 0.12f)
                        "CONFIRMED" -> Color(0xFF1976D2).copy(alpha = 0.12f)
                        "CANCELLED" -> MutedRedError.copy(alpha = 0.12f)
                        else -> AmberWarning.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = ProduceVisualUtils.getOrderStatusDisplayName(order.status),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (order.status) {
                            "FULFILLED", "DELIVERED" -> ActionGreen
                            "CONFIRMED" -> Color(0xFF1976D2)
                            "CANCELLED" -> MutedRedError
                            else -> AmberWarning
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val produceSummary = order.items.take(3).joinToString { ProduceVisualUtils.getProduceDisplayName(it.displayName, it.hindiName) }
            Text(
                text = "${stringResource(R.string.orders_item_count, order.items.size)} • $produceSummary",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.common_total)}: ${ProduceVisualUtils.formatCurrency(order.totalAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = ActionGreen
                )

                TextButton(onClick = onClick) {
                    Text(stringResource(R.string.orders_view_weighment_cta), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ActionGreen)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrderDetailBottomSheet(
    order: OrderDto,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val currentLang by (com.freshveg.app.core.i18n.LanguageManager.instance?.currentLanguage ?: remember { mutableStateOf(com.freshveg.app.core.i18n.AppLanguage.ENGLISH) }).let {
        if (it is kotlinx.coroutines.flow.StateFlow<*>) (it as kotlinx.coroutines.flow.StateFlow<com.freshveg.app.core.i18n.AppLanguage>).collectAsState() else remember { mutableStateOf(com.freshveg.app.core.i18n.AppLanguage.ENGLISH) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val locale = remember(currentLang) { java.util.Locale(currentLang.code) }
        val configuration = remember(currentLang, locale) {
            val conf = android.content.res.Configuration(context.resources.configuration)
            conf.setLocale(locale)
            conf.setLayoutDirection(locale)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                conf.setLocales(android.os.LocaleList(locale))
            }
            conf
        }
        val localizedContext = remember(currentLang, locale, configuration) {
            val configContext = context.createConfigurationContext(configuration)
            object : android.content.ContextWrapper(context) {
                override fun getResources(): android.content.res.Resources = configContext.resources
                override fun getAssets(): android.content.res.AssetManager = configContext.assets
            }
        }

        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalConfiguration provides configuration,
            androidx.compose.ui.platform.LocalContext provides localizedContext
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.orders_card_number, order.orderNumber.ifBlank { order.id.takeLast(6) }),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = stringResource(R.string.orders_placed_on, (order.placedAt ?: order.createdAt ?: "").safeDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }

                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = ActionGreen)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(10.dp))

            Text(stringResource(R.string.orders_itemized_weighment), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(order.items, key = { it.id }) { item ->
                    val unit = item.unitSnapshot.lowercase()
                    val itemTitle = ProduceVisualUtils.getProduceDisplayName(item.displayName, item.hindiName)
                    val itemSubtitle = ProduceVisualUtils.getProduceSecondaryName(item.displayName, item.hindiName)
                    val displayUnit = ProduceVisualUtils.getUnitDisplayName(unit)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NeutralSurface,
                        border = androidx.compose.foundation.BorderStroke(0.75.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ProduceThumbnailBadge(
                                    name = item.displayName,
                                    hindiName = item.hindiName,
                                    size = 42.dp,
                                    cornerRadius = 8.dp
                                )
                                Column {
                                    Text(itemTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MainInk)
                                    if (!itemSubtitle.isNullOrBlank()) {
                                        Text(itemSubtitle, style = MaterialTheme.typography.labelSmall, color = InkSecondary)
                                    }
                                    Text(
                                        text = if (item.deliveredQuantity != null && item.deliveredQuantity!! > 0) {
                                            stringResource(
                                                R.string.orders_item_weighed_and_ordered,
                                                ProduceVisualUtils.formatQuantity(item.deliveredQuantity!!, displayUnit),
                                                ProduceVisualUtils.formatQuantity(item.quantity, displayUnit)
                                            )
                                        } else {
                                            stringResource(
                                                R.string.orders_item_ordered_only,
                                                ProduceVisualUtils.formatQuantity(item.quantity, displayUnit)
                                            )
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.deliveredQuantity != null) ActionGreen else InkSecondary
                                    )
                                }
                            }

                            Text(ProduceVisualUtils.formatCurrency(item.total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MainInk)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.common_total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Text(ProduceVisualUtils.formatCurrency(order.totalAmount), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = ActionGreen)
                }
            }
        }
    }
}
}
