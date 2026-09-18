package dev.abstra.pro.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.abstra.pro.data.model.Playlist
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.ui.components.SongListItem
import dev.abstra.pro.ui.theme.LocalAbstraColors
import dev.abstra.pro.ui.utils.rememberReorderableState
import dev.abstra.pro.ui.utils.reorderable
import dev.abstra.pro.ui.utils.reorderableItem
import dev.abstra.pro.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current
    
    val songs by viewModel.observePlaylistSongs(playlistId).collectAsState(initial = emptyList())
    var currentSongs by remember(songs) { mutableStateOf(songs) }
    
    val listState = rememberLazyListState()
    
    val reorderableState = rememberReorderableState(
        listState = listState,
        onMove = { fromIndex, toIndex ->
            if (fromIndex in currentSongs.indices && toIndex in 0..currentSongs.size) {
                currentSongs = currentSongs.toMutableList().apply {
                    val item = removeAt(fromIndex)
                    // If moving downwards, the target index shifts by 1 because we removed an item before it.
                    // Actually, Compose Reorderable assumes `toIndex` is the raw index of the target item.
                    // A safe way to move an item to a new index:
                    add(toIndex.coerceAtMost(size), item)
                }
            }
        },
        onDragEnd = {
            viewModel.updatePlaylistOrder(playlistId, currentSongs)
        }
    )

    var showAddSongsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBackground)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            
            // Thumbnail
            val firstSong = currentSongs.firstOrNull()
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(
                                colors.primary.copy(alpha = 0.6f),
                                colors.accent.copy(alpha = 0.4f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (firstSong != null) {
                    dev.abstra.pro.ui.components.ThumbnailOnly(
                        trackId = firstSong.id,
                        title = firstSong.title,
                        artist = firstSong.artist,
                        uriString = firstSong.uriString,
                        modifier = Modifier.fillMaxSize(),
                        cornerRadius = 12.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlistName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "${currentSongs.size} songs",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
            IconButton(onClick = { showAddSongsDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Songs", tint = colors.primary)
            }
        }
        
        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { onPlayAll(currentSongs) },
                enabled = currentSongs.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = colors.bgCard,
                    contentColor = colors.textPrimary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play All", fontSize = 12.sp)
            }

            Button(
                onClick = { onShuffleAll(currentSongs) },
                enabled = currentSongs.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Shuffle", fontSize = 12.sp)
            }
        }

        // List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .reorderable(reorderableState),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            itemsIndexed(currentSongs, key = { _, song -> song.id }) { index, song ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.removeSongFromPlaylist(playlistId, song)
                            true
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red.copy(alpha = 0.8f))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.White)
                        }
                    },
                    enableDismissFromStartToEnd = false
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.bgBackground)
                            .reorderableItem(reorderableState, index)
                    ) {
                        SongListItem(
                            song = song,
                            isPlaying = false,
                            onClick = { onPlaySong(song, currentSongs) },
                            onFavoriteToggle = {},
                            onOptionsClick = {},
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        // Drag Handle
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = colors.textMuted,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddSongsDialog) {
        AddSongsToPlaylistDialog(
            playlistId = playlistId,
            allSongs = viewModel.allSongs.value,
            initialSelectedIds = currentSongs.map { it.id }.toSet(),
            onDismiss = { showAddSongsDialog = false },
            onSave = { selectedIds ->
                viewModel.addSongsToPlaylist(playlistId, viewModel.allSongs.value.filter { it.id in selectedIds })
                showAddSongsDialog = false
            }
        )
    }
}

@Composable
fun AddSongsToPlaylistDialog(
    playlistId: Long,
    allSongs: List<Song>,
    initialSelectedIds: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateOf(initialSelectedIds.toMutableSet()) }

    val filteredSongs = remember(searchQuery, allSongs) {
        if (searchQuery.isBlank()) allSongs
        else allSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Add Songs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search songs...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(filteredSongs) { _, song ->
                        val isSelected = selectedIds.value.contains(song.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) selectedIds.value.remove(song.id)
                                    else selectedIds.value.add(song.id)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = song.title, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(text = song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(selectedIds.value) }) {
                        Text("Add")
                    }
                }
            }
        }
    }
}
