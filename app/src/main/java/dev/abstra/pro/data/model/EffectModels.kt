package dev.abstra.pro.data.model

/** 10-Band Equalizer Preset */
data class EqPreset(
    val name: String,
    val bandLevels: List<Int>, // 10 bands in millibels (-1500 to +1500 mB)
    val bassBoost: Int = 0,    // 0 to 1000
    val virtualizer: Int = 0   // 0 to 1000
) {
    val displayName: String get() = name

    companion object {
        val FLAT = EqPreset("Flat", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
        val POP = EqPreset("Pop", listOf(200, 350, 450, 300, 0, -200, -200, 0, 300, 400), bassBoost = 250, virtualizer = 150)
        val ROCK = EqPreset("Rock", listOf(500, 350, 200, 0, -150, 0, 250, 400, 500, 600), bassBoost = 400)
        val JAZZ = EqPreset("Jazz", listOf(0, 0, 100, 250, 250, 200, 0, 150, 250, 300), virtualizer = 300)
        val CLASSICAL = EqPreset("Classical", listOf(400, 300, 200, 100, -100, -100, 0, 200, 350, 400), virtualizer = 400)
        val BASS_BOOST = EqPreset("Bass Boost", listOf(700, 600, 500, 300, 100, 0, 0, 0, 0, 0), bassBoost = 750)
        val VOCAL_BOOST = EqPreset("Vocal Boost", listOf(-200, -200, 0, 200, 450, 500, 400, 200, 0, -100))
        val ELECTRONIC = EqPreset("Electronic", listOf(550, 450, 150, 0, -200, 200, 100, 300, 500, 550), bassBoost = 500, virtualizer = 250)

        val ALL_PRESETS = listOf(FLAT, POP, ROCK, JAZZ, CLASSICAL, BASS_BOOST, VOCAL_BOOST, ELECTRONIC)
        fun values(): List<EqPreset> = ALL_PRESETS
    }
}

/** State of Hardware Equalizer & Enhancements */
data class EqualizerState(
    val enabled: Boolean = false,
    val currentPreset: EqPreset = EqPreset.FLAT,
    val bandLevels: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    val centerFrequencies: List<Int> = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000),
    val minBandLevel: Int = -1500,
    val maxBandLevel: Int = 1500,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0
)

/** Reverb Environment */
enum class ReverbEnvironment(val id: Int, val displayName: String) {
    NONE(0, "None"),
    SMALL_ROOM(1, "Small Room"),
    MEDIUM_ROOM(2, "Medium Room"),
    LARGE_ROOM(3, "Large Room"),
    MEDIUM_HALL(4, "Medium Hall"),
    LARGE_HALL(5, "Large Hall"),
    PLATE(6, "Plate")
}

/** Dynamics Processing Multi-Band Compressor Presets */
enum class DynamicsPreset(val id: Int, val displayName: String, val ratioLabel: String) {
    NONE(0, "None", "Off"),
    LIGHT(1, "Light", "1.5:1"),
    MEDIUM(2, "Medium", "3.0:1"),
    STRONG(3, "Strong", "5.0:1"),
    NIGHT(4, "Night", "8.0:1")
}

/** Stereo Balance Pan State */
data class BalanceState(
    val pan: Float = 0.5f // 0.0 (Full Left) to 1.0 (Full Right), 0.5 = Center
) {
    val isCenter: Boolean get() = kotlin.math.abs(pan - 0.5f) < 0.01f
}
