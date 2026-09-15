package dev.abstra.pro.data.db.dao

import androidx.room.*
import dev.abstra.pro.data.db.entities.MarkovTransitionEntity
import dev.abstra.pro.data.db.entities.PlaybackTelemetryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {
    @Query("SELECT * FROM playback_telemetry")
    fun getAllTelemetry(): Flow<List<PlaybackTelemetryEntity>>

    @Query("SELECT * FROM playback_telemetry")
    suspend fun getAllTelemetrySync(): List<PlaybackTelemetryEntity>

    @Query("SELECT * FROM playback_telemetry WHERE songId = :songId LIMIT 1")
    suspend fun getTelemetry(songId: String): PlaybackTelemetryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTelemetry(telemetry: PlaybackTelemetryEntity)

    // ── Markov Chain Transitions ──
    @Query("SELECT * FROM markov_transitions WHERE fromSongId = :fromSongId")
    suspend fun getTransitionsFrom(fromSongId: String): List<MarkovTransitionEntity>

    @Query("SELECT * FROM markov_transitions WHERE fromSongId = :fromSongId AND toSongId = :toSongId LIMIT 1")
    suspend fun getTransition(fromSongId: String, toSongId: String): MarkovTransitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordTransition(transition: MarkovTransitionEntity)

    // ── Aggregated stats ──
    @Query("SELECT SUM(totalPlayedMs) FROM playback_telemetry")
    fun getTotalLifetimePlaytime(): Flow<Long?>

    @Query("SELECT SUM(playCount) FROM playback_telemetry")
    fun getTotalPlayCount(): Flow<Int?>
}
