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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 4,
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN selectedFont TEXT NOT NULL DEFAULT 'SYSTEM'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN greetingStyle TEXT NOT NULL DEFAULT 'CLASSIC'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "abstra_pro.db"
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
