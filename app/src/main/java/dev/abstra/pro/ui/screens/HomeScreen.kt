package dev.abstra.pro.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abstra.pro.data.prefs.AppPreferences
import dev.abstra.pro.ui.theme.GreetingStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.ai.model.AiMix
import dev.abstra.pro.data.model.Artist
import dev.abstra.pro.data.model.Playlist
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.ui.components.ThematicAlbumArt
import dev.abstra.pro.ui.theme.LocalAbstraColors
import dev.abstra.pro.ui.theme.AbstraColors
import java.util.Calendar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Favorite

@Composable
fun HomeScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    recentlyPlayed: List<Song>,
    recentlyAdded: List<Song>,
    mostPlayed: List<Song>,
    playlists: List<Playlist>,
    aiMixes: List<AiMix>,
    topArtists: List<Artist>,
    topGenres: List<Pair<String, Int>>,
    totalSongs: Int,
    favoriteCount: Int,
    lifetimeListeningMs: Long,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onMixClick: (AiMix) -> Unit,
    onTogglePlay: () -> Unit,
    onExpandPlayer: () -> Unit,
    userName: String? = null,
    greetingEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // ─────────────────────────────────────────────────────────────
        // 0. GREETING HERO CARD
        // ─────────────────────────────────────────────────────────────
        item {
            GreetingHeroCard(
                userName = userName,
                greetingEnabled = greetingEnabled,
                dailyListeningTimeStr = formatListeningTime(lifetimeListeningMs),
                totalSongs = totalSongs,
                favoriteCount = favoriteCount,
                colors = colors
            )
        }

        // ─────────────────────────────────────────────────────────────
                // ─────────────────────────────────────────────────────────────
        // 1. RECENTLY PLAYED
        // ─────────────────────────────────────────────────────────────
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "RECENTLY PLAYED",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textHeading,
                        fontWeight = FontWeight.ExtraBold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(recentlyPlayed.take(12), key = { it.id }) { song ->
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { onSongClick(song, recentlyPlayed) }
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
                        items(recentlyAdded.take(12), key = { it.id }) { song ->
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { onSongClick(song, recentlyAdded) }
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
        // 3.5 MOST PLAYED
        // ─────────────────────────────────────────────────────────────
        if (mostPlayed.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "MOST PLAYED",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textHeading,
                        fontWeight = FontWeight.ExtraBold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(mostPlayed, key = { "mp_${it.id}" }) { song ->
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { onSongClick(song, mostPlayed) }
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
                        items(aiMixes, key = { it.title }) { mix ->
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
                                        .background(remember(mix.gradientColors) { Brush.linearGradient(mix.gradientColors) })
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
                        items(topArtists.take(10), key = { it.name }) { artist ->
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

@Composable
fun GreetingHeroCard(
    userName: String?,
    greetingEnabled: Boolean,
    dailyListeningTimeStr: String,
    totalSongs: Int,
    favoriteCount: Int,
    colors: AbstraColors = LocalAbstraColors.current,
) {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 0..5 -> "Good night"
        in 6..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    val style by AppPreferences.greetingStyleFlow.collectAsStateWithLifecycle(
        initialValue = GreetingStyle.DEFAULT
    )

    val greetingGradient = remember(colors.primary, colors.bgSurface) {
        Brush.horizontalGradient(
            listOf(colors.primary.copy(alpha = 0.25f), Color.Transparent)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.bgSurface,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = greetingGradient)
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (greetingEnabled) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = colors.textHeading,
                    )
                    if (!userName.isNullOrBlank()) {
                        NameBlock(
                            name = userName,
                            style = style,
                            primary = colors.primary,
                            textHeading = colors.textHeading,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(Icons.Default.History, dailyListeningTimeStr, colors)
                    StatChip(Icons.Default.MusicNote, totalSongs.toString(), colors)
                    StatChip(Icons.Default.Favorite, favoriteCount.toString(), colors)
                }
            }
        }
    }
}

@Composable
private fun NameBlock(
    name: String,
    style: GreetingStyle,
    primary: Color,
    textHeading: Color,
) {
    when (style) {
        GreetingStyle.CLASSIC -> ClassicName(name, primary)
        GreetingStyle.MINIMAL -> MinimalName(name, primary)
        GreetingStyle.BOLD -> BoldName(name, primary)
        GreetingStyle.GRADIENT -> GradientName(name, primary)
        GreetingStyle.ELEGANT -> ElegantName(name, primary)
    }
}

@Composable
private fun ClassicName(name: String, primary: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Text(
            text = "♡",
            fontSize = 18.sp,
            color = primary.copy(alpha = 0.85f),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = name,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            fontStyle = FontStyle.Italic,
            color = primary,
            maxLines = 2,
            modifier = Modifier.drawBehind {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.0f),
                            primary.copy(alpha = 0.35f),
                            primary.copy(alpha = 0.0f)
                        )
                    ),
                    topLeft = Offset(0f, size.height - 2.dp.toPx()),
                    size = Size(size.width, 2.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        )
        Text(
            text = "✦",
            fontSize = 14.sp,
            color = primary.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun MinimalName(name: String, primary: Color) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Medium,
        ),
        color = primary,
        maxLines = 2,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun BoldName(name: String, primary: Color) {
    Text(
        text = name.uppercase(),
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
        ),
        color = primary,
        maxLines = 2,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun GradientName(name: String, primary: Color) {
    val brush = remember(primary) {
        Brush.linearGradient(
            colors = listOf(
                primary,
                primary.copy(alpha = 0.6f),
                primary,
            )
        )
    }
    Text(
        text = name,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            letterSpacing = 0.5.sp,
        ),
        color = primary,
        maxLines = 2,
        modifier = Modifier
            .padding(top = 4.dp)
            .background(Color.Transparent)
    )
}

@Composable
private fun ElegantName(name: String, primary: Color) {
    Text(
        text = name,
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Normal,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            letterSpacing = 4.sp,
        ),
        color = primary,
        maxLines = 2,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, colors: AbstraColors) {
    Surface(
        color = colors.bgCard.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
            Text(text, color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatListeningTime(ms: Long): String {
    val totalMinutes = ms / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        totalMinutes > 0 -> "${totalMinutes}m"
        else -> "—"
    }
}
