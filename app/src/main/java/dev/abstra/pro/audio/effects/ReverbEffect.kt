package dev.abstra.pro.audio.effects

import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.PresetReverb
import android.os.Build
import android.util.Log

/**
 * Reverb effect wrapper using EnvironmentalReverb (API 31+) with PresetReverb fallback.
 */
class ReverbEffect {
    private var reverb: EnvironmentalReverb? = null
    private var secondaryReverb: EnvironmentalReverb? = null
    private var reverbLegacy: PresetReverb? = null
    private var secondaryReverbLegacy: PresetReverb? = null

    var currentPreset: Int = 0
        private set

    fun setup(sessionId: Int, isSecondary: Boolean, preset: Int) {
        release(isSecondary)
        currentPreset = preset
        if (sessionId == 0) return

        try {
            if (Build.VERSION.SDK_INT >= 31) {
                val r = EnvironmentalReverb(0, sessionId).apply {
                    applyPreset(preset)
                    enabled = preset != 0
                }
                if (isSecondary) secondaryReverb = r else reverb = r
            } else {
                val r = PresetReverb(0, sessionId).apply {
                    setPreset(preset.toShort())
                    enabled = preset != 0
                }
                if (isSecondary) secondaryReverbLegacy = r else reverbLegacy = r
            }
        } catch (e: Exception) {
            Log.e("ReverbEffect", "Failed to setup reverb effect", e)
        }
    }

    fun setPreset(preset: Int) {
        currentPreset = preset
        val enabled = preset != 0
        if (Build.VERSION.SDK_INT >= 31) {
            reverb?.let { it.applyPreset(preset); it.enabled = enabled }
            secondaryReverb?.let { it.applyPreset(preset); it.enabled = enabled }
        } else {
            reverbLegacy?.let { it.setPreset(preset.toShort()); it.enabled = enabled }
            secondaryReverbLegacy?.let { it.setPreset(preset.toShort()); it.enabled = enabled }
        }
    }

    private fun EnvironmentalReverb.applyPreset(preset: Int) {
        when (preset) {
            1 -> { // Small Room
                decayTime = 500
                setDensity(300.toShort())
                setDiffusion(300.toShort())
                setReverbLevel((-2000).toShort())
                setRoomLevel((-1000).toShort())
            }
            2 -> { // Medium Room
                decayTime = 1000
                setDensity(500.toShort())
                setDiffusion(500.toShort())
                setReverbLevel((-1000).toShort())
                setRoomLevel((-500).toShort())
            }
            3 -> { // Large Room
                decayTime = 1500
                setDensity(700.toShort())
                setDiffusion(700.toShort())
                setReverbLevel((-500).toShort())
                setRoomLevel((-200).toShort())
            }
            4 -> { // Medium Hall
                decayTime = 2200
                setDensity(800.toShort())
                setDiffusion(800.toShort())
                setReverbLevel(0.toShort())
                setRoomLevel(0.toShort())
            }
            5 -> { // Large Hall
                decayTime = 3500
                setDensity(900.toShort())
                setDiffusion(900.toShort())
                setReverbLevel(0.toShort())
                setRoomLevel(0.toShort())
            }
            6 -> { // Plate
                decayTime = 1500
                setDensity(500.toShort())
                setDiffusion(1000.toShort())
                setReverbLevel(100.toShort())
                setRoomLevel(0.toShort())
            }
        }
    }

    fun handover() {
        if (Build.VERSION.SDK_INT >= 31) {
            reverb?.release()
            reverb = secondaryReverb
            secondaryReverb = null
        } else {
            reverbLegacy?.release()
            reverbLegacy = secondaryReverbLegacy
            secondaryReverbLegacy = null
        }
    }

    fun release(isSecondary: Boolean = false) {
        if (isSecondary) {
            secondaryReverb?.release()
            secondaryReverb = null
            secondaryReverbLegacy?.release()
            secondaryReverbLegacy = null
        } else {
            reverb?.release()
            reverb = null
            reverbLegacy?.release()
            reverbLegacy = null
        }
    }

    fun releaseAll() {
        reverb?.release()
        reverb = null
        secondaryReverb?.release()
        secondaryReverb = null
        reverbLegacy?.release()
        reverbLegacy = null
        secondaryReverbLegacy?.release()
        secondaryReverbLegacy = null
    }
}
