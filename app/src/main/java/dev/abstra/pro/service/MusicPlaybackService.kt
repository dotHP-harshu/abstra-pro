package dev.abstra.pro.service

import android.app.Notification
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
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.abstra.pro.MainActivity
import dev.abstra.pro.R
import dev.abstra.pro.audio.AudioEngine
import dev.abstra.pro.ui.screens.AvailableRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.sample

@UnstableApi
class MusicPlaybackService : MediaSessionService() {
    lateinit var audioEngine: AudioEngine
        private set

    private var mediaSession: MediaSession? = null
    private var invalidatePlayerState: (() -> Unit)? = null

    private val _availableRoutes = MutableStateFlow<List<AvailableRoute>>(emptyList())
    val availableRoutes: StateFlow<List<AvailableRoute>> = _availableRoutes.asStateFlow()

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefs by lazy { applicationContext.getSharedPreferences("abstra_pro", Context.MODE_PRIVATE) }
    private val CHANNEL_ID = "abstra_pro_playback_channel"
    private val NOTIFICATION_ID = 1001
    private var progressTickerJob: Job? = null
    private var hasPostedFirstNotification = false

    companion object {
        const val ACTION_PLAY = "dev.abstra.pro.ACTION_PLAY"
        const val ACTION_PAUSE = "dev.abstra.pro.ACTION_PAUSE"
        const val ACTION_NEXT = "dev.abstra.pro.ACTION_NEXT"
        const val ACTION_PREVIOUS = "dev.abstra.pro.ACTION_PREVIOUS"
        const val ACTION_STOP = "dev.abstra.pro.ACTION_STOP"
        const val ACTION_VOLUME = "dev.abstra.pro.ACTION_VOLUME"
        const val ACTION_SEEK = "dev.abstra.pro.ACTION_SEEK"
        const val ACTION_UPDATE_WIDGET = "dev.abstra.pro.ACTION_UPDATE_WIDGET"

        @Volatile var instance: MusicPlaybackService? = null
            private set

        /**
         * Global accessor for the singleton AudioEngine owned by the running service.
         * Returns null until the service has been started and onCreate has run.
         */
        val audioEngine: AudioEngine?
            get() = instance?.audioEngine
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioEngine = AudioEngine(this)
        
        // Apply any persisted toggle values to the engine immediately.
        val bg = prefs.getBoolean("background_playback", true)
        val share = prefs.getBoolean("share_audio_focus", false)
        audioEngine.setBackgroundPlaybackEnabled(bg)
        audioEngine.setShareAudioFocusEnabled(share)

        createNotificationChannel()
        buildMediaSession()

        // Defer first notification — gives MediaSession time to publish metadata.
        serviceScope.launch {
            delay(300)
            updateNotification()
        }

        serviceScope.launch {
            _availableRoutes.value = listOf(AvailableRoute("default", "System Default", true))
        }
        serviceScope.launch {
            audioEngine.currentSong.collect { 
                updateNotification()
                broadcastWidgetUpdate()
                invalidatePlayerState?.invoke()
            }
        }
        serviceScope.launch {
            audioEngine.isPlaying.collect { 
                updateNotification()
                broadcastWidgetUpdate()
                invalidatePlayerState?.invoke()
            }
        }
        serviceScope.launch {
            var lastInvalidatedAt = 0L
            audioEngine.currentPosition.sample(50).collect {
                if (audioEngine.isPlaying.value && it - lastInvalidatedAt > 250) {
                    invalidatePlayerState?.invoke()
                    lastInvalidatedAt = it
                }
            }
        }
        startProgressTicker()
    }

