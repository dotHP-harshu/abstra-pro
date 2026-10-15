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
import dev.abstra.pro.audio.synth.ProceduralSynthPlayer
import dev.abstra.pro.data.model.RepeatMode
import dev.abstra.pro.data.model.Song
import dev.abstra.pro.data.prefs.PlaybackStateSaver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioEngine(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val effectsManager = AudioEffectsManager()

    private val exoPlayer: PlaybackPlayer
    private val synthPlayer: PlaybackPlayer
    @Volatile
    private var activePlayer: PlaybackPlayer? = null

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

    private val _seekEvents = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val seekEvents: SharedFlow<Long> = _seekEvents.asSharedFlow()

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

    private val _gaplessEnabled = MutableStateFlow(true)
    val gaplessEnabled: StateFlow<Boolean> = _gaplessEnabled.asStateFlow()

    private val _currentVolume = MutableStateFlow(1.0f)
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()

    fun setVolume(v: Float) {
        val clamped = v.coerceIn(0f, 1f)
        _currentVolume.value = clamped
        try { activePlayer?.setVolume(clamped) } catch (_: Exception) {}
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
    var onRepeatModeChanged: ((RepeatMode) -> Unit)? = null
    var onShuffleChanged: ((Boolean) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private var audioFocusRequest: AudioFocusRequest? = null
    private var originalQueue: List<Song> = emptyList()
    private var lastPreviousClickTimeMs = 0L
    private var isInForeground = true

    val currentSessionId: Int get() = activePlayer?.getAudioSessionId() ?: 0

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
        
        exoPlayer = ExoPlaybackAdapter(
            context = context,
            scope = scope,
            onSongStarted = { s -> 
                _isPlaying.value = true
                _pendingSong.value = s
                consecutiveFailures.set(0)
                onSongStarted?.invoke(s)
                onPlaybackStateChange?.invoke(true)
            },
            onSongCompleted = {
                scope.launch { handleSongCompletion() }
            },
            onSeamlessTransition = { song ->
                _currentSong.value?.let { onSongCompleted?.invoke(it) }
                _currentSong.value = song
                _pendingSong.value = song
                _duration.value = song.durationMs
                _currentPosition.value = 0L
                val q = _queue.value
                val newIdx = q.indexOfFirst { it.id == song.id }
                if (newIdx != -1) {
                    _currentIndex.value = newIdx
                }
                persistPlaybackState()
            },
            onError = {
                _isPlaying.value = false
                val q = _queue.value
                if (consecutiveFailures.incrementAndGet() > q.size) {
                    consecutiveFailures.set(0)
                } else {
                    scope.launch { delay(400); playNextInternal(isUserInitiated = false) }
                }
            },
            onIsPlayingChanged = { playing ->
                if (_isPlaying.value != playing) {
                    _isPlaying.value = playing
                    onPlaybackStateChange?.invoke(playing)
                }
            },
            onAudioSessionIdChanged = { sessionId ->
                effectsManager.attachSession(sessionId)
            }
        )

        synthPlayer = ProceduralSynthPlayer(
            scope = scope,
            onSongStarted = { s ->
                _isPlaying.value = true
                _pendingSong.value = s
                consecutiveFailures.set(0)
                onSongStarted?.invoke(s)
                onPlaybackStateChange?.invoke(true)
            },
            onSongCompleted = {
                scope.launch { handleSongCompletion() }
            }
        )

        activePlayer = exoPlayer
        
        startProgressTracker()
    }

    fun setForegroundState(foreground: Boolean) {
        isInForeground = foreground
    }

    // ─────────────────────────────────────────────────────────────
    // QUEUE & PLAYBACK CONTROLS
    // ─────────────────────────────────────────────────────────────

    fun setQueue(songs: List<Song>, startIndex: Int = 0, autoPlay: Boolean = true, initialPositionMs: Long = 0L) {
        if (songs.isEmpty()) return
        originalQueue = songs
        val safeIndex = startIndex.coerceIn(0, songs.size - 1)
        val effective = if (_isShuffle.value) shuffledQueue(songs, safeIndex) else songs
        val startAt = if (_isShuffle.value) 0 else safeIndex
        _queue.value = effective
        _currentIndex.value = startAt
        playSong(effective[startAt], autoPlay, initialPositionMs)
    }

    fun playTrackAtIndex(index: Int, autoPlay: Boolean = true, initialPositionMs: Long = 0L) {
        val q = _queue.value
        if (index in q.indices) {
            _currentIndex.value = index
            playSong(q[index], autoPlay, initialPositionMs)
        }
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
        removeQueueItemAt(idx)
    }

    fun removeQueueItemAt(index: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (index !in currentQueue.indices) return
        val removed = currentQueue.removeAt(index)
        _queue.value = currentQueue
        val origIdx = originalQueue.indexOf(removed)
        if (origIdx != -1) {
            originalQueue = originalQueue.toMutableList().apply { removeAt(origIdx) }
        }
        val cur = _currentIndex.value
        if (index < cur) {
            _currentIndex.value = cur - 1
        } else if (index == cur) {
            if (currentQueue.isEmpty()) {
                clearEntireQueue()
            } else {
                val nextIdx = cur.coerceIn(0, currentQueue.size - 1)
                _currentIndex.value = nextIdx
                playSong(currentQueue[nextIdx])
            }
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val currentQueue = _queue.value.toMutableList()
        if (from !in currentQueue.indices || to !in currentQueue.indices || from == to) return
        val cur = _currentIndex.value
        val item = currentQueue.removeAt(from)
        currentQueue.add(to, item)
        _queue.value = currentQueue
        if (!_isShuffle.value) {
            originalQueue = currentQueue
        }
        activePlayer?.clearPendingNextTrack()

        if (cur == from) {
            _currentIndex.value = to
        } else if (from < cur && to >= cur) {
            _currentIndex.value = cur - 1
        } else if (from > cur && to <= cur) {
            _currentIndex.value = cur + 1
        }
        persistPlaybackState()
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
        _currentSong.value = null
        _pendingSong.value = null
        _currentPosition.value = 0L
        activePlayer?.clearPendingNextTrack()
        stopNativePlayers()
        pause()
        _shouldBePlaying = false
        PlaybackStateSaver.clearState(context)
    }

    fun playSong(song: Song, autoPlay: Boolean = true, initialPositionMs: Long = 0L) {
        val prev = _currentSong.value
        if (prev != null && prev.id != song.id) {
            val playedSec = _currentPosition.value / 1000L
            val totalSec = _duration.value / 1000L
            if (playedSec < totalSec * 0.85) {
                onSongSkipped?.invoke(prev, playedSec, totalSec)
            }
        }

        _pendingSong.value = song
        _currentPosition.value = initialPositionMs
        _duration.value = song.durationMs.coerceAtLeast(1000L)
        _isPlaying.value = false
        _shouldBePlaying = autoPlay

        val isSynth = song.id.startsWith("demo_")
        val targetPlayer = if (isSynth) synthPlayer else exoPlayer
        val otherPlayer = if (isSynth) exoPlayer else synthPlayer

        otherPlayer.stop()
        activePlayer = targetPlayer

        if (!isSynth && autoPlay && !requestAudioFocus()) {
            _pendingSong.value = null
            return
        }

        _currentSong.value = song

        targetPlayer.setVolume(_currentVolume.value)
        targetPlayer.setSpeed(_playbackSpeed.value)
        targetPlayer.isGaplessEnabled = _gaplessEnabled.value
        applyBalance()

        targetPlayer.playSong(song, autoPlay, initialPositionMs)

        if (!autoPlay) {
            _isPlaying.value = false
            onPlaybackStateChange?.invoke(false)
        }

        scope.launch {
            delay(100)
            val sessionId = activePlayer?.getAudioSessionId() ?: 0
            if (sessionId != 0) {
                effectsManager.attachSession(sessionId)
            }
        }
        persistPlaybackState()
    }

    fun resume() {
        val current = _currentSong.value
        val pending = _pendingSong.value
        if (current == null && pending == null) return

        if (pending != null && current?.id != pending.id) {
            Log.d("AudioEngine", "resume ignored: song ${pending.title} is still preparing")
            return
        }

        val song = current ?: return
        val isSynth = song.id.startsWith("demo_")
        if (!isSynth && !requestAudioFocus()) return

        try {
            activePlayer?.resume()
            _isPlaying.value = true
            onPlaybackStateChange?.invoke(true)
        } catch (_: Exception) {}
    }

    fun persistPlaybackState() {
        try {
            PlaybackStateSaver.saveState(
                context = context,
                currentSong = _currentSong.value,
                queue = _queue.value,
                originalQueue = originalQueue,
                currentIndex = _currentIndex.value,
                positionMs = _currentPosition.value,
                repeatMode = _repeatMode.value,
                isShuffle = _isShuffle.value
            )
        } catch (_: Exception) {}
    }

    fun pause() {
        _shouldBePlaying = false
        _isPlaying.value = false
        onPlaybackStateChange?.invoke(false)
        try {
            activePlayer?.pause()
        } catch (_: Exception) {}
        persistPlaybackState()
    }

    fun seekTo(positionMs: Long) {
        if (_currentSong.value == null) return
        _currentPosition.value = positionMs
        _seekEvents.tryEmit(positionMs)
        try {
            activePlayer?.seekTo(positionMs)
        } catch (_: Exception) {}
        persistPlaybackState()
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
        val currentIdx = _currentIndex.value
        val prevIdx = if (currentIdx <= 0) {
            if (_repeatMode.value == RepeatMode.REPEAT_ALL) {
                q.size - 1
            } else {
                seekTo(0L)
                return
            }
        } else {
            currentIdx - 1
        }
        _currentIndex.value = prevIdx
        consecutiveFailures.incrementAndGet()
        playSong(q[prevIdx])
    }

    /** User-initiated next (button tap, headset skip, notification next). */
    fun playNext() {
        playNextInternal(isUserInitiated = true)
    }

    /** Natural track end completion. */
    fun onTrackEnded() {
        playNextInternal(isUserInitiated = false)
    }

    private fun playNextInternal(isUserInitiated: Boolean) {
        val q = _queue.value
        if (q.isEmpty()) return

        if (consecutiveFailures.get() > q.size) {
            _isPlaying.value = false
            consecutiveFailures.set(0)
            return
        }

        // Repeat-One only restarts on natural track end. User Next skips to the next track.
        if (!isUserInitiated && _repeatMode.value == RepeatMode.REPEAT_ONE) {
            val current = _currentSong.value ?: q[_currentIndex.value.coerceIn(0, q.size - 1)]
            consecutiveFailures.incrementAndGet()
            playSong(current, autoPlay = true)
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
        onTrackEnded()
    }

    // ─────────────────────────────────────────────────────────────
    // SHUFFLE & REPEAT
    // ─────────────────────────────────────────────────────────────

    fun setRepeatMode(mode: RepeatMode) {
        prefs.edit().putString("repeat_mode", mode.name).apply()
        _repeatMode.value = mode
        activePlayer?.clearPendingNextTrack()
        onRepeatModeChanged?.invoke(mode)
        persistPlaybackState()
    }

    fun setShuffle(enabled: Boolean) {
        if (_isShuffle.value == enabled) return
        toggleShuffle()
    }

    /**
     * Restores an active and original queue on app cold-start without re-shuffling
     * or accidentally auto-playing.
     */
    fun restoreQueue(
        currentQueue: List<Song>,
        origQueue: List<Song>,
        targetSongId: String?,
        fallbackIndex: Int,
        initialPositionMs: Long
    ) {
        if (currentQueue.isEmpty()) return
        originalQueue = if (origQueue.isNotEmpty()) origQueue else currentQueue
        _queue.value = currentQueue

        val foundIdx = if (targetSongId != null) {
            currentQueue.indexOfFirst { it.id == targetSongId }
        } else -1

        val finalIdx = if (foundIdx != -1) foundIdx else fallbackIndex.coerceIn(0, currentQueue.size - 1)
        _currentIndex.value = finalIdx

        val targetSong = currentQueue[finalIdx]
        playSong(targetSong, autoPlay = false, initialPositionMs = initialPositionMs)
        persistPlaybackState()
    }

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
            val orig = if (originalQueue.isNotEmpty()) originalQueue else _queue.value
            _queue.value = orig
            _currentIndex.value = orig.indexOfFirst { it.id == activeSong?.id }.coerceAtLeast(0)
        }
        activePlayer?.clearPendingNextTrack()
        onShuffleChanged?.invoke(newVal)
        persistPlaybackState()
    }

    fun toggleRepeatMode() {
        val newMode = when (_repeatMode.value) {
            RepeatMode.REPEAT_ALL -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.REPEAT_OFF
            RepeatMode.REPEAT_OFF -> RepeatMode.REPEAT_ALL
        }
        setRepeatMode(newMode)
    }

    private fun shuffledQueue(queue: List<Song>, currentIndex: Int): List<Song> {
        if (queue.size < 2) return queue
        val current = queue[currentIndex.coerceIn(0, queue.size - 1)]
        val rest = queue.filterIndexed { i, _ -> i != currentIndex }.shuffled()
        return listOf(current) + rest
    }

    // ─────────────────────────────────────────────────────────────
    // SPEED, PITCH & BALANCE
    // ─────────────────────────────────────────────────────────────

    private var isGaplessEnabled: Boolean = true

    fun setGapless(enabled: Boolean) {
        isGaplessEnabled = enabled
        _gaplessEnabled.value = enabled
        activePlayer?.isGaplessEnabled = enabled
        if (!enabled) {
            activePlayer?.clearPendingNextTrack()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.5f, 2.0f)
        applySpeed()
    }

    private fun applySpeed() {
        activePlayer?.setSpeed(_playbackSpeed.value)
    }

    fun setBalancePan(pan: Float) {
        effectsManager.setBalancePan(pan)
        applyBalance()
    }

    private fun applyBalance() {
        val (left, right) = effectsManager.getChannelVolumes()
        try {
            activePlayer?.leftVolume = left
            activePlayer?.rightVolume = right
            activePlayer?.setVolume(_currentVolume.value)
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
                    activePlayer?.setVolume(factor)
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
            var lastValidPos = 0L
            var lastPersistTimeMs = System.currentTimeMillis()
            while (isActive) {
                if (activePlayer != null) {
                    try {
                        val isActuallyPlaying = activePlayer?.isPlaying() == true
                        if (isActuallyPlaying) {
                            val newPos = activePlayer?.getCurrentPosition() ?: 0L
                            if (newPos >= 0L) {
                                lastValidPos = newPos
                                _currentPosition.value = newPos
                            }
                            val now = System.currentTimeMillis()
                            if (now - lastPersistTimeMs >= 20000L) {
                                lastPersistTimeMs = now
                                persistPlaybackState()
                            }
                        } else {
                            lastValidPos = _currentPosition.value
                        }

                        if (isActuallyPlaying && _repeatMode.value != RepeatMode.REPEAT_ONE) {
                            // Prepare next track for gapless if needed
                            if (_duration.value > 0 && (_duration.value - lastValidPos) < 20000L) { // prepare 20s in advance
                                val q = _queue.value
                                val nextIdx = _currentIndex.value + 1
                                if (nextIdx < q.size) {
                                    activePlayer?.prepareNextTrack(q[nextIdx])
                                } else if (_repeatMode.value == RepeatMode.REPEAT_ALL && q.isNotEmpty()) {
                                    activePlayer?.prepareNextTrack(q[0])
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
                // We got focus back. Restore to the user's volume setting and resume if desired.
                val userVol = _currentVolume.value
                try { activePlayer?.setVolume(userVol) } catch (_: Exception) {}
                applyBalance()
                if (_shouldBePlaying) resume()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Duck to 20% of user volume
                val duckVol = _currentVolume.value * 0.2f
                try { activePlayer?.setVolume(duckVol) } catch (_: Exception) {}
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_shareAudioFocusEnabled.value) {
                    val duckVol = _currentVolume.value * 0.2f
                    try { activePlayer?.setVolume(duckVol) } catch (_: Exception) {}
                } else {
                    pause()
                    _shouldBePlaying = true
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                if (_shareAudioFocusEnabled.value) {
                    val shareVol = _currentVolume.value * 0.5f
                    try { activePlayer?.setVolume(shareVol) } catch (_: Exception) {}
                } else {
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
            exoPlayer.stop()
            synthPlayer.stop()
        } catch (_: Exception) {}
    }

    fun releaseEngine() {
        persistPlaybackState()
        progressJob?.cancel()
        progressJob = null
        try {
            context.unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}
        exoPlayer.release()
        synthPlayer.release()
        effectsManager.releaseAll()
        scope.cancel()
    }
}
