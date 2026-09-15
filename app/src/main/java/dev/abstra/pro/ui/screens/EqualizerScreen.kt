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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.abstra.pro.audio.AudioEffectsManager
import dev.abstra.pro.data.model.DynamicsPreset
import dev.abstra.pro.data.model.EqPreset
import dev.abstra.pro.data.model.ReverbEnvironment
import dev.abstra.pro.ui.theme.LocalAbstraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    effectsManager: AudioEffectsManager,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAbstraColors.current

    // State bindings from effectsManager
    val eqState by effectsManager.equalizerState.collectAsState()
    val reverbEnv by effectsManager.reverbEnvironment.collectAsState()
    val dynamicsPreset by effectsManager.dynamicsPreset.collectAsState()
    val balancePan by effectsManager.balancePan.collectAsState()
    val loudnessGain by effectsManager.loudnessGainMb.collectAsState()

    var showPresetMenu by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ─────────────────────────────────────────────────────────────
        // 1. TOP HEADER & MASTER TOGGLE
        // ─────────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.bgBackground
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.bgCard)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textPrimary
                    )
                }

                Text(
                    text = "DSP Audio Effects",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                // Master EQ Switch
                Switch(
                    checked = eqState.enabled,
                    onCheckedChange = { effectsManager.setEqualizerEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.primary,
                        uncheckedThumbColor = colors.textMuted,
                        uncheckedTrackColor = colors.bgCard
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─────────────────────────────────────────────────────────────
            // 2. PRESETS BAR
            // ─────────────────────────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "EQ Preset",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = eqState.currentPreset.displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Preset Selector Dropdown
                            Box {
                                FilledTonalButton(
                                    onClick = { showPresetMenu = true },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = colors.primary.copy(alpha = 0.15f),
                                        contentColor = colors.primary
                                    )
                                ) {
                                    Text("Change", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                                }

                                DropdownMenu(
                                    expanded = showPresetMenu,
                                    onDismissRequest = { showPresetMenu = false },
                                    modifier = Modifier.background(colors.bgSurface)
                                ) {
                                    EqPreset.values().forEach { preset ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = preset.displayName,
                                                    color = if (preset == eqState.currentPreset) colors.primary else colors.textPrimary,
                                                    fontWeight = if (preset == eqState.currentPreset) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                effectsManager.setEqualizerPreset(preset)
                                                showPresetMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Reset to Flat
                            IconButton(
                                onClick = { effectsManager.setEqualizerPreset(EqPreset.FLAT) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colors.bgSurface)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset EQ",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 3. HARDWARE EQUALIZER BANDS (Vertical Sliders)
            // ─────────────────────────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Hardware Equalizer (Multi-Band)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (eqState.bandLevels.isEmpty()) {
                            Text(
                                text = "Hardware EQ is not supported on this audio device/session.",
                                color = colors.textMuted,
                                fontSize = 13.sp
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                eqState.bandLevels.forEachIndexed { index, level ->
                                    val freqHz = eqState.centerFrequencies.getOrNull(index) ?: ((index + 1) * 1000)
                                    val freqLabel = if (freqHz >= 1000) "${freqHz / 1000}k" else "${freqHz}"

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "${level / 100} dB",
                                            fontSize = 9.sp,
                                            color = colors.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Vertical slider equivalent
                                        Box(
                                            modifier = Modifier
                                                .height(130.dp)
                                                .width(26.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Slider(
                                                value = level.toFloat(),
                                                onValueChange = { newLevel ->
                                                    effectsManager.setBandLevel(index, newLevel.toInt().toShort())
                                                },
                                                valueRange = (eqState.minBandLevel.toFloat())..(eqState.maxBandLevel.toFloat()),
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(130.dp)
                                                    .rotate(-90f),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = colors.primary,
                                                    activeTrackColor = colors.primary,
                                                    inactiveTrackColor = colors.border
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = freqLabel,
                                            fontSize = 10.sp,
                                            color = colors.textSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 4. BASS BOOST & VIRTUALIZER
            // ─────────────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bass Boost
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.bgCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Bass Boost",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${eqState.bassBoostStrength / 10}%",
                                fontSize = 11.sp,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = eqState.bassBoostStrength.toFloat(),
                                onValueChange = { effectsManager.setBassBoostStrength(it.toInt()) },
                                valueRange = 0f..1000f,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }
                    }

                    // Virtualizer
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.bgCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Virtualizer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${eqState.virtualizerStrength / 10}%",
                                fontSize = 11.sp,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = eqState.virtualizerStrength.toFloat(),
                                onValueChange = { effectsManager.setVirtualizerStrength(it.toInt()) },
                                valueRange = 0f..1000f,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 5. LOUDNESS ENHANCER (0 dB to 30 dB)
            // ─────────────────────────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Loudness Enhancer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "+${loudnessGain / 100} dB",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                        Slider(
                            value = loudnessGain.toFloat(),
                            onValueChange = { effectsManager.setLoudnessGainMb(it.toInt()) },
                            valueRange = 0f..3000f,
                            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 6. DYNAMICS PROCESSOR (MBC + LIMITER)
            // ─────────────────────────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Dynamics Processing (Limiter & Compressor)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DynamicsPreset.values().forEach { preset ->
                                val isSelected = dynamicsPreset == preset
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) colors.primary else colors.bgSurface,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { effectsManager.setDynamicsPreset(preset) }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = preset.displayName,
                                            fontSize = 10.sp,
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
            // 7. STEREO BALANCE PAN & REVERB
            // ─────────────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Balance Pan
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.bgCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Stereo Pan",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            val panLabel = when {
                                balancePan < -0.05f -> "L ${(-balancePan * 100).toInt()}%"
                                balancePan > 0.05f -> "R ${(balancePan * 100).toInt()}%"
                                else -> "Center"
                            }
                            Text(text = panLabel, fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                            Slider(
                                value = balancePan,
                                onValueChange = { effectsManager.setBalancePan(it) },
                                valueRange = -1.0f..1.0f,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }
                    }

                    // Playback Speed
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.bgCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Speed & Pitch",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = String.format("%.2fx", playbackSpeed),
                                fontSize = 11.sp,
                                color = colors.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = playbackSpeed,
                                onValueChange = { onSpeedChange(it) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────────────────────
            // 8. REVERB ENVIRONMENT
            // ─────────────────────────────────────────────────────────────
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.bgCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Reverb Environment",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        var reverbMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { reverbMenuOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(reverbEnv.displayName, color = colors.primary)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null, tint = colors.primary)
                            }

                            DropdownMenu(
                                expanded = reverbMenuOpen,
                                onDismissRequest = { reverbMenuOpen = false },
                                modifier = Modifier.background(colors.bgSurface)
                            ) {
                                ReverbEnvironment.values().forEach { env ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = env.displayName,
                                                color = if (env == reverbEnv) colors.primary else colors.textPrimary,
                                                fontWeight = if (env == reverbEnv) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            effectsManager.setReverbEnvironment(env)
                                            reverbMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
