package com.worldradio.auto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricEmerald,
    onPrimary = DeepObsidian,
    secondary = EmeraldGlow,
    background = DeepObsidian,
    onBackground = TextHighEmphasis,
    surface = DarkCardSurface,
    onSurface = TextHighEmphasis,
    surfaceVariant = DarkCardElevated,
    onSurfaceVariant = TextMediumEmphasis,
    outline = DarkCardBorder
)

@Composable
fun WorldRadioAutoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
