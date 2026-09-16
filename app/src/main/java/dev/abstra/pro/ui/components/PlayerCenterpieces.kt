package dev.abstra.pro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.ui.theme.AbstraColors
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.GenericShape

// ==========================================
// 01. ROTATING CD / VINYL CENTERPIECE
// ==========================================
@Composable
fun RotatingCdCenterpiece(
    song: Song?,
    isPlaying: Boolean,
    rotation: Float,
    colors: AbstraColors,
    modifier: Modifier = Modifier,
    showLoadingShimmer: Boolean = false
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Rotating Vinyl / CD disc (GPU-accelerated via graphicsLayer)
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .graphicsLayer {
                    rotationZ = if (isPlaying) rotation else 0f
                }
                .shadow(16.dp, shape = CircleShape, spotColor = colors.primary.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFD6E7),
                            Color(0xFFFFBBD7),
                            Color(0xFFFFE0ED),
                            Color(0xFFF799C0),
                            Color(0xFFFFD6E7)
                        )
                    )
                )
                .border(3.5.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Metallic / Holographic iridescent sheen & concentric grooves
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f

                // Concentric data grooves
                drawCircle(Color(0x33FFFFFF), radius = w * 0.42f, center = Offset(cx, cy), style = Stroke(width = 1.5f))
                drawCircle(Color(0x28E14E86), radius = w * 0.38f, center = Offset(cx, cy), style = Stroke(width = 1f))
                drawCircle(Color(0x33FFFFFF), radius = w * 0.34f, center = Offset(cx, cy), style = Stroke(width = 1.5f))

                // Rainbow iridescent sheen triangle fans
                val sheenPath1 = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx + w * 0.48f, cy - h * 0.25f)
                    lineTo(cx + w * 0.35f, cy - h * 0.45f)
                    close()
                }
                drawPath(sheenPath1, Color(0x44FFFFFF))

                val sheenPath2 = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx - w * 0.48f, cy + h * 0.25f)
                    lineTo(cx - w * 0.35f, cy + h * 0.45f)
                    close()
                }
                drawPath(sheenPath2, Color(0x44FFFFFF))
            }

            // Outer transparent hub ring
            Box(
                modifier = Modifier
                    .fillMaxSize(0.62f)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .border(2.dp, Color(0x66FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Center Album Artwork Label
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.72f)
                        .clip(CircleShape)
                        .border(2.5.dp, colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    ThematicAlbumArt(
                        trackId = song?.id ?: "demo",
                        title = song?.title ?: "daydreams",
                        artist = song?.artist ?: "lovelyy",
                        uriString = song?.uriString ?: "",
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 999.dp,
                        showDetailsOnLarge = false,
                        showLoadingShimmer = showLoadingShimmer
                    )

                    // Center Spindle Hole
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF0F5))
                            .border(2.dp, Color(0xFFE14E86), CircleShape)
                    )
                }
            }
        }

        // Decorative Bow on top-left edge
        BowRibbonIcon(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 6.dp, y = 6.dp)
                .size(44.dp)
        )

        // Cute Cassette Tape Sticker on bottom-right edge
        TapeStickerBadge(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp, y = 6.dp)
        )
    }
}

