package dev.abstra.pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.testTag
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

// ─────────────────────────────────────────────────────────────
// Vinyl Record centerpiece decorations (restored from the
// original Abstra "04. Vinyl Record" preset)
// ─────────────────────────────────────────────────────────────

/**
 * Clean 4-point sparkle vector star illustration
 */
@Composable
fun SparkleStarIcon(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    color: Color = Color(0xFFF06FA0)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w * 0.45f

        val path = Path().apply {
            moveTo(cx, cy - radius)
            cubicTo(cx, cy, cx, cy, cx + radius, cy)
            cubicTo(cx, cy, cx, cy, cx, cy + radius)
            cubicTo(cx, cy, cx, cy, cx - radius, cy)
            cubicTo(cx, cy, cx, cy, cx, cy - radius)
            close()
        }
        drawPath(path, color)
    }
}

/**
 * Glowing floating heart gem badge
 */
@Composable
fun FloatingHeartBadge(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    fillColor: Color = Color(0xFFFFB3D1),
    borderColor: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val heartPath = Path().apply {
            val ox = w * 0.08f
            val oy = h * 0.08f
            val sw = w * 0.84f
            val sh = h * 0.84f
            moveTo(ox + sw * 0.5f, oy + sh * 0.85f)
            cubicTo(ox + sw * 0.1f, oy + sh * 0.55f, ox, oy + sh * 0.25f, ox + sw * 0.25f, oy + sh * 0.12f)
            cubicTo(ox + sw * 0.42f, oy + sh * 0.05f, ox + sw * 0.5f, oy + sh * 0.3f, ox + sw * 0.5f, oy + sh * 0.3f)
            cubicTo(ox + sw * 0.5f, oy + sh * 0.3f, ox + sw * 0.58f, oy + sh * 0.05f, ox + sw * 0.75f, oy + sh * 0.12f)
            cubicTo(ox + sw * 1.0f, oy + sh * 0.25f, ox + sw * 0.9f, oy + sh * 0.55f, ox + sw * 0.5f, oy + sh * 0.85f)
            close()
        }

        drawPath(heartPath, fillColor)
        drawPath(heartPath, borderColor, style = Stroke(width = 3f))

        // Glossy highlight
        drawCircle(
            color = Color(0x99FFFFFF),
            radius = w * 0.12f,
            center = Offset(w * 0.38f, h * 0.35f)
        )
    }
}

/**
 * Cute plush teddy bear with blush cheeks and round ears
 */
@Composable
fun MascotBearIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Left ear
        drawCircle(
            color = Color(0xFFF7B0CD),
            radius = w * 0.16f,
            center = Offset(w * 0.28f, h * 0.28f)
        )
        drawCircle(
            color = Color(0xFFFF85B3),
            radius = w * 0.09f,
            center = Offset(w * 0.28f, h * 0.28f)
        )

        // Right ear
        drawCircle(
            color = Color(0xFFF7B0CD),
            radius = w * 0.16f,
            center = Offset(w * 0.72f, h * 0.28f)
        )
        drawCircle(
            color = Color(0xFFFF85B3),
            radius = w * 0.09f,
            center = Offset(w * 0.72f, h * 0.28f)
        )

        // Head
        drawCircle(
            color = Color(0xFFFFC5DC),
            radius = w * 0.38f,
            center = Offset(w * 0.5f, h * 0.52f)
        )

        // Snout
        drawOval(
            color = Color(0xFFFFFFFF),
            topLeft = Offset(w * 0.36f, h * 0.48f),
            size = Size(w * 0.28f, h * 0.22f)
        )

        // Cute Nose
        drawOval(
            color = Color(0xFF6B223D),
            topLeft = Offset(w * 0.44f, h * 0.52f),
            size = Size(w * 0.12f, h * 0.08f)
        )

        // Eyes
        drawCircle(
            color = Color(0xFF6B223D),
            radius = 3.5f,
            center = Offset(w * 0.36f, h * 0.44f)
        )
        drawCircle(
            color = Color(0xFF6B223D),
            radius = 3.5f,
            center = Offset(w * 0.64f, h * 0.44f)
        )

        // Eye sparkles
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = 1.2f,
            center = Offset(w * 0.35f, h * 0.43f)
        )
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = 1.2f,
            center = Offset(w * 0.63f, h * 0.43f)
        )

        // Blush cheeks
        drawCircle(
            color = Color(0x66FF4D7E),
            radius = 5f,
            center = Offset(w * 0.27f, h * 0.56f)
        )
        drawCircle(
            color = Color(0x66FF4D7E),
            radius = 5f,
            center = Offset(w * 0.73f, h * 0.56f)
        )
    }
}

/**
 * Metallic turntable tonearm with stylus headshell
 */
