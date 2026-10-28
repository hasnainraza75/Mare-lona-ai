package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// LONA AI futuristic cyberpunk neon palette
val CyberBackground = Color(0xFF07090E)
val CyberSurface = Color(0xFF0E131F)
val CyberCard = Color(0xFF131A2B)
val CyberCardBorder = Color(0xFF222C46)
val CyberCardHighlight = Color(0xFF1C263D)

// Neon Accents
val NeonPurple = Color(0xFFA855F7)
val NeonPurpleGlow = Color(0xFFC084FC)
val ElectricCyan = Color(0xFF00F0FF)
val ElectricCyanGlow = Color(0xFF67E8F9)
val CyberBlue = Color(0xFF3B82F6)
val NeonPink = Color(0xFFEC4899)
val NeonPinkGlow = Color(0xFFF472B6)
val HoloGreen = Color(0xFF10B981)
val AlertRed = Color(0xFFEF4444)

// Text
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Gradients
val NeonCyberGradient = Brush.linearGradient(
    colors = listOf(NeonPurple, ElectricCyan)
)

val NeonPurplePinkGradient = Brush.linearGradient(
    colors = listOf(NeonPurple, NeonPink)
)

val CyanBlueGradient = Brush.linearGradient(
    colors = listOf(ElectricCyan, CyberBlue)
)

val CardGlowGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1A2238), Color(0xFF0E131F))
)
