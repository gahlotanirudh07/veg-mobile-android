package com.freshveg.app.core.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Zomato-style vertical rolling odometer for numerical values (e.g. prices and quantities).
 */
@Composable
fun AnimatedOdometerText(
    value: Int,
    prefix: String = "₹",
    suffix: String = "",
    textStyle: TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White),
    modifier: Modifier = Modifier
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (prefix.isNotEmpty()) {
            Text(text = prefix, style = textStyle)
        }

        AnimatedContent(
            targetState = value,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically(animationSpec = tween(220)) { height -> height } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutVertically(animationSpec = tween(220)) { height -> -height } + fadeOut(animationSpec = tween(220)))
                } else {
                    (slideInVertically(animationSpec = tween(220)) { height -> -height } + fadeIn(animationSpec = tween(220)))
                        .togetherWith(slideOutVertically(animationSpec = tween(220)) { height -> height } + fadeOut(animationSpec = tween(220)))
                }
            },
            label = "odometerTransition"
        ) { targetVal ->
            Text(text = "$targetVal", style = textStyle)
        }

        if (suffix.isNotEmpty()) {
            Text(text = suffix, style = textStyle)
        }
    }
}
