package com.freshveg.app.features.seller.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.freshveg.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCutoffBottomSheet(
    currentCutoffTime: String,
    onDismiss: () -> Unit,
    onSaveCutoffTime: (String) -> Unit
) {
    var selectedTime by remember { mutableStateOf(currentCutoffTime) }
    val presets = listOf("02:00", "02:30", "03:00", "03:30", "04:00", "04:30", "05:00")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Order Cutoff Time",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MainInk
                    )
                    Text(
                        text = "Order closing deadline for morning delivery",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
                Icon(Icons.Default.Timer, contentDescription = null, tint = ActionGreen, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(14.dp))

            Text("Quick Presets (Morning Cutoff):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // Preset Pills
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { time ->
                    val isSelected = selectedTime.startsWith(time)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTime = "$time AM" },
                        label = { Text("$time AM", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ActionGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current / Custom Time TextField
            OutlinedTextField(
                value = selectedTime,
                onValueChange = { selectedTime = it },
                label = { Text("Selected Cutoff Time") },
                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = ActionGreen) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ℹ️ After this cutoff time, buyer apps will inform restaurants that new orders will be processed for the following day's morning delivery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = { onSaveCutoffTime(selectedTime.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = ActionGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Save & Update Cutoff Deadline", fontWeight = FontWeight.Bold)
            }
        }
    }
}