// ==========================================
// 01b. VINYL RECORD CENTERPIECE
// (Restored from the original Abstra "04. Vinyl Record" preset:
//  black LP + tonearm + plush bear + sparkles + floating heart)
// ==========================================
@Composable
fun VinylRecordCenterpiece(
    song: Song?,
    isPlaying: Boolean,
    rotation: Float,
    colors: AbstraColors,
    modifier: Modifier = Modifier,
    showLoadingShimmer: Boolean = false
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Floating Music Notes in top-left
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 8.dp, y = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "♪",
                color = colors.primaryStrong,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            SparkleStarIcon(size = 12.dp, color = colors.primary)
        }

        // Spinning Black Vinyl Record LP — GPU-accelerated via graphicsLayer (battery optimized)
        Box(
            modifier = Modifier
                .fillMaxSize(0.90f)
                .graphicsLayer {
                    rotationZ = if (isPlaying) rotation else 0f
                }
                .shadow(16.dp, shape = CircleShape, spotColor = Color(0x88000000))
                .clip(CircleShape)
                .background(Color(0xFF19161E))
                .border(4.dp, Color(0xFF332B3A), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Vinyl Grooves & Sheen
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val r = w / 2f

                // Spiral / Concentric Grooves
                val grooveRadii = listOf(0.92f, 0.86f, 0.80f, 0.74f, 0.68f, 0.62f)
                grooveRadii.forEach { factor ->
                    drawCircle(
                        color = Color(0x3344384E),
                        radius = r * factor,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )
                }

                // Glossy Vinyl Light Reflections (4-point gloss)
                val reflection1 = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx + w * 0.45f, cy - h * 0.2f)
                    lineTo(cx + w * 0.35f, cy - h * 0.4f)
                    close()
                }
                drawPath(reflection1, Color(0x28FFFFFF))

                val reflection2 = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx - w * 0.45f, cy + h * 0.2f)
                    lineTo(cx - w * 0.35f, cy + h * 0.4f)
                    close()
                }
                drawPath(reflection2, Color(0x28FFFFFF))
            }

            // Center Pink Label
            Box(
                modifier = Modifier
                    .fillMaxSize(0.52f)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB3D1))
                    .border(2.5.dp, colors.primaryStrong, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ThematicAlbumArt(
                    trackId = song?.id ?: "demo",
                    title = song?.title ?: "daydreams",
                    artist = song?.artist ?: "lovelyy",
                    uriString = song?.uriString ?: "",
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 999.dp,
                    showDetailsOnLarge = false,
                    showLoadingShimmer = showLoadingShimmer
                )

                // Center Spindle Hole
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF19161E))
                        .border(1.5.dp, Color.White, CircleShape)
                )
            }
        }

        // Turntable Tonearm Stylus in Top-Right
        TurntableArmIllustration(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-4).dp)
                .size(110.dp)
        )

        // Cute Plush Teddy Bear in Bottom-Left
        MascotBearIllustration(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 2.dp, y = 4.dp)
                .size(62.dp)
        )

        // Floating Heart Gem in Bottom-Right
        FloatingHeartBadge(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 6.dp, y = 2.dp),
            size = 32.dp,
            fillColor = Color(0xFFFF85B3)
        )
    }
}

// ==========================================
// 02. POLAROID FRAME CENTERPIECE
// ==========================================
@Composable
fun PolaroidCenterpiece(
    song: Song?,
    isPlaying: Boolean,
    colors: AbstraColors,
    modifier: Modifier = Modifier,
    showLoadingShimmer: Boolean = false
) {
    Box(
        modifier = modifier.fillMaxSize(0.92f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = colors.primary.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    ThematicAlbumArt(
                        trackId = song?.id ?: "demo",
                        title = song?.title ?: "",
                        artist = song?.artist ?: "",
                        uriString = song?.uriString ?: "",
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 8.dp,
                        showLoadingShimmer = showLoadingShimmer
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "♥ ${song?.title?.take(16) ?: "daydreams"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textHeading
                )
            }
        }

        PaperclipIllustration(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 6.dp, y = (-4).dp)
                .rotate(-15f)
        )

        FloatingHeartBadge(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = 2.dp),
            size = 32.dp,
            fillColor = Color(0xFFFF85B3)
        )

        WashiTapeSticker(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 4.dp)
                .rotate(-12f)
        )
    }
}



// ── Subtle Cute Vector Accents ──

@Composable
fun BowRibbonIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pink = Color(0xFFFFD1E3)
        val pinkStrong = Color(0xFFE14E86)

        // Bow wings
        val p1 = Path().apply {
            moveTo(w * 0.5f, h * 0.5f)
            lineTo(w * 0.1f, h * 0.2f)
            lineTo(w * 0.1f, h * 0.8f)
            close()
        }
        drawPath(p1, pink)

        val p2 = Path().apply {
            moveTo(w * 0.5f, h * 0.5f)
            lineTo(w * 0.9f, h * 0.2f)
            lineTo(w * 0.9f, h * 0.8f)
            close()
        }
        drawPath(p2, pink)

        // Center knot
        drawCircle(pinkStrong, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.5f))
    }
}

@Composable
fun TapeStickerBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(6.dp)),
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFFFF0F5)
    ) {
        Text(
            text = "♥ MIX",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE14E86),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}