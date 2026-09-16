package dev.abstra.pro.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.data.model.Playlist
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.ui.components.SongListItem
import dev.abstra.pro.ui.theme.LocalAbstraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlists: List<Playlist>,
    favoriteSongs: List<Song>,
    recentlyAddedSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onPlaylistClick: (Playlist) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onImportPlaylistClick: () -> Unit,
    onExportAllClick: () -> Unit,
    onExportPlaylistClick: (Playlist) -> Unit,
    onDeletePlaylistClick: (Playlist) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current
    var selectedSmartPlaylist by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBackground)
    ) {
        // ─────────────────────────────────────────────────────────────
        // 1. TOP HEADER & SAF BACKUP ACTIONS
        // ─────────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.bgBackground,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playlists",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Import Button
                        FilledTonalIconButton(
                            onClick = onImportPlaylistClick,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.bgCard,
                                contentColor = colors.primary
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Import JSON Backup",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Export All Button
                        FilledTonalIconButton(
                            onClick = onExportAllClick,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = colors.bgCard,
                                contentColor = colors.primary
                            ),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "Export All to JSON",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // New Playlist Button
                        Button(
                            onClick = onCreatePlaylistClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(19.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ─────────────────────────────────────────────────────────────
                // 2. SMART SYSTEM PLAYLISTS (Favorites & Recent)
                // ─────────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Favorites Card
                    SmartPlaylistChip(
                        title = "Favorites",
                        count = favoriteSongs.size,
                        icon = Icons.Default.Favorite,
                        gradientColors = listOf(Color(0xFFFF7597), Color(0xFFFF94B4)),
                        isSelected = selectedSmartPlaylist == "favorites",
                        onClick = {
                            selectedSmartPlaylist = if (selectedSmartPlaylist == "favorites") null else "favorites"
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Recently Added Card
                    SmartPlaylistChip(
                        title = "Recent",
                        count = recentlyAddedSongs.size,
                        icon = Icons.Default.Schedule,
                        gradientColors = listOf(Color(0xFF6C63FF), Color(0xFF8B80F9)),
                        isSelected = selectedSmartPlaylist == "recent",
                        onClick = {
                            selectedSmartPlaylist = if (selectedSmartPlaylist == "recent") null else "recent"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 3. PLAYLIST DETAIL OR PLAYLISTS GRID
        // ─────────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = selectedSmartPlaylist,
            label = "playlistContentAnim"
        ) { targetSmart ->
            when (targetSmart) {
                "favorites" -> {
                    SmartPlaylistDetailView(
                        title = "Favorites",
                        songs = favoriteSongs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = onSongClick,
                        onPlayAll = { onPlayAll(favoriteSongs) },
                        onShuffleAll = { onShuffleAll(favoriteSongs) },
                        onBack = { selectedSmartPlaylist = null }
                    )
                }
                "recent" -> {
                    SmartPlaylistDetailView(
                        title = "Recently Added",
                        songs = recentlyAddedSongs,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        onSongClick = onSongClick,
                        onPlayAll = { onPlayAll(recentlyAddedSongs) },
                        onShuffleAll = { onShuffleAll(recentlyAddedSongs) },
                        onBack = { selectedSmartPlaylist = null }
                    )
                }
                else -> {
                    // Normal User Playlists Grid
                    if (playlists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "No custom playlists yet",
                                    color = colors.textSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Create one or restore from a JSON backup file.",
                                    color = colors.textMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 130.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(playlists, key = { it.id }) { playlist ->
                                PlaylistCard(
                                    playlist = playlist,
                                    onClick = { onPlaylistClick(playlist) },
                                    onExport = { onExportPlaylistClick(playlist) },
                                    onDelete = { onDeletePlaylistClick(playlist) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistChip(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current
    Surface(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, colors.primary, RoundedCornerShape(20.dp))
                else Modifier.border(1.dp, colors.border, RoundedCornerShape(20.dp))
            ),
        shape = RoundedCornerShape(20.dp),
        color = colors.bgCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$count tracks",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .border(1.2.dp, colors.border, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = colors.bgCard
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Artwork placeholder or gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.6f),
                                colors.accent.copy(alpha = 0.4f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${playlist.songCount} songs",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(colors.bgSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export to JSON (SAF)") },
                            leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                            onClick = {
                                showMenu = false
                                onExport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Playlist", color = Color(0xFFFF5252)) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistDetailView(
    title: String,
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalAbstraColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Row(
                modifier = Modifier.wrapContentWidth(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onPlayAll,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.bgCard,
                        contentColor = colors.textPrimary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", fontSize = 12.sp)
                }
                Button(
                    onClick = onShuffleAll,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Shuffle", fontSize = 12.sp)
                }
            }
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No songs in $title yet.", color = colors.textMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 130.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        onClick = { onSongClick(song) },
                        onFavoriteToggle = {}, // TODO: pass actual toggles
                        onOptionsClick = {}
                    )
                }
            }
        }
    }
}
