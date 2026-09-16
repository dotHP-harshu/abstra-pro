package dev.abstra.pro.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import dev.abstra.pro.ui.util.detectVerticalSwipes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.data.model.PlayerDesignStyle
import dev.abstra.pro.data.model.RepeatMode
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.ui.components.*
import dev.abstra.pro.ui.theme.LocalAbstraColors
import dev.abstra.pro.ui.viewmodel.FullPlayerBackground
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerScreen(
    currentSong: Song?,
    pendingSong: Song?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    repeatMode: RepeatMode,
    isShuffle: Boolean,
    sleepTimerRemainingSec: Int,
    designStyle: PlayerDesignStyle,
    backgroundStyle: FullPlayerBackground,
    showVisualizer: Boolean,
    onToggleVisualizer: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onStyleChange: (PlayerDesignStyle) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current
    var dominantColor by remember { mutableStateOf(colors.primary) }

    // Horizontal swipe gesture across player to skip tracks (Lune UX)
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = androidx.compose.animation.core.LinearEasing)
        )
    )

    val bgModifier = when (backgroundStyle) {
        FullPlayerBackground.GRADIENT -> Modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    dominantColor.copy(alpha = 0.28f),
                    colors.bgBackground.copy(alpha = 0.95f),
                    colors.bgBackground
                )
            )
        )
        FullPlayerBackground.SOLID -> Modifier.background(colors.bgBackground)
    }

    val showLoadingShimmer = currentSong != null && !isPlaying && pendingSong != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(bgModifier)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragAccumulator > 120f) {
                            onPrevious()
                        } else if (dragAccumulator < -120f) {
                            onNext()
                        }
                        dragAccumulator = 0f
                    },
                    onDragCancel = { dragAccumulator = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalSwipes(
                    thresholdPx = 100.dp.toPx(),
                    onSwipeUp = { /* no-op when already expanded */ },
                    onSwipeDown = { onCollapse() }
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ─────────────────────────────────────────────────────────────
            // 1. TOP APP BAR
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.bgCard.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Sleep Timer Status / Trigger Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (sleepTimerRemainingSec > 0) colors.primary else colors.bgCard.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenSleepTimer() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = if (sleepTimerRemainingSec > 0) Color.White else colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (sleepTimerRemainingSec > 0) {
                                val mins = sleepTimerRemainingSec / 60
                                val secs = sleepTimerRemainingSec % 60
                                String.format("%02d:%02d", mins, secs)
                            } else "Timer",
                            color = if (sleepTimerRemainingSec > 0) Color.White else colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Audio Effects (EQ) Shortcut
                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.bgCard.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Audio Equalizer & Effects",
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ─────────────────────────────────────────────────────────────
            // 2. CENTERPIECE SWITCHER (Turntable CD, Polaroid, Cassette, Minimal)
            // ─────────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (designStyle) {
                    PlayerDesignStyle.ROTATING_CD -> {
                        RotatingCdCenterpiece(
                            song = currentSong,
                            isPlaying = isPlaying,
                            rotation = rotationAngle,
                            colors = colors,
                            modifier = Modifier.size(280.dp),
                            showLoadingShimmer = showLoadingShimmer
                        )
                    }
                    PlayerDesignStyle.VINYL_RECORD -> {
                        VinylRecordCenterpiece(
                            song = currentSong,
                            isPlaying = isPlaying,
                            rotation = rotationAngle,
                            colors = colors,
                            modifier = Modifier.size(280.dp),
                            showLoadingShimmer = showLoadingShimmer
                        )
                    }
                    PlayerDesignStyle.POLAROID -> {
                        PolaroidCenterpiece(
                            song = currentSong,
                            isPlaying = isPlaying,
                            colors = colors,
                            modifier = Modifier.size(270.dp, 320.dp),
                            showLoadingShimmer = showLoadingShimmer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─────────────────────────────────────────────────────────────
            // 3. TRACK METADATA & FAVORITE
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    MarqueeText(
                        text = currentSong?.title ?: "No track playing",
                        color = colors.textPrimary,
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentSong?.artist ?: "Unknown Artist"} • ${currentSong?.album ?: "Unknown Album"}",
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Favorite Heart Button
                IconButton(
                    onClick = { currentSong?.let { onToggleFavorite(it) } },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.bgCard.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = if (currentSong?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (currentSong?.isFavorite == true) Color(0xFFFF5252) else colors.textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─────────────────────────────────────────────────────────────
            // 4. WAVEFORM VISUALIZER (Palette-Tinted)
            // ─────────────────────────────────────────────────────────────
            if (showVisualizer) {
                WaveformVisualizer(
                    isPlaying = isPlaying,
                    tintColor = dominantColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clickable { onToggleVisualizer() }
                )
            } else {
                Spacer(modifier = Modifier.fillMaxWidth().height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─────────────────────────────────────────────────────────────
            // 5. PROGRESS SEEKER & TIMESTAMPS
            // ─────────────────────────────────────────────────────────────
            IsolatedPlaybackSlider(
                currentPosition = currentPosition,
                duration = duration,
                colors = colors,
                onSeekTo = onSeekTo
            )

            Spacer(modifier = Modifier.height(6.dp))

            // ─────────────────────────────────────────────────────────────
            // 6. MAIN PLAYBACK CONTROLS
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) colors.primary else colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Aesthetic Play / Pause Center Button
                Surface(
                    onClick = onTogglePlay,
                    shape = CircleShape,
                    color = colors.primary,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(12.dp, CircleShape, spotColor = colors.primary.copy(alpha = 0.5f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Repeat Mode
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.REPEAT_ONE -> Icons.Default.RepeatOne
                            RepeatMode.REPEAT_ALL -> Icons.Default.Repeat
                            RepeatMode.REPEAT_OFF -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat Mode",
                        tint = if (repeatMode != RepeatMode.REPEAT_OFF) colors.primary else colors.textMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ─────────────────────────────────────────────────────────────
            // 7. BOTTOM QUICK ACTIONS (Lyrics, Queue, Centerpiece Switcher)
            // ─────────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Synced Lyrics Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenLyrics() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Lyrics",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }
                }

                // Centerpiece Visualizer Switcher
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            val nextStyle = when (designStyle) {
                                PlayerDesignStyle.ROTATING_CD -> PlayerDesignStyle.VINYL_RECORD
                                PlayerDesignStyle.VINYL_RECORD -> PlayerDesignStyle.POLAROID
                                PlayerDesignStyle.POLAROID -> PlayerDesignStyle.ROTATING_CD
                            }
                            onStyleChange(nextStyle)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = designStyle.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }
                }

                // Queue Drawer Button
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenQueue() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Queue",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun IsolatedPlaybackSlider(
    currentPosition: Long,
    duration: Long,
    colors: dev.abstra.pro.ui.theme.AbstraColors,
    onSeekTo: (Long) -> Unit
) {
    var userSeekingPosition by remember { mutableStateOf<Long?>(null) }
    val effectivePosition = userSeekingPosition ?: currentPosition

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = effectivePosition.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
            onValueChange = { pos ->
                userSeekingPosition = pos.toLong()
            },
            onValueChangeFinished = {
                userSeekingPosition?.let { onSeekTo(it) }
                userSeekingPosition = null
            },
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.border
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(effectivePosition),
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(duration),
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
