package dev.abstra.pro.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import dev.abstra.pro.data.model.Song
import kotlinx.coroutines.*
import kotlin.math.max

/**
 * Handles dual MediaPlayer instances for Gapless playback.
 */
class PlaybackRouter(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSongStarted: (Song) -> Unit,
    private val onSongCompleted: () -> Unit,
    private val onSeamlessTransition: (Song) -> Unit,
    private val onError: () -> Unit
) {
    private var activePlayer: MediaPlayer? = null
    private var pendingNextSong: Song? = null
    
    private var prepareNextJob: Job? = null
    
    var isGaplessEnabled: Boolean = true
    
    var globalVolume: Float = 1.0f
    var playbackSpeed: Float = 1.0f
    
    @Volatile
    private var isPlaying = false
    
    fun playSong(song: Song, autoPlay: Boolean = true) {
        stopAll()
        
        val player = createPlayer(song) ?: return
        activePlayer = player
        pendingNextSong = null
        
        player.setOnPreparedListener { mp ->
            applySpeed(mp)
            applyVolume(mp, 1.0f)
            if (autoPlay) {
                mp.start()
                isPlaying = true
                onSongStarted(song)
            }
        }
        
        player.setOnCompletionListener {
            handleCompletion(it)
        }
        
        player.prepareAsync()
    }
    
    fun prepareNextTrack(song: Song) {
        if (!isGaplessEnabled) return
        pendingNextSong = song
    }
    

    
    private fun handleCompletion(mp: MediaPlayer) {
        if (mp !== activePlayer) return
        val next = pendingNextSong
        if (next != null && isGaplessEnabled) {
            pendingNextSong = null
            val oldPlayer = activePlayer
            scope.launch(Dispatchers.Main) {
                val newPlayer = createPlayer(next)
                if (newPlayer == null) {
                    oldPlayer?.release()
                    onSongCompleted()
                    return@launch
                }
                activePlayer = newPlayer
                newPlayer.setOnPreparedListener { mp2 ->
                    applySpeed(mp2)
                    applyVolume(mp2, 1.0f)
                    mp2.start()
                    isPlaying = true
                    oldPlayer?.release()
                    onSeamlessTransition(next)
                    onSongStarted(next)
                }
                newPlayer.setOnCompletionListener { handleCompletion(it) }
                newPlayer.prepareAsync()
            }
        } else {
            activePlayer?.release()
            activePlayer = null
            onSongCompleted()
        }
    }
    
    private fun createPlayer(song: Song): MediaPlayer? {
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(song.uriString))
                setOnErrorListener { _, _, _ ->
                    onError()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackRouter", "Failed to create player", e)
            null
        }
    }
    
    private fun applyVolume(mp: MediaPlayer?, localMultiplier: Float) {
        val v = (globalVolume * localMultiplier).coerceIn(0f, 1f)
        try { mp?.setVolume(v, v) } catch (_: Exception) {}
    }
    
    private fun applySpeed(mp: MediaPlayer?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mp?.let {
                    val params = it.playbackParams
                    params.speed = playbackSpeed
                    params.pitch = playbackSpeed
                    it.playbackParams = params
                }
            } catch (_: Exception) {}
        }
    }
    
    fun pause() {
        isPlaying = false
        activePlayer?.pause()
    }
    
    fun resume() {
        isPlaying = true
        activePlayer?.start()
    }
    
    fun seekTo(positionMs: Long) {
        activePlayer?.seekTo(positionMs.toInt())
    }
    
    fun setVolume(volume: Float) {
        globalVolume = volume
        applyVolume(activePlayer, 1.0f)
        // If we are crossfading, global volume is adjusted by the loop, but this sets base
    }
    
    fun setSpeed(speed: Float) {
        playbackSpeed = speed
        applySpeed(activePlayer)
    }
    
    fun getCurrentPosition(): Long {
        return activePlayer?.currentPosition?.toLong() ?: 0L
    }
    
    fun isPlaying(): Boolean {
        return activePlayer?.isPlaying == true
    }
    
    fun getAudioSessionId(): Int {
        return activePlayer?.audioSessionId ?: 0
    }
    
    private fun stopAll() {
        prepareNextJob?.cancel()
        
        try { activePlayer?.stop() } catch (_: Exception) {}
        
        activePlayer?.release()
        
        activePlayer = null
        pendingNextSong = null
        
        isPlaying = false
    }
    
    fun stop() {
        stopAll()
    }
    
    fun release() {
        stopAll()
    }
}
