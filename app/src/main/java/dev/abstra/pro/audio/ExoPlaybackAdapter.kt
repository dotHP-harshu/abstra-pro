package dev.abstra.pro.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dev.abstra.pro.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ExoPlayer-backed playback engine implementing PlaybackPlayer.
 * Provides rock-solid gapless playback, precise seek transitions, and standard audio focus/offload.
 */
class ExoPlaybackAdapter(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onSongStarted: (Song) -> Unit,
    private val onSongCompleted: () -> Unit,
    private val onSeamlessTransition: (Song) -> Unit,
    private val onError: () -> Unit,
    private val onIsPlayingChanged: ((Boolean) -> Unit)? = null,
    private val onAudioSessionIdChanged: ((Int) -> Unit)? = null
) : PlaybackPlayer {

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            /* handleAudioFocus = */ false // AudioEngine manages fine-grained ducking & policies
        )
        .build()

    override var isGaplessEnabled: Boolean = true
    override var leftVolume: Float = 1.0f
    override var rightVolume: Float = 1.0f
    override var globalVolume: Float = 1.0f
    override var playbackSpeed: Float = 1.0f

    private var activeSong: Song? = null
    private var pendingNextSong: Song? = null
    private var startedMediaId: String? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val current = activeSong
                        if (current != null && player.playWhenReady && startedMediaId != current.id) {
                            startedMediaId = current.id
                            scope.launch(Dispatchers.Main) {
                                onSongStarted(current)
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        scope.launch(Dispatchers.Main) {
                            onSongCompleted()
                        }
                    }
                    else -> {}
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0) {
                    scope.launch(Dispatchers.Main) {
                        onAudioSessionIdChanged?.invoke(audioSessionId)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val effectivePlaying = isPlaying || (player.playWhenReady && player.playbackState == Player.STATE_BUFFERING)
                scope.launch(Dispatchers.Main) {
                    onIsPlayingChanged?.invoke(effectivePlaying)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    // Remove already played items behind current position
                    val currentIndex = player.currentMediaItemIndex
                    if (currentIndex > 0) {
                        player.removeMediaItems(0, currentIndex)
                    }

                    val next = pendingNextSong
                    if (next != null && isGaplessEnabled) {
                        activeSong = next
                        pendingNextSong = null
                        startedMediaId = next.id
                        scope.launch(Dispatchers.Main) {
                            onSeamlessTransition(next)
                            onSongStarted(next)
                        }
                    } else {
                        startedMediaId = null
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("ExoPlaybackAdapter", "Player error: ${error.errorCodeName}", error)
                scope.launch(Dispatchers.Main) {
                    onError()
                }
            }
        })
    }

    override fun playSong(song: Song, autoPlay: Boolean, initialPositionMs: Long) {
        activeSong = song
        pendingNextSong = null
        startedMediaId = null

        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(Uri.parse(song.uriString))
            .build()

        player.setMediaItem(mediaItem)
        applySpeed()
        applyVolume()

        if (initialPositionMs > 0) {
            player.seekTo(initialPositionMs)
        }

        player.prepare()
        player.playWhenReady = autoPlay
    }

    override fun prepareNextTrack(song: Song) {
        if (!isGaplessEnabled) return
        val currentIdx = player.currentMediaItemIndex
        val nextIdx = currentIdx + 1

        // Check if next item is already correctly enqueued
        if (player.mediaItemCount > nextIdx) {
            val existingItem = player.getMediaItemAt(nextIdx)
            if (existingItem.mediaId == song.id) {
                pendingNextSong = song
                return
            }
            // Remove incorrect trailing items
            player.removeMediaItems(nextIdx, player.mediaItemCount)
        }

        pendingNextSong = song
        val mediaItem = MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(Uri.parse(song.uriString))
            .build()

        player.addMediaItem(mediaItem)
    }

    override fun clearPendingNextTrack() {
        pendingNextSong = null
        val currentIdx = player.currentMediaItemIndex
        val nextIdx = currentIdx + 1
        if (player.mediaItemCount > nextIdx) {
            player.removeMediaItems(nextIdx, player.mediaItemCount)
        }
    }

    override fun pause() {
        player.playWhenReady = false
    }

    override fun resume() {
        player.playWhenReady = true
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun setVolume(volume: Float) {
        globalVolume = volume
        applyVolume()
    }

    override fun setSpeed(speed: Float) {
        playbackSpeed = speed
        applySpeed()
    }

    private fun applyVolume() {
        val avgPan = ((leftVolume + rightVolume) / 2f).coerceIn(0f, 1f)
        val finalVolume = (globalVolume * avgPan).coerceIn(0f, 1f)
        player.volume = finalVolume
    }

    private fun applySpeed() {
        val params = PlaybackParameters(playbackSpeed, playbackSpeed)
        player.playbackParameters = params
    }

    override fun getCurrentPosition(): Long {
        return player.currentPosition.coerceAtLeast(0L)
    }

    override fun isPlaying(): Boolean {
        return player.isPlaying
    }

    override fun getAudioSessionId(): Int {
        return player.audioSessionId
    }

    override fun stop() {
        player.stop()
        player.clearMediaItems()
        activeSong = null
        pendingNextSong = null
    }

    override fun release() {
        player.stop()
        player.clearMediaItems()
        player.release()
        activeSong = null
        pendingNextSong = null
    }
}
