package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HoloGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.voice.AssistantVoiceState

/**
 * Animated edge-lighting frame that pulses or rotates around the screen border
 */
@Composable
fun EdgeLightFrame(
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    if (!enabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
        ) {
            content()
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "edge_light")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "edge_offset"
    )

    val edgeBrush = Brush.sweepGradient(
        colors = listOf(
            NeonPurple,
            ElectricCyan,
            NeonPink,
            ElectricCyan,
            NeonPurple
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        // Outer glowing subtle border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp)
                .border(
                    width = 1.5.dp,
                    brush = edgeBrush,
                    shape = RoundedCornerShape(12.dp)
                )
        )

        content()
    }
}

/**
 * Futuristic card with glowing neon border and glass effect
 */
@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NeonPurple.copy(alpha = 0.5f),
    shape: Shape = RoundedCornerShape(18.dp),
    onClick: (() -> Unit)? = null,
    testTag: String = "",
    content: @Composable () -> Unit
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Card(
        modifier = modifier
            .testTag(testTag.ifBlank { "neon_card" })
            .border(
                border = BorderStroke(1.2.dp, borderColor),
                shape = shape
            )
            .then(clickableModifier),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = CyberCard
        )
    ) {
        content()
    }
}

/**
 * Glowing status badge for Assistant state (Listening, Processing, Speaking, Error, Connected)
 */
@Composable
fun AssistantStatusBadge(
    state: AssistantVoiceState,
    modifier: Modifier = Modifier
) {
    val (statusText, badgeColor) = when (state) {
        AssistantVoiceState.IDLE -> "Connected · Standby" to HoloGreen
        AssistantVoiceState.LISTENING -> "Listening..." to ElectricCyan
        AssistantVoiceState.PROCESSING -> "Processing..." to NeonPurple
        AssistantVoiceState.SPEAKING -> "Speaking..." to NeonPink
        AssistantVoiceState.ERROR -> "Attention Required" to Color(0xFFEF4444)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CyberCard.copy(alpha = 0.85f))
            .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(badgeColor.copy(alpha = if (state != AssistantVoiceState.IDLE) alphaAnim else 1f))
        )
        Text(
            text = statusText,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Real-time audio waveform visualizer bars
 */
@Composable
fun VoiceWaveformVisualizer(
    amplitude: Float,
    barCount: Int = 18,
    modifier: Modifier = Modifier,
    activeColor: Color = ElectricCyan
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val factor = ((i + 1).toFloat() / barCount.toFloat())
            val phase = Math.sin(factor * Math.PI).toFloat()
            val barHeight = (8.dp + (32.dp * (amplitude * phase).coerceIn(0.05f, 1f)))
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(activeColor, NeonPurple)
                        )
                    )
            )
        }
    }
}
