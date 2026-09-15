package dev.abstra.pro.audio.synth

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real-time procedural ambient synth audio generator for demo tracks.
 * Allows instant, pleasant offline playback and visualizer testing even if library is empty.
 */
class ProceduralAudioGenerator {
    private var audioTrack: AudioTrack? = null
    private var generatorJob: Job? = null
    private val generatorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    @Volatile
    private var isPlaying = false
    private val sampleRate = 44100
    private var volume = 0.85f
    private var speed = 1.0f

    @Synchronized
    fun startPlayback(
        trackId: String,
        initialPositionMs: Long = 0L,
        onProgressUpdate: (Long) -> Unit,
        onCompletion: () -> Unit
    ) {
        stop()

        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = if (minBuf > 0) minBuf * 4 else 8192

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.setVolume(volume)
            audioTrack?.play()
            isPlaying = true
        } catch (_: Exception) {
            isPlaying = true
        }

        val melodyNotes = getMelodyForTrack(trackId)
        val chordNotes = getChordsForTrack(trackId)
        val bpm = getBpmForTrack(trackId)

        generatorJob = generatorScope.launch {
            var currentSec = (initialPositionMs / 1000.0).coerceAtLeast(0.0)
            val maxDurationSec = 225.0
            val chunkSize = 1024
            val buffer = ShortArray(chunkSize * 2)

            var sampleIdx = (currentSec * sampleRate).toLong()
            var lastProgressUpdate = 0L

            while (isActive && isPlaying && currentSec < maxDurationSec) {
                for (i in 0 until chunkSize) {
                    val time = sampleIdx.toDouble() / sampleRate
                    val beatIndex = ((time * (bpm / 60.0) * speed).toInt())

                    val melodyFreq = melodyNotes[beatIndex % melodyNotes.size]
                    val chordFreq = chordNotes[(beatIndex / 4) % chordNotes.size]

                    val melodyEnv = (1.0 - ((time * (bpm / 60.0) * speed) % 1.0) * 0.4).coerceIn(0.2, 1.0)
                    val sMelody = sin(2 * PI * melodyFreq * time) * 0.35 * melodyEnv
                    val sChord = (sin(2 * PI * chordFreq * time) + sin(2 * PI * (chordFreq * 1.5) * time) * 0.5) * 0.2
                    val sBass = sin(2 * PI * (chordFreq / 2.0) * time) * 0.25

                    val beatFraction = (time * (bpm / 60.0) * speed) % 1.0
                    val isKick = (beatIndex % 2 == 0) && beatFraction < 0.15
                    val kick = if (isKick) sin(2 * PI * 65.0 * (1.0 - beatFraction * 5.0) * time) * 0.3 else 0.0

                    val sampleValue = ((sMelody + sChord + sBass + kick) * 32767 * 0.6).toInt().coerceIn(-32767, 32767)

                    val leftPan = 0.9 + 0.1 * sin(time * 0.5)
                    val rightPan = 0.9 - 0.1 * sin(time * 0.5)

                    buffer[i * 2] = (sampleValue * leftPan).toInt().toShort()
                    buffer[i * 2 + 1] = (sampleValue * rightPan).toInt().toShort()

                    sampleIdx++
                }

                try {
                    audioTrack?.write(buffer, 0, buffer.size)
                } catch (_: Exception) {}

                currentSec = sampleIdx.toDouble() / sampleRate
                val nowMs = (currentSec * 1000).toLong()
                if (nowMs - lastProgressUpdate >= 200) {
                    lastProgressUpdate = nowMs
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(nowMs)
                    }
                }
            }

            if (currentSec >= maxDurationSec && isPlaying) {
                withContext(Dispatchers.Main) {
                    onCompletion()
                }
            }
        }
    }

    @Synchronized
    fun pause() {
        isPlaying = false
        generatorJob?.cancel()
        generatorJob = null
        try {
            audioTrack?.pause()
        } catch (_: Exception) {}
    }

    @Synchronized
    fun stop() {
        isPlaying = false
        generatorJob?.cancel()
        generatorJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun release() {
        stop()
        generatorScope.cancel()
    }

    fun setSpeed(newSpeed: Float) {
        speed = newSpeed.coerceIn(0.5f, 2.0f)
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(volume)
        } catch (_: Exception) {}
    }

    private fun getMelodyForTrack(trackId: String): List<Double> {
        return when (trackId) {
            "demo_daydreams" -> listOf(523.25, 659.25, 783.99, 880.00, 783.99, 659.25, 587.33, 523.25)
            "demo_strawberry" -> listOf(587.33, 659.25, 739.99, 880.00, 987.77, 880.00, 739.99, 659.25)
            "demo_candy" -> listOf(659.25, 783.99, 987.77, 1046.50, 987.77, 783.99, 659.25, 523.25)
            "demo_pinkmoon" -> listOf(440.00, 523.25, 659.25, 783.99, 659.25, 523.25, 493.88, 440.00)
            else -> listOf(523.25, 659.25, 783.99, 880.00, 783.99, 659.25, 587.33, 523.25)
        }
    }

    private fun getChordsForTrack(trackId: String): List<Double> {
        return when (trackId) {
            "demo_daydreams" -> listOf(261.63, 329.63, 392.00, 349.23)
            "demo_strawberry" -> listOf(293.66, 369.99, 440.00, 329.63)
            "demo_pinkmoon" -> listOf(220.00, 261.63, 329.63, 293.66)
            else -> listOf(261.63, 329.63, 392.00, 349.23)
        }
    }

    private fun getBpmForTrack(trackId: String): Double {
        return when (trackId) {
            "demo_daydreams" -> 84.0
            "demo_strawberry" -> 92.0
            "demo_candy" -> 96.0
            "demo_pinkmoon" -> 78.0
            else -> 90.0
        }
    }
}