@Composable
fun TurntableArmIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Top-right base pivot
        drawCircle(
            color = Color(0xFFE5C8D4),
            radius = w * 0.12f,
            center = Offset(w * 0.85f, h * 0.18f)
        )
        drawCircle(
            color = Color(0xFFF06FA0),
            radius = w * 0.07f,
            center = Offset(w * 0.85f, h * 0.18f)
        )

        // Arm rod
        val armPath = Path().apply {
            moveTo(w * 0.85f, h * 0.18f)
            lineTo(w * 0.58f, h * 0.55f)
            lineTo(w * 0.48f, h * 0.82f)
        }
        drawPath(armPath, Color(0xFFE8D0DC), style = Stroke(width = 4.5f, cap = StrokeCap.Round))

        // Stylus cartridge headshell
        val headshell = Path().apply {
            moveTo(w * 0.48f, h * 0.82f)
            lineTo(w * 0.38f, h * 0.95f)
            lineTo(w * 0.30f, h * 0.90f)
            lineTo(w * 0.42f, h * 0.78f)
            close()
        }
        drawPath(headshell, Color(0xFFFF85B3))
        drawPath(headshell, Color(0xFFE14E86), style = Stroke(width = 1.5f))
    }
}

@Composable
fun HeartIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = Color(0xFFFF4D7E),
    withBorder: Boolean = false,
    borderColor: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.85f)
            cubicTo(w * 0.1f, h * 0.55f, 0f, h * 0.25f, w * 0.25f, h * 0.12f)
            cubicTo(w * 0.42f, h * 0.05f, w * 0.5f, h * 0.3f, w * 0.5f, h * 0.3f)
            cubicTo(w * 0.5f, h * 0.3f, w * 0.58f, h * 0.05f, w * 0.75f, h * 0.12f)
            cubicTo(w * 1.0f, h * 0.25f, w * 0.9f, h * 0.55f, w * 0.5f, h * 0.85f)
            close()
        }
        drawPath(path, color)
        if (withBorder) {
            drawPath(path, borderColor, style = Stroke(width = 2.5f))
        }
    }
}

@Composable
fun PaperclipIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    color: Color = Color(0xFFC090A5)
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.12f

        val path = Path().apply {
            moveTo(w * 0.32f, h * 0.85f)
            lineTo(w * 0.32f, h * 0.25f)
            cubicTo(w * 0.32f, h * 0.08f, w * 0.72f, h * 0.08f, w * 0.72f, h * 0.25f)
            lineTo(w * 0.72f, h * 0.75f)
            cubicTo(w * 0.72f, h * 0.92f, w * 0.48f, h * 0.92f, w * 0.48f, h * 0.75f)
            lineTo(w * 0.48f, h * 0.38f)
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
fun WashiTapeSticker(
    modifier: Modifier = Modifier,
    width: Dp = 54.dp,
    height: Dp = 22.dp,
    color: Color = Color(0xBBFF99C2)
) {
    Canvas(modifier = modifier.size(width, height)) {
        val w = this.size.width
        val h = this.size.height

        val path = Path().apply {
            moveTo(0f, h * 0.2f)
            lineTo(w * 0.08f, 0f)
            lineTo(w * 0.92f, 0f)
            lineTo(w, h * 0.2f)
            lineTo(w * 0.96f, h)
            lineTo(w * 0.04f, h)
            close()
        }
        drawPath(path, color)
        for (i in 1..4) {
            drawLine(
                color = Color(0x33FFFFFF),
                start = Offset(w * (i * 0.2f), 0f),
                end = Offset(w * (i * 0.2f + 0.1f), h),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun CassetteTapeSticker(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 82.dp, height = 50.dp).testTag("cassette_sticker")) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            color = Color(0xFFFFFFFF),
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            cornerRadius = CornerRadius(14f, 14f)
        )

        drawRoundRect(
            color = Color(0xFFFF85B3),
            topLeft = Offset(3f, 3f),
            size = Size(w - 6f, h - 6f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        drawRoundRect(
            color = Color(0xFFFFF0F5),
            topLeft = Offset(8f, 8f),
            size = Size(w - 16f, h * 0.44f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        drawCircle(
            color = Color(0xFFE14E86),
            radius = 2.5f,
            center = Offset(20f, 15f)
        )

        drawRoundRect(
            color = Color(0xFF4A1528),
            topLeft = Offset(16f, h * 0.56f),
            size = Size(w - 32f, h * 0.32f),
            cornerRadius = CornerRadius(5f, 5f)
        )

        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = 6f,
            center = Offset(26f, h * 0.72f)
        )
        drawCircle(
            color = Color(0xFF4A1528),
            radius = 3f,
            center = Offset(26f, h * 0.72f)
        )

        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = 6f,
            center = Offset(w - 26f, h * 0.72f)
        )
        drawCircle(
            color = Color(0xFF4A1528),
            radius = 3f,
            center = Offset(w - 26f, h * 0.72f)
        )
    }
}
