package dev.abstra.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.ai.model.AiMix
import dev.abstra.pro.data.model.Artist
import dev.abstra.pro.data.model.Playlist
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.ui.components.ThematicAlbumArt
import dev.abstra.pro.ui.theme.LocalAbstraColors

@Composable
fun HomeScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    recentlyAdded: List<Song>,
    playlists: List<Playlist>,
    aiMixes: List<AiMix>,
    topArtists: List<Artist>,
    topGenres: List<Pair<String, Int>>,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onMixClick: (AiMix) -> Unit,
    onTogglePlay: () -> Unit,
    onExpandPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // ─────────────────────────────────────────────────────────────
        // 1. HERO "CONTINUE LISTENING" CARD
        // ─────────────────────────────────────────────────────────────
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .shadow(12.dp, RoundedCornerShape(26.dp), spotColor = colors.primary.copy(alpha = 0.35f))
                    .border(1.5.dp, colors.border, RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { onExpandPlayer() },
                color = colors.bgSurface,
                shape = RoundedCornerShape(26.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Soft ambient gradient bleed
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(colors.primary.copy(alpha = 0.18f), Color.Transparent)
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThematicAlbumArt(
                            trackId = currentSong?.id ?: "demo_daydreams",
                            title = currentSong?.title ?: "daydreams",
                            artist = currentSong?.artist ?: "Abstra",
                            uriString = currentSong?.uriString ?: "",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            cornerRadius = 20.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "CONTINUE LISTENING",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primaryStrong,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentSong?.title ?: "Select a track",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                                color = colors.textHeading,
                                maxLines = 1
                            )
                            Text(
                                text = currentSong?.artist ?: "Abstra Pro",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                                maxLines = 1
                            )
                        }

                        FilledIconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colors.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 2. AI SMART MIXES CAROUSEL
        // ─────────────────────────────────────────────────────────────
        if (aiMixes.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colors.primaryStrong,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI SMART MIXES",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textHeading,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "On-Device",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primaryStrong
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(aiMixes) { mix ->
                            Surface(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(130.dp)
                                    .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = mix.gradientColors.first().copy(alpha = 0.4f))
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { onMixClick(mix) },
                                color = Color.Transparent,
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(mix.gradientColors))
                                        .padding(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = mix.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = mix.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 2
                                        )
                                        Text(
                                            text = "${mix.songs.size} tracks",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.95f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 3. RECENTLY ADDED
        // ─────────────────────────────────────────────────────────────
        if (recentlyAdded.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "RECENTLY ADDED",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textHeading,
                        fontWeight = FontWeight.ExtraBold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(recentlyAdded.take(12)) { song ->
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { onSongClick(song) }
                            ) {
                                ThematicAlbumArt(
                                    trackId = song.id,
                                    title = song.title,
                                    artist = song.artist,
                                    uriString = song.uriString,
                                    modifier = Modifier
                                        .size(110.dp)
                                        .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = colors.primary.copy(alpha = 0.25f))
                                        .clip(RoundedCornerShape(18.dp)),
                                    cornerRadius = 18.dp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textHeading,
                                    maxLines = 1
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 4. TOP ARTISTS
        // ─────────────────────────────────────────────────────────────
        if (topArtists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "TOP ARTISTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textHeading,
                        fontWeight = FontWeight.ExtraBold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(topArtists.take(10)) { artist ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(85.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(75.dp)
                                        .shadow(6.dp, CircleShape, spotColor = colors.primary.copy(alpha = 0.3f))
                                        .clip(CircleShape)
                                        .background(colors.surfaceAlt)
                                        .border(2.dp, colors.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = artist.name.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.primaryStrong,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.textHeading,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${artist.songCount} songs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom spacer to clear miniplayer
        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}
