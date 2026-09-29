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
    private var playbackRouter: PlaybackRouter? = null

    private val consecutiveFailures = java.util.concurrent.atomic.AtomicInteger(0)

    // ── Observable StateFlows ──
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _pendingSong = MutableStateFlow<Song?>(null)
    val pendingSong: StateFlow<Song?> = _pendingSong.asStateFlow()

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

    private val prefs = context.getSharedPreferences("abstra_prefs", Context.MODE_PRIVATE)

    private val _repeatMode = MutableStateFlow(
        RepeatMode.valueOf(prefs.getString("repeat_mode", RepeatMode.REPEAT_ALL.name) ?: RepeatMode.REPEAT_ALL.name)
    )
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(prefs.getBoolean("shuffle_enabled", false))
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(0)
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _gaplessEnabled = MutableStateFlow(true)
    val gaplessEnabled: StateFlow<Boolean> = _gaplessEnabled.asStateFlow()

    private val _currentVolume = MutableStateFlow(1.0f)
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()

    fun setVolume(v: Float) {
        val clamped = v.coerceIn(0f, 1f)
        _currentVolume.value = clamped
        try { playbackRouter?.setVolume(clamped) } catch (_: Exception) {}
        proceduralSynth.setVolume(clamped)
    }

    private val _backgroundPlaybackEnabled = MutableStateFlow(
        prefs.getBoolean("background_playback", true)
    )
    val backgroundPlaybackEnabled: StateFlow<Boolean> = _backgroundPlaybackEnabled.asStateFlow()

    private val _shareAudioFocusEnabled = MutableStateFlow(
        prefs.getBoolean("share_audio_focus", false)
    )
    val shareAudioFocusEnabled: StateFlow<Boolean> = _shareAudioFocusEnabled.asStateFlow()

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("background_playback", enabled).apply()
        _backgroundPlaybackEnabled.value = enabled
    }

    fun setShareAudioFocusEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("share_audio_focus", enabled).apply()
        _shareAudioFocusEnabled.value = enabled
        applyFocusPolicy()
    }

    fun applyFocusPolicy() {
        // Abandons and re-acquires focus so the new policy takes effect immediately.
        abandonAudioFocus()
        if (_currentSong.value != null) requestAudioFocus()
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

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

    val currentSessionId: Int get() = playbackRouter?.getAudioSessionId() ?: 0

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
        
        playbackRouter = PlaybackRouter(
            context = context,
            scope = scope,
            onSongStarted = { s -> 
                _isPlaying.value = true
                onSongStarted?.invoke(s)
                onPlaybackStateChange?.invoke(true)
            },
            onSongCompleted = {
                scope.launch { handleSongCompletion() }
            },
            onSeamlessTransition = { song ->
                // PlaybackRouter handled gapless/crossfade, so we just update our state 
                // to match the new song, WITHOUT triggering a new play request.
                _currentSong.value = song
                _duration.value = song.durationMs
                val q = _queue.value
                val newIdx = q.indexOfFirst { it.id == song.id }
                if (newIdx != -1) {
                    _currentIndex.value = newIdx
                }
            },
            onError = {
                _isPlaying.value = false
                scope.launch { delay(400); playNext() }
            }
        )
        
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

    /** Clears the entire queue including the currently playing track, and stops playback. */
    fun clearEntireQueue() {
        _queue.value = emptyList()
        originalQueue = emptyList()
        _currentIndex.value = 0
        pause()
        _shouldBePlaying = false
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

        _pendingSong.value = song
        _currentPosition.value = 0L
        _duration.value = song.durationMs.coerceAtLeast(1000L)
        _isPlaying.value = false
        _shouldBePlaying = autoPlay

        if (song.id.startsWith("demo_")) {
            _currentSong.value = song
            // Use procedural generator
            stopNativePlayers()
            proceduralSynth.setSpeed(_playbackSpeed.value)
            applyBalance()
            proceduralSynth.startPlayback(
                trackId = song.id,
                initialPositionMs = 0L,
                onProgressUpdate = { pos -> _currentPosition.value = pos },
                onCompletion = { scope.launch { handleSongCompletion() } }
            )
            _isPlaying.value = true
            onSongStarted?.invoke(song)
            onPlaybackStateChange?.invoke(true)
            return
        }

        // Native Media Player
        proceduralSynth.stop()
        if (!requestAudioFocus()) return

        stopNativePlayers()
        consecutiveFailures.set(0)
        
        val router = playbackRouter ?: return
        
        _currentSong.value = song
        
        router.setVolume(_currentVolume.value)
        router.setSpeed(_playbackSpeed.value)
        router.crossfadeSeconds = _crossfadeSeconds.value
        router.isGaplessEnabled = _gaplessEnabled.value
        
        router.playSong(song, autoPlay)
        
        if (!autoPlay) {
            _isPlaying.value = false
            onPlaybackStateChange?.invoke(false)
        }
        
        // Let the eq attach when ready. We poll occasionally since router hides inner players
        scope.launch {
            delay(300)
            effectsManager.attachSession(router.getAudioSessionId())
        }
    }

    fun resume() {
        if (!requestAudioFocus()) return
        val current = _currentSong.value
        val pending = _pendingSong.value
        if (current == null && pending == null) return

        if (pending != null && current?.id != pending.id) {
            Log.d("AudioEngine", "resume ignored: song ${pending.title} is still preparing")
            return
        }

        val song = current ?: return

        if (song.id.startsWith("demo_")) {
            proceduralSynth.startPlayback(
                trackId = song.id,
                initialPositionMs = _currentPosition.value,
                onProgressUpdate = { pos -> _currentPosition.value = pos },
                onCompletion = { scope.launch { handleSongCompletion() } }
            )
            _isPlaying.value = true
            onPlaybackStateChange?.invoke(true)
            return
        }

        try {
            playbackRouter?.resume()
            _isPlaying.value = true
            onPlaybackStateChange?.invoke(true)
        } catch (_: Exception) {}
    }

    fun pause() {
        _shouldBePlaying = false
        _isPlaying.value = false
        onPlaybackStateChange?.invoke(false)
        proceduralSynth.pause()
        try {
            playbackRouter?.pause()
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
                    onCompletion = { scope.launch { handleSongCompletion() } }
                )
            }
            return
        }

        try {
            playbackRouter?.seekTo(positionMs)
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

        if (consecutiveFailures.get() > q.size) {
            _isPlaying.value = false
            consecutiveFailures.set(0)
            return
        }

        if (_repeatMode.value == RepeatMode.REPEAT_ONE) {
            seekTo(0L)
            resume()
            return
        }

        val nextIdx = _currentIndex.value + 1
        if (nextIdx >= q.size) {
            if (_repeatMode.value == RepeatMode.REPEAT_ALL) {
                _currentIndex.value = 0
                consecutiveFailures.incrementAndGet()
                playSong(q[0])
            } else {
                pause()
                seekTo(0L)
                consecutiveFailures.set(0)
            }
        } else {
            _currentIndex.value = nextIdx
            consecutiveFailures.incrementAndGet()
            playSong(q[nextIdx])
        }
    }

    private suspend fun handleSongCompletion() {
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
        val newVal = !current
        prefs.edit().putBoolean("shuffle_enabled", newVal).apply()
        _isShuffle.value = newVal
        val activeSong = _currentSong.value
        if (newVal) {
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
        val newMode = when (_repeatMode.value) {
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.REPEAT_OFF
            RepeatMode.REPEAT_OFF -> RepeatMode.REPEAT_ALL
        }
        prefs.edit().putString("repeat_mode", newMode.name).apply()
        _repeatMode.value = newMode
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
        val valid = seconds.coerceIn(0, 12)
        crossfadeDurationSec = valid
        _crossfadeSeconds.value = valid
        playbackRouter?.crossfadeSeconds = valid
    }

    fun setGapless(enabled: Boolean) {
        isGaplessEnabled = enabled
        playbackRouter?.isGaplessEnabled = enabled
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.5f, 2.0f)
        applySpeed()
        proceduralSynth.setSpeed(speed)
    }

    private fun applySpeed() {
        playbackRouter?.setSpeed(_playbackSpeed.value)
    }

    fun setBalancePan(pan: Float) {
        effectsManager.setBalancePan(pan)
        applyBalance()
    }

    private fun applyBalance() {
        val (left, right) = effectsManager.getChannelVolumes()
        try {
            val v = (left + right) / 2f
            playbackRouter?.setVolume(v)
            proceduralSynth.setVolume(v)
        } catch (_: Exception) {}
    }

    // ─────────────────────────────────────────────────────────────
    // DISCONNECT FADE & SLEEP TIMER
    // ─────────────────────────────────────────────────────────────

    private fun smoothFadeAndPause() {
        scope.launch {
            val steps = 10
            for (i in steps downTo 0) {
                val factor = i.toFloat() / steps
                try {
                    playbackRouter?.setVolume(factor)
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
                if (playbackRouter != null) {
                    try {
                        val isActuallyPlaying = playbackRouter?.isPlaying() == true
                        if (_isPlaying.value && !isActuallyPlaying) {
                            _isPlaying.value = false
                            onPlaybackStateChange?.invoke(false)
                        } else if (!_isPlaying.value && isActuallyPlaying) {
                            _isPlaying.value = true
                            onPlaybackStateChange?.invoke(true)
                        }
                        if (isActuallyPlaying) {
                            val pos = playbackRouter?.getCurrentPosition() ?: 0L
                            _currentPosition.value = pos
                            playbackRouter?.checkCrossfade(pos, _duration.value)
                            
                            // Prepare next track for gapless/crossfade if needed
                            if (_duration.value > 0 && (_duration.value - pos) < 20000L) { // prepare 20s in advance
                                val q = _queue.value
                                val nextIdx = _currentIndex.value + 1
                                if (nextIdx < q.size) {
                                    playbackRouter?.prepareNextTrack(q[nextIdx])
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                val delayMs = if (isInForeground) 200L else 1000L
                delay(delayMs)
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focus ->
        when (focus) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // We got focus back. Restore volume and resume if user wanted playback.
                try { playbackRouter?.setVolume(1.0f) } catch (_: Exception) {}
                proceduralSynth.setVolume(1.0f)
                if (_shouldBePlaying) resume()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Another app wants us to duck (e.g., notification, voice message).
                // Always honor this — ducking is cheap and non-disruptive.
                try { playbackRouter?.setVolume(0.2f) } catch (_: Exception) {}
                proceduralSynth.setVolume(0.2f)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Another app wants temporary exclusive focus (e.g., phone call, voice note).
                if (_shareAudioFocusEnabled.value) {
                    // User wants us to share: duck instead of pausing.
                    try { playbackRouter?.setVolume(0.2f) } catch (_: Exception) {}
                    proceduralSynth.setVolume(0.2f)
                } else {
                    // Default: pause and remember intent to resume.
                    pause()
                    _shouldBePlaying = true
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // Another app wants permanent exclusive focus (e.g., YouTube, Spotify).
                if (_shareAudioFocusEnabled.value) {
                    // User wants us to share: ignore and keep playing.
                    // Both streams mix; user hears both. Set our volume to half so
                    // the user can still hear YouTube clearly.
                    try { playbackRouter?.setVolume(0.5f) } catch (_: Exception) {}
                    proceduralSynth.setVolume(0.5f)
                } else {
                    // Default: pause.
                    pause()
                    _shouldBePlaying = false
                }
            }
        }
    }

    @Volatile private var _shouldBePlaying = false

    private fun requestAudioFocus(): Boolean {
        // Music apps always request AUDIOFOCUS_GAIN (exclusive). The share setting
        // doesn't change what we ASK for — it changes how we RESPOND when another
        // app asks for focus and the system takes it from us.
        val focusGain = AudioManager.AUDIOFOCUS_GAIN
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(focusGain)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setWillPauseWhenDucked(false)        // we'll duck ourselves in the listener
                .setAcceptsDelayedFocusGain(true)     // queue focus request if other app is active
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener, AudioManager.STREAM_MUSIC, focusGain
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun stopNativePlayers() {
        try {
            playbackRouter?.stop()
        } catch (_: Exception) {}
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
