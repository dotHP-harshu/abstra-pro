package dev.abstra.pro.data.db.dao

import androidx.room.*
import dev.abstra.pro.data.db.entities.CustomEqPresetEntity
import dev.abstra.pro.data.db.entities.EffectSettingsEntity
import dev.abstra.pro.data.db.entities.WidgetPrefsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    // ── Audio Effects Settings ──
    @Query("SELECT * FROM effect_settings WHERE id = 1 LIMIT 1")
    fun getEffectSettings(): Flow<EffectSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveEffectSettings(settings: EffectSettingsEntity)

    // ── Custom EQ Presets ──
    @Query("SELECT * FROM custom_eq_presets ORDER BY name ASC")
    fun getCustomPresets(): Flow<List<CustomEqPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomPreset(preset: CustomEqPresetEntity)

    @Query("DELETE FROM custom_eq_presets WHERE name = :name")
    suspend fun deleteCustomPreset(name: String)

    // ── Widget Prefs ──
    @Query("SELECT * FROM widget_prefs WHERE id = 1 LIMIT 1")
    fun getWidgetPrefs(): Flow<WidgetPrefsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWidgetPrefs(prefs: WidgetPrefsEntity)
}
