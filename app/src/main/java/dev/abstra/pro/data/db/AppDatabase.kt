package dev.abstra.pro.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.abstra.pro.data.db.dao.LibraryDao
import dev.abstra.pro.data.db.dao.PlaylistDao
import dev.abstra.pro.data.db.dao.SettingsDao
import dev.abstra.pro.data.db.dao.TelemetryDao
import dev.abstra.pro.data.db.entities.*

@Database(
    entities = [
        SongEntity::class,
        SongOverrideEntity::class,
        PlaylistEntity::class,
        PlaylistSongEntity::class,
        PlaybackTelemetryEntity::class,
        MarkovTransitionEntity::class,
        EffectSettingsEntity::class,
        CustomEqPresetEntity::class,
        LyricsCacheEntity::class,
        WidgetPrefsEntity::class,
        SettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun telemetryDao(): TelemetryDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = getDatabase(context)

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "abstra_pro.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
