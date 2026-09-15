package dev.abstra.pro.data.db.dao

import androidx.room.*
import dev.abstra.pro.data.db.entities.LyricsCacheEntity
import dev.abstra.pro.data.db.entities.SongEntity
import dev.abstra.pro.data.db.entities.SongOverrideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
    suspend fun getSongById(id: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSong(id: String)

    @Query("DELETE FROM songs")
    suspend fun clearAllSongs()

    // ── Overrides ──
    @Query("SELECT * FROM song_overrides")
    fun getAllOverrides(): Flow<List<SongOverrideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOverride(override: SongOverrideEntity)

    @Query("UPDATE song_overrides SET isFavorite = :isFav WHERE songId = :songId")
    suspend fun setFavorite(songId: String, isFav: Boolean)

    @Query("DELETE FROM song_overrides WHERE songId = :songId")
    suspend fun deleteOverride(songId: String)

    // ── Lyrics Cache ──
    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId LIMIT 1")
    suspend fun getLyrics(songId: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(lyrics: LyricsCacheEntity)
}
