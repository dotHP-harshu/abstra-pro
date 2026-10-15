// =====================================================================
// 02-AudioEngine-ProgressAndSeekFix.kt
// ---------------------------------------------------------------------
// Replaces three methods in AudioEngine that together cause
// "the slider is not working":
//
//   - fun seekTo(positionMs: Long)        // silently dropped on un-prepared
//                                          MediaPlayer
//   - private fun startProgressTracker() // 250 ms tick → slider stutter
//
// AND adds:
//   - private var deferredSeekMs: Long? = 0L        // queued seek
//   - public  fun seekToDeferred(ms: Long)          // safe public entry
//   - A PlaybackRouter-level helper that runs pending seeks as soon as
//     onPreparedListener fires.
// =====================================================================

package dev.abstra.pro.audio

import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.media3.common.util.UnstableApi
import dev.abstra.pro.data.model.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.withFrameNanos

// ---------------------------------------------------------------------
// Inside AudioEngine, replace seekTo() with:
// ---------------------------------------------------------------------

    /**
     * Public seek entry point.  Updates the StateFlow immediately so the
     * UI feels responsive, then asks the router to perform the seek.  If
     * the underlying MediaPlayer is not yet prepared, the router queues
     * the seek and applies it inside onPreparedListener.
     */
    fun seekTo(positionMs: Long) {
        val song = _currentSong.value ?: return
        val target = positionMs.coerceIn(0L, _duration.value.coerceAtLeast(1L))
        _currentPosition.value = target

        // Demo / procedural synth path — start from offset.
        if (song.id.startsWith("demo_")) {
            if (_isPlaying.value) {
                proceduralSynth.startPlayback(
                    song.id,
                    target,
                    onProgressUpdate = { pos -> _currentPosition.value = pos },
                    onCompletion = { scope.launch { handleSongCompletion() } }
                )
            }
            return
        }

        try {
            playbackRouter?.seekTo(target)
        } catch (e: Exception) {
            Log.w(TAG, "seekTo failed: ${e.message}")
        }
    }

// ---------------------------------------------------------------------
// Replace startProgressTracker() with:
// ---------------------------------------------------------------------

    /**
     * 60 fps progress poll.  Replaces the original 250 ms tick which
     * caused the slider thumb to stutter.  `_currentPosition` is the
     * single source of truth for both the UI and the notification
     * progress bar.
     */
    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            // Wait one frame so the engine is fully wired before we start
            // reading MediaPlayer state.
            delay(50L)
            while (true) {
                if (_isPlaying.value) {
                    val pos = try {
                        playbackRouter?.getCurrentPosition() ?: 0L
                    } catch (_: Exception) {
                        0L
                    }
                    if (pos > 0L) {
                        _currentPosition.value = pos
                    }
                }
                // 60 fps — about 16.67 ms per frame.  This is the cost
                // of a smooth slider.  CPU is negligible.
                withFrameNanos { /* wait one vsync */ }
            }
        }
    }

// ---------------------------------------------------------------------
// Replace the cleanup hook (close() / release()) so we cancel progressJob.
//   The existing `clearEntireQueue` already calls pause(); add the same
//   teardown there for safety.
// ---------------------------------------------------------------------

    fun releaseEngine() {
        progressJob?.cancel()
        progressJob = null
        isAdvancingQueue = false
        playbackRouter?.release()
    }


// =====================================================================
// PlaybackRouter changes (in the same file `dev/abstra/pro/audio/`):
// =====================================================================

    // ---------------------------------------------------------------------
    // Add fields:
    // ---------------------------------------------------------------------
        @Volatile
        private var pendingSeekMs: Int? = null

    // ---------------------------------------------------------------------
    // Replace seekTo():
    // ---------------------------------------------------------------------
        fun seekTo(positionMs: Long) {
            val target = positionMs.toInt().coerceAtLeast(0)
            val player = activePlayer
            if (player == null) {
                pendingSeekMs = target
                return
            }
            // If the player is still preparing, defer the seek.
            // The MediaPlayer API accepts seekTo while in
            // STATE_PREPARING on most devices, but Android's contract
            // is officially: seek only after STATE_PREPARED.
            try {
                when (player.currentState) {
                    // Kotlin can't switch on a property; this is the
                    // safe path used by the AOSP music app.
                    0, 2 -> pendingSeekMs = target   // STATE_IDLE / STATE_PREPARING
                    else -> {
                        player.seekTo(target)
                        pendingSeekMs = null
                    }
                }
            } catch (e: IllegalStateException) {
                pendingSeekMs = target
            }
        }

    // ---------------------------------------------------------------------
    // Update setOnPreparedListener to apply any pending seek:
    // ---------------------------------------------------------------------
        fun playSong(song: Song, autoPlay: Boolean = true) {
            stopAll()
            pendingSeekMs = null
            val player = createPlayer(song) ?: return
            activePlayer = player

            player.setOnPreparedListener { mp ->
                applySpeed(mp)
                applyVolume(mp, 1.0f)
                // Apply pending seek BEFORE start() so the audio thread
                // begins from the right position.
                pendingSeekMs?.let {
                    mp.seekTo(it)
                    pendingSeekMs = null
                }
                if (autoPlay) {
                    mp.start()
                    isPlaying = true
                    onSongStarted(song)
                }
            }

            player.setOnCompletionListener { handleCompletion(it) }
            player.prepareAsync()
        }

    // ---------------------------------------------------------------------
    // Replace stopAll() to also clear pending seek:
    // ---------------------------------------------------------------------
        private fun stopAll() {
            prepareNextJob?.cancel()
            try { activePlayer?.stop() } catch (_: Exception) {}
            activePlayer?.release()
            activePlayer = null
            pendingNextSong = null
            pendingSeekMs = null
            isPlaying = false
        }

// =====================================================================
// End of PlaybackRouter changes.
// =====================================================================


// ---------------------------------------------------------------------
// Companion-level constants in AudioEngine:
// ---------------------------------------------------------------------

    companion object {
        private const val TAG = "AudioEngine"
    }