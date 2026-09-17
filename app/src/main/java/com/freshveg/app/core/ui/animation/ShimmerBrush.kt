package com.freshveg.app.core.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Reusable animated shimmer brush for skeleton loading placeholders.
 */
@Composable
fun rememberShimmerBrush(
    shimmerColors: List<Color> = listOf(
        Color(0xFFEBEBEB),
        Color(0xFFF7F7F7),
        Color(0xFFEBEBEB)
    )
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnimation.value, y = translateAnimation.value)
    )
}
