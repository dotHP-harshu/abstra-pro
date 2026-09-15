package dev.abstra.pro.ai.model

import androidx.compose.ui.graphics.Color
import dev.abstra.pro.data.model.Song
import java.util.Calendar

/** Categorization for generated AI mixes */
enum class MixCategory {
    DAILY_FLOW,
    DISCOVERY,
    ENERGY,
    CHILL,
    TIME_OF_DAY,
    FORGOTTEN_GEMS,
    ACOUSTIC_LIVE,
    ARTIST_SPOTLIGHT
}

/** Definition of an auto-generated AI mix */
data class AiMix(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: MixCategory,
    val songs: List<Song>,
    val gradientColors: List<Color>,
    val iconName: String,
    val description: String
)

/** Time of day for contextual listening habits */
enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT;

    companion object {
        fun current(): TimeOfDay {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> MORNING
                in 12..17 -> AFTERNOON
                in 18..22 -> EVENING
                else -> NIGHT
            }
        }
    }
}

/** Day of week type */
enum class DayType {
    WEEKDAY,
    WEEKEND;

    companion object {
        fun current(): DayType {
            val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            return if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) WEEKEND else WEEKDAY
        }
    }
}

/** Acoustic features profile inferred from metadata heuristics */
data class AudioProfile(
    val energyScore: Float,       // 0.0 (calm/chill) to 1.0 (energetic/fast)
    val acousticScore: Float,     // 0.0 (electronic/produced) to 1.0 (raw acoustic)
    val isLiveOrAcoustic: Boolean,
    val isFocusFriendly: Boolean
)
