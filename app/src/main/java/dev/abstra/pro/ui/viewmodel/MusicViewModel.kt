package dev.abstra.pro.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.abstra.pro.ai.AbstraAiEngine
import dev.abstra.pro.ai.model.AiMix
import dev.abstra.pro.audio.AudioEngine
import dev.abstra.pro.data.db.AppDatabase
import dev.abstra.pro.data.db.entities.LyricsCacheEntity
import dev.abstra.pro.data.model.*
import dev.abstra.pro.data.repository.MetadataManager
import dev.abstra.pro.data.repository.MusicRepository
import dev.abstra.pro.lyrics.LyricsParser
import dev.abstra.pro.lyrics.model.LyricsLine
import dev.abstra.pro.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import dev.abstra.pro.ui.components.MainNavigationTab

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    val database = AppDatabase.getInstance(context)
    val repository = MusicRepository(context, database)
    val audioEngine = AudioEngine(context)
    val aiEngine = AbstraAiEngine(context, database)

    // ── Navigation & UI State ──
    private val _currentTab = MutableStateFlow(MainNavigationTab.HOME)
    val currentTab: StateFlow<MainNavigationTab> = _currentTab.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _isEqualizerOpen = MutableStateFlow(false)
    val isEqualizerOpen: StateFlow<Boolean> = _isEqualizerOpen.asStateFlow()

    private val _isLyricsOpen = MutableStateFlow(false)
    val isLyricsOpen: StateFlow<Boolean> = _isLyricsOpen.asStateFlow()

    private val _isSleepTimerDialogOpen = MutableStateFlow(false)
    val isSleepTimerDialogOpen: StateFlow<Boolean> = _isSleepTimerDialogOpen.asStateFlow()

    private val _isCreatePlaylistDialogOpen = MutableStateFlow(false)
    val isCreatePlaylistDialogOpen: StateFlow<Boolean> = _isCreatePlaylistDialogOpen.asStateFlow()

    private val _editingMetadataSong = MutableStateFlow<Song?>(null)
    val editingMetadataSong: StateFlow<Song?> = _editingMetadataSong.asStateFlow()

    private val prefs = context.getSharedPreferences("abstra_prefs", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(AppTheme.COTTON_CANDY)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _currentDesignStyle = MutableStateFlow(PlayerDesignStyle.VINYL_RECORD)
    val currentDesignStyle: StateFlow<PlayerDesignStyle> = _currentDesignStyle.asStateFlow()

    private val _writePhysicalTags = MutableStateFlow(false)
    val writePhysicalTags: StateFlow<Boolean> = _writePhysicalTags.asStateFlow()

    private val _minDurationFilterSec = MutableStateFlow(10)
    val minDurationFilterSec: StateFlow<Int> = _minDurationFilterSec.asStateFlow()

    private val _musicFolderUri = MutableStateFlow(prefs.getString("music_folder_uri", "") ?: "")
    val musicFolderUri: StateFlow<String> = _musicFolderUri.asStateFlow()

    fun setMusicFolderUri(uri: String) {
        prefs.edit().putString("music_folder_uri", uri).apply()
        _musicFolderUri.value = uri
        rescanLibrary()
    }

    // ── Audio Engine StateFlows Exposed to UI ──
    val currentSong: StateFlow<Song?> = audioEngine.currentSong
    val pendingSong: StateFlow<Song?> = audioEngine.pendingSong
    val isPlaying: StateFlow<Boolean> = audioEngine.isPlaying
    val currentPosition: StateFlow<Long> = audioEngine.currentPosition
    val duration: StateFlow<Long> = audioEngine.duration
    val queue: StateFlow<List<Song>> = audioEngine.queue
    val currentIndex: StateFlow<Int> = audioEngine.currentIndex
    val repeatMode: StateFlow<RepeatMode> = audioEngine.repeatMode
    val isShuffle: StateFlow<Boolean> = audioEngine.isShuffle
    val playbackSpeed: StateFlow<Float> = audioEngine.playbackSpeed
    val crossfadeSeconds: StateFlow<Int> = audioEngine.crossfadeSeconds
    val gaplessEnabled: StateFlow<Boolean> = audioEngine.gaplessEnabled
    val sleepTimerRemainingSec: StateFlow<Int> = audioEngine.sleepTimerSecondsRemaining

    // ── Repository Library Data ──
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLifetimePlaytime: StateFlow<Long> = repository.totalLifetimePlaytime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalPlayCount: StateFlow<Int> = repository.totalPlayCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Excluded (hidden) folders, persisted in prefs ──
    private val _excludedFolderPaths = MutableStateFlow(
        prefs.getStringSet("excluded_folders", emptySet())?.toSet() ?: emptySet()
    )
    val excludedFolderPaths: StateFlow<Set<String>> = _excludedFolderPaths.asStateFlow()

    fun excludeFolder(folderPath: String) {
        val updated = _excludedFolderPaths.value + folderPath
        prefs.edit().putStringSet("excluded_folders", updated).apply()
        _excludedFolderPaths.value = updated
    }

    val visibleSongs: StateFlow<List<Song>> = combine(
        allSongs,
        excludedFolderPaths,
        minDurationFilterSec
    ) { songs, excluded, minDurSec ->
        val minDurMs = minDurSec * 1000L
        songs.filter { song ->
            val parent = java.io.File(song.filePath).parent ?: return@filter true
            parent !in excluded && song.durationMs >= minDurMs
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreFolder(folderPath: String) {
        val updated = _excludedFolderPaths.value - folderPath
        prefs.edit().putStringSet("excluded_folders", updated).apply()
        _excludedFolderPaths.value = updated
    }

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .map { list ->
            list.map { entity ->
                val songsInPl = repository.getSongsForPlaylistSync(entity.id)
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    songCount = songsInPl.size
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isScanning: StateFlow<Boolean> = repository.isScanning

    // ── AI Engine Mixes ──
    val aiMixes: StateFlow<List<AiMix>> = aiEngine.aiMixes

    // ── Lyrics State ──
    private var _lastLyricsSongId = ""
    private val _currentLyrics = MutableStateFlow<List<LyricsLine>>(emptyList())
    val currentLyrics: StateFlow<List<LyricsLine>> = _currentLyrics.asStateFlow()

    private val _lyricsTimeOffsetMs = MutableStateFlow(0L)
    val lyricsTimeOffsetMs: StateFlow<Long> = _lyricsTimeOffsetMs.asStateFlow()

    init {
        // Wire telemetry from AudioEngine to AbstraAiEngine
        audioEngine.onSongStarted = { song ->
            aiEngine.onSongStarted(song)
            loadLyricsForSong(song)
        }
        audioEngine.onSongCompleted = { song ->
            aiEngine.onSongCompleted(song)
        }
        audioEngine.onSongSkipped = { song, playedSec, totalSec ->
            aiEngine.onSongSkipped(song, playedSec, totalSec)
        }

        // Initial scan on boot
        viewModelScope.launch {
            repository.scanLocalMedia(_musicFolderUri.value, _minDurationFilterSec.value)
            // Regenerate AI mixes once songs loaded
            val initialSongs = visibleSongs.first { it.isNotEmpty() }
            aiEngine.regenerateMixes(initialSongs)
        }
    }

    // ── Navigation Functions ──
    fun setTab(tab: MainNavigationTab) {
        _currentTab.value = tab
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    fun setEqualizerOpen(open: Boolean) {
        _isEqualizerOpen.value = open
    }

    fun setLyricsOpen(open: Boolean) {
        _isLyricsOpen.value = open
    }

    fun setSleepTimerDialogOpen(open: Boolean) {
        _isSleepTimerDialogOpen.value = open
    }

    fun setCreatePlaylistDialogOpen(open: Boolean) {
        _isCreatePlaylistDialogOpen.value = open
    }

    fun setEditingMetadataSong(song: Song?) {
        _editingMetadataSong.value = song
    }

    fun setTheme(theme: AppTheme) {
        _currentTheme.value = theme
    }

    fun setDesignStyle(style: PlayerDesignStyle) {
        _currentDesignStyle.value = style
    }

    fun setWritePhysicalTags(write: Boolean) {
        _writePhysicalTags.value = write
    }

    fun setMinDurationFilterSec(seconds: Int) {
        _minDurationFilterSec.value = seconds
        rescanLibrary()
    }

    // ── Playback Controls ──
    fun playSong(song: Song) {
        if (currentSong.value?.id == song.id) {
            togglePlayPause()
            return
        }
        val currentList = allSongs.value
        val index = currentList.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        audioEngine.setQueue(currentList, index, autoPlay = true)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        audioEngine.setQueue(songs, startIndex, autoPlay = true)
    }

    fun shuffleQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        audioEngine.setQueue(shuffled, 0, autoPlay = true)
    }

    fun playNext(song: Song) {
        audioEngine.enqueueNext(song)
    }

    fun addToQueue(song: Song) {
        audioEngine.enqueueLast(song)
    }

    fun removeQueueItem(song: Song) {
        audioEngine.removeQueueItem(song)
    }

    fun moveQueueItem(from: Int, to: Int) {
        audioEngine.moveQueueItem(from, to)
    }

    fun clearQueue() {
        audioEngine.clearUpNext()
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            audioEngine.pause()
        } else {
            audioEngine.resume()
        }
    }

    fun skipNext() {
        audioEngine.playNext()
    }

    fun skipPrevious() {
        audioEngine.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        audioEngine.seekTo(positionMs)
    }

    fun toggleShuffle() {
        audioEngine.toggleShuffle()
    }

    fun toggleRepeat() {
        audioEngine.toggleRepeatMode()
    }

    fun setPlaybackSpeed(speed: Float) {
        audioEngine.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int) {
        audioEngine.setSleepTimer(minutes)
    }

    fun setCrossfade(seconds: Int) {
        audioEngine.setCrossfade(seconds)
    }

    fun setGapless(enabled: Boolean) {
        audioEngine.setGapless(enabled)
    }

    private val _playlistPickerForSong = MutableStateFlow<Song?>(null)
    val playlistPickerForSong: StateFlow<Song?> = _playlistPickerForSong.asStateFlow()

    fun openPlaylistPicker(song: Song) { _playlistPickerForSong.value = song }
    fun closePlaylistPicker() { _playlistPickerForSong.value = null }

    // ── Library & Playlist Operations ──
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            repository.scanLocalMedia(_musicFolderUri.value, _minDurationFilterSec.value)
            val songs = visibleSongs.value
            if (songs.isNotEmpty()) {
                aiEngine.regenerateMixes(songs)
            }
        }
    }

    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song.id)
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songs: List<Song>) {
        viewModelScope.launch {
            repository.addSongsToPlaylist(playlistId, songs.map { it.id })
        }
    }

    suspend fun getSongsForPlaylist(playlistId: Long): List<Song> {
        val entityIds = repository.getSongsForPlaylistSync(playlistId).map { it.id }
        val currentMap = allSongs.value.associateBy { it.id }
        return entityIds.mapNotNull { currentMap[it] }
    }

    // ── Song Deletion ──
    private val _pendingDeleteSong = MutableStateFlow<Song?>(null)

    /**
     * Kicks off a device delete for the song. On API 30+ a system confirmation
     * dialog is shown; the caller launches the returned IntentSender and later
     * invokes [onSongDeleteConfirmed] with the user's decision.
     */
    suspend fun requestDeleteSong(song: Song): IntentSender? {
        val sender = repository.requestSongDelete(song)
        if (sender != null) {
            _pendingDeleteSong.value = song
        }
        return sender
    }

    fun onSongDeleteConfirmed(deleted: Boolean) {
        val song = _pendingDeleteSong.value
        _pendingDeleteSong.value = null
        if (!deleted || song == null) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSongFromLibrary(song.id)
        }
    }

    fun updateSongMetadata(
        songId: String,
        newTitle: String?,
        newArtist: String?,
        newAlbum: String?,
        newGenre: String?,
        filePath: String?
    ) {
        viewModelScope.launch {
            repository.updateSongMetadata(
                songId = songId,
                newTitle = newTitle,
                newArtist = newArtist,
                newAlbum = newAlbum,
                newGenre = newGenre,
                writeToFile = _writePhysicalTags.value,
                filePath = filePath
            )
        }
    }

    // ── SAF JSON Backup / Restore ──
    fun exportBackup(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.exportPlaylists(outputStream)
        }
    }

    fun importBackup(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.importPlaylists(inputStream)
        }
    }

    // ── Lyrics Management ──
    fun setLyricsOffset(offsetMs: Long) {
        _lyricsTimeOffsetMs.value = offsetMs
    }

    private fun loadLyricsForSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_currentLyrics.value.isNotEmpty() && _lastLyricsSongId == song.id) return@launch
            _lastLyricsSongId = song.id
            _lyricsTimeOffsetMs.value = 0L
            _currentLyrics.value = emptyList()

            // 1. Check matching .lrc file in same directory
            if (song.filePath.isNotBlank()) {
                val baseNoExt = song.filePath.substringBeforeLast('.', missingDelimiterValue = song.filePath)
                val lrcFile = File("$baseNoExt.lrc")
                if (lrcFile.exists()) {
                    val parsed = LyricsParser.parse(lrcFile.readText())
                    _currentLyrics.value = parsed
                    return@launch
                }
            }

            // 2. Check Room Database lyrics cache
            val cached = database.libraryDao().getLyrics(song.id)
            if (cached != null && cached.rawLrc.isNotBlank()) {
                val parsed = LyricsParser.parse(cached.rawLrc)
                _currentLyrics.value = parsed
                return@launch
            }

            // 3. Fall back to embedded lyrics tags (USLT / LYRICS) via jaudiotagger
            if (song.filePath.isNotBlank()) {
                val embedded = MetadataManager.readEmbeddedLyrics(song.filePath)
                if (!embedded.isNullOrBlank()) {
                    _currentLyrics.value = LyricsParser.parse(embedded)
                    // Cache so the next play is instant (no file re-parse)
                    database.libraryDao().saveLyrics(
                        LyricsCacheEntity(songId = song.id, rawLrc = embedded)
                    )
                }
            }
        }
    }

    fun importLrcFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().use { it.readText() }
                    val parsed = LyricsParser.parse(content)
                    _currentLyrics.value = parsed

                    val active = currentSong.value
                    if (active != null) {
                        database.libraryDao().saveLyrics(
                            dev.abstra.pro.data.db.entities.LyricsCacheEntity(
                                songId = active.id,
                                rawLrc = content
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
