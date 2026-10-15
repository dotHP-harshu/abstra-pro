// =====================================================================
// 01-AudioEngine-QueueFix.kt
// ---------------------------------------------------------------------
// Drop-in replacement for the broken repeat-mode / queue logic in
// dev/abstra/pro/audio/AudioEngine.kt
//
// Replaces:
//   - private suspend fun handleSongCompletion()
//   - fun playNext()
//   - fun playPrevious()
//   - fun toggleRepeatMode()
//   - fun toggleShuffle()
//
// AND adds:
//   - @Volatile private var isAdvancingQueue: Boolean
//
// Why:
//   The original implementation had three correlated bugs:
//     (1) REPEAT_ONE used seekTo(0)+resume() which re-triggered
//         onCompletion() and could lose audio focus.
//     (2) REPEAT_ALL incremented consecutiveFailures on the wrap
//         transition, polluting the "broken track" heuristic.
//     (3) REPEAT_OFF did not guard against re-entry from the completion
//         callback, so subsequent user taps could collide with the
//         still-settling pause.
//
// Lune avoids all three by going through Media3, which serializes state
// transitions. We achieve the same property on top of the existing
// coroutine + MediaPlayer engine by adding a single re-entry guard.
// =====================================================================

package dev.abstra.pro.audio

import android.util.Log
import dev.abstra.pro.data.model.RepeatMode
import dev.abstra.pro.data.model.Song
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------
// Inside AudioEngine, add:
// ---------------------------------------------------------------------

    /**
     * Re-entry guard. While a queue-advance is in flight (the engine has
     * already kicked off `playSong(...)` and the previous MediaPlayer
     * has not yet released), additional `handleSongCompletion()` calls
     * are dropped. This is the single change that kills the
     * "infinite loop on REPEAT_ONE" bug.
     */
    @Volatile
    private var isAdvancingQueue: Boolean = false

// ---------------------------------------------------------------------
// Replace the original playNext() with:
// ---------------------------------------------------------------------

    fun playNext() {
        if (isAdvancingQueue) {
            Log.d(TAG, "playNext() ignored: queue advance already in flight")
            return
        }
        val q = _queue.value
        if (q.isEmpty()) return

        // Hard-stop on broken-track heuristic.  We only count it when
        // playSong() actually reports an error.  Wrapping to the
        // beginning of the queue on REPEAT_ALL does NOT count as a
        // failure anymore.
        if (consecutiveFailures.get() > q.size) {
            _isPlaying.value = false
            consecutiveFailures.set(0)
            return
        }

        isAdvancingQueue = true
        try {
            when (_repeatMode.value) {
                RepeatMode.REPEAT_ONE -> {
                    // Replay the SAME song.  We still go through the
                    // normal playSong path so MediaPlayer is fully
                    // prepared, audio focus is re-requested, and the
                    // progress tracker is reset.
                    val current = q[_currentIndex.value.coerceIn(0, q.size - 1)]
                    consecutiveFailures.set(0)
                    playSong(current)
                }
                RepeatMode.REPEAT_ALL -> {
                    val nextIdx = (_currentIndex.value + 1) % q.size
                    _currentIndex.value = nextIdx
                    consecutiveFailures.set(0)
                    playSong(q[nextIdx])
                }
                RepeatMode.REPEAT_OFF -> {
                    val nextIdx = _currentIndex.value + 1
                    if (nextIdx >= q.size) {
                        // No more songs in the queue.  Pause, reset
                        // position, but DO NOT playSong() — we want the
                        // UI to settle into "stopped at end of queue".
                        pause()
                        seekTo(0L)
                        consecutiveFailures.set(0)
                    } else {
                        _currentIndex.value = nextIdx
                        consecutiveFailures.set(0)
                        playSong(q[nextIdx])
                    }
                }
            }
        } finally {
            // playSong() schedules its own work on the main looper;
            // releasing the guard after a short delay guarantees that
            // any second onCompletion() that arrives from the previous
            // player (race during MediaPlayer release) is dropped.
            scope.launch {
                delay(120)
                isAdvancingQueue = false
            }
        }
    }

