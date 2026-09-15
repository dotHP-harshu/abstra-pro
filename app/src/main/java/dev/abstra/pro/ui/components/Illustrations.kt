package dev.abstra.pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abstra.pro.ui.theme.LocalAbstraColors

@Composable
fun EmptyLibraryIllustration(modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val colors = LocalAbstraColors.current
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Outer soft circle
        drawCircle(colors.surfaceAlt.copy(alpha = 0.5f), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.5f))

        // Vinyl Disc outline
        drawCircle(colors.primary, radius = w * 0.32f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 3f))
        drawCircle(colors.primary.copy(alpha = 0.5f), radius = w * 0.24f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 1.5f))
        drawCircle(colors.primaryStrong, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
        drawCircle(colors.white, radius = w * 0.04f, center = Offset(w * 0.5f, h * 0.5f))
    }
}

@Composable
fun EmptyPlaylistsIllustration(modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val colors = LocalAbstraColors.current
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawCircle(colors.surfaceAlt.copy(alpha = 0.5f), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.5f))

        // Folder outline
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.35f)
            lineTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.52f, h * 0.42f)
            lineTo(w * 0.75f, h * 0.42f)
            lineTo(w * 0.75f, h * 0.70f)
            lineTo(w * 0.25f, h * 0.70f)
            close()
        }
        drawPath(path, colors.primary, style = Stroke(width = 3.5f))

        // Plus mark
        drawLine(colors.primaryStrong, Offset(w * 0.5f, h * 0.50f), Offset(w * 0.5f, h * 0.62f), strokeWidth = 3f)
        drawLine(colors.primaryStrong, Offset(w * 0.44f, h * 0.56f), Offset(w * 0.56f, h * 0.56f), strokeWidth = 3f)
    }
}

@Composable
fun EmptyLyricsIllustration(modifier: Modifier = Modifier, size: Dp = 140.dp) {
    val colors = LocalAbstraColors.current
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawCircle(colors.surfaceAlt.copy(alpha = 0.5f), radius = w * 0.45f, center = Offset(w * 0.5f, h * 0.5f))

        // Sheet of paper lines
        drawLine(colors.primary, Offset(w * 0.3f, h * 0.38f), Offset(w * 0.7f, h * 0.38f), strokeWidth = 3f)
        drawLine(colors.primary.copy(alpha = 0.7f), Offset(w * 0.3f, h * 0.50f), Offset(w * 0.65f, h * 0.50f), strokeWidth = 3f)
        drawLine(colors.primary.copy(alpha = 0.5f), Offset(w * 0.3f, h * 0.62f), Offset(w * 0.55f, h * 0.62f), strokeWidth = 3f)
    }
}
