package dev.abstra.pro.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.abstra.pro.data.model.RepeatMode
import dev.abstra.pro.data.model.Song

/**
 * Persisted snapshot of the playback engine state.
 * Allows restoring queue, active track, seek position, and modes
 * across app cold-starts and service lifecycles without auto-playing.
 */
data class SavedPlaybackState(
    val currentSongId: String? = null,
    val queueSongIds: List<String> = emptyList(),
    val originalQueueSongIds: List<String> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val repeatMode: String = RepeatMode.REPEAT_ALL.name,
    val isShuffle: Boolean = false
)

object PlaybackStateSaver {
    private const val PREFS_NAME = "abstra_playback_state"
    private const val KEY_CURRENT_SONG_ID = "current_song_id"
    private const val KEY_QUEUE_IDS = "queue_song_ids_json"
    private const val KEY_ORIG_QUEUE_IDS = "orig_queue_ids_json"
    private const val KEY_CURRENT_INDEX = "current_index"
    private const val KEY_POSITION_MS = "position_ms"
    private const val KEY_REPEAT_MODE = "repeat_mode"
    private const val KEY_SHUFFLE = "is_shuffle"

    private val gson = Gson()

    fun saveState(
        context: Context,
        currentSong: Song?,
        queue: List<Song>,
        originalQueue: List<Song>,
        currentIndex: Int,
        positionMs: Long,
        repeatMode: RepeatMode,
        isShuffle: Boolean
    ) {
        val prefs = getPrefs(context)
        val songIds = queue.map { it.id }
        val origIds = originalQueue.map { it.id }
        val idsJson = gson.toJson(songIds)
        val origJson = gson.toJson(origIds)

        prefs.edit()
            .putString(KEY_CURRENT_SONG_ID, currentSong?.id)
            .putString(KEY_QUEUE_IDS, idsJson)
            .putString(KEY_ORIG_QUEUE_IDS, origJson)
            .putInt(KEY_CURRENT_INDEX, currentIndex)
            .putLong(KEY_POSITION_MS, positionMs)
            .putString(KEY_REPEAT_MODE, repeatMode.name)
            .putBoolean(KEY_SHUFFLE, isShuffle)
            .apply()
    }

    fun loadState(context: Context): SavedPlaybackState {
        val prefs = getPrefs(context)
        val currentSongId = prefs.getString(KEY_CURRENT_SONG_ID, null)
        val queueJson = prefs.getString(KEY_QUEUE_IDS, null)
        val origJson = prefs.getString(KEY_ORIG_QUEUE_IDS, null)
        val currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)
        val positionMs = prefs.getLong(KEY_POSITION_MS, 0L)
        val repeatModeStr = prefs.getString(KEY_REPEAT_MODE, RepeatMode.REPEAT_ALL.name) ?: RepeatMode.REPEAT_ALL.name
        val isShuffle = prefs.getBoolean(KEY_SHUFFLE, false)

        val queueIds: List<String> = if (!queueJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(queueJson, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val origQueueIds: List<String> = if (!origJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(origJson, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            queueIds
        }

        return SavedPlaybackState(
            currentSongId = currentSongId,
            queueSongIds = queueIds,
            originalQueueSongIds = origQueueIds,
            currentIndex = currentIndex,
            positionMs = positionMs,
            repeatMode = repeatModeStr,
            isShuffle = isShuffle
        )
    }

    fun clearState(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
