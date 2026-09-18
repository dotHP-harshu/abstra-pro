package dev.abstra.pro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.lyrics.model.LyricsLine
import dev.abstra.pro.ui.components.RotatingCdCenterpiece
import dev.abstra.pro.ui.components.ThematicAlbumArt
import dev.abstra.pro.ui.components.PaletteColors
import dev.abstra.pro.ui.theme.LocalAbstraColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun LyricsScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    lyricsLines: List<LyricsLine>,
    timeOffsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    onLineClick: (Long) -> Unit,
    onSelectLrcFile: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current
    val listState = rememberLazyListState()

    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = androidx.compose.animation.core.LinearEasing)
        )
    )
    var keepScreenOn by remember { mutableStateOf(false) }
    var palette by remember { mutableStateOf<PaletteColors?>(null) }

    // Suppress auto-scroll for a few seconds after the user touches/drags the list,
    // so manual scrolling never fights the position-follow animation.
    var manualScrollUntilMs by remember { mutableStateOf(0L) }

    // Find current active line index
    val adjustedPosition = currentPositionMs + timeOffsetMs
    val activeIndex = remember(adjustedPosition, lyricsLines) {
        val index = lyricsLines.indexOfLast { it.startTimeMs <= adjustedPosition }
        if (index >= 0) index else 0
    }

    // Auto-scroll when active line changes
    LaunchedEffect(activeIndex) {
        if (lyricsLines.isNotEmpty() && activeIndex in lyricsLines.indices) {
            if (System.currentTimeMillis() >= manualScrollUntilMs) {
                listState.animateScrollToItem(
                    index = (activeIndex - 2).coerceAtLeast(0)
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = if (palette != null) {
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(palette!!.dominant).copy(alpha = 0.4f),
                            colors.bgBackground.copy(alpha = 0.95f),
                            colors.bgBackground
                        )
                    )
                } else {
                    androidx.compose.ui.graphics.SolidColor(colors.bgBackground)
                }
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ─────────────────────────────────────────────────────────────
        // 1. TOP HEADER & SCREEN ON TOGGLE
        // ─────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.bgCard)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Lyrics",
                    tint = colors.textPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Synced Lyrics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = currentSong?.title ?: "",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 1
                )
                Box(modifier = Modifier.size(0.dp)) {
                    ThematicAlbumArt(
                        trackId = currentSong?.id ?: "",
                        title = currentSong?.title ?: "",
                        artist = currentSong?.artist ?: "",
                        uriString = currentSong?.uriString ?: "",
                        onPaletteReady = { palette = it }
                    )
                }
            }

            // Keep Screen On Chip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (keepScreenOn) colors.primary else colors.bgCard,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { keepScreenOn = !keepScreenOn }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = if (keepScreenOn) Color.White else colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Stay On",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (keepScreenOn) Color.White else colors.textPrimary
                    )
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 2. TIMING OFFSET TUNER (-/+ MS)
        // ─────────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = colors.bgCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sync Offset: ${if (timeOffsetMs >= 0) "+$timeOffsetMs" else "$timeOffsetMs"} ms",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { onOffsetChange(timeOffsetMs - 500) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("-0.5s", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { onOffsetChange(timeOffsetMs + 500) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+0.5s", fontSize = 11.sp)
                    }
                    if (timeOffsetMs != 0L) {
                        IconButton(
                            onClick = { onOffsetChange(0L) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, "Reset Offset", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ─────────────────────────────────────────────────────────────
        // 3. LYRICS LIST OR NO LYRICS PLACEHOLDER
        // ─────────────────────────────────────────────────────────────
        if (lyricsLines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RotatingCdCenterpiece(
                        song = currentSong,
                        isPlaying = isPlaying,
                        rotation = rotationAngle,
                        colors = colors,
                        modifier = Modifier.size(200.dp)
                    )
                    Text(
                        text = "No lyrics found for this track",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "You can place a matching .lrc file alongside the track or pick one manually.",
                        fontSize = 13.sp,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onSelectLrcFile,
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select .LRC File", fontSize = 13.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Any touch on the list pauses auto-scroll for 4 seconds,
                        // so user scrolling is never hijacked by the position tracker.
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            manualScrollUntilMs = System.currentTimeMillis() + 4000
                        }
                    },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 40.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(lyricsLines) { index, line ->
                    val isActive = index == activeIndex
                    val isPast = index < activeIndex

                    val textColor = when {
                        isActive -> colors.primary
                        isPast -> colors.textPrimary.copy(alpha = 0.5f)
                        else -> colors.textMuted
                    }

                    val fontSize = if (isActive) 22.sp else 17.sp
                    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLineClick(line.startTimeMs) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val displayText = if (line.words.isNotEmpty() && isActive) {
                            androidx.compose.ui.text.buildAnnotatedString {
                                line.words.forEach { word ->
                                    if (adjustedPosition >= word.timeMs) {
                                        withStyle(androidx.compose.ui.text.SpanStyle(color = colors.primary)) {
                                            append(word.text)
                                        }
                                    } else {
                                        withStyle(androidx.compose.ui.text.SpanStyle(color = colors.textPrimary.copy(alpha = 0.5f))) {
                                            append(word.text)
                                        }
                                    }
                                }
                            }
                        } else {
                            androidx.compose.ui.text.buildAnnotatedString {
                                withStyle(androidx.compose.ui.text.SpanStyle(color = textColor)) {
                                    append(line.content)
                                }
                            }
                        }

                        Text(
                            text = displayText,
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp
                        )
                    }
                }
            }
        }
    }
}
