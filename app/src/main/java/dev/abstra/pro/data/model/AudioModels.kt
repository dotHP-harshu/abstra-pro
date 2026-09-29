package dev.abstra.pro.data.model

import android.net.Uri
import com.google.gson.annotations.SerializedName

/** Core song data class used throughout the app */
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val folderName: String,
    val filePath: String,
    val dateAdded: Long,
    val genre: String? = null,
    val albumId: Long = 0L,
    val isFavorite: Boolean = false,
    val coverUrl: String? = null,
    val lyrics: String? = null
) {
    val durationFormatted: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    val uri: Uri
        get() = Uri.parse(uriString)
}

/** Album data class with list of songs */
data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val albumArtUri: Uri? = null,
    val coverUrl: String? = null,
    val songs: List<Song> = emptyList()
)

/** Artist data model */
data class Artist(
    val name: String,
    val songCount: Int,
    val coverUrl: String? = null,
    val songs: List<Song> = emptyList()
)

/** Folder browsing item */
data class Folder(
    val name: String,
    val path: String,
    val songCount: Int,
    val songs: List<Song> = emptyList()
)

/** Playlist data model */
data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val songCount: Int = 0,
    val isSmartFavorites: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/** Storage volume descriptor (Internal storage vs SD Card) */
data class StorageInfo(
    val name: String,
    val path: String,
    val isPrimary: Boolean,
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val isRemovable: Boolean = !isPrimary,
    val description: String = name
)

/** Sorting options for song lists */
enum class SongSortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST_ASC("Artist (A-Z)"),
    ARTIST_DESC("Artist (Z-A)"),
    ALBUM_ASC("Album (A-Z)"),
    ALBUM_DESC("Album (Z-A)"),
    DATE_ADDED_DESC("Date Added (Newest)"),
    DATE_ADDED_ASC("Date Added (Oldest)"),
    DURATION_DESC("Duration (Longest)"),
    DURATION_ASC("Duration (Shortest)"),
    RECENTLY_ADDED("Recently Added")
}

/** Loop / Repeat modes */
enum class RepeatMode {
    REPEAT_ALL,
    REPEAT_ONE,
    REPEAT_OFF
}

/** UI Density scaling */
enum class UiDensity {
    COMPACT,
    MEDIUM,
    COMFORTABLE
}

/** Now-Playing Centerpiece Styles */
enum class PlayerDesignStyle {
    ROTATING_CD,
    VINYL_RECORD,
    POLAROID
}

// ─────────────────────────────────────────────────────────────
// JSON EXPORT / IMPORT DATA CONTRACTS (SAF BACKUP)
// ─────────────────────────────────────────────────────────────

data class BackupData(
    @SerializedName("version") val version: Int = 2,
    @SerializedName("exportTimestamp") val exportTimestamp: Long = System.currentTimeMillis(),
    @SerializedName("playlists") val playlists: List<PlaylistData>,
    @SerializedName("settings") val settings: List<SettingEntry>? = null,
    @SerializedName("songOverrides") val songOverrides: List<dev.abstra.pro.data.db.entities.SongOverrideEntity> = emptyList(),
    @SerializedName("appSettings") val appSettings: dev.abstra.pro.data.db.entities.SettingsEntity? = null,
    @SerializedName("widgetPrefs") val widgetPrefs: dev.abstra.pro.data.db.entities.WidgetPrefsEntity? = null,
    @SerializedName("customEqPresets") val customEqPresets: List<dev.abstra.pro.data.db.entities.CustomEqPresetEntity> = emptyList()
)

data class SettingEntry(
    @SerializedName("key") val key: String,
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: String
)

data class PlaylistData(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("songs") val songs: List<ExportSongData>
)

data class ExportSongData(
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String,
    @SerializedName("durationMs") val durationMs: Long,
    @SerializedName("relativeFilePath") val relativeFilePath: String? = null
)
