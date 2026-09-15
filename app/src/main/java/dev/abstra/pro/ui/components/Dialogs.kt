package dev.abstra.pro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.ui.theme.LocalAbstraColors

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val colors = LocalAbstraColors.current
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.bgSurface,
        title = {
            Text(
                text = "Create Playlist",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textHeading
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border
                    )
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.border
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), desc.trim()) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Create", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMuted)
            }
        }
    )
}

@Composable
fun SaveEqPresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val colors = LocalAbstraColors.current
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.bgSurface,
        title = {
            Text("Save EQ Preset", style = MaterialTheme.typography.titleLarge, color = colors.textHeading)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Preset Name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.border
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMuted)
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    itemTitle: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalAbstraColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.bgSurface,
        title = {
            Text("Confirm Deletion", style = MaterialTheme.typography.titleLarge, color = colors.textHeading)
        },
        text = {
            Text("Are you sure you want to delete \"$itemTitle\"?", color = colors.textBody)
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentHeart)
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMuted)
            }
        }
    )
}

@Composable
fun SleepTimerDialog(
    currentRemainingSec: Int,
    onDismiss: () -> Unit,
    onSelectDuration: (Int) -> Unit
) {
    val colors = LocalAbstraColors.current
    val durations = listOf(15, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.bgSurface,
        title = {
            Text("Sleep Timer", style = MaterialTheme.typography.titleLarge, color = colors.textHeading)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentRemainingSec > 0) {
                    val mins = currentRemainingSec / 60
                    val secs = currentRemainingSec % 60
                    Text(
                        text = "Active: $mins mins $secs secs remaining",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { onSelectDuration(0) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Turn Off Sleep Timer", color = colors.accentHeart)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                durations.forEach { mins ->
                    Button(
                        onClick = { onSelectDuration(mins) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.bgCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("$mins Minutes", color = colors.textPrimary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textMuted)
            }
        }
    )
}

