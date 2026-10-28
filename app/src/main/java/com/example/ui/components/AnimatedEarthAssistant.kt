package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.voice.AssistantVoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedEarthAssistant(
    voiceState: AssistantVoiceState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    onEarthClicked: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "earth_anim")

    // Rotation speed changes dynamically based on voice state
    val rotationDuration = when (voiceState) {
        AssistantVoiceState.PROCESSING -> 4000
        AssistantVoiceState.LISTENING -> 8000
        AssistantVoiceState.SPEAKING -> 6000
        else -> 18000
    }

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "globe_rotation"
    )

    // Orbital ring 1 angle
    val orbitAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle_1"
    )

    // Pulse factor for atmospheric glow
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    // Reactive accent colors based on conversation state
    val stateAuraColor = when (voiceState) {
        AssistantVoiceState.LISTENING -> ElectricCyan
        AssistantVoiceState.PROCESSING -> NeonPurple
        AssistantVoiceState.SPEAKING -> NeonPink
        AssistantVoiceState.ERROR -> Color(0xFFEF4444)
        AssistantVoiceState.IDLE -> ElectricCyan.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .size(310.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEarthClicked
            )
            .testTag("animated_earth_globe"),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing atmospheric energy ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) - 18.dp.toPx()

            // 1. Atmospheric Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        stateAuraColor.copy(alpha = 0.35f * pulseGlow),
                        stateAuraColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.35f
                ),
                radius = radius * 1.35f,
                center = center
            )

            // 2. Tilted Elliptical Orbital Ring 1 (60 deg tilt)
            val ringWidth = radius * 1.3f
            val ringHeight = radius * 0.45f
            val orbitPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center = center,
                        radius = ringWidth
                    )
                )
            }

            // Draw angled orbital ring with dash effect
            drawOval(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        stateAuraColor,
                        NeonPurple,
                        NeonPink,
                        ElectricCyan,
                        stateAuraColor
                    ),
                    center = center
                ),
                topLeft = Offset(center.x - ringWidth, center.y - ringHeight),
                size = Size(ringWidth * 2, ringHeight * 2),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), orbitAngle1 * 2)
                )
            )

            // Satellite node moving on orbit 1
            val satRad = Math.toRadians(orbitAngle1.toDouble())
            val satX = center.x + ringWidth * cos(satRad).toFloat()
            val satY = center.y + ringHeight * sin(satRad).toFloat()
            drawCircle(
                color = Color.White,
                radius = 4.5.dp.toPx(),
                center = Offset(satX, satY)
            )
            drawCircle(
                color = stateAuraColor,
                radius = 8.dp.toPx(),
                center = Offset(satX, satY),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Dynamic acoustic ripple when speaking or listening
            if (voiceState == AssistantVoiceState.SPEAKING || voiceState == AssistantVoiceState.LISTENING) {
                val rippleRadius = radius * (1.0f + (amplitude * 0.3f))
                drawCircle(
                    color = stateAuraColor.copy(alpha = (0.6f - (amplitude * 0.3f)).coerceAtLeast(0.1f)),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Inner Earth Sphere with Holographic Texture
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .border(2.dp, stateAuraColor.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Earth globe asset with steady / reactive rotation
            Image(
                painter = painterResource(id = R.drawable.earth_globe),
                contentDescription = "Holographic Earth Assistant",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotationAngle * 0.15f)
            )

            // Directional Day/Night Terminator Shadow and Cyber Grid Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2

                // Curved shadow across the globe for 3D sphere volume
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x55000000),
                            Color(0xCC07090E)
                        ),
                        center = Offset(center.x * 0.7f, center.y * 0.7f),
                        radius = radius * 1.1f
                    ),
                    radius = radius,
                    center = center
                )

                // 3D Geodesic Coordinate Grid lines (meridians and parallels)
                val numMeridians = 6
                for (i in 0 until numMeridians) {
                    val angleOffset = Math.toRadians((rotationAngle + (i * (360f / numMeridians))).toDouble())
                    val xOffset = (radius * sin(angleOffset)).toFloat()
                    val isFront = cos(angleOffset) > 0

                    if (isFront) {
                        drawOval(
                            color = stateAuraColor.copy(alpha = 0.25f),
                            topLeft = Offset(center.x - Math.abs(xOffset), 0f),
                            size = Size(Math.abs(xOffset) * 2, size.height),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }

                // Latitude parallels
                val parallels = listOf(0.3f, 0.5f, 0.7f)
                parallels.forEach { factor ->
                    val y = size.height * factor
                    val dy = Math.abs(y - center.y)
                    val chordHalfWidth = kotlin.math.sqrt((radius * radius - dy * dy).coerceAtLeast(0f))
                    if (chordHalfWidth > 0) {
                        drawLine(
                            color = stateAuraColor.copy(alpha = 0.2f),
                            start = Offset(center.x - chordHalfWidth, y),
                            end = Offset(center.x + chordHalfWidth, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }
        }

        // Prominent glowing microphone button at the bottom of the globe
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 12.dp)
                .size(62.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(stateAuraColor, NeonPurple)
                    )
                )
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEarthClicked
                )
                .testTag("earth_center_mic_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (voiceState == AssistantVoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Microphone Toggle",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
