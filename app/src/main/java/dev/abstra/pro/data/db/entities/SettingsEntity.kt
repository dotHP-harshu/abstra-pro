package dev.abstra.pro.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val userName: String? = null,
    val greetingEnabled: Boolean = true,
    val hasAskedForName: Boolean = false,
    val excludedFolderPaths: String = "",
    val themeMode: String = "system",
    val accentColor: String = "",
    val crossfadeSeconds: Int = 0,
    val gaplessEnabled: Boolean = false,
    val defaultSleepTimerMin: Int = 30,
    val keepScreenOnInPlayer: Boolean = false,
    val sortOption: String = "date_desc",
    val showQueueOnSongClick: Boolean = false,
    val playlistOrder: String = "", // Storing lists as CSV strings for simplicity, or we can use TypeConverters
    val selectedFont: String = "SYSTEM",
    val greetingStyle: String = "CLASSIC"
)
