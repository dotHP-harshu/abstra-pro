package dev.abstra.pro.audio.effects

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log

/**
 * Hardware Equalizer, Bass Boost, and Spatial Virtualizer wrapper.
 */
class EqualizerEffect {
    private var equalizer: Equalizer? = null
    private var secondaryEqualizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var secondaryBassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var secondaryVirtualizer: Virtualizer? = null

    val numberOfBands: Int get() = equalizer?.numberOfBands?.toInt() ?: 5
    val bandLevelRange: Pair<Short, Short>
        get() {
            val range = equalizer?.bandLevelRange
            return if (range != null && range.size >= 2) range[0] to range[1] else (-1500).toShort() to 1500.toShort()
        }

    fun setup(sessionId: Int, isSecondary: Boolean, isEnabled: Boolean, bandLevels: List<Int>, bassLevel: Int, virtLevel: Int) {
        release(isSecondary)
        if (sessionId == 0) return

        try {
            val eq = Equalizer(0, sessionId).apply {
                enabled = isEnabled
                val num = numberOfBands.toInt()
                for (i in 0 until num) {
                    if (i < bandLevels.size) {
                        try {
                            setBandLevel(i.toShort(), bandLevels[i].toShort())
                        } catch (_: Exception) {}
                    }
                }
            }

            val bb = BassBoost(0, sessionId).apply {
                enabled = isEnabled && bassLevel > 0
                if (strengthSupported) {
                    setStrength(bassLevel.toShort().coerceIn(0, 1000))
                }
            }

            val virt = Virtualizer(0, sessionId).apply {
                enabled = isEnabled && virtLevel > 0
                if (strengthSupported) {
                    setStrength(virtLevel.toShort().coerceIn(0, 1000))
                }
            }

            if (isSecondary) {
                secondaryEqualizer = eq
                secondaryBassBoost = bb
                secondaryVirtualizer = virt
            } else {
                equalizer = eq
                bassBoost = bb
                virtualizer = virt
            }
        } catch (e: Exception) {
            Log.e("EqualizerEffect", "Failed to setup Equalizer", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        secondaryEqualizer?.enabled = enabled
        bassBoost?.enabled = enabled
        secondaryBassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        secondaryVirtualizer?.enabled = enabled
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
            secondaryEqualizer?.setBandLevel(band, level)
        } catch (e: Exception) {
            Log.e("EqualizerEffect", "Failed to setBandLevel", e)
        }
    }

    fun setBassBoost(strength: Int) {
        try {
            val s = strength.toShort().coerceIn(0, 1000)
            bassBoost?.apply {
                enabled = s > 0
                if (strengthSupported) setStrength(s)
            }
            secondaryBassBoost?.apply {
                enabled = s > 0
                if (strengthSupported) setStrength(s)
            }
        } catch (_: Exception) {}
    }

    fun setVirtualizer(strength: Int) {
        try {
            val s = strength.toShort().coerceIn(0, 1000)
            virtualizer?.apply {
                enabled = s > 0
                if (strengthSupported) setStrength(s)
            }
            secondaryVirtualizer?.apply {
                enabled = s > 0
                if (strengthSupported) setStrength(s)
            }
        } catch (_: Exception) {}
    }

    fun getCenterFreq(band: Short): Int {
        return try {
            equalizer?.getCenterFreq(band) ?: 1000
        } catch (_: Exception) { 1000 }
    }

    fun handover() {
        equalizer?.release()
        equalizer = secondaryEqualizer
        secondaryEqualizer = null

        bassBoost?.release()
        bassBoost = secondaryBassBoost
        secondaryBassBoost = null

        virtualizer?.release()
        virtualizer = secondaryVirtualizer
        secondaryVirtualizer = null
    }

    fun release(isSecondary: Boolean = false) {
        if (isSecondary) {
            secondaryEqualizer?.release()
            secondaryEqualizer = null
            secondaryBassBoost?.release()
            secondaryBassBoost = null
            secondaryVirtualizer?.release()
            secondaryVirtualizer = null
        } else {
            equalizer?.release()
            equalizer = null
            bassBoost?.release()
            bassBoost = null
            virtualizer?.release()
            virtualizer = null
        }
    }

    fun releaseAll() {
        release(false)
        release(true)
    }
}
