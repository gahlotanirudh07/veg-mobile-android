package com.freshveg.app.features.catalog.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.freshveg.app.core.network.ProductDto
import com.freshveg.app.core.ui.theme.AmberWarning
import com.freshveg.app.core.ui.theme.CardSurface
import com.freshveg.app.core.ui.theme.FarmGreenSecondary
import com.freshveg.app.core.ui.theme.ForestGreenPrimary

@Composable
fun VegetableCard(
    product: ProductDto,
    quantity: Int,
    selectedUnitIndex: Int,
    onUnitSelected: (Int) -> Unit,
    onQuantityChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val unitOptions = listOf("250g", "500g", "1kg", "Crate")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Produce Visual Thumbnail (Crisp Emoji + Tinted Palette or Image)
            com.freshveg.app.core.ui.ProduceThumbnailBadge(
                name = product.name,
                hindiName = product.hindiName,
                imageUrl = product.imageUrl,
                size = 105.dp,
                cornerRadius = 12.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Title & Rating
            Text(
                text = if (!product.hindiName.isNullOrEmpty()) "${product.name} / ${product.hindiName}" else product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "₹${product.currentPrice.toInt()} / ${product.unitType.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FarmGreenSecondary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Star, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                Text("4.8", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Unit Selection Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                unitOptions.take(3).forEachIndexed { index, label ->
                    val isSelected = index == selectedUnitIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) FarmGreenSecondary else Color(0xFFEFF4EF))
                            .clickable { onUnitSelected(index) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.DarkGray,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add to Cart / Stepper
            if (quantity == 0) {
                Button(
                    onClick = { onQuantityChanged(1) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ForestGreenPrimary),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onQuantityChanged(quantity - 1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                    }
                    Text("$quantity", color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onQuantityChanged(quantity + 1) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                    }
                }
            }
        }
    }
}
