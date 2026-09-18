package dev.abstra.pro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.ui.theme.LocalAbstraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<Song>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onReorder: (from: Int, to: Int) -> Unit,
    onRemove: (Song) -> Unit,
    onClearQueue: () -> Unit,
    onClearEntireQueue: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    val colors = LocalAbstraColors.current
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableState(
        listState = listState,
        canDragOver = { it != currentIndex },   // never drag over the currently playing track
        onMove = { from, to -> onReorder(from, to) }
    )
    
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear entire queue?") },
            text = { Text("Playback will stop and all songs will be removed from the queue.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearEntireQueue()
                    showClearConfirm = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Header: "Up Next" + count + clear button ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Up Next",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${(queue.size - (currentIndex + 1)).coerceAtLeast(0)} songs queued" +
                            " • long-press & drag to reorder",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
                TextButton(
                    onClick = { showClearConfirm = true },
                    enabled = queue.size > 1
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear all", fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Queue is empty", color = colors.textMuted, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .reorderable(reorderState),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
                ) {
                    itemsIndexed(queue, key = { _, s -> s.id }) { index, song ->
                        val isCurrent = index == currentIndex
                        SongListItem(
                            song = song,
                            isPlaying = isCurrent,
                            onClick = { onSongClick(song) },
                            onFavoriteToggle = {},
                            onOptionsClick = { if (!isCurrent) onRemove(song) },
                            modifier = Modifier.reorderableItem(reorderState, index),
                            onSwipeNext = null
                        )
                    }
                }
            }
        }
    }
}
