package dev.abstra.pro.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.abstra.pro.ai.AbstraAiEngine
import dev.abstra.pro.ai.model.AiMix
import dev.abstra.pro.audio.AudioEngine
import dev.abstra.pro.data.db.AppDatabase
import dev.abstra.pro.data.model.*
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

    // ── App Settings ──
    private val _currentTheme = MutableStateFlow(AppTheme.COTTON_CANDY)
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _currentDesignStyle = MutableStateFlow(PlayerDesignStyle.TURNTABLE_CD)
    val currentDesignStyle: StateFlow<PlayerDesignStyle> = _currentDesignStyle.asStateFlow()

    private val _writePhysicalTags = MutableStateFlow(false)
    val writePhysicalTags: StateFlow<Boolean> = _writePhysicalTags.asStateFlow()

    private val _minDurationFilterSec = MutableStateFlow(10)
    val minDurationFilterSec: StateFlow<Int> = _minDurationFilterSec.asStateFlow()

    // ── Audio Engine StateFlows Exposed to UI ──
    val currentSong: StateFlow<Song?> = audioEngine.currentSong
    val isPlaying: StateFlow<Boolean> = audioEngine.isPlaying
    val currentPosition: StateFlow<Long> = audioEngine.currentPosition
    val duration: StateFlow<Long> = audioEngine.duration
    val queue: StateFlow<List<Song>> = audioEngine.queue
    val repeatMode: StateFlow<RepeatMode> = audioEngine.repeatMode
    val isShuffle: StateFlow<Boolean> = audioEngine.isShuffle
    val playbackSpeed: StateFlow<Float> = audioEngine.playbackSpeed
    val crossfadeSeconds: StateFlow<Int> = audioEngine.crossfadeSeconds
    val gaplessEnabled: StateFlow<Boolean> = audioEngine.gaplessEnabled
    val sleepTimerRemainingSec: StateFlow<Int> = audioEngine.sleepTimerSecondsRemaining

    // ── Repository Library Data ──
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            repository.scanLocalMedia()
            // Regenerate AI mixes once songs loaded
            val initialSongs = allSongs.first { it.isNotEmpty() }
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

    // ── Library & Playlist Operations ──
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
        }
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            repository.scanLocalMedia()
            val songs = allSongs.value
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
            _lyricsTimeOffsetMs.value = 0L
            _currentLyrics.value = emptyList()

            // 1. Check matching .lrc file in same directory
            if (song.filePath.isNotBlank()) {
                val lrcFile = File(song.filePath.substringBeforeLast(".") + ".lrc")
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
