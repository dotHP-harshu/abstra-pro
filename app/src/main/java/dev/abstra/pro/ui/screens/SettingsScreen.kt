package dev.abstra.pro.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.data.model.PlayerDesignStyle
import dev.abstra.pro.ui.theme.AppTheme
import dev.abstra.pro.ui.theme.LocalAbstraColors

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    currentDesignStyle: PlayerDesignStyle,
    onDesignStyleChange: (PlayerDesignStyle) -> Unit,
    crossfadeSeconds: Int,
    onCrossfadeChange: (Int) -> Unit,
    gaplessEnabled: Boolean,
    onGaplessToggle: (Boolean) -> Unit,
    minDurationFilterSec: Int,
    onMinDurationChange: (Int) -> Unit,
    writePhysicalTags: Boolean,
    onWritePhysicalTagsToggle: (Boolean) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onRescanLibrary: () -> Unit,
    onSelectFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBackground)
    ) {
        // ─────────────────────────────────────────────────────────────
        // 1. TOP HEADER
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
                Text(
                    text = "Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Preferences, audio engine, Lune rules & aesthetics",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─────────────────────────────────────────────────────────────
            // 2. APPEARANCE & THEMES (Abstra Pastel & AMOLED)
            // ─────────────────────────────────────────────────────────────
            item {
                SettingsSection(title = "Appearance & Aesthetics") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Color Palette",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )

                        // Theme Swatches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AppTheme.values().forEach { theme ->
                                val isSelected = currentTheme == theme
                                val swatchColor = when (theme) {
                                    AppTheme.COTTON_CANDY -> Color(0xFFFFB5C5)
                                    AppTheme.PEACH_BLOSSOM -> Color(0xFFFFDAB9)
                                    AppTheme.MILKY_PINK -> Color(0xFFFFD1DC)
                                    AppTheme.NIGHT_BUBBLE -> Color(0xFF4B0082)
                                    AppTheme.VINTAGE_TAPE -> Color(0xFFD2B48C)
                                    AppTheme.MIDNIGHT_ROOM -> Color(0xFF2C3E50)
                                    AppTheme.AMOLED_BLACK -> Color(0xFF1E1E1E)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .clickable { onThemeChange(theme) }
                                        .then(
                                            if (isSelected) Modifier.border(3.dp, colors.primary, CircleShape)
                                            else Modifier.border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (theme == AppTheme.AMOLED_BLACK) Color.White else Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Default Centerpiece Style
                        Text(
                            text = "Default Centerpiece Style",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PlayerDesignStyle.values().forEach { style ->
                                val isSelected = currentDesignStyle == style
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) colors.primary else colors.bgSurface,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onDesignStyleChange(style) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (style) {
                                                PlayerDesignStyle.ROTATING_CD -> "CD"
                                                PlayerDesignStyle.VINYL_RECORD -> "Vinyl"
                                                PlayerDesignStyle.POLAROID -> "Polaroid"
                                                PlayerDesignStyle.CASSETTE -> "Cassette"
                                                PlayerDesignStyle.MINIMAL_ART -> "Minimal"
                                                PlayerDesignStyle.HEART_WINDOW -> "Heart"
                                            },
                                            fontSize = 11.sp,
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
            // 3. AUDIO ENGINE & PLAYBACK
            // ─────────────────────────────────────────────────────────────
            item {
                SettingsSection(title = "Audio Engine & Playback") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Gapless Playback Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Gapless Playback", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                                Text("Eliminate silence gaps between consecutive tracks", fontSize = 12.sp, color = colors.textSecondary)
                            }
                            Switch(
                                checked = gaplessEnabled,
                                onCheckedChange = onGaplessToggle,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.primary)
                            )
                        }

                        Divider(color = colors.border.copy(alpha = 0.5f))

                        // Crossfade Duration
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Crossfade Duration", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                                Text("${crossfadeSeconds}s", fontWeight = FontWeight.Bold, color = colors.primary, fontSize = 13.sp)
                            }
                            Slider(
                                value = crossfadeSeconds.toFloat(),
                                onValueChange = { onCrossfadeChange(it.toInt()) },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 4. LUNE SCANNER RULES & LIBRARY
            // ─────────────────────────────────────────────────────────────
            item {
                SettingsSection(title = "Lune Library & Folder Rules") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Min duration filter
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Filter Short Audio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                                    Text("Ignore audio shorter than threshold (voice notes, UI alerts)", fontSize = 12.sp, color = colors.textSecondary)
                                }
                                Text("${minDurationFilterSec}s", fontWeight = FontWeight.Bold, color = colors.primary, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(10, 30, 60).forEach { sec ->
                                    val isSelected = minDurationFilterSec == sec
                                    FilledTonalButton(
                                        onClick = { onMinDurationChange(sec) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (isSelected) colors.primary else colors.bgSurface,
                                            contentColor = if (isSelected) Color.White else colors.textPrimary
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("≥ ${sec}s", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Divider(color = colors.border.copy(alpha = 0.5f))

                        // Physical Tag Writing (Jaudiotagger)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Write Tags to Audio Files", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                                Text("Embed edited tags directly into MP3 / FLAC files via Jaudiotagger", fontSize = 12.sp, color = colors.textSecondary)
                            }
                            Switch(
                                checked = writePhysicalTags,
                                onCheckedChange = onWritePhysicalTagsToggle,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = colors.primary)
                            )
                        }

                        Divider(color = colors.border.copy(alpha = 0.5f))

                        // Rescan MediaStore
                        OutlinedButton(
                            onClick = onRescanLibrary,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rescan Device Storage Now", color = colors.primary)
                        }

                        Divider(color = colors.border.copy(alpha = 0.5f))

                        // Music Folder selection
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Music Library Folder", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                                    Text("Restrict scanning to a specific folder", fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            FilledTonalButton(
                                onClick = { onSelectFolder() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = colors.bgSurface,
                                    contentColor = colors.textPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Select Folder", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 5. SAF BACKUP & RESTORE (Lune / Abstra JSON)
            // ─────────────────────────────────────────────────────────────
            item {
                SettingsSection(title = "Playlists & SAF Backup") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Export and restore your playlists and custom tags to offline JSON files without cloud sync.",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onExportBackup,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = onImportBackup,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = colors.bgSurface,
                                    contentColor = colors.textPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import JSON", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 6. ABOUT & PRIVACY
            // ─────────────────────────────────────────────────────────────
            item {
                SettingsSection(title = "About Abstra Pro") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Version", fontSize = 13.sp, color = colors.textSecondary)
                            Text("1.0.0 (GPLv3 Open Source)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Network Access", fontSize = 13.sp, color = colors.textSecondary)
                            Text("None (100% Offline & Private)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Audio Engine", fontSize = 13.sp, color = colors.textSecondary)
                            Text("Media3 + Hardware Multi-Band DSP", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    val colors = LocalAbstraColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
