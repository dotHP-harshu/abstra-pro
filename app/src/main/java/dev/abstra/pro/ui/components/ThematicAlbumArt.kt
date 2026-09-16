package dev.abstra.pro.ui.components

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.palette.graphics.Palette
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.ui.theme.LocalAbstraColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class PaletteColors(
    val dominant: Int,
    val vibrant: Int,
    val lightVibrant: Int,
    val darkVibrant: Int,
    val lightMuted: Int,
    val muted: Int
)

object EmbeddedArtCache {
    private const val MAX_ENTRIES = 256
    val decodeSemaphore = Semaphore(1)
    private val cache = object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
            size > MAX_ENTRIES
    }

    fun get(key: String): ImageBitmap? = synchronized(this) { cache[key] }
    fun put(key: String, bitmap: ImageBitmap) {
        synchronized(this) { cache[key] = bitmap }
    }
    fun clear() = synchronized(this) { cache.clear() }
}

@Composable
fun ThematicAlbumArt(
    trackId: String,
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    uriString: String = "",
    cornerRadius: Dp = 12.dp,
    showDetailsOnLarge: Boolean = true,
    onPaletteReady: ((PaletteColors) -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = LocalAbstraColors.current
    val isDemoTrack = trackId.startsWith("demo_")
    val cacheKey = if (uriString.isNotBlank() && !isDemoTrack) uriString else null
    var embeddedBitmap by remember(trackId, uriString) { mutableStateOf(EmbeddedArtCache.get(cacheKey ?: "")) }

    LaunchedEffect(trackId, uriString) {
        val key = cacheKey
        if (key != null && embeddedBitmap == null) {
            var retriever: MediaMetadataRetriever? = null
            var loaded: ImageBitmap? = null
            withContext(Dispatchers.IO) {
                EmbeddedArtCache.decodeSemaphore.acquire()
                try {
                    retriever = MediaMetadataRetriever()
                    retriever?.setDataSource(context, android.net.Uri.parse(uriString))
                    val artBytes = retriever?.embeddedPicture
                    if (artBytes != null && artBytes.isNotEmpty()) {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, opts)
                        if (bitmap != null) {
                            val scaled = if (bitmap.width > 300 || bitmap.height > 300) {
                                android.graphics.Bitmap.createScaledBitmap(bitmap, 300, 300, true)
                            } else bitmap
                            val imageBitmap = scaled.asImageBitmap()
                            EmbeddedArtCache.put(key, imageBitmap)
                            loaded = imageBitmap
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    try { retriever?.release() } catch (_: Exception) {}
                    EmbeddedArtCache.decodeSemaphore.release()
                }
            }
            if (loaded != null) embeddedBitmap = loaded
        }
    }

    var paletteColors by remember(trackId) { mutableStateOf<PaletteColors?>(null) }

    LaunchedEffect(embeddedBitmap) {
        embeddedBitmap?.let { bmp ->
            withContext(Dispatchers.Default) {
                try {
                    val androidBmp = bmp.asAndroidBitmap()
                    val palette = Palette.from(androidBmp).generate()
                    val p = PaletteColors(
                        dominant = palette.getDominantColor(0xFFF06FA0.toInt()),
                        vibrant = palette.getVibrantColor(0xFFE14E86.toInt()),
                        lightVibrant = palette.getLightVibrantColor(0xFFFFF0F5.toInt()),
                        darkVibrant = palette.getDarkVibrantColor(0xFF702444.toInt()),
                        lightMuted = palette.getLightMutedColor(0xFFFAD2E1.toInt()),
                        muted = palette.getMutedColor(0xFFB0567E.toInt())
                    )
                    paletteColors = p
                    onPaletteReady?.invoke(p)
                } catch (_: Exception) {}
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        val sizeDp = minOf(maxWidth, maxHeight)

        if (embeddedBitmap != null) {
            Image(
                bitmap = embeddedBitmap!!,
                contentDescription = "$title album art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Deterministic 3-color pastel gradient based on hash of track ID
            val seed = abs(trackId.hashCode())
            val c1 = remember(trackId) {
                when (seed % 6) {
                    0 -> Color(0xFFF06FA0)
                    1 -> Color(0xFFFD9692)
                    2 -> Color(0xFFF9A1B2)
                    3 -> Color(0xFF8D517B)
                    4 -> Color(0xFFB9706A)
                    else -> Color(0xFFE5819E)
                }
            }
            val c2 = remember(trackId) {
                when ((seed / 6) % 6) {
                    0 -> Color(0xFFFFF0F5)
                    1 -> Color(0xFFFDECE8)
                    2 -> Color(0xFFFEDFDD)
                    3 -> Color(0xFFCCB0E9)
                    4 -> Color(0xFFFBDDC6)
                    else -> Color(0xFFD7BFF3)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(c1, c2, colors.bgSurface))),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f),
                        radius = w * 0.45f,
                        center = Offset(w * 0.8f, h * 0.2f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = w * 0.35f,
                        center = Offset(w * 0.2f, h * 0.8f)
                    )
                }

                if (sizeDp > 70.dp && showDetailsOnLarge) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size((sizeDp * 0.28f).coerceIn(16.dp, 48.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title.take(12),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size((sizeDp * 0.45f).coerceIn(14.dp, 36.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun ThumbnailOnly(
    trackId: String,
    title: String,
    artist: String,
    uriString: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val colors = LocalAbstraColors.current
    val cached = remember(uriString) { EmbeddedArtCache.get(if (uriString.isNotBlank() && !trackId.startsWith("demo_")) uriString else "") }

    Box(
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        if (cached != null) {
            Image(
                bitmap = cached,
                contentDescription = "$title album art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val seed = abs(trackId.hashCode())
            val c1 = remember(trackId) {
                when (seed % 6) {
                    0 -> Color(0xFFF06FA0)
                    1 -> Color(0xFFFD9692)
                    2 -> Color(0xFFF9A1B2)
                    3 -> Color(0xFF8D517B)
                    4 -> Color(0xFFB9706A)
                    else -> Color(0xFFE5819E)
                }
            }
            val c2 = remember(trackId) {
                when ((seed / 6) % 6) {
                    0 -> Color(0xFFFFF0F5)
                    1 -> Color(0xFFFDECE8)
                    2 -> Color(0xFFFEDFDD)
                    3 -> Color(0xFFCCB0E9)
                    4 -> Color(0xFFFBDDC6)
                    else -> Color(0xFFD7BFF3)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(c1, c2, colors.bgSurface))),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f),
                        radius = w * 0.45f,
                        center = Offset(w * 0.8f, h * 0.2f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = w * 0.35f,
                        center = Offset(w * 0.2f, h * 0.8f)
                    )
                }
                
                BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val sizeDp = minOf(maxWidth, maxHeight)
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size((sizeDp * 0.45f).coerceIn(14.dp, 36.dp))
                    )
                }
            }
        }
    }
}
