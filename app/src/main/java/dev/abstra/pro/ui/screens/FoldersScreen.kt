package dev.abstra.pro.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.data.model.Folder
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.data.model.StorageInfo
import dev.abstra.pro.ui.components.SongListItem
import dev.abstra.pro.ui.theme.LocalAbstraColors

@Composable
fun FoldersScreen(
    folders: List<Folder>,
    storageVolumes: List<StorageInfo>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayFolder: (Folder) -> Unit,
    onShuffleFolder: (Folder) -> Unit,
    onExcludeFolder: (Folder) -> Unit,
    onRescanLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }
    var selectedVolumeIndex by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBackground)
    ) {
        // ─────────────────────────────────────────────────────────────
        // 1. TOP HEADER & STORAGE VOLUME SELECTOR
        // ─────────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.bgBackground
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
                        text = "Folders",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    IconButton(
                        onClick = onRescanLibrary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.bgCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan Storage",
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Storage Volume Selector Tabs (Internal vs SD Card)
                if (storageVolumes.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        storageVolumes.forEachIndexed { index, volume ->
                            val isSelected = selectedVolumeIndex == index
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) colors.primary else colors.bgCard,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedVolumeIndex = index }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (volume.isRemovable) Icons.Default.SdCard else Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = volume.description,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 2. FOLDER CONTENT OR FOLDER LIST (Lune Scanner Navigation)
        // ─────────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = selectedFolder,
            label = "folderNavigationAnim"
        ) { folder ->
            if (folder != null) {
                // Inside a single folder
                FolderDetailView(
                    folder = folder,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onSongClick = onSongClick,
                    onPlayAll = { onPlayFolder(folder) },
                    onShuffleAll = { onShuffleFolder(folder) },
                    onBack = { selectedFolder = null }
                )
            } else {
                // Folder List
                if (folders.isEmpty()) {
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
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No audio folders found",
                                color = colors.textSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Lune scanner ignores audio < 10 seconds (voice notes/alerts). Tap refresh to rescan.",
                                color = colors.textMuted,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 130.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folders, key = { it.path }) { item ->
                            FolderRowCard(
                                folder = item,
                                onClick = { selectedFolder = item },
                                onPlay = { onPlayFolder(item) },
                                onExclude = { onExcludeFolder(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRowCard(
    folder: Folder,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onExclude: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .border(1.2.dp, colors.border, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = colors.bgCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.songCount} audio tracks • ${folder.path}",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Folder",
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Folder Options",
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
                        text = { Text("Hide / Exclude Folder") },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                        onClick = {
                            showMenu = false
                            onExclude()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderDetailView(
    folder: Folder,
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Column {
                    Text(
                        text = folder.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${folder.songCount} songs",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onPlayAll,
                    shape = RoundedCornerShape(16.dp),
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 130.dp)
        ) {
            items(folder.songs, key = { it.id }) { song ->
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
