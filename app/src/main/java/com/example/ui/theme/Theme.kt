package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LonaDarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2C164D),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = ElectricCyan,
    onSecondary = Color(0xFF041E28),
    secondaryContainer = Color(0xFF063345),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = NeonPink,
    onTertiary = Color.White,
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCard,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    outlineVariant = Color(0xFF1E293B)
)

@Composable
fun LonaAiTheme(
    accentColor: Color = NeonPurple,
    content: @Composable () -> Unit
) {
    val dynamicScheme = LonaDarkColorScheme.copy(
        primary = accentColor
    )

    MaterialTheme(
        colorScheme = dynamicScheme,
        typography = Typography,
        content = content
    )
}
