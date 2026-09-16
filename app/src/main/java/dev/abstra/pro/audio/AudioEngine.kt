package dev.abstra.pro.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.util.Log
import dev.abstra.pro.audio.synth.ProceduralAudioGenerator
import dev.abstra.pro.data.model.RepeatMode
import dev.abstra.pro.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioEngine(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val effectsManager = AudioEffectsManager()
    private val proceduralSynth = ProceduralAudioGenerator()

    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    private var secondaryMediaPlayer: MediaPlayer? = null

    // ── Observable StateFlows ──
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(180000L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.REPEAT_ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(0)
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _gaplessEnabled = MutableStateFlow(true)
    val gaplessEnabled: StateFlow<Boolean> = _gaplessEnabled.asStateFlow()

    // ── Sleep Timer ──
    private val _sleepTimerSecondsRemaining = MutableStateFlow(0)
    val sleepTimerSecondsRemaining: StateFlow<Int> = _sleepTimerSecondsRemaining.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Callbacks for Telemetry & Stats logging
    var onSongStarted: ((Song) -> Unit)? = null
    var onSongCompleted: ((Song) -> Unit)? = null
    var onSongSkipped: ((Song, Long, Long) -> Unit)? = null
    var onPlaybackStateChange: ((Boolean) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var crossfadeJob: Job? = null

    private var audioFocusRequest: AudioFocusRequest? = null
    private var originalQueue: List<Song> = emptyList()
    private var lastPreviousClickTimeMs = 0L
    private var isInForeground = true

    val currentSessionId: Int get() = mediaPlayer?.audioSessionId ?: 0

    // Noisy broadcast receiver (smooth 200ms fade on headphone unplug)
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                smoothFadeAndPause()
            }
        }
    }

    init {
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        context.registerReceiver(noisyReceiver, filter)
        startProgressTracker()
    }

    fun setForegroundState(foreground: Boolean) {
        isInForeground = foreground
    }

    // ─────────────────────────────────────────────────────────────
    // QUEUE & PLAYBACK CONTROLS
    // ─────────────────────────────────────────────────────────────

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true) {
        if (songs.isEmpty()) return
        originalQueue = songs
        val safeIndex = startIndex.coerceIn(0, songs.size - 1)
        val effective = if (_isShuffle.value) shuffledQueue(songs, safeIndex) else songs
        _queue.value = effective
        _currentIndex.value = safeIndex
        playSong(effective[safeIndex], autoPlay)
    }

    fun enqueueNext(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        if (currentQueue.isEmpty()) {
            setQueue(listOf(song), 0, true)
            return
        }
        val insertAt = (_currentIndex.value + 1).coerceIn(0, currentQueue.size)
        currentQueue.add(insertAt, song)
        _queue.value = currentQueue
        originalQueue = if (_isShuffle.value) {
            val orig = originalQueue.toMutableList()
            orig.add(song)
            orig
        } else {
            currentQueue
        }
    }

    fun enqueueLast(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        if (currentQueue.isEmpty()) {
            setQueue(listOf(song), 0, true)
            return
        }
        currentQueue.add(song)
        _queue.value = currentQueue
        originalQueue = if (_isShuffle.value) {
            val orig = originalQueue.toMutableList()
            orig.add(song)
            orig
        } else {
            currentQueue
        }
    }

    fun removeQueueItem(song: Song) {
        val currentQueue = _queue.value.toMutableList()
        val idx = currentQueue.indexOf(song)
        if (idx == -1) return
        currentQueue.removeAt(idx)
        _queue.value = currentQueue
        originalQueue = originalQueue.filter { it.id != song.id }
        if (idx < _currentIndex.value) {
            _currentIndex.value -= 1
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (from !in currentQueue.indices || to !in currentQueue.indices) return
        val item = currentQueue.removeAt(from)
        currentQueue.add(to, item)
        _queue.value = currentQueue
        // simple approach for originalQueue
        originalQueue = currentQueue
    }

    /** Clears everything after the currently playing track ("Clear queue" button). */
    fun clearUpNext() {
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return
        val currentIdx = _currentIndex.value
        if (currentIdx in currentQueue.indices) {
            _queue.value = currentQueue.take(currentIdx + 1)
            originalQueue = _queue.value
        } else {
            _queue.value = emptyList()
            originalQueue = emptyList()
        }
    }

    fun playSong(song: Song, autoPlay: Boolean = true) {
        val prev = _currentSong.value
        if (prev != null && prev.id != song.id) {
            val playedSec = _currentPosition.value / 1000L
            val totalSec = _duration.value / 1000L
            if (playedSec < totalSec * 0.85) {
                onSongSkipped?.invoke(prev, playedSec, totalSec)
            }
        }

        _currentSong.value = song
        _currentPosition.value = 0L
        _duration.value = song.durationMs.coerceAtLeast(1000L)

        if (song.id.startsWith("demo_")) {
            // Use procedural generator
            stopNativePlayers()
            proceduralSynth.setSpeed(_playbackSpeed.value)
            applyBalance()
            proceduralSynth.startPlayback(
                trackId = song.id,
                initialPositionMs = 0L,
                onProgressUpdate = { pos -> _currentPosition.value = pos },
                onCompletion = { handleSongCompletion() }
            )
            _isPlaying.value = true
            onSongStarted?.invoke(song)
            onPlaybackStateChange?.invoke(true)
            return
        }

        // Native Media Player
        proceduralSynth.stop()
        if (!requestAudioFocus()) return

        try {
            stopNativePlayers()
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(song.uriString))
                prepare()
            }

            mediaPlayer = player

            // Attach hardware effects
            effectsManager.attachSession(player.audioSessionId)
            applyBalance()
            applySpeed()

            player.setOnCompletionListener {
                handleSongCompletion()
            }

            if (autoPlay) {
                player.start()
                _isPlaying.value = true
                onSongStarted?.invoke(song)
                onPlaybackStateChange?.invoke(true)
            } else {
                _isPlaying.value = false
                onPlaybackStateChange?.invoke(false)
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error playing song: ${song.title}", e)
            playNext()
        }
    }

    fun resume() {
        if (!requestAudioFocus()) return
        val song = _currentSong.value ?: return

        if (song.id.startsWith("demo_")) {
            proceduralSynth.startPlayback(
                trackId = song.id,
                initialPositionMs = _currentPosition.value,
                onProgressUpdate = { pos -> _currentPosition.value = pos },
                onCompletion = { handleSongCompletion() }
            )
            _isPlaying.value = true
            onPlaybackStateChange?.invoke(true)
            return
        }

        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            onPlaybackStateChange?.invoke(true)
        } catch (_: Exception) {}
    }

    fun pause() {
        _isPlaying.value = false
        onPlaybackStateChange?.invoke(false)
        proceduralSynth.pause()
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
    }

    fun seekTo(positionMs: Long) {
        val song = _currentSong.value ?: return
        _currentPosition.value = positionMs

        if (song.id.startsWith("demo_")) {
            if (_isPlaying.value) {
                proceduralSynth.startPlayback(
                    song.id, positionMs,
                    onProgressUpdate = { pos -> _currentPosition.value = pos },
                    onCompletion = { handleSongCompletion() }
                )
            }
            return
        }

        try {
            mediaPlayer?.seekTo(positionMs.toInt())
        } catch (_: Exception) {}
    }

    // ─────────────────────────────────────────────────────────────
    // SMART PREVIOUS & NEXT
    // ─────────────────────────────────────────────────────────────

    fun playPrevious() {
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
        val prevIdx = (_currentIndex.value - 1 + q.size) % q.size
        _currentIndex.value = prevIdx
        playSong(q[prevIdx])
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_repeatMode.value == RepeatMode.REPEAT_ONE) {
            seekTo(0L)
            resume()
            return
        }

        val nextIdx = _currentIndex.value + 1
        if (nextIdx >= q.size) {
            if (_repeatMode.value == RepeatMode.REPEAT_ALL) {
                _currentIndex.value = 0
                playSong(q[0])
            } else {
                pause()
                seekTo(0L)
            }
        } else {
            _currentIndex.value = nextIdx
            playSong(q[nextIdx])
        }
    }

    private fun handleSongCompletion() {
        val s = _currentSong.value
        if (s != null) {
            onSongCompleted?.invoke(s)
        }
        playNext()
    }

    // ─────────────────────────────────────────────────────────────
    // SHUFFLE & REPEAT
    // ─────────────────────────────────────────────────────────────

    fun toggleShuffle() {
        val current = _isShuffle.value
        _isShuffle.value = !current
        val activeSong = _currentSong.value
        if (!current) {
            // Shuffle
            val q = _queue.value
            val idx = _currentIndex.value
            val shuffled = shuffledQueue(q, idx)
            _queue.value = shuffled
            _currentIndex.value = shuffled.indexOfFirst { it.id == activeSong?.id }.coerceAtLeast(0)
        } else {
            // Restore original
            _queue.value = originalQueue
            _currentIndex.value = originalQueue.indexOfFirst { it.id == activeSong?.id }.coerceAtLeast(0)
        }
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.REPEAT_OFF
            RepeatMode.REPEAT_OFF -> RepeatMode.REPEAT_ALL
        }
    }

    private fun shuffledQueue(queue: List<Song>, currentIndex: Int): List<Song> {
        if (queue.size < 2) return queue
        val current = queue[currentIndex.coerceIn(0, queue.size - 1)]
        val rest = queue.filterIndexed { i, _ -> i != currentIndex }.shuffled()
        return rest.toMutableList().apply { add(currentIndex.coerceIn(0, size), current) }
    }

    // ─────────────────────────────────────────────────────────────
    // SPEED, PITCH & BALANCE
    // ─────────────────────────────────────────────────────────────

    private var crossfadeDurationSec: Int = 0
    private var isGaplessEnabled: Boolean = true

    fun setCrossfade(seconds: Int) {
        crossfadeDurationSec = seconds.coerceIn(0, 12)
    }

    fun setGapless(enabled: Boolean) {
        isGaplessEnabled = enabled
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.5f, 2.0f)
        applySpeed()
        proceduralSynth.setSpeed(speed)
    }

    private fun applySpeed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let {
                    val params = it.playbackParams
                    params.speed = _playbackSpeed.value
                    params.pitch = _playbackSpeed.value
                    it.playbackParams = params
                }
            } catch (_: Exception) {}
        }
    }

    fun setBalancePan(pan: Float) {
        effectsManager.setBalancePan(pan)
        applyBalance()
    }

    private fun applyBalance() {
        val (left, right) = effectsManager.getChannelVolumes()
        try {
            mediaPlayer?.setVolume(left, right)
            proceduralSynth.setVolume((left + right) / 2f)
        } catch (_: Exception) {}
    }

    // ─────────────────────────────────────────────────────────────
    // DISCONNECT FADE & SLEEP TIMER
    // ─────────────────────────────────────────────────────────────

    private fun smoothFadeAndPause() {
        scope.launch {
            val (origL, origR) = effectsManager.getChannelVolumes()
            val steps = 10
            for (i in steps downTo 0) {
                val factor = i.toFloat() / steps
                try {
                    mediaPlayer?.setVolume(origL * factor, origR * factor)
                } catch (_: Exception) {}
                delay(20)
            }
            pause()
            applyBalance()
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSecondsRemaining.value = 0
            return
        }

        _sleepTimerSecondsRemaining.value = minutes * 60
        sleepTimerJob = scope.launch {
            while (_sleepTimerSecondsRemaining.value > 0) {
                delay(1000)
                _sleepTimerSecondsRemaining.value -= 1
            }
            smoothFadeAndPause()
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PROGRESS TRACKING & BATTERY OPTIMIZATION
    // ─────────────────────────────────────────────────────────────

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (_isPlaying.value && mediaPlayer != null) {
                    try {
                        _currentPosition.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    } catch (_: Exception) {}
                }
                val delayMs = if (isInForeground) 200L else 1000L
                delay(delayMs)
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focus ->
        when (focus) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_GAIN -> resume()
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
            }
            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun stopNativePlayers() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun release() {
        try {
            context.unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}
        stopNativePlayers()
        proceduralSynth.release()
        effectsManager.releaseAll()
        scope.cancel()
    }
}
