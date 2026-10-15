package dev.abstra.pro.audio

import dev.abstra.pro.data.model.Song

/**
 * Common playback controller interface decoupling high-level AudioEngine
 * from the underlying player implementation (MediaPlayer vs ExoPlayer).
 */
interface PlaybackPlayer {
    var isGaplessEnabled: Boolean
    var leftVolume: Float
    var rightVolume: Float
    var globalVolume: Float
    var playbackSpeed: Float

    fun playSong(song: Song, autoPlay: Boolean = true, initialPositionMs: Long = 0L)
    fun prepareNextTrack(song: Song)
    fun clearPendingNextTrack()
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float)
    fun setSpeed(speed: Float)
    fun getCurrentPosition(): Long
    fun isPlaying(): Boolean
    fun getAudioSessionId(): Int
    fun stop()
    fun release()
}
