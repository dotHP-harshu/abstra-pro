package dev.abstra.pro.audio.synth

import dev.abstra.pro.audio.PlaybackPlayer
import dev.abstra.pro.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * PlaybackPlayer adapter for the procedural ambient audio generator.
 * Encapsulates all demo_* audio generation cleanly behind PlaybackPlayer,
 * decoupling synthetic music generation from the core AudioEngine pipeline.
 */
class ProceduralSynthPlayer(
    private val scope: CoroutineScope,
    private val onSongStarted: (Song) -> Unit,
    private val onSongCompleted: () -> Unit
) : PlaybackPlayer {

    private val generator = ProceduralAudioGenerator()
    private var activeSong: Song? = null
    @Volatile
    private var currentPos: Long = 0L
    @Volatile
    private var playing: Boolean = false

    override var isGaplessEnabled: Boolean = false
    override var leftVolume: Float = 1.0f
    override var rightVolume: Float = 1.0f
    override var globalVolume: Float = 1.0f
    override var playbackSpeed: Float = 1.0f

    override fun playSong(song: Song, autoPlay: Boolean, initialPositionMs: Long) {
        activeSong = song
        currentPos = initialPositionMs
        generator.setSpeed(playbackSpeed)
        applyVolume()

        if (autoPlay) {
            playing = true
            generator.startPlayback(
                trackId = song.id,
                initialPositionMs = initialPositionMs,
                onProgressUpdate = { pos -> currentPos = pos },
                onCompletion = {
                    playing = false
                    scope.launch(Dispatchers.Main) {
                        onSongCompleted()
                    }
                }
            )
            onSongStarted(song)
        } else {
            playing = false
            generator.stop()
        }
    }

    override fun prepareNextTrack(song: Song) {
        // Procedural synth generates continuous real-time audio; pre-buffering not needed
    }

    override fun clearPendingNextTrack() {}

    override fun pause() {
        playing = false
        generator.pause()
    }

    override fun resume() {
        val s = activeSong ?: return
        playing = true
        generator.startPlayback(
            trackId = s.id,
            initialPositionMs = currentPos,
            onProgressUpdate = { pos -> currentPos = pos },
            onCompletion = {
                playing = false
                scope.launch(Dispatchers.Main) {
                    onSongCompleted()
                }
            }
        )
    }

    override fun seekTo(positionMs: Long) {
        currentPos = positionMs.coerceAtLeast(0L)
        val s = activeSong
        if (s != null && playing) {
            generator.startPlayback(
                trackId = s.id,
                initialPositionMs = currentPos,
                onProgressUpdate = { pos -> currentPos = pos },
                onCompletion = {
                    playing = false
                    scope.launch(Dispatchers.Main) {
                        onSongCompleted()
                    }
                }
            )
        }
    }

    override fun setVolume(volume: Float) {
        globalVolume = volume
        applyVolume()
    }

    override fun setSpeed(speed: Float) {
        playbackSpeed = speed
        generator.setSpeed(speed)
    }

    private fun applyVolume() {
        val avgPan = ((leftVolume + rightVolume) / 2f).coerceIn(0f, 1f)
        val finalVol = (globalVolume * avgPan).coerceIn(0f, 1f)
        generator.setVolume(finalVol)
    }

    override fun getCurrentPosition(): Long = currentPos

    override fun isPlaying(): Boolean = playing

    override fun getAudioSessionId(): Int = 0

    override fun stop() {
        playing = false
        generator.stop()
    }

    override fun release() {
        playing = false
        generator.release()
    }
}
