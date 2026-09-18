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
import com.google.gson.Gson

enum class FullPlayerBackground { GRADIENT, SOLID }

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application.applicationContext
    val database = AppDatabase.getInstance(context)
    val repository = MusicRepository(context, database)
    val aiEngine = AbstraAiEngine(context, database)

    /**
     * AudioEngine is owned by MusicPlaybackService. The viewmodel reads through
     * this singleton accessor — null until the service has been started.
     */
    private val engine: AudioEngine?
        get() = dev.abstra.pro.service.MusicPlaybackService.audioEngine

    private val prefs = context.getSharedPreferences("abstra_prefs", Context.MODE_PRIVATE)

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

    private val _isOutputRoutePickerOpen = MutableStateFlow(false)
    val isOutputRoutePickerOpen: StateFlow<Boolean> = _isOutputRoutePickerOpen.asStateFlow()

    private val _isCreatePlaylistDialogOpen = MutableStateFlow(false)
    val isCreatePlaylistDialogOpen: StateFlow<Boolean> = _isCreatePlaylistDialogOpen.asStateFlow()

    private val _editingMetadataSong = MutableStateFlow<Song?>(null)
    val editingMetadataSong: StateFlow<Song?> = _editingMetadataSong.asStateFlow()

    private val _currentTheme = MutableStateFlow(
        AppTheme.values().getOrNull(prefs.getInt("current_theme_idx", 0)) ?: AppTheme.COTTON_CANDY
    )
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _visualizerEnabled = MutableStateFlow(prefs.getBoolean("visualizer_enabled", true))
    val visualizerEnabled: StateFlow<Boolean> = _visualizerEnabled.asStateFlow()

    private val _fullPlayerBg = MutableStateFlow(
        FullPlayerBackground.valueOf(
            prefs.getString("full_player_bg", FullPlayerBackground.GRADIENT.name) 
                ?: FullPlayerBackground.GRADIENT.name
        )
    )
    val fullPlayerBg: StateFlow<FullPlayerBackground> = _fullPlayerBg.asStateFlow()

    private val _showTabBar = MutableStateFlow(prefs.getBoolean("show_tab_bar", true))
    val showTabBar: StateFlow<Boolean> = _showTabBar.asStateFlow()

    private val _customAppName = MutableStateFlow(prefs.getString("custom_app_name", "Abstra Pro") ?: "Abstra Pro")
    val customAppName: StateFlow<String> = _customAppName.asStateFlow()

    private val _currentDesignStyle = MutableStateFlow(
        PlayerDesignStyle.values().getOrNull(prefs.getInt("current_design_idx", 1)) ?: PlayerDesignStyle.VINYL_RECORD
    )
    val currentDesignStyle: StateFlow<PlayerDesignStyle> = _currentDesignStyle.asStateFlow()

    private val _writePhysicalTags = MutableStateFlow(prefs.getBoolean("write_physical_tags", false))
    val writePhysicalTags: StateFlow<Boolean> = _writePhysicalTags.asStateFlow()

    private val _minDurationFilterSec = MutableStateFlow(prefs.getInt("min_duration_filter_sec", 10))
    val minDurationFilterSec: StateFlow<Int> = _minDurationFilterSec.asStateFlow()

    private val _musicFolderUri = MutableStateFlow(prefs.getString("music_folder_uri", "") ?: "")
    val musicFolderUri: StateFlow<String> = _musicFolderUri.asStateFlow()

    fun setMusicFolderUri(uri: String) {
        prefs.edit().putString("music_folder_uri", uri).apply()
        _musicFolderUri.value = uri
        rescanLibrary()
    }

    private val _backgroundPlayback = MutableStateFlow(
        prefs.getBoolean("background_playback", true)        // default ON — the Lune-style default
    )
    val backgroundPlayback: StateFlow<Boolean> = _backgroundPlayback.asStateFlow()

    fun setBackgroundPlayback(enabled: Boolean) {
        // commit() not apply() — onTaskRemoved in the service reads this prefs
        // value synchronously when the user swipes from recents.
        prefs.edit().putBoolean("background_playback", enabled).commit()
        _backgroundPlayback.value = enabled
        // No engine call needed — the service reads prefs directly in onTaskRemoved.
    }

    private val _shareAudioFocus = MutableStateFlow(
        prefs.getBoolean("share_audio_focus", false)         // default OFF — exclusive playback
    )
    val shareAudioFocus: StateFlow<Boolean> = _shareAudioFocus.asStateFlow()

    fun setShareAudioFocus(enabled: Boolean) {
        prefs.edit().putBoolean("share_audio_focus", enabled).commit()
        _shareAudioFocus.value = enabled
        // Tell the engine to update its internal focus policy and re-acquire focus.
        engine?.setShareAudioFocusEnabled(enabled)
    }

    // ── Audio Engine StateFlows Exposed to UI ──
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    private val _pendingSong = MutableStateFlow<Song?>(null)
    val pendingSong: StateFlow<Song?> = _pendingSong.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    private val _repeatMode = MutableStateFlow(RepeatMode.REPEAT_ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()
    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    private val _crossfadeSeconds = MutableStateFlow(0)
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()
    private val _gaplessEnabled = MutableStateFlow(true)
    val gaplessEnabled: StateFlow<Boolean> = _gaplessEnabled.asStateFlow()
    private val _sleepTimerSecondsRemaining = MutableStateFlow(0)
    val sleepTimerRemainingSec: StateFlow<Int> = _sleepTimerSecondsRemaining.asStateFlow()

    val availableRoutes: StateFlow<List<dev.abstra.pro.ui.screens.AvailableRoute>> = 
        dev.abstra.pro.service.MusicPlaybackService.instance?.availableRoutes ?: MutableStateFlow(emptyList())

    fun selectRoute(id: String) {
        dev.abstra.pro.service.MusicPlaybackService.instance?.selectRoute(id)
    }

    // ── Repository Library Data ──
    val allSongs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLifetimePlaytime: StateFlow<Long> = repository.totalLifetimePlaytime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalPlayCount: StateFlow<Int> = repository.totalPlayCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Recently Played ──
    private val _recentlyPlayedIds = MutableStateFlow<List<String>>(
        prefs.getString("recently_played_ids", "")
            ?.split(",")?.filter { it.isNotBlank() }
            ?: emptyList()
    )
    val recentlyPlayedIds: StateFlow<List<String>> = _recentlyPlayedIds.asStateFlow()

    val recentlyPlayed: StateFlow<List<Song>> = combine(
        recentlyPlayedIds, allSongs
    ) { ids, songs ->
        ids.mapNotNull { id -> songs.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addToRecentlyPlayed(songId: String) {
        val current = _recentlyPlayedIds.value
        val updated = (listOf(songId) + current.filter { it != songId }).take(20)
        _recentlyPlayedIds.value = updated
        prefs.edit().putString("recently_played_ids", updated.joinToString(",")).apply()
    }

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

    private val _sortOption = MutableStateFlow(
        runCatching { dev.abstra.pro.data.model.SongSortOption.valueOf(prefs.getString("sort_option", dev.abstra.pro.data.model.SongSortOption.TITLE_ASC.name)!!) }
            .getOrDefault(dev.abstra.pro.data.model.SongSortOption.TITLE_ASC)
    )
    val sortOption: StateFlow<dev.abstra.pro.data.model.SongSortOption> = _sortOption.asStateFlow()

    fun setSortOption(option: dev.abstra.pro.data.model.SongSortOption) {
        prefs.edit().putString("sort_option", option.name).apply()
        _sortOption.value = option
    }

    val visibleSongs: StateFlow<List<Song>> = combine(
        allSongs,
        excludedFolderPaths,
        minDurationFilterSec,
        sortOption
    ) { songs, excluded, minDurSec, sortOpt ->
        val minDurMs = minDurSec * 1000L
        val filtered = songs.filter { song ->
            val parent = java.io.File(song.filePath).parent ?: return@filter true
            parent !in excluded && song.durationMs >= minDurMs
        }
        when (sortOpt) {
            dev.abstra.pro.data.model.SongSortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            dev.abstra.pro.data.model.SongSortOption.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            dev.abstra.pro.data.model.SongSortOption.ARTIST_ASC -> filtered.sortedBy { it.artist.lowercase() }
            dev.abstra.pro.data.model.SongSortOption.ARTIST_DESC -> filtered.sortedByDescending { it.artist.lowercase() }
            dev.abstra.pro.data.model.SongSortOption.ALBUM_ASC -> filtered.sortedBy { it.album.lowercase() }
            dev.abstra.pro.data.model.SongSortOption.ALBUM_DESC -> filtered.sortedByDescending { it.album.lowercase() }
            dev.abstra.pro.data.model.SongSortOption.DATE_ADDED_DESC, dev.abstra.pro.data.model.SongSortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.dateAdded }
            dev.abstra.pro.data.model.SongSortOption.DATE_ADDED_ASC -> filtered.sortedBy { it.dateAdded }
            dev.abstra.pro.data.model.SongSortOption.DURATION_DESC -> filtered.sortedByDescending { it.durationMs }
            dev.abstra.pro.data.model.SongSortOption.DURATION_ASC -> filtered.sortedBy { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreFolder(folderPath: String) {
        val updated = _excludedFolderPaths.value - folderPath
        prefs.edit().putStringSet("excluded_folders", updated).apply()
        _excludedFolderPaths.value = updated
    }

    val playlists: StateFlow<List<Playlist>> = repository.playlistsWithCount
        .map { list ->
            list.map { entity ->
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    songCount = entity.songCount
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

    private val _lyricsLoadingState = MutableStateFlow(false)
    val lyricsLoadingState: StateFlow<Boolean> = _lyricsLoadingState.asStateFlow()

    private val _lyricsTimeOffsetMs = MutableStateFlow(0L)
    val lyricsTimeOffsetMs: StateFlow<Long> = _lyricsTimeOffsetMs.asStateFlow()

    private suspend fun waitForEngine(): AudioEngine {
        while (true) {
            dev.abstra.pro.service.MusicPlaybackService.audioEngine?.let { return it }
            kotlinx.coroutines.delay(50)
        }
    }

    init {
        viewModelScope.launch {
            // Wait for the service's engine, then mirror its flows.
            val e = waitForEngine()
            
            // Wire telemetry
            e.onSongStarted = { song ->
                aiEngine.onSongStarted(song)
                loadLyricsForSong(song)
                addToRecentlyPlayed(song.id)
            }
            e.onSongCompleted = { song ->
                aiEngine.onSongCompleted(song)
            }
            e.onSongSkipped = { song, playedSec, totalSec ->
                aiEngine.onSongSkipped(song, playedSec, totalSec)
            }

            launch { e.currentSong.collect { current ->
                if (current == null) _currentSong.value = null
                else _currentSong.value = allSongs.value.find { it.id == current.id } ?: current
            } }
            launch { e.isPlaying.collect { _isPlaying.value = it } }
            launch { e.currentPosition.collect { _currentPosition.value = it } }
            launch { e.duration.collect { _duration.value = it } }
            launch { e.queue.collect { _queue.value = it } }
            launch { e.currentIndex.collect { _currentIndex.value = it } }
            launch { e.repeatMode.collect { _repeatMode.value = it } }
            launch { e.isShuffle.collect { _isShuffle.value = it } }
            launch { e.playbackSpeed.collect { _playbackSpeed.value = it } }
            launch { e.crossfadeSeconds.collect { _crossfadeSeconds.value = it } }
            launch { e.gaplessEnabled.collect { _gaplessEnabled.value = it } }
            launch { e.sleepTimerSecondsRemaining.collect { _sleepTimerSecondsRemaining.value = it } }
            launch { e.pendingSong.collect { _pendingSong.value = it } }

            // Init Audio Engine settings from SharedPreferences
            e.setCrossfade(prefs.getInt("crossfade_sec", 0))
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
        prefs.edit().putInt("current_tab_idx", tab.ordinal).apply()
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

    fun setOutputRoutePickerOpen(open: Boolean) {
        _isOutputRoutePickerOpen.value = open
    }

    fun setCreatePlaylistDialogOpen(open: Boolean) {
        _isCreatePlaylistDialogOpen.value = open
    }

    fun setEditingMetadataSong(song: Song?) {
        _editingMetadataSong.value = song
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putInt("current_theme_idx", theme.ordinal).apply()
        _currentTheme.value = theme
    }

    fun setVisualizerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("visualizer_enabled", enabled).apply()
        _visualizerEnabled.value = enabled
    }

    fun setFullPlayerBg(bg: FullPlayerBackground) {
        prefs.edit().putString("full_player_bg", bg.name).apply()
        _fullPlayerBg.value = bg
    }

    fun setShowTabBar(show: Boolean) {
        prefs.edit().putBoolean("show_tab_bar", show).apply()
        _showTabBar.value = show
    }

    fun setCustomAppName(name: String) {
        prefs.edit().putString("custom_app_name", name).apply()
        _customAppName.value = name
    }

    fun setDesignStyle(style: PlayerDesignStyle) {
        prefs.edit().putInt("current_design_idx", style.ordinal).apply()
        _currentDesignStyle.value = style
    }

    fun setWritePhysicalTags(write: Boolean) {
        prefs.edit().putBoolean("write_physical_tags", write).apply()
        _writePhysicalTags.value = write
    }

    fun setMinDurationFilterSec(seconds: Int) {
        prefs.edit().putInt("min_duration_filter_sec", seconds).apply()
        _minDurationFilterSec.value = seconds
        rescanLibrary()
    }

    // ── Playback Controls ──
    fun playSong(song: Song) {
        val e = engine ?: return
        if (e.currentSong.value?.id == song.id) {
            togglePlayPause()
            return
        }
        val currentList = allSongs.value
        val index = currentList.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        e.setQueue(currentList, index, autoPlay = true)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        engine?.setQueue(songs, startIndex, autoPlay = true)
    }

    fun shuffleQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val e = engine ?: return
        e.setQueue(songs.shuffled(), 0, autoPlay = true)
    }

    fun playNext(song: Song) = engine?.enqueueNext(song)
    fun addToQueue(song: Song) = engine?.enqueueLast(song)
    fun removeQueueItem(song: Song) = engine?.removeQueueItem(song)
    fun moveQueueItem(from: Int, to: Int) = engine?.moveQueueItem(from, to)
    fun clearQueue() = engine?.clearUpNext()

    fun togglePlayPause() {
        val e = engine ?: return
        if (e.isPlaying.value) e.pause() else e.resume()
    }
    fun skipNext() = engine?.playNext()
    fun skipPrevious() = engine?.playPrevious()
    fun seekTo(positionMs: Long) = engine?.seekTo(positionMs)
    fun toggleShuffle() = engine?.toggleShuffle()
    fun toggleRepeat() = engine?.toggleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = engine?.setPlaybackSpeed(speed)
    fun setSleepTimer(minutes: Int) = engine?.setSleepTimer(minutes)
    fun setCrossfade(seconds: Int) = engine?.setCrossfade(seconds)
    fun setGapless(enabled: Boolean) = engine?.setGapless(enabled)

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

    fun observePlaylistSongs(playlistId: Long): kotlinx.coroutines.flow.Flow<List<Song>> {
        return kotlinx.coroutines.flow.combine(repository.getSongsForPlaylist(playlistId), allSongs) { entityList, all ->
            val allMap = all.associateBy { it.id }
            entityList.mapNotNull { allMap[it.id] }
        }
    }

    fun updatePlaylistOrder(playlistId: Long, songs: List<Song>) {
        viewModelScope.launch {
            repository.updatePlaylistOrder(playlistId, songs.map { it.id })
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, song.id)
        }
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
        val active = currentSong.value
        if (active != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val cached = database.libraryDao().getLyrics(active.id)
                if (cached != null) {
                    database.libraryDao().saveLyrics(cached.copy(userOffsetMs = offsetMs))
                } else if (_currentLyrics.value.isNotEmpty()) {
                    database.libraryDao().saveLyrics(
                        dev.abstra.pro.data.db.entities.LyricsCacheEntity(
                            songId = active.id,
                            rawLrc = "",
                            userOffsetMs = offsetMs
                        )
                    )
                }
            }
        }
    }

    private fun loadLyricsForSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_currentLyrics.value.isNotEmpty() && _lastLyricsSongId == song.id) return@launch
            _lastLyricsSongId = song.id
            _lyricsLoadingState.value = true

            val cached = database.libraryDao().getLyrics(song.id)
            _lyricsTimeOffsetMs.value = cached?.userOffsetMs ?: 0L

            var parsed: List<LyricsLine>? = null

            // 1. Check matching .lrc file in same directory
            if (song.filePath.isNotBlank()) {
                val songFile = File(song.filePath)
                val baseNoExt = songFile.nameWithoutExtension
                val dir = songFile.parentFile
                
                val possibleLrcs = mutableListOf<File>()
                if (dir != null) {
                    possibleLrcs.add(File(dir, "$baseNoExt.lrc"))
                    possibleLrcs.add(File(dir, "$baseNoExt.LRC"))
                    dir.listFiles()?.find { it.name.equals("$baseNoExt.lrc", ignoreCase = true) }?.let { possibleLrcs.add(it) }
                    
                    val lyricsFolder = File(dir, "Lyrics")
                    if (lyricsFolder.exists() && lyricsFolder.isDirectory) {
                        lyricsFolder.listFiles()?.find { it.name.equals("$baseNoExt.lrc", ignoreCase = true) }?.let { possibleLrcs.add(it) }
                    }
                }
                
                val lrcFile = possibleLrcs.find { it.exists() && it.isFile }
                if (lrcFile != null) {
                    parsed = LyricsParser.parse(dev.abstra.pro.lyrics.util.CharsetUtils.readText(lrcFile))
                }
            }

            // 2. Check Room Database lyrics cache
            if (parsed.isNullOrEmpty() && cached != null && cached.rawLrc.isNotBlank()) {
                parsed = LyricsParser.parse(cached.rawLrc)
            }
            
            // 3. Fall back to embedded lyrics from SongEntity (scan-time extracted)
            if (parsed.isNullOrEmpty() && !song.lyrics.isNullOrBlank()) {
                parsed = LyricsParser.parse(song.lyrics)
            }

            // 4. Fall back to embedded lyrics tags (USLT / LYRICS) via jaudiotagger
            if (parsed.isNullOrEmpty() && song.filePath.isNotBlank()) {
                val embedded = MetadataManager.readEmbeddedLyrics(song.filePath)
                if (!embedded.isNullOrBlank()) {
                    parsed = LyricsParser.parse(embedded)
                    database.libraryDao().saveLyrics(
                        dev.abstra.pro.data.db.entities.LyricsCacheEntity(
                            songId = song.id, 
                            rawLrc = embedded,
                            userOffsetMs = _lyricsTimeOffsetMs.value
                        )
                    )
                }
            }
            
            _currentLyrics.value = parsed ?: emptyList()
            _lyricsLoadingState.value = false
        }
    }

    fun importLrcFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = dev.abstra.pro.lyrics.util.CharsetUtils.decodeBytes(stream.readBytes())
                    val parsed = LyricsParser.parse(content)
                    _currentLyrics.value = parsed

                    val active = currentSong.value
                    if (active != null) {
                        database.libraryDao().saveLyrics(
                            dev.abstra.pro.data.db.entities.LyricsCacheEntity(
                                songId = active.id,
                                rawLrc = content,
                                userOffsetMs = _lyricsTimeOffsetMs.value
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun persistNowPlaying() {
        val e = engine ?: return
        val song = e.currentSong.value ?: return
        prefs.edit()
            .putString("last_song_id", song.id)
            .putLong("last_position_ms", e.currentPosition.value)
            .putString("last_queue_json", Gson().toJson(e.queue.value))
            .commit()
    }


    fun clearLastSession() {
        prefs.edit()
            .remove("last_song_id")
            .remove("last_position_ms")
            .remove("last_queue_json")
            .apply()
    }

    override fun onCleared() {
        super.onCleared()
        persistNowPlaying()
    }
}
