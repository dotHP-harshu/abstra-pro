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
 * Handles dual MediaPlayer instances for Gapless and Crossfade playback.
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
    private var fadingPlayer: MediaPlayer? = null
    private var nextPlayer: MediaPlayer? = null
    private var nextSong: Song? = null
    
    private var crossfadeJob: Job? = null
    private var prepareNextJob: Job? = null
    
    var crossfadeSeconds: Int = 0
    var isGaplessEnabled: Boolean = true
    
    var globalVolume: Float = 1.0f
    var playbackSpeed: Float = 1.0f
    
    @Volatile
    private var isPlaying = false
    
    fun playSong(song: Song, autoPlay: Boolean = true) {
        stopAll()
        
        val player = createPlayer(song) ?: return
        activePlayer = player
        
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
        if (!isGaplessEnabled && crossfadeSeconds == 0) return
        
        prepareNextJob?.cancel()
        nextPlayer?.release()
        
        nextSong = song
        nextPlayer = createPlayer(song)
        nextPlayer?.setOnPreparedListener { mp ->
            applySpeed(mp)
            applyVolume(mp, 1.0f)
            
            // If just gapless, tell the active player about it
            if (isGaplessEnabled && crossfadeSeconds == 0) {
                try {
                    activePlayer?.setNextMediaPlayer(mp)
                } catch (e: Exception) {
                    Log.e("PlaybackRouter", "Error setting next media player", e)
                }
            }
        }
        
        nextPlayer?.setOnCompletionListener {
            handleCompletion(it)
        }
        
        nextPlayer?.prepareAsync()
    }
    
    fun checkCrossfade(currentPositionMs: Long, durationMs: Long) {
        if (crossfadeSeconds <= 0 || nextPlayer == null || !isPlaying) return
        
        val remainingMs = durationMs - currentPositionMs
        val crossfadeMs = crossfadeSeconds * 1000L
        
        // Start crossfade if we are within the crossfade window and haven't started fading yet
        if (remainingMs <= crossfadeMs && fadingPlayer == null) {
            startCrossfade()
        }
    }
    
    private fun startCrossfade() {
        val oldPlayer = activePlayer
        val newPlayer = nextPlayer
        
        if (oldPlayer == null || newPlayer == null) return
        
        fadingPlayer = oldPlayer
        activePlayer = newPlayer
        nextPlayer = null
        
        try {
            newPlayer.start()
            nextSong?.let { 
                onSeamlessTransition(it) 
                onSongStarted(it) 
            }
        } catch (e: Exception) {
            Log.e("PlaybackRouter", "Failed to start crossfade player", e)
            return
        }
        
        crossfadeJob?.cancel()
        crossfadeJob = scope.launch {
            val steps = 20
            val delayMs = (crossfadeSeconds * 1000L) / steps
            
            for (i in 0..steps) {
                if (!isActive) break
                val fadeOut = 1.0f - (i.toFloat() / steps)
                val fadeIn = (i.toFloat() / steps)
                
                applyVolume(fadingPlayer, fadeOut)
                applyVolume(activePlayer, fadeIn)
                
                delay(delayMs)
            }
            
            fadingPlayer?.stop()
            fadingPlayer?.release()
            fadingPlayer = null
            
            applyVolume(activePlayer, 1.0f)
            // We already told AudioEngine via onSeamlessTransition that we moved forward.
            // DO NOT call onSongCompleted(), it would cause AudioEngine to advance again.
        }
    }
    
    private fun handleCompletion(mp: MediaPlayer) {
        if (mp === fadingPlayer) {
            fadingPlayer?.release()
            fadingPlayer = null
            return
        }
        
        if (mp === activePlayer) {
            if (nextPlayer != null && isGaplessEnabled && crossfadeSeconds == 0) {
                // Gapless took over
                activePlayer?.release()
                activePlayer = nextPlayer
                nextPlayer = null
                nextSong?.let { 
                    onSeamlessTransition(it) 
                    onSongStarted(it)
                }
                // DONT call onSongCompleted here because we handled the transition seamlessly.
            } else {
                onSongCompleted()
            }
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
        fadingPlayer?.pause()
    }
    
    fun resume() {
        isPlaying = true
        activePlayer?.start()
        fadingPlayer?.start()
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
        applySpeed(fadingPlayer)
    }
    
    fun getCurrentPosition(): Long {
        return activePlayer?.currentPosition?.toLong() ?: 0L
    }
    
    fun isPlaying(): Boolean {
        return activePlayer?.isPlaying == true || fadingPlayer?.isPlaying == true
    }
    
    fun getAudioSessionId(): Int {
        return activePlayer?.audioSessionId ?: 0
    }
    
    private fun stopAll() {
        crossfadeJob?.cancel()
        prepareNextJob?.cancel()
        
        try { activePlayer?.stop() } catch (_: Exception) {}
        try { fadingPlayer?.stop() } catch (_: Exception) {}
        try { nextPlayer?.stop() } catch (_: Exception) {}
        
        activePlayer?.release()
        fadingPlayer?.release()
        nextPlayer?.release()
        
        activePlayer = null
        fadingPlayer = null
        nextPlayer = null
        
        isPlaying = false
    }
    
    fun stop() {
        stopAll()
    }
    
    fun release() {
        stopAll()
    }
}
