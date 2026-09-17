package com.freshveg.app.core.ui.animation

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import androidx.compose.foundation.interaction.collectIsPressedAsState

/**
 * Zomato-style bouncy touch effect.
 * Compresses the target composable to [pressedScale] on touch down with spring damping.
 */
fun Modifier.bounceClick(
    pressedScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )
    val view = LocalView.current

    LaunchedEffect(isPressed) {
        if (isPressed) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Exception) {}
        }
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onClick()
                }
            } else {
                Modifier
            }
        )
}

/**
 * Helper to perform subtle tactile haptics on user interaction
 */
@Composable
fun rememberTactileHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {}
    }
}
