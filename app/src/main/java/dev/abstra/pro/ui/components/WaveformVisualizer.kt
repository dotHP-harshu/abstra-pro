package dev.abstra.pro.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abstra.pro.ui.theme.LocalAbstraColors
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    barCount: Int = 28,
    tintColor: Color? = null
) {
    val colors = LocalAbstraColors.current
    val primaryColor = tintColor ?: colors.primary
    val secondaryColor = colors.primaryStrong

    val infiniteTransition = rememberInfiniteTransition(label = "visualizerAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnim"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = (totalWidth / (barCount * 1.6f)).coerceAtLeast(3f)
        val gap = (totalWidth - (barCount * barWidth)) / (barCount - 1).coerceAtLeast(1)

        val brush = Brush.verticalGradient(
            listOf(primaryColor, secondaryColor)
        )

        for (i in 0 until barCount) {
            val fraction = i.toFloat() / barCount
            val wave = if (isPlaying) {
                (sin(phase + fraction * 4 * Math.PI) * 0.45f + 0.55f).toFloat()
            } else {
                0.12f
            }

            val bHeight = (totalHeight * wave).coerceIn(4f, totalHeight)
            val left = i * (barWidth + gap)
            val top = totalHeight - bHeight

            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(barWidth, bHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
