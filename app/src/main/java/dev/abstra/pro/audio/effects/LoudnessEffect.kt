package dev.abstra.pro.audio.effects

import android.media.audiofx.LoudnessEnhancer
import android.util.Log

/**
 * Loudness Enhancer wrapper (0–3000 mB / 0–30 dB gain).
 */
class LoudnessEffect {
    private var enhancer: LoudnessEnhancer? = null
    private var secondaryEnhancer: LoudnessEnhancer? = null
    var targetGain: Int = 0
        private set

    fun setup(sessionId: Int, isSecondary: Boolean, enabled: Boolean, gain: Int) {
        release(isSecondary)
        if (sessionId == 0) return

        try {
            val e = LoudnessEnhancer(sessionId).apply {
                this.enabled = enabled
                setTargetGain(gain)
            }
            targetGain = gain
            if (isSecondary) {
                secondaryEnhancer = e
            } else {
                enhancer = e
            }
        } catch (e: Exception) {
            Log.e("LoudnessEffect", "Failed to setup LoudnessEnhancer", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        enhancer?.enabled = enabled
        secondaryEnhancer?.enabled = enabled
    }

    fun setTargetGain(gain: Int) {
        targetGain = gain
        enhancer?.setTargetGain(gain)
        secondaryEnhancer?.setTargetGain(gain)
    }

    fun handover() {
        enhancer?.release()
        enhancer = secondaryEnhancer
        secondaryEnhancer = null
    }

    fun release(isSecondary: Boolean = false) {
        if (isSecondary) {
            secondaryEnhancer?.release()
            secondaryEnhancer = null
        } else {
            enhancer?.release()
            enhancer = null
        }
    }

    fun releaseAll() {
        enhancer?.release()
        enhancer = null
        secondaryEnhancer?.release()
        secondaryEnhancer = null
    }
}
