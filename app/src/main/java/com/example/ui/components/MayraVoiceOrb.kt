package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.MayraCyanAccent
import com.example.ui.theme.MayraMagentaAccent
import com.example.ui.theme.MayraPinkGlow
import com.example.ui.theme.MayraPurplePrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MayraVoiceOrb(
    state: AssistantState,
    rmsLevel: Float, // 0f to 1f live audio level
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    // Idle breathing scale
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IdlePulse"
    )

    // Rotation angle for thinking state
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Wave ripple phase for speaking state
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // Audio reactive boost during speech input
    val audioBoost = remember { Animatable(1f) }
    LaunchedEffect(rmsLevel, state) {
        if (state == AssistantState.LISTENING) {
            audioBoost.animateTo(
                targetValue = 1f + (rmsLevel * 0.35f),
                animationSpec = tween(80)
            )
        } else {
            audioBoost.animateTo(1f, animationSpec = tween(300))
        }
    }

    val effectiveScale = when (state) {
        AssistantState.IDLE -> idlePulse
        AssistantState.LISTENING -> audioBoost.value
        AssistantState.THINKING -> 1.0f
        AssistantState.SPEAKING -> 1.02f + (sin(wavePhase * 2 * PI).toFloat() * 0.05f)
        AssistantState.ERROR -> 0.95f
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("mayra_voice_orb")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer soundwave ripples
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.width * 0.38f

            when (state) {
                AssistantState.SPEAKING -> {
                    // Expanding dynamic ripples
                    for (i in 0..2) {
                        val phaseOffset = (wavePhase + (i * 0.33f)) % 1f
                        val rippleRadius = baseRadius + (phaseOffset * (this.size.width * 0.16f))
                        val alpha = (1f - phaseOffset) * 0.65f
                        val rippleColor = if (i % 2 == 0) MayraMagentaAccent else MayraPurplePrimary

                        drawCircle(
                            color = rippleColor.copy(alpha = alpha),
                            radius = rippleRadius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx() * (1f - phaseOffset))
                        )
                    }
                }
                AssistantState.LISTENING -> {
                    // Voice reactive ripples
                    val rippleCount = 3
                    for (i in 0 until rippleCount) {
                        val extra = (rmsLevel * (i + 1) * 18f).dp.toPx()
                        drawCircle(
                            color = MayraCyanAccent.copy(alpha = (0.5f - (i * 0.12f))),
                            radius = baseRadius + extra + (i * 8f),
                            center = center,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
                AssistantState.THINKING -> {
                    // Swirling particle orbit rings
                    val radius = baseRadius * 1.15f
                    val angleRad = (rotationAngle * PI / 180.0).toFloat()
                    val p1 = Offset(
                        center.x + radius * cos(angleRad),
                        center.y + radius * sin(angleRad)
                    )
                    val p2 = Offset(
                        center.x - radius * cos(angleRad),
                        center.y - radius * sin(angleRad)
                    )
                    drawCircle(
                        color = MayraCyanAccent.copy(alpha = 0.8f),
                        radius = 6.dp.toPx(),
                        center = p1
                    )
                    drawCircle(
                        color = MayraMagentaAccent.copy(alpha = 0.8f),
                        radius = 6.dp.toPx(),
                        center = p2
                    )
                }
                else -> {
                    // Subtle ambient aura
                    drawCircle(
                        color = MayraPurplePrimary.copy(alpha = 0.12f),
                        radius = baseRadius * 1.12f,
                        center = center
                    )
                }
            }
        }

        // Central Orb Body
        Box(
            modifier = Modifier
                .size(size * 0.68f)
                .scale(effectiveScale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = when (state) {
                            AssistantState.LISTENING -> listOf(
                                MayraCyanAccent,
                                MayraPurplePrimary,
                                Color(0xFF160D27)
                            )
                            AssistantState.THINKING -> listOf(
                                MayraPinkGlow,
                                MayraPurplePrimary,
                                MayraCyanAccent
                            )
                            AssistantState.SPEAKING -> listOf(
                                MayraMagentaAccent,
                                MayraPurplePrimary,
                                Color(0xFF1E0E38)
                            )
                            AssistantState.ERROR -> listOf(
                                Color(0xFFF87171),
                                Color(0xFFDC2626),
                                Color(0xFF450A0A)
                            )
                            AssistantState.IDLE -> listOf(
                                MayraPurplePrimary,
                                MayraMagentaAccent.copy(alpha = 0.8f),
                                Color(0xFF160D27)
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Icon in the core
            when (state) {
                AssistantState.LISTENING -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listening",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                AssistantState.SPEAKING -> {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speaking",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                AssistantState.THINKING -> {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Thinking",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mayra AI",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
