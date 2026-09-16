package dev.abstra.pro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.ui.theme.LocalAbstraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    onDismissRequest: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onEditMetadata: () -> Unit,
    onDeleteSong: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    val colors = LocalAbstraColors.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = colors.bgSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThematicAlbumArt(
                    trackId = song.id,
                    title = song.title,
                    artist = song.artist,
                    uriString = song.uriString,
                    modifier = Modifier.size(56.dp),
                    cornerRadius = 12.dp,
                    showDetailsOnLarge = false
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = song.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.5f))

            // Actions
            if (onToggleFavorite != null) {
                BottomSheetOption(
                    icon = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (song.isFavorite) "Remove from Favorites" else "Add to Favorites",
                    onClick = onToggleFavorite
                )
            }
            BottomSheetOption(
                icon = Icons.Default.PlayArrow,
                label = "Play Next",
                onClick = onPlayNext
            )
            BottomSheetOption(
                icon = Icons.Default.QueueMusic,
                label = "Add to Queue",
                onClick = onAddToQueue
            )
            BottomSheetOption(
                icon = Icons.Default.PlaylistAdd,
                label = "Add to Playlist",
                onClick = onAddToPlaylist
            )
            BottomSheetOption(
                icon = Icons.Default.Edit,
                label = "Edit Metadata",
                onClick = onEditMetadata
            )
            BottomSheetOption(
                icon = Icons.Default.Delete,
                label = "Delete from Device",
                onClick = onDeleteSong,
                isDestructive = true
            )
        }
    }
}

@Composable
private fun BottomSheetOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val colors = LocalAbstraColors.current
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else colors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}
