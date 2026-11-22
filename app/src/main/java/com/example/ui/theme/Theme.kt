package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RedGameDarkColorScheme = darkColorScheme(
    primary = RedNeon,
    onPrimary = TextPrimary,
    primaryContainer = RedDarkSurface,
    onPrimaryContainer = TextPrimary,
    secondary = GoldCoin,
    onSecondary = GoldTextDark,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = GoldCoin,
    tertiary = CyberCyan,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianCard,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderRedGlow,
    outlineVariant = BorderSubtle,
    error = RedNeonBright,
    onError = TextPrimary
)

val RedGameShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RedGameTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RedGameDarkColorScheme,
        typography = RedGameTypography,
        shapes = RedGameShapes,
        content = content
    )
}