    private fun buildMediaSession() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        try {
            mediaSession = MediaSession.Builder(this, createPlayerAdapter())
                .setSessionActivity(pendingIntent)
                .setCallback(object : MediaSession.Callback {
                    override fun onAddMediaItems(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        mediaItems: MutableList<MediaItem>
                    ): ListenableFuture<MutableList<MediaItem>> =
                        Futures.immediateFuture(mediaItems)

                    override fun onConnect(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo
                    ): MediaSession.ConnectionResult {
                        // A system controller (Quick Settings tile, lock screen, etc.)
                        // just bound. The session token is now non-null — re-post the
                        // notification so the system picks up our metadata.
                        serviceScope.launch {
                            delay(150)
                            updateNotification()
                        }
                        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                            .setAvailablePlayerCommands(
                                MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                            )
                            .build()
                    }
                })
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> audioEngine.resume()
            ACTION_PAUSE -> audioEngine.pause()
            ACTION_NEXT -> audioEngine.playNext()
            ACTION_PREVIOUS -> audioEngine.playPrevious()
            ACTION_STOP -> {
                audioEngine.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE_WIDGET -> broadcastWidgetUpdate()
        }
        updateNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * Player adapter that mirrors AudioEngine state into Media3's player contract,
     * so the system Quick Settings tile reads our metadata and dispatches commands.
     */
    private fun createPlayerAdapter(): SimpleBasePlayer {
        return object : SimpleBasePlayer(android.os.Looper.getMainLooper()) {
            init {
                invalidatePlayerState = {
                    serviceScope.launch(Dispatchers.Main) { invalidateState() }
                }
            }

            override fun getState(): State {
                val song = audioEngine.currentSong.value
                val isPlaying = audioEngine.isPlaying.value
                val duration = audioEngine.duration.value.coerceAtLeast(1L)
                val position = audioEngine.currentPosition.value.coerceIn(0L, duration)

                val cmds = Player.Commands.Builder()
                    .addAll(
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SET_MEDIA_ITEM,
                        Player.COMMAND_SET_VOLUME,
                        Player.COMMAND_STOP
                    ).build()

                val builder = State.Builder()
                    .setAvailableCommands(cmds)
                    .setPlayWhenReady(isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                    .setPlaybackState(if (song != null) Player.STATE_READY else Player.STATE_IDLE)
                    .setVolume(audioEngine.currentVolume.value)

                if (song != null) {
                    val metadata = MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setIsPlayable(true)
                        .setIsBrowsable(false)
                        .setArtworkUri(
                            song.uriString.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                        )
                        .build()

                    // The metadata is set on the MediaItem itself.
                    // builder.setMediaMetadata(metadata) // REMOVED: Unresolved reference

                    val mediaItem = MediaItem.Builder()
                        .setMediaId(song.id)
                        .setMediaMetadata(metadata)
                        .setUri(song.uriString.takeIf { it.isNotBlank() }?.let { Uri.parse(it) })
                        .build()

                    builder.setPlaylist(
                        listOf(
                            MediaItemData.Builder(song.id)
                                .setMediaItem(mediaItem)
                                .setDurationUs(song.durationMs * 1000L)
                                .build()
                        )
                    )
                    builder.setContentPositionMs(PositionSupplier { audioEngine.currentPosition.value })
                }

                return builder.build()
            }

            override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
                if (playWhenReady) audioEngine.resume() else audioEngine.pause()
                return Futures.immediateVoidFuture()
            }

            override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
                when (seekCommand) {
                    Player.COMMAND_SEEK_TO_NEXT -> audioEngine.playNext()
                    Player.COMMAND_SEEK_TO_PREVIOUS -> audioEngine.playPrevious()
                    else -> if (positionMs >= 0) audioEngine.seekTo(positionMs)
                }
                return Futures.immediateVoidFuture()
            }

            override fun handleSetVolume(volume: Float): ListenableFuture<*> {
                audioEngine.setVolume(volume.coerceIn(0f, 1f))
                return Futures.immediateVoidFuture()
            }

            override fun handleStop(): ListenableFuture<*> {
                audioEngine.pause()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return Futures.immediateVoidFuture()
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
                description = "Custom media controls for the current track"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val song = audioEngine.currentSong.value
        val isPlaying = audioEngine.isPlaying.value
        val duration = audioEngine.duration.value.coerceAtLeast(1L)
        val position = audioEngine.currentPosition.value.coerceIn(0L, duration)
        val progress = (position * 1000L / duration).toInt()

        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val prevIntent = makeServiceIntent(ACTION_PREVIOUS, 1)
        val playPauseIntent = makeServiceIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY, 2)
        val nextIntent = makeServiceIntent(ACTION_NEXT, 3)
        val stopIntent = makeServiceIntent(ACTION_STOP, 4)
        val volumeIntent = makeActivityIntentForVolume(5)
        val albumArt: Bitmap? = song?.uriString
            ?.takeIf { it.isNotBlank() && !it.startsWith("demo_") }
            ?.let { loadAlbumArt(it) }

        val expanded = RemoteViews(packageName, R.layout.notification_media_panel).apply {
            setTextViewText(R.id.title, song?.title ?: "Abstra Pro")
            setTextViewText(R.id.artist, song?.artist ?: "Unknown Artist")
            setTextViewText(R.id.album, song?.album ?: "")
            setTextViewText(R.id.position, formatMs(position))
            setTextViewText(R.id.duration, formatMs(duration))
            setProgressBar(R.id.progress, 1000, progress, false)
            if (albumArt != null) setImageViewBitmap(R.id.album_art, albumArt)
            setImageViewResource(
                R.id.play_pause,
                if (isPlaying) R.drawable.ic_notify_pause else R.drawable.ic_notify_play
            )
            setOnClickPendingIntent(R.id.prev, prevIntent)
            setOnClickPendingIntent(R.id.play_pause, playPauseIntent)
            setOnClickPendingIntent(R.id.next, nextIntent)
            setOnClickPendingIntent(R.id.close, stopIntent)
            setOnClickPendingIntent(R.id.volume, volumeIntent)
        }

        val compact = RemoteViews(packageName, R.layout.notification_media_panel).apply {
            setTextViewText(R.id.title, song?.title ?: "Abstra Pro")
            setTextViewText(R.id.artist, song?.artist ?: "")
            setImageViewResource(
                R.id.play_pause,
                if (isPlaying) R.drawable.ic_notify_pause else R.drawable.ic_notify_play
            )
            setProgressBar(R.id.progress, 1000, progress, false)
            setOnClickPendingIntent(R.id.play_pause, playPauseIntent)
            setOnClickPendingIntent(R.id.prev, prevIntent)
            setOnClickPendingIntent(R.id.next, nextIntent)
            setTextViewText(R.id.duration, formatMs(duration))
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(song?.title ?: "Abstra Pro")
            .setContentText(song?.artist ?: "")
            .setSubText(song?.album)
            .setLargeIcon(albumArt)
            .setContentIntent(openIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_notify_skip_prev, "Previous", prevIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_notify_pause else R.drawable.ic_notify_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(R.drawable.ic_notify_skip_next, "Next", nextIntent)
            .addAction(R.drawable.ic_notify_close, "Stop", stopIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopIntent)
                    .setMediaSession(mediaSession?.sessionCompatToken)
            )
            .setCustomBigContentView(expanded)
            .setCustomContentView(compact)

        try {
            startForeground(NOTIFICATION_ID, builder.build())
            hasPostedFirstNotification = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun makeServiceIntent(action: String, requestCode: Int): PendingIntent {
        return PendingIntent.getService(
            this, requestCode,
            Intent(this, MusicPlaybackService::class.java).apply { this.action = action },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun makeActivityIntentForVolume(requestCode: Int): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("open_volume", true)
        }
        return PendingIntent.getActivity(
            this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = serviceScope.launch {
            while (true) {
                delay(250L)
                // Push fresh state to the system so the QS tile, lockscreen,
                // and notification slider all update.
                invalidatePlayerState?.invoke()
                if (audioEngine.isPlaying.value) {
                    updateNotification()
                }
            }
        }
    }

    private fun formatMs(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return String.format("%d:%02d", m, s)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val bg = prefs.getBoolean("background_playback", true)
        val song = audioEngine.currentSong.value
        val isPlaying = audioEngine.isPlaying.value
        if (bg && song != null && isPlaying) {
            // Keep the service alive; the user just swiped from recents.
            return
        }
        if (!isPlaying) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun loadAlbumArt(uriString: String): Bitmap? = try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this, Uri.parse(uriString))
        val art = retriever.embeddedPicture
        retriever.release()
        if (art != null) {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bmp = BitmapFactory.decodeByteArray(art, 0, art.size, opts) ?: return null
            Bitmap.createScaledBitmap(bmp, 256, 256, true)
        } else null
    } catch (_: Exception) { null }

    private fun broadcastWidgetUpdate() {
        sendBroadcast(Intent("dev.abstra.pro.WIDGET_UPDATE"))
    }

    override fun onDestroy() {
        super.onDestroy()
        progressTickerJob?.cancel()
        instance = null
        mediaSession?.release()
        audioEngine.release()
        serviceScope.cancel()
    }

    fun selectRoute(id: String) { /* reserved for future MediaRouter integration */ }
}
