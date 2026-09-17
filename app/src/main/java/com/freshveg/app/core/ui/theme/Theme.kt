package com.freshveg.app.core.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = CardSurface,
    secondary = FarmGreenSecondary,
    onSecondary = CardSurface,
    tertiary = MintGreenTertiary,
    background = BackgroundSurface,
    surface = CardSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = RedError
)

@Composable
fun FreshVegTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(
                fontFamily = AppFontFamily,
                color = MainInk
            ),
            content = content
        )
    }
}
