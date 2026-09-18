package dev.abstra.pro.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import com.google.gson.Gson
import dev.abstra.pro.data.db.AppDatabase
import dev.abstra.pro.data.db.entities.*
import dev.abstra.pro.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class MusicRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val libraryDao = db.libraryDao()
    private val playlistDao = db.playlistDao()
    private val telemetryDao = db.telemetryDao()
    private val settingsDao = db.settingsDao()
    private val gson = Gson()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // ── Streams of data ──
    val rawSongs: Flow<List<SongEntity>> = libraryDao.getAllSongs()
    val overrides: Flow<List<SongOverrideEntity>> = libraryDao.getAllOverrides()

    /** Combined songs merged with user metadata overrides */
    val allSongs: Flow<List<Song>> = combine(rawSongs, overrides) { songs, overrideList ->
        val overrideMap = overrideList.associateBy { it.songId }
        songs.map { entity ->
            val ov = overrideMap[entity.id]
            Song(
                id = entity.id,
                title = ov?.customTitle ?: entity.title,
                artist = ov?.customArtist ?: entity.artist,
                album = ov?.customAlbum ?: entity.album,
                durationMs = entity.durationMs,
                uriString = entity.uriString,
                folderName = entity.folderName,
                filePath = entity.filePath,
                dateAdded = entity.dateAdded,
                genre = ov?.customGenre ?: entity.genre,
                albumId = entity.albumId,
                isFavorite = ov?.isFavorite ?: entity.isFavorite,
                coverUrl = ov?.customCoverUri,
                lyrics = entity.lyrics
            )
        }
    }

    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
    val playlistsWithCount: Flow<List<dev.abstra.pro.data.db.entities.PlaylistWithCount>> = playlistDao.getAllPlaylistsWithCount()
    val totalLifetimePlaytime: Flow<Long> = telemetryDao.getTotalLifetimePlaytime().map { it ?: 0L }
    val totalPlayCount: Flow<Int> = telemetryDao.getTotalPlayCount().map { it ?: 0 }

    // ─────────────────────────────────────────────────────────────
    // MEDIASTORE SCANNER (Filters unwanted audio, <10s audio, voice notes)
    // ─────────────────────────────────────────────────────────────

    suspend fun scanLocalMedia(targetFolderUri: String = "", minDurationSec: Int = 10) = withContext(Dispatchers.IO) {
        if (_isScanning.value) return@withContext
        _isScanning.value = true
        val foundSongs = mutableListOf<SongEntity>()
        val prefs = context.getSharedPreferences("abstra_prefs", Context.MODE_PRIVATE)

        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ALBUM_ID
            )

            // Filter out tracks shorter than 10 seconds and ringtones/alarms
            var selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ${minDurationSec * 1000}"
            var selectionArgs: Array<String>? = null

            if (targetFolderUri.isNotBlank()) {
                try {
                    val uri = Uri.parse(targetFolderUri)
                    val pathSegment = uri.lastPathSegment?.substringAfterLast(":")
                    if (!pathSegment.isNullOrBlank()) {
                        val customFolder = File(android.os.Environment.getExternalStorageDirectory(), pathSegment)
                        if (customFolder.exists()) {
                            selection += " AND ${MediaStore.Audio.Media.DATA} LIKE ?"
                            selectionArgs = arrayOf("${customFolder.absolutePath}/%")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MusicRepository", "Failed to parse target folder URI", e)
                }
            }

            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    val mediaId = c.getLong(idCol)
                    val rawTitle = c.getString(titleCol) ?: "Unknown"
                    val rawArtist = c.getString(artistCol) ?: "Unknown Artist"
                    val rawAlbum = c.getString(albumCol) ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val path = c.getString(dataCol) ?: ""
                    val dateAdded = c.getLong(dateCol)
                    val albumId = c.getLong(albumIdCol)

                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId).toString()
                    val folderName = if (path.isNotEmpty()) {
                        File(path).parentFile?.name ?: "Unknown"
                    } else "Unknown"

                    foundSongs.add(
                        SongEntity(
                            id = mediaId.toString(),
                            title = rawTitle,
                            artist = if (rawArtist == "<unknown>") "Unknown Artist" else rawArtist,
                            album = if (rawAlbum == "<unknown>") "Unknown Album" else rawAlbum,
                            durationMs = duration,
                            uriString = uri,
                            folderName = folderName,
                            filePath = path,
                            dateAdded = dateAdded,
                            genre = null,
                            albumId = albumId,
                            isFavorite = false,
                            lyrics = null
                        )
                    )
                }
            }

            val excluded = prefs.getStringSet("excluded_folders", emptySet()) ?: emptySet()
            val filtered = foundSongs.filter { entity ->
                val parent = java.io.File(entity.filePath).parent ?: return@filter true
                parent !in excluded
            }
            val currentSongs = libraryDao.getAllSongs().first()
            val newIds = filtered.map { it.id }.toSet()
            val toDelete = currentSongs.filter { it.id !in newIds }
            toDelete.forEach { libraryDao.deleteSong(it.id) }
            
            if (filtered.isNotEmpty()) {
                libraryDao.insertSongs(filtered)
            }
            Log.i("MusicRepository", "Scanned ${foundSongs.size} songs from storage. Deleted ${toDelete.size} stale tracks.")
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to scan local media", e)
        } finally {
            _isScanning.value = false
        }
    }

    // ─────────────────────────────────────────────────────────────
    // FAVORITES & METADATA OVERRIDES
    // ─────────────────────────────────────────────────────────────

    suspend fun toggleFavorite(songId: String, currentFav: Boolean) = withContext(Dispatchers.IO) {
        val newFav = !currentFav
        libraryDao.saveOverride(
            SongOverrideEntity(
                songId = songId,
                isFavorite = newFav
            )
        )
    }

    suspend fun updateSongMetadata(
        songId: String,
        newTitle: String?,
        newArtist: String?,
        newAlbum: String?,
        newGenre: String?,
        writeToFile: Boolean,
        filePath: String?
    ) = withContext(Dispatchers.IO) {
        // App-only database override
        libraryDao.saveOverride(
            SongOverrideEntity(
                songId = songId,
                customTitle = newTitle,
                customArtist = newArtist,
                customAlbum = newAlbum,
                customGenre = newGenre
            )
        )

        // Opt-in physical file tag writing
        if (writeToFile && !filePath.isNullOrBlank()) {
            MetadataManager.writeMetadataToFile(filePath, newTitle, newArtist, newAlbum, newGenre)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PLAYLIST MANAGEMENT
    // ─────────────────────────────────────────────────────────────

    suspend fun createPlaylist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name, description = description))
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(id)
        playlistDao.clearPlaylist(id)
    }

    suspend fun renamePlaylist(id: Long, newName: String, newDesc: String) = withContext(Dispatchers.IO) {
        playlistDao.updatePlaylist(id, newName, newDesc)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        val existing = playlistDao.getSongsInPlaylistSync(playlistId)
        playlistDao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                sortOrder = existing.size
            )
        )
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<String>) = withContext(Dispatchers.IO) {
        val existing = playlistDao.getSongsInPlaylistSync(playlistId)
        var order = existing.size
        val items = songIds.map { id ->
            PlaylistSongEntity(playlistId = playlistId, songId = id, sortOrder = order++)
        }
        playlistDao.insertPlaylistSongs(items)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun updatePlaylistOrder(playlistId: Long, songIds: List<String>) = withContext(Dispatchers.IO) {
        val items = songIds.mapIndexed { index, id ->
            PlaylistSongEntity(playlistId = playlistId, songId = id, sortOrder = index)
        }
        playlistDao.insertPlaylistSongs(items)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> =
        playlistDao.getSongsInPlaylist(playlistId)

    suspend fun getSongsForPlaylistSync(playlistId: Long): List<SongEntity> =
        playlistDao.getSongsInPlaylistSync(playlistId)

    // ─────────────────────────────────────────────────────────────
    // PLAYLIST BACKUP / RESTORE VIA STORAGE ACCESS FRAMEWORK (JSON)
    // ─────────────────────────────────────────────────────────────

    suspend fun exportPlaylists(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val allPl = playlistDao.getAllPlaylists().first()
            val exportList = mutableListOf<PlaylistData>()

            for (pl in allPl) {
                val songs = playlistDao.getSongsInPlaylistSync(pl.id)
                val exportSongs = songs.map { s ->
                    ExportSongData(
                        title = s.title,
                        artist = s.artist,
                        album = s.album,
                        durationMs = s.durationMs,
                        relativeFilePath = s.filePath
                    )
                }
                exportList.add(PlaylistData(name = pl.name, description = pl.description, songs = exportSongs))
            }

            val exportData = PlaylistExportData(playlists = exportList)
            val jsonString = gson.toJson(exportData)
            outputStream.bufferedWriter().use { it.write(jsonString) }
            true
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to export playlists", e)
            false
        }
    }

    suspend fun importPlaylists(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val importData = gson.fromJson(jsonString, PlaylistExportData::class.java) ?: return@withContext false
            val currentSongs = libraryDao.getAllSongs().first()

            for (pl in importData.playlists) {
                val playlistId = playlistDao.insertPlaylist(PlaylistEntity(name = pl.name, description = pl.description))
                var sort = 0
                for (es in pl.songs) {
                    val match = currentSongs.find {
                        it.title.equals(es.title, ignoreCase = true) &&
                        it.artist.equals(es.artist, ignoreCase = true)
                    } ?: currentSongs.find { it.title.equals(es.title, ignoreCase = true) }

                    if (match != null) {
                        playlistDao.insertPlaylistSong(
                            PlaylistSongEntity(playlistId = playlistId, songId = match.id, sortOrder = sort++)
                        )
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to import playlists", e)
            false
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SONG DELETION (MediaStore + local DB cleanup)
    // ─────────────────────────────────────────────────────────────

    /**
     * Requests a system-confirmed delete for the given song.
     * Returns an IntentSender the caller must launch (API 30+), or null when
     * the song was deleted directly (legacy path) or the request failed.
     */
    suspend fun requestSongDelete(song: Song): IntentSender? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(song.uriString)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                MediaStore.createDeleteRequest(context.contentResolver, listOf(uri)).intentSender
            } else {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) deleteSongFromLibrary(song.id)
                null
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Failed to request delete for ${song.title}", e)
            null
        }
    }

    /** Removes all local DB traces of a song (library, overrides, playlists, telemetry, lyrics). */
    suspend fun deleteSongFromLibrary(songId: String) = withContext(Dispatchers.IO) {
        libraryDao.deleteSong(songId)
        libraryDao.deleteOverride(songId)
        libraryDao.deleteLyrics(songId)
        playlistDao.removeSongFromAllPlaylists(songId)
        telemetryDao.deleteTelemetry(songId)
    }

    // ─────────────────────────────────────────────────────────────
    // STORAGE VOLUMES (Internal vs SD Card)
    // ─────────────────────────────────────────────────────────────

    fun getStorageVolumes(): List<StorageInfo> {
        val volumes = mutableListOf<StorageInfo>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sm = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            for (vol in sm.storageVolumes) {
                val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    vol.directory
                } else {
                    @Suppress("DEPRECATION")
                    if (vol.isPrimary) android.os.Environment.getExternalStorageDirectory()
                    else null
                }
                dir?.let {
                    volumes.add(StorageInfo(vol.getDescription(context), it.absolutePath, vol.isPrimary))
                }
            }
        }
        if (volumes.isEmpty()) {
            @Suppress("DEPRECATION")
            val primary = android.os.Environment.getExternalStorageDirectory()
            volumes.add(StorageInfo("Internal Storage", primary.absolutePath, true))
        }
        return volumes
    }
}
