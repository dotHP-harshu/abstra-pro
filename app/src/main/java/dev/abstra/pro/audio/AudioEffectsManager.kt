package dev.abstra.pro.audio

import dev.abstra.pro.audio.effects.*
import dev.abstra.pro.data.model.DynamicsPreset
import dev.abstra.pro.data.model.EqPreset
import dev.abstra.pro.data.model.EqualizerState
import dev.abstra.pro.data.model.ReverbEnvironment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates all hardware audio DSP effects across audio session IDs.
 */
class AudioEffectsManager {
    val equalizer = EqualizerEffect()
    val dynamics = DynamicsEffect()
    val loudness = LoudnessEffect()
    val reverb = ReverbEffect()

    private val _equalizerState = MutableStateFlow(
        EqualizerState(
            enabled = false,
            currentPreset = EqPreset.FLAT,
            bandLevels = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            centerFrequencies = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000),
            minBandLevel = -1500,
            maxBandLevel = 1500,
            bassBoostStrength = 0,
            virtualizerStrength = 0
        )
    )
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _reverbEnvironment = MutableStateFlow(ReverbEnvironment.NONE)
    val reverbEnvironment: StateFlow<ReverbEnvironment> = _reverbEnvironment.asStateFlow()

    private val _dynamicsPreset = MutableStateFlow(DynamicsPreset.NONE)
    val dynamicsPreset: StateFlow<DynamicsPreset> = _dynamicsPreset.asStateFlow()

    private val _balancePan = MutableStateFlow(0f)
    val balancePan: StateFlow<Float> = _balancePan.asStateFlow()

    private val _loudnessGainMb = MutableStateFlow(0)
    val loudnessGainMb: StateFlow<Int> = _loudnessGainMb.asStateFlow()

    var isEqEnabled: Boolean = false
        private set

    var internalBalancePan: Float = 0.5f
        private set

    var targetLoudnessGain: Int = 0
        private set

    var targetDynamicsPreset: Int = 0
        private set

    var targetReverbPreset: Int = 0
        private set

    var currentBandLevels: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        private set

    var bassBoostStrength: Int = 0
        private set

    var virtualizerStrength: Int = 0
        private set

    private var attachedSessionId = 0

    fun attachSession(sessionId: Int, isSecondary: Boolean = false, force: Boolean = false) {
        if (sessionId == 0) return
        if (!force && !isSecondary && sessionId == attachedSessionId) return
        if (!isSecondary) attachedSessionId = sessionId

        equalizer.setup(
            sessionId = sessionId,
            isSecondary = isSecondary,
            isEnabled = isEqEnabled,
            bandLevels = currentBandLevels,
            bassLevel = bassBoostStrength,
            virtLevel = virtualizerStrength
        )

        dynamics.setup(sessionId, isSecondary, targetDynamicsPreset)
        loudness.setup(sessionId, isSecondary, isEqEnabled && targetLoudnessGain > 0, targetLoudnessGain)
        reverb.setup(sessionId, isSecondary, targetReverbPreset)
    }

    fun setEqEnabled(enabled: Boolean) {
        isEqEnabled = enabled
        equalizer.setEnabled(enabled)
        loudness.setEnabled(enabled && targetLoudnessGain > 0)
        _equalizerState.value = _equalizerState.value.copy(enabled = enabled)
    }

    fun setEqualizerEnabled(enabled: Boolean) = setEqEnabled(enabled)

    fun setEqualizerPreset(preset: EqPreset) {
        setAllBands(preset.bandLevels)
        setBassBoost(preset.bassBoost)
        setVirtualizer(preset.virtualizer)
        _equalizerState.value = _equalizerState.value.copy(
            currentPreset = preset,
            bandLevels = preset.bandLevels,
            bassBoostStrength = preset.bassBoost,
            virtualizerStrength = preset.virtualizer
        )
    }

    fun setBandLevel(band: Short, level: Short) {
        equalizer.setBandLevel(band, level)
        val mutable = currentBandLevels.toMutableList()
        if (band < mutable.size) {
            mutable[band.toInt()] = level.toInt()
            currentBandLevels = mutable
            _equalizerState.value = _equalizerState.value.copy(bandLevels = mutable)
        }
    }

    fun setBandLevel(band: Int, level: Short) = setBandLevel(band.toShort(), level)

    fun setAllBands(levels: List<Int>) {
        currentBandLevels = levels
        for (i in levels.indices) {
            equalizer.setBandLevel(i.toShort(), levels[i].toShort())
        }
        _equalizerState.value = _equalizerState.value.copy(bandLevels = levels)
    }

    fun setBassBoost(strength: Int) {
        bassBoostStrength = strength
        equalizer.setBassBoost(strength)
        _equalizerState.value = _equalizerState.value.copy(bassBoostStrength = strength)
    }

    fun setBassBoostStrength(strength: Int) = setBassBoost(strength)

    fun setVirtualizer(strength: Int) {
        virtualizerStrength = strength
        equalizer.setVirtualizer(strength)
        _equalizerState.value = _equalizerState.value.copy(virtualizerStrength = strength)
    }

    fun setVirtualizerStrength(strength: Int) = setVirtualizer(strength)

    fun setLoudnessGain(gain: Int) {
        targetLoudnessGain = gain
        loudness.setTargetGain(gain)
        loudness.setEnabled(isEqEnabled && gain > 0)
        _loudnessGainMb.value = gain
    }

    fun setLoudnessGainMb(gainMb: Int) = setLoudnessGain(gainMb)

    fun setDynamicsPreset(preset: Int) {
        targetDynamicsPreset = preset
        dynamics.setPreset(preset)
        _dynamicsPreset.value = DynamicsPreset.entries.firstOrNull { it.id == preset } ?: DynamicsPreset.NONE
    }

    fun setDynamicsPreset(preset: DynamicsPreset) {
        setDynamicsPreset(preset.id)
        _dynamicsPreset.value = preset
    }

    fun setReverbPreset(preset: Int) {
        targetReverbPreset = preset
        reverb.setPreset(preset)
        _reverbEnvironment.value = ReverbEnvironment.entries.firstOrNull { it.id == preset } ?: ReverbEnvironment.NONE
    }

    fun setReverbEnvironment(env: ReverbEnvironment) {
        setReverbPreset(env.id)
        _reverbEnvironment.value = env
    }

    fun setBalancePan(pan: Float) {
        _balancePan.value = pan
        internalBalancePan = ((pan + 1f) / 2f).coerceIn(0f, 1f)
    }

    fun getChannelVolumes(): Pair<Float, Float> {
        return BalanceEffect.volumesForBalance(internalBalancePan)
    }

    fun handover() {
        equalizer.handover()
        dynamics.handover()
        loudness.handover()
        reverb.handover()
    }

    fun releaseAll() {
        attachedSessionId = 0
        equalizer.releaseAll()
        dynamics.releaseAll()
        loudness.releaseAll()
        reverb.releaseAll()
    }
}
