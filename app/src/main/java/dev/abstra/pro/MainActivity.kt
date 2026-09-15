package dev.abstra.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.abstra.pro.data.model.Folder
import dev.abstra.pro.data.model.StorageInfo
import dev.abstra.pro.ui.components.*
import dev.abstra.pro.ui.screens.*
import dev.abstra.pro.ui.theme.AbstraTheme
import dev.abstra.pro.ui.theme.LocalAbstraColors
import dev.abstra.pro.ui.components.MainNavigationTab
import dev.abstra.pro.ui.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MusicViewModel = viewModel()

            val currentTheme by viewModel.currentTheme.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsState()
            val isEqualizerOpen by viewModel.isEqualizerOpen.collectAsState()
            val isLyricsOpen by viewModel.isLyricsOpen.collectAsState()
            val isSleepTimerDialogOpen by viewModel.isSleepTimerDialogOpen.collectAsState()
            val isCreatePlaylistDialogOpen by viewModel.isCreatePlaylistDialogOpen.collectAsState()
            val editingSong by viewModel.editingMetadataSong.collectAsState()

            // Playback state
            val currentSong by viewModel.currentSong.collectAsState()
            val isPlaying by viewModel.isPlaying.collectAsState()
            val currentPosition by viewModel.currentPosition.collectAsState()
            val duration by viewModel.duration.collectAsState()
            val repeatMode by viewModel.repeatMode.collectAsState()
            val isShuffle by viewModel.isShuffle.collectAsState()
            val playbackSpeed by viewModel.playbackSpeed.collectAsState()
            val sleepTimerRemaining by viewModel.sleepTimerRemainingSec.collectAsState()
            val currentDesignStyle by viewModel.currentDesignStyle.collectAsState()
            val crossfadeSeconds by viewModel.crossfadeSeconds.collectAsState()
            val gaplessEnabled by viewModel.gaplessEnabled.collectAsState()
            val minDurationFilterSec by viewModel.minDurationFilterSec.collectAsState()
            val writePhysicalTags by viewModel.writePhysicalTags.collectAsState()

            // Library data
            val allSongs by viewModel.allSongs.collectAsState()
            val playlists by viewModel.playlists.collectAsState()
            val aiMixes by viewModel.aiMixes.collectAsState()
            val lyricsLines by viewModel.currentLyrics.collectAsState()
            val lyricsOffsetMs by viewModel.lyricsTimeOffsetMs.collectAsState()

            // SAF Activity Launchers
            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.let { stream ->
                        viewModel.exportBackup(stream)
                    }
                }
            }

            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    contentResolver.openInputStream(uri)?.let { stream ->
                        viewModel.importBackup(stream)
                    }
                }
            }

            val lrcPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    viewModel.importLrcFile(uri)
                }
            }

            // Permissions Launcher
            var hasAudioPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                    } else {
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    }
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms[Manifest.permission.READ_MEDIA_AUDIO] == true
                } else {
                    perms[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                }
                hasAudioPermission = granted
                if (granted) {
                    viewModel.rescanLibrary()
                }
            }

            LaunchedEffect(Unit) {
                if (!hasAudioPermission) {
                    val reqs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(reqs)
                }
            }

            AbstraTheme(appTheme = currentTheme) {
                val colors = LocalAbstraColors.current

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bgBackground)
                        .statusBarsPadding()
                ) {
                    // Main Content Scaffold
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. Top Segmented Pill Tabs
                        TopSegmentedTabs(
                            selectedTab = currentTab,
                            onTabSelected = { tab ->
                                viewModel.setTab(tab)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // 2. Active Screen Content
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (currentTab) {
                                MainNavigationTab.HOME -> {
                                    HomeScreen(
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        recentlyAdded = allSongs.take(10),
                                        playlists = playlists,
                                        aiMixes = aiMixes,
                                        topArtists = emptyList(),
                                        topGenres = emptyList(),
                                        onSongClick = { viewModel.playSong(it) },
                                        onPlaylistClick = { pl ->
                                            viewModel.setTab(MainNavigationTab.PLAYLISTS)
                                        },
                                        onMixClick = { mix ->
                                            viewModel.playQueue(mix.songs)
                                        },
                                        onTogglePlay = { viewModel.togglePlayPause() },
                                        onExpandPlayer = { viewModel.setPlayerExpanded(true) }
                                    )
                                }
                                MainNavigationTab.SONGS -> {
                                    SongsScreen(
                                        songs = allSongs,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onSongClick = { viewModel.playSong(it) },
                                        onPlayAll = { viewModel.playQueue(it) },
                                        onShuffleAll = { viewModel.shuffleQueue(it) },
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onAddToQueue = { /* enqueued */ },
                                        onPlayNext = { /* play next */ },
                                        onAddToPlaylist = { /* add to pl */ },
                                        onEditMetadata = { viewModel.setEditingMetadataSong(it) },
                                        onDeleteSong = { /* delete */ },
                                        onRescanLibrary = { viewModel.rescanLibrary() }
                                    )
                                }
                                MainNavigationTab.PLAYLISTS -> {
                                    PlaylistsScreen(
                                        playlists = playlists,
                                        favoriteSongs = allSongs.filter { it.isFavorite },
                                        recentlyAddedSongs = allSongs.take(20),
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onPlaylistClick = { /* open playlist */ },
                                        onCreatePlaylistClick = { viewModel.setCreatePlaylistDialogOpen(true) },
                                        onImportPlaylistClick = { importLauncher.launch(arrayOf("application/json")) },
                                        onExportAllClick = { exportLauncher.launch("abstra_pro_backup.json") },
                                        onExportPlaylistClick = { pl -> exportLauncher.launch("${pl.name}.json") },
                                        onDeletePlaylistClick = { viewModel.deletePlaylist(it) },
                                        onSongClick = { viewModel.playSong(it) },
                                        onPlayAll = { viewModel.playQueue(it) },
                                        onShuffleAll = { viewModel.shuffleQueue(it) }
                                    )
                                }
                                MainNavigationTab.FOLDERS -> {
                                    // Group songs by folder
                                    val folders = remember(allSongs) {
                                        allSongs.groupBy { it.folderName }.map { (name, songs) ->
                                            Folder(
                                                name = name,
                                                path = songs.firstOrNull()?.filePath ?: "",
                                                songCount = songs.size,
                                                songs = songs
                                            )
                                        }
                                    }
                                    val defaultStorage = listOf(
                                        StorageInfo("Internal Storage", "/storage/emulated/0", false, 0, 0)
                                    )

                                    FoldersScreen(
                                        folders = folders,
                                        storageVolumes = defaultStorage,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onSongClick = { viewModel.playSong(it) },
                                        onPlayFolder = { f -> viewModel.playQueue(f.songs) },
                                        onShuffleFolder = { f -> viewModel.shuffleQueue(f.songs) },
                                        onExcludeFolder = { /* exclude */ },
                                        onRescanLibrary = { viewModel.rescanLibrary() }
                                    )
                                }
                                MainNavigationTab.SETTINGS -> {
                                    SettingsScreen(
                                        currentTheme = currentTheme,
                                        onThemeChange = { viewModel.setTheme(it) },
                                        currentDesignStyle = currentDesignStyle,
                                        onDesignStyleChange = { viewModel.setDesignStyle(it) },
                                        crossfadeSeconds = crossfadeSeconds,
                                        onCrossfadeChange = { viewModel.setCrossfade(it) },
                                        gaplessEnabled = gaplessEnabled,
                                        onGaplessToggle = { viewModel.setGapless(it) },
                                        minDurationFilterSec = minDurationFilterSec,
                                        onMinDurationChange = { viewModel.setMinDurationFilterSec(it) },
                                        writePhysicalTags = writePhysicalTags,
                                        onWritePhysicalTagsToggle = { viewModel.setWritePhysicalTags(it) },
                                        onExportBackup = { exportLauncher.launch("abstra_pro_backup.json") },
                                        onImportBackup = { importLauncher.launch(arrayOf("application/json")) },
                                        onRescanLibrary = { viewModel.rescanLibrary() }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Floating MiniPlayer (Visible whenever not expanded and song is present)
                    AnimatedVisibility(
                        visible = currentSong != null && !isPlayerExpanded,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        MiniPlayer(
                            song = currentSong,
                            isPlaying = isPlaying,
                            progressFraction = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onNext = { viewModel.skipNext() },
                            onPrevious = { viewModel.skipPrevious() },
                            onClick = { viewModel.setPlayerExpanded(true) }
                        )
                    }

                    // 4. Full Player Modal Sheet
                    AnimatedVisibility(
                        visible = isPlayerExpanded,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        FullPlayerScreen(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            repeatMode = repeatMode,
                            isShuffle = isShuffle,
                            sleepTimerRemainingSec = sleepTimerRemaining,
                            designStyle = currentDesignStyle,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onPrevious = { viewModel.skipPrevious() },
                            onNext = { viewModel.skipNext() },
                            onSeekTo = { viewModel.seekTo(it) },
                            onToggleRepeat = { viewModel.toggleRepeat() },
                            onToggleShuffle = { viewModel.toggleShuffle() },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onOpenQueue = { /* open queue drawer */ },
                            onOpenLyrics = { viewModel.setLyricsOpen(true) },
                            onOpenEqualizer = { viewModel.setEqualizerOpen(true) },
                            onOpenSleepTimer = { viewModel.setSleepTimerDialogOpen(true) },
                            onStyleChange = { viewModel.setDesignStyle(it) },
                            onCollapse = { viewModel.setPlayerExpanded(false) }
                        )
                    }

                    // 5. Equalizer Overlay Screen
                    AnimatedVisibility(
                        visible = isEqualizerOpen,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        EqualizerScreen(
                            effectsManager = viewModel.audioEngine.effectsManager,
                            playbackSpeed = playbackSpeed,
                            onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                            onClose = { viewModel.setEqualizerOpen(false) }
                        )
                    }

                    // 6. Lyrics Overlay Screen
                    AnimatedVisibility(
                        visible = isLyricsOpen,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LyricsScreen(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPosition,
                            lyricsLines = lyricsLines,
                            timeOffsetMs = lyricsOffsetMs,
                            onOffsetChange = { viewModel.setLyricsOffset(it) },
                            onLineClick = { pos -> viewModel.seekTo(pos) },
                            onSelectLrcFile = { lrcPickerLauncher.launch(arrayOf("*/*")) },
                            onClose = { viewModel.setLyricsOpen(false) }
                        )
                    }

                    // 7. Dialogs
                    if (isCreatePlaylistDialogOpen) {
                        CreatePlaylistDialog(
                            onDismiss = { viewModel.setCreatePlaylistDialogOpen(false) },
                            onConfirm = { name, desc ->
                                viewModel.createPlaylist(name, desc)
                                viewModel.setCreatePlaylistDialogOpen(false)
                            }
                        )
                    }

                    if (isSleepTimerDialogOpen) {
                        SleepTimerDialog(
                            currentRemainingSec = sleepTimerRemaining,
                            onDismiss = { viewModel.setSleepTimerDialogOpen(false) },
                            onSelectDuration = { mins ->
                                viewModel.setSleepTimer(mins)
                                viewModel.setSleepTimerDialogOpen(false)
                            }
                        )
                    }
                }
            }
        }
    }
}
