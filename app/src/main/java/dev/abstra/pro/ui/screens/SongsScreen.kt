package dev.abstra.pro.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.data.model.SongSortOption
import dev.abstra.pro.ui.components.EmptyLibraryIllustration
import dev.abstra.pro.ui.components.SongListItem
import dev.abstra.pro.ui.theme.LocalAbstraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onEditMetadata: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit,
    onRescanLibrary: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SongSortOption.TITLE_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Multi-selection state
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateListOf<String>() }

    // Bottom sheet state
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }

    // Filter & Sort
    val filteredSongs = remember(songs, searchQuery, sortOption) {
        val filtered = if (searchQuery.isBlank()) {
            songs
        } else {
            val q = searchQuery.trim().lowercase()
            songs.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
            }
        }

        when (sortOption) {
            SongSortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            SongSortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            SongSortOption.ARTIST_ASC -> filtered.sortedBy { it.artist.lowercase() }
            SongSortOption.ARTIST_DESC -> filtered.sortedByDescending { it.artist.lowercase() }
            SongSortOption.ALBUM_ASC -> filtered.sortedBy { it.album.lowercase() }
            SongSortOption.ALBUM_DESC -> filtered.sortedByDescending { it.album.lowercase() }
            SongSortOption.DATE_ADDED_DESC -> filtered.sortedByDescending { it.dateAdded }
            SongSortOption.DATE_ADDED_ASC -> filtered.sortedBy { it.dateAdded }
            SongSortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            SongSortOption.DURATION_ASC -> filtered.sortedBy { it.durationMs }
            SongSortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.dateAdded }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBackground)
    ) {
        // ─────────────────────────────────────────────────────────────
        // 1. TOP SEARCH & CONTROLS HEADER
        // ─────────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.bgBackground,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search Input Field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(colors.bgSurface, RoundedCornerShape(24.dp))
                        .border(1.2.dp, colors.border, RoundedCornerShape(24.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search in ${songs.size} tracks...",
                                    color = colors.textMuted,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Count, Sort, Shuffle All, Selection Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredSongs.size} songs",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sort Button
                        Box {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.bgCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { showSortMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = "Sort",
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = sortOption.displayName,
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(colors.bgSurface)
                            ) {
                                SongSortOption.values().forEach { opt ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = opt.displayName,
                                                color = if (opt == sortOption) colors.primary else colors.textPrimary,
                                                fontWeight = if (opt == sortOption) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            sortOption = opt
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Shuffle All Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (filteredSongs.isNotEmpty()) {
                                        onShuffleAll(filteredSongs)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Shuffle",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 2. MULTI-SELECTION FLOATING BAR (If active)
        // ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = colors.primary.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedSongIds.size} selected",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            selectedSongIds.clear()
                            selectedSongIds.addAll(filteredSongs.map { it.id })
                        }) {
                            Text("Select All", color = colors.primary, fontSize = 12.sp)
                        }
                        TextButton(onClick = {
                            isSelectionMode = false
                            selectedSongIds.clear()
                        }) {
                            Text("Cancel", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 3. SONGS LIST OR EMPTY STATE
        // ─────────────────────────────────────────────────────────────
        if (filteredSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmptyLibraryIllustration()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No songs match \"$searchQuery\"" else "No local music found.\nTap below to rescan your storage.",
                        color = colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRescanLibrary) {
                            Text("Rescan Library")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 130.dp)
            ) {
                itemsIndexed(
                    items = filteredSongs,
                    key = { _, song -> song.id },
                    contentType = { _, _ -> "song" }
                ) { _, song ->
                    val isCurrent = currentSong?.id == song.id
                    val isSelected = selectedSongIds.contains(song.id)

                    SongListItem(
                        song = song,
                        isPlaying = isPlaying && isCurrent,
                        isSelectionMode = isSelectionMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                if (isSelected) selectedSongIds.remove(song.id)
                                else selectedSongIds.add(song.id)
                            } else {
                                onSongClick(song)
                            }
                        },
                        onSelectToggle = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedSongIds.add(song.id)
                            }
                        },
                        onFavoriteToggle = { onToggleFavorite(song) },
                        onOptionsClick = {
                            selectedSongForOptions = song
                        },
                        onSwipeNext = { onPlayNext(song) }
                    )
                }
            }
        }
        
        selectedSongForOptions?.let { song ->
            dev.abstra.pro.ui.components.SongOptionsBottomSheet(
                song = song,
                onDismissRequest = { selectedSongForOptions = null },
                onPlayNext = {
                    onPlayNext(song)
                    selectedSongForOptions = null
                },
                onAddToQueue = {
                    onAddToQueue(song)
                    selectedSongForOptions = null
                },
                onAddToPlaylist = {
                    onAddToPlaylist(song)
                    selectedSongForOptions = null
                },
                onEditMetadata = {
                    onEditMetadata(song)
                    selectedSongForOptions = null
                },
                onDeleteSong = {
                    onDeleteSong(song)
                    selectedSongForOptions = null
                },
                onToggleFavorite = {
                    onToggleFavorite(song)
                    selectedSongForOptions = null
                }
            )
        }
    }
}
