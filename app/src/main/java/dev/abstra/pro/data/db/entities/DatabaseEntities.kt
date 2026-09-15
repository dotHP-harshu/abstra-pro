package dev.abstra.pro.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cached media store song entity */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val folderName: String,
    val filePath: String,
    val dateAdded: Long,
    val genre: String?,
    val albumId: Long,
    val isFavorite: Boolean = false
)

/** User custom metadata overrides (App-only edit mode) */
@Entity(tableName = "song_overrides")
data class SongOverrideEntity(
    @PrimaryKey val songId: String,
    val customTitle: String? = null,
    val customArtist: String? = null,
    val customAlbum: String? = null,
    val customGenre: String? = null,
    val customCoverUri: String? = null,
    val isFavorite: Boolean = false
)

/** User playlist */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** Playlist song mapping */
@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: String,
    val sortOrder: Int
)

/** On-device playback telemetry for AI learning & stats */
@Entity(tableName = "playback_telemetry")
data class PlaybackTelemetryEntity(
    @PrimaryKey val songId: String,
    val playCount: Int = 0,
    val fullCompletions: Int = 0,
    val fastSkips: Int = 0,
    val consecutiveSkips: Int = 0,
    val repeatCount: Int = 0,
    val morningPlays: Int = 0,
    val afternoonPlays: Int = 0,
    val eveningPlays: Int = 0,
    val nightPlays: Int = 0,
    val weekdayPlays: Int = 0,
    val weekendPlays: Int = 0,
    val lastPlayedTimestamp: Long = 0L,
    val totalPlayedMs: Long = 0L
)

/** Markov chain transitions between consecutive tracks */
@Entity(tableName = "markov_transitions", primaryKeys = ["fromSongId", "toSongId"])
data class MarkovTransitionEntity(
    val fromSongId: String,
    val toSongId: String,
    val transitionCount: Int = 1
)

/** Audio effect settings persistence */
@Entity(tableName = "effect_settings")
data class EffectSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isEqEnabled: Boolean = false,
    val activePresetName: String = "Pop",
    val eqBandsString: String = "0,0,0,0,0,0,0,0,0,0",
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val loudnessGain: Int = 0,
    val balancePan: Float = 0.5f,
    val reverbPreset: Int = 0,
    val dynamicsPreset: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val crossfadeSeconds: Int = 0,
    val gaplessEnabled: Boolean = true
)

/** User saved custom EQ presets */
@Entity(tableName = "custom_eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey val name: String,
    val bandLevelsString: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Synced lyrics cache */
@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey val songId: String,
    val rawLrc: String,
    val userOffsetMs: Long = 0L,
    val cachedTimestamp: Long = System.currentTimeMillis()
)

/** Home-screen widget preferences */
@Entity(tableName = "widget_prefs")
data class WidgetPrefsEntity(
    @PrimaryKey val id: Int = 1,
    val coverShape: String = "ROUNDED", // ROUNDED or CIRCLE
    val blurStrength: Float = 30.0f,
    val useSolidBg: Boolean = false,
    val accentColor: Int = 0
)
