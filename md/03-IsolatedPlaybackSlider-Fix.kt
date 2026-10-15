// =====================================================================
// 03-IsolatedPlaybackSlider-Fix.kt
// ---------------------------------------------------------------------
// Drop-in replacement for IsolatedPlaybackSlider in
// dev/abstra/pro/ui/screens/HomeScreen.kt (or wherever it currently
// lives — around line 11816 in the source you provided).
//
// Mirrors the Lune pattern from QuickPlayerActivity.kt (around line 13675):
//   - explicit isDragging / dragProgress state
//   - 0f..1f normalized slider, mapped back to ms at onValueChangeFinished
//   - LaunchedEffect pushes engine position to the slider ONLY while
//     not dragging (no thumb-jump while the user is dragging)
//   - smooth 60 fps propagation driven by AudioEngine's withFrameNanos
//     ticker (see 02-AudioEngine-ProgressAndSeekFix.kt)
// =====================================================================

package dev.abstra.pro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.abstra.pro.ui.theme.AbstraColors

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * Robust playback progress slider.
 *
 * Differences vs. the previous version:
 *   1. Decouples the user's drag position from the engine's reported
 *      position. While dragging, only `dragProgress` moves; the engine
 *      value never overwrites it.
 *   2. Slider is normalized 0..1 (Lune pattern) and mapped to ms only
 *      when committing the seek. This avoids precision loss for very
 *      long tracks.
 *   3. `seekTo` is called on `onValueChangeFinished` — never on every
 *      tick — so MediaPlayer.seekTo() is hit at most once per gesture.
 *   4. While the user is dragging we don't write back to `currentPosition`
 *      so the engine doesn't fight the user's intent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsolatedPlaybackSlider(
    currentPosition: Long,
    duration: Long,
    colors: AbstraColors,
    onSeekTo: (Long) -> Unit,
) {
    val safeDuration = duration.coerceAtLeast(1L)
    val engineFraction = (currentPosition.toFloat() / safeDuration.toFloat())
        .coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(engineFraction) }

    // SliderState holds the visual position. We push engine updates into
    // it ONLY while the user is not interacting. This is the fix for
    // "slider jumps while user is dragging".
    val sliderState = remember {
        SliderState(value = engineFraction)
    }

    LaunchedEffect(engineFraction) {
        if (!isDragging) {
            sliderState.value = engineFraction
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            state = sliderState,
            onValueChange = { fraction ->
                isDragging = true
                dragProgress = fraction.coerceIn(0f, 1f)
            },
            onValueChangeFinished = {
                isDragging = false
                val targetMs = (dragProgress * safeDuration).toLong()
                onSeekTo(targetMs)
            },
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.border
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayMs = if (isDragging) {
                (dragProgress * safeDuration).toLong()
            } else {
                currentPosition.coerceIn(0L, safeDuration)
            }
            Text(
                text = formatTime(displayMs),
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(safeDuration),
                color = colors.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}