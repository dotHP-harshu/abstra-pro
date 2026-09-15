package dev.abstra.pro.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dev.abstra.pro.MainActivity
import dev.abstra.pro.R
import dev.abstra.pro.audio.AudioEngine
import kotlinx.coroutines.*
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class MusicPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    lateinit var audioEngine: AudioEngine
        private set

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val CHANNEL_ID = "abstra_pro_playback_channel"
    private val NOTIFICATION_ID = 1001

    companion object {
        const val ACTION_PLAY = "dev.abstra.pro.ACTION_PLAY"
        const val ACTION_PAUSE = "dev.abstra.pro.ACTION_PAUSE"
        const val ACTION_NEXT = "dev.abstra.pro.ACTION_NEXT"
        const val ACTION_PREVIOUS = "dev.abstra.pro.ACTION_PREVIOUS"
        const val ACTION_UPDATE_WIDGET = "dev.abstra.pro.ACTION_UPDATE_WIDGET"

        @Volatile
        var instance: MusicPlaybackService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioEngine = AudioEngine(this)

        createNotificationChannel()

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build MediaSession
        try {
            val sessionCallback = object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    return com.google.common.util.concurrent.Futures.immediateFuture(mediaItems)
                }
            }

            // Simple media session wrapper
            mediaSession = MediaSession.Builder(this, createPlayerAdapter())
                .setSessionActivity(pendingIntent)
                .setCallback(sessionCallback)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Listen for track & playback state changes to update notification
        serviceScope.launch {
            audioEngine.currentSong.collect { song ->
                updateNotification()
                broadcastWidgetUpdate()
            }
        }

        serviceScope.launch {
            audioEngine.isPlaying.collect { isPlaying ->
                updateNotification()
                broadcastWidgetUpdate()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> audioEngine.resume()
            ACTION_PAUSE -> audioEngine.pause()
            ACTION_NEXT -> audioEngine.playNext()
            ACTION_PREVIOUS -> audioEngine.playPrevious()
            ACTION_UPDATE_WIDGET -> broadcastWidgetUpdate()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun createPlayerAdapter(): androidx.media3.common.SimpleBasePlayer {
        return object : androidx.media3.common.SimpleBasePlayer(android.os.Looper.getMainLooper()) {
            override fun getState(): State {
                val song = audioEngine.currentSong.value
                val isPlaying = audioEngine.isPlaying.value

                val availableCmds = Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_DEFAULT_POSITION
                    ).build()

                val b = State.Builder()
                    .setAvailableCommands(availableCmds)
                    .setPlayWhenReady(isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                    .setPlaybackState(if (song != null) Player.STATE_READY else Player.STATE_IDLE)

                if (song != null) {
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(song.id)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setArtist(song.artist)
                                .setAlbumTitle(song.album)
                                .build()
                        )
                        .build()

                    val itemData = MediaItemData.Builder(song.id)
                        .setMediaItem(mediaItem)
                        .setDurationUs(song.durationMs * 1000L)
                        .build()

                    b.setPlaylist(listOf(itemData))
                    b.setContentPositionMs(PositionSupplier { audioEngine.currentPosition.value })
                }
                return b.build()
            }

            override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
                if (playWhenReady) audioEngine.resume() else audioEngine.pause()
                return com.google.common.util.concurrent.Futures.immediateVoidFuture()
            }

            override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
                when (seekCommand) {
                    Player.COMMAND_SEEK_TO_NEXT -> audioEngine.playNext()
                    Player.COMMAND_SEEK_TO_PREVIOUS -> audioEngine.playPrevious()
                    else -> if (positionMs >= 0) audioEngine.seekTo(positionMs)
                }
                return com.google.common.util.concurrent.Futures.immediateVoidFuture()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls and active track details"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val song = audioEngine.currentSong.value
        val isPlaying = audioEngine.isPlaying.value

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicPlaybackService::class.java).apply {
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 3,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE
        )

        val albumArtBitmap = loadAlbumArt(song?.uriString)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(song?.title ?: "Abstra Pro")
            .setContentText(song?.artist ?: "No track playing")
            .setSubText(song?.album)
            .setLargeIcon(albumArtBitmap)
            .setContentIntent(openIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_skip_previous, "Previous", prevIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(R.drawable.ic_skip_next, "Next", nextIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )

        startForeground(NOTIFICATION_ID, builder.build())
    }

    private fun loadAlbumArt(uriString: String?): Bitmap? {
        if (uriString.isNullOrBlank() || uriString.startsWith("demo_")) return null
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, Uri.parse(uriString))
            val art = retriever.embeddedPicture
            retriever.release()
            if (art != null) {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeByteArray(art, 0, art.size, opts)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun broadcastWidgetUpdate() {
        val intent = Intent("dev.abstra.pro.WIDGET_UPDATE")
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        mediaSession?.release()
        audioEngine.release()
        serviceScope.cancel()
    }
}
