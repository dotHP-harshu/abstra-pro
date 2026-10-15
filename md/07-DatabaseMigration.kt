// =====================================================================
// 07-DatabaseMigration.kt
// ---------------------------------------------------------------------
// Adds the two new columns the font & name-style customization needs to
// persist in the SettingsEntity.  We DO NOT persist the actual font
// objects in Room — we persist the enum key (a String) and re-resolve
// via AppFont.fromKey() / GreetingStyle.fromKey() at read time.  This
// keeps the schema small and forward-compatible.
//
// If you also want to mirror these settings in Room (e.g. for backup
// sync), the alternative is to leave AppPreferences as the fast in-
// memory store and just add the new columns to SettingsEntity so the
// export/import path captures them.  That is what this migration does.
//
// All existing rows keep their previous values; the new columns default
// to "SYSTEM" / "CLASSIC".
// =====================================================================
package dev.abstra.pro.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from schema version 1 → 2.  Adds:
 *   - selectedFont TEXT NOT NULL DEFAULT "SYSTEM"
 *   - greetingStyle TEXT NOT NULL DEFAULT "CLASSIC"
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE app_settings ADD COLUMN selectedFont TEXT NOT NULL DEFAULT 'SYSTEM'")
        db.execSQL("ALTER TABLE app_settings ADD COLUMN greetingStyle TEXT NOT NULL DEFAULT 'CLASSIC'")
    }
}

// =====================================================================
// Updated SettingsEntity (replace the existing one in
// app/src/main/java/dev/abstra/pro/data/db/entities/SettingsEntity.kt)
// =====================================================================
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
    val playlistOrder: String = "",

    // NEW (v2):
    val selectedFont: String = "SYSTEM",
    val greetingStyle: String = "CLASSIC",
)

// =====================================================================
// Updated SettingsDao additions (merge into existing dao/SettingsDao.kt)
// =====================================================================
package dev.abstra.pro.data.db.dao

import androidx.room.*
import dev.abstra.pro.data.db.entities.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    // existing methods ...

    @Query("UPDATE app_settings SET selectedFont = :font WHERE id = 0")
    suspend fun setSelectedFont(font: String)

    @Query("UPDATE app_settings SET greetingStyle = :style WHERE id = 0")
    suspend fun setGreetingStyle(style: String)
}

// =====================================================================
// Register the migration when you create the database:
// =====================================================================
//
//     Room.databaseBuilder(app, AppDatabase::class.java, "abstra.db")
//         .addMigrations(MIGRATION_1_2)
//         .build()
//
// If you have not yet shipped v1, you can simply bump the schema
// version to 2 and add the new columns to the entity without writing a
// migration.  Otherwise, register MIGRATION_1_2 above.
// =====================================================================