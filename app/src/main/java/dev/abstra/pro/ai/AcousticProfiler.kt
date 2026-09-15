package dev.abstra.pro.ai

import dev.abstra.pro.ai.model.AudioProfile
import dev.abstra.pro.data.model.Song
import java.util.Locale

/**
 * Heuristically infers acoustic profile (energy, acousticness, focus, live) from local track metadata.
 */
object AcousticProfiler {

    fun profileSong(song: Song): AudioProfile {
        val titleLower = song.title.lowercase(Locale.getDefault())
        val genreLower = (song.genre ?: "").lowercase(Locale.getDefault())

        var energy = 0.50f
        var acoustic = 0.30f
        var isLiveOrAcoustic = false

        // Acoustic / Unplugged detection
        if (titleLower.contains("acoustic") || titleLower.contains("unplugged") ||
            titleLower.contains("piano version") || titleLower.contains("stripped") ||
            genreLower.contains("acoustic") || genreLower.contains("folk")) {
            acoustic += 0.50f
            energy -= 0.25f
            isLiveOrAcoustic = true
        }

        // Live performance detection
        if (titleLower.contains("live") || titleLower.contains("session") ||
            titleLower.contains("en vivo") || titleLower.contains("concert")) {
            isLiveOrAcoustic = true
        }

        // High Energy / Workout / EDM / Rock
        if (titleLower.contains("remix") || titleLower.contains("extended") ||
            titleLower.contains("club") || titleLower.contains("speed up") ||
            genreLower.contains("rock") || genreLower.contains("metal") ||
            genreLower.contains("dance") || genreLower.contains("edm") ||
            genreLower.contains("electronic") || genreLower.contains("trap") ||
            genreLower.contains("hardcore")) {
            energy += 0.35f
            acoustic -= 0.20f
        }

        // Low Energy / Chill / Study / Focus
        if (titleLower.contains("lofi") || titleLower.contains("lo-fi") ||
            titleLower.contains("slowed") || titleLower.contains("ambient") ||
            titleLower.contains("instrumental") || genreLower.contains("ambient") ||
            genreLower.contains("classical") || genreLower.contains("jazz") ||
            genreLower.contains("chill")) {
            energy -= 0.30f
            acoustic += 0.25f
        }

        // Duration heuristics
        if (song.durationMs > 300_000L) {
            energy -= 0.08f
        } else if (song.durationMs in 120_000L..210_000L) {
            energy += 0.08f
        }

        energy = energy.coerceIn(0.05f, 0.98f)
        acoustic = acoustic.coerceIn(0.05f, 0.98f)

        val isFocus = acoustic > 0.40f || energy in 0.20f..0.55f ||
                titleLower.contains("instrumental") || titleLower.contains("lo-fi")

        return AudioProfile(
            energyScore = energy,
            acousticScore = acoustic,
            isLiveOrAcoustic = isLiveOrAcoustic,
            isFocusFriendly = isFocus
        )
    }
}
