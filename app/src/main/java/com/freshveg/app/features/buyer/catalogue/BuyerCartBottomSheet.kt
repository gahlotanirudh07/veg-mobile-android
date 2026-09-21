package com.freshveg.app.features.buyer.catalogue

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.R
import com.freshveg.app.core.network.CartItemDetail
import com.freshveg.app.core.ui.ProduceThumbnailBadge
import com.freshveg.app.core.ui.ProduceVisualUtils
import com.freshveg.app.core.ui.theme.*
import com.freshveg.app.core.utils.MandiTranslationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerCartBottomSheet(
    cartItems: List<CartItemDetail>,
    estimatedTotal: Double,
    isPlacingOrder: Boolean,
    onUpdateQuantity: (String, Double) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onPlaceOrder: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var deliveryNotes by remember { mutableStateOf("") }
    val view = LocalView.current
    val focusManager = LocalFocusManager.current

    val totalSavings = remember(cartItems) {
        cartItems.sumOf { item ->
            val origPrice = item.product.basePrice ?: item.product.currentPrice
            val lineOrig = origPrice * item.quantity
            val saving = lineOrig - item.lineTotal
            if (saving > 0) saving else 0.0
        }
    }

    fun triggerHaptic() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    val currentLang by (com.freshveg.app.core.i18n.LanguageManager.instance?.currentLanguage ?: remember { mutableStateOf(com.freshveg.app.core.i18n.AppLanguage.ENGLISH) }).let {
        if (it is kotlinx.coroutines.flow.StateFlow<*>) (it as kotlinx.coroutines.flow.StateFlow<com.freshveg.app.core.i18n.AppLanguage>).collectAsState() else remember { mutableStateOf(com.freshveg.app.core.i18n.AppLanguage.ENGLISH) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
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
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.buyer_cart_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MainInk
                        )
                        Text(
                            text = stringResource(R.string.buyer_cart_items_count, cartItems.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }

                if (cartItems.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            triggerHaptic()
                            onClearCart()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.buyer_cart_clear),
                            color = MutedRedError,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderSubtle)

            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛒", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.buyer_cart_empty),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MainInk
                        )
                        Text(
                            text = stringResource(R.string.buyer_cart_empty_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = InkTertiary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        val product = item.product
                        val unitName = product.unitType.name.lowercase()
                        val isHindi = currentLang == com.freshveg.app.core.i18n.AppLanguage.HINDI
                        val hindiName = product.hindiName ?: MandiTranslationUtils.translateEnglishToHindi(product.safeName)
                        val displayName = ProduceVisualUtils.getProduceDisplayName(product.safeName, hindiName, isHindi)
                        val secondaryName = ProduceVisualUtils.getProduceSecondaryName(product.safeName, hindiName, isHindi)
                        val displayUnit = ProduceVisualUtils.getUnitDisplayName(unitName, isHindi)
                        var textInput by remember(item.quantity) {
                            mutableStateOf(ProduceVisualUtils.formatQuantityValue(item.quantity))
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeutralSurface,
                            border = BorderStroke(0.75.dp, BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProduceThumbnailBadge(
                                    name = product.safeName,
                                    hindiName = hindiName,
                                    imageUrl = product.imageUrl,
                                    size = 50.dp,
                                    cornerRadius = 10.dp
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MainInk
                                    )
                                    if (!secondaryName.isNullOrBlank()) {
                                        Text(
                                            text = secondaryName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = InkSecondary
                                        )
                                    }
                                    Text(
                                        text = "₹${product.effectivePrice.toInt()}/$displayUnit",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ActionGreen
                                    )
                                }

                                // Interactive Decimal Stepper Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            triggerHaptic()
                                            val next = item.quantity - 0.5
                                            if (next <= 0.0) {
                                                onRemoveItem(product.id)
                                            } else {
                                                onUpdateQuantity(product.id, next)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SecondarySurface)
                                    ) {
                                        Icon(
                                            imageVector = if (item.quantity <= 0.5) Icons.Outlined.Delete else Icons.Default.Remove,
                                            contentDescription = "Decrease",
                                            tint = if (item.quantity <= 0.5) MutedRedError else MainInk,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    // Direct typing box
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, BorderSubtle),
                                        modifier = Modifier
                                            .width(52.dp)
                                            .height(30.dp)
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
                                                    if (parsed != null && parsed > 0) {
                                                        onUpdateQuantity(product.id, parsed)
                                                    }
                                                },
                                                singleLine = true,
                                                textStyle = TextStyle(
                                                    fontSize = 13.sp,
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
                                                        val parsed = textInput.toDoubleOrNull()
                                                        if (parsed == null || parsed <= 0) {
                                                            onRemoveItem(product.id)
                                                        } else {
                                                            onUpdateQuantity(product.id, parsed)
                                                        }
                                                    }
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            triggerHaptic()
                                            onUpdateQuantity(product.id, item.quantity + 0.5)
                                        },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ActionGreen)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = ProduceVisualUtils.formatCurrency(item.lineTotal),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MainInk
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Verified Rate Banner
            Surface(
                color = ActionGreen.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = ActionGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.buyer_cart_delivery_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Special Dispatch Notes
            OutlinedTextField(
                value = deliveryNotes,
                onValueChange = { deliveryNotes = it },
                label = { Text(stringResource(R.string.buyer_cart_note)) },
                placeholder = { Text(stringResource(R.string.buyer_cart_note_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ActionGreen,
                    unfocusedBorderColor = BorderSubtle,
                    focusedContainerColor = SecondarySurface,
                    unfocusedContainerColor = SecondarySurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Pricing & Order Submit CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (totalSavings > 0.0) {
                        Text(
                            text = stringResource(R.string.buyer_cart_savings, ProduceVisualUtils.formatCurrency(totalSavings)),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ActionGreen
                        )
                    }
                    Text(
                        text = stringResource(R.string.common_total),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkTertiary
                    )
                    Text(
                        text = ProduceVisualUtils.formatCurrency(estimatedTotal),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                }

                Button(
                    onClick = {
                        triggerHaptic()
                        onPlaceOrder(deliveryNotes)
                    },
                    enabled = cartItems.isNotEmpty() && !isPlacingOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .padding(start = 12.dp)
                ) {
                    if (isPlacingOrder) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.buyer_place_order),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}
}