// ---------------------------------------------------------------------
// Replace playPrevious() with:
// ---------------------------------------------------------------------

    fun playPrevious() {
        if (isAdvancingQueue) return
        val now = System.currentTimeMillis()
        val isDoubleTap = (now - lastPreviousClickTimeMs) < 1200L
        lastPreviousClickTimeMs = now

        val pos = _currentPosition.value
        if (!isDoubleTap && pos > 3000L) {
            seekTo(0L)
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return

        // If we are <3s into a song and the user double-taps, we go to
        // the PREVIOUS track.  Otherwise we restart the current track.
        // (Same semantics as Lune / Spotify / System Media3.)
        val current = q[_currentIndex.value.coerceIn(0, q.size - 1)]
        val target = if (pos < 3000L) {
            val prevIdx = (_currentIndex.value - 1 + q.size) % q.size
            q[prevIdx]
        } else {
            current
        }
        // Re-emit through playSong so the engine resets properly.
        isAdvancingQueue = true
        try {
            _currentIndex.value = q.indexOfFirst { it.id == target.id }.coerceAtLeast(0)
            playSong(target)
        } finally {
            scope.launch {
                delay(120)
                isAdvancingQueue = false
            }
        }
    }

// ---------------------------------------------------------------------
// Replace handleSongCompletion() with:
// ---------------------------------------------------------------------

    private suspend fun handleSongCompletion() {
        val s = _currentSong.value
        if (s != null) {
            onSongCompleted?.invoke(s)
        }
        playNext()
    }

// ---------------------------------------------------------------------
// Replace toggleRepeatMode() with:
// ---------------------------------------------------------------------

    fun toggleRepeatMode() {
        val newMode = when (_repeatMode.value) {
            RepeatMode.REPEAT_OFF -> RepeatMode.REPEAT_ALL
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.REPEAT_OFF
        }
        prefs.edit().putString("repeat_mode", newMode.name).apply()
        _repeatMode.value = newMode
        // Notify the media-session adapter so the system Quick Settings
        // tile updates its repeat icon immediately.
        onRepeatModeChanged?.invoke(newMode)
    }

// ---------------------------------------------------------------------
// Replace toggleShuffle() with:
// ---------------------------------------------------------------------

    fun toggleShuffle() {
        val newVal = !_isShuffle.value
        prefs.edit().putBoolean("shuffle_enabled", newVal).apply()
        _isShuffle.value = newVal
        val activeSong = _currentSong.value
        if (newVal) {
            val q = _queue.value
            val idx = _currentIndex.value
            val shuffled = shuffledQueue(q, idx)
            _queue.value = shuffled
            _currentIndex.value = shuffled.indexOfFirst { it.id == activeSong?.id }.coerceAtLeast(0)
        } else {
            _queue.value = originalQueue
            _currentIndex.value = originalQueue.indexOfFirst { it.id == activeSong?.id }.coerceAtLeast(0)
        }
        onShuffleChanged?.invoke(newVal)
    }

// ---------------------------------------------------------------------
// New public hooks (declare them next to onPlaybackStateChange):
// ---------------------------------------------------------------------

    var onRepeatModeChanged: ((RepeatMode) -> Unit)? = null
    var onShuffleChanged: ((Boolean) -> Unit)? = null

// ---------------------------------------------------------------------
// Companion-level constants:
// ---------------------------------------------------------------------

    companion object {
        private const val TAG = "AudioEngine"
    }

// ---------------------------------------------------------------------
// Integration sites:
//   1. Paste the new methods inside `class AudioEngine`.
//   2. Add the `isAdvancingQueue` field next to the other @Volatile
//      state (after `lastPreviousClickTimeMs`).
//   3. Hook the new callbacks from `MusicPlaybackService.onCreate` so
//      the SimpleBasePlayer mirror in the service stays in sync:
//          audioEngine.onRepeatModeChanged = { rm ->
//              invalidatePlayerState?.invoke()
//          }
//          audioEngine.onShuffleChanged = { _ ->
//              invalidatePlayerState?.invoke()
//          }
// ---------------------------------------------------------------------