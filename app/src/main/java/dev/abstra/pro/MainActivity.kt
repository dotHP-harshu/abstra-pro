package dev.abstra.pro

import android.Manifest
import android.content.pm.PackageManager
import dev.abstra.pro.ui.util.detectVerticalSwipes
import java.io.InputStream
import java.io.OutputStream
import android.net.Uri
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.util.fastAny
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
import kotlinx.coroutines.launch
import dev.abstra.pro.ui.components.AddToPlaylistDialog

import android.content.Intent
import android.content.ServiceConnection
import android.content.ComponentName
import android.os.IBinder
import androidx.lifecycle.ViewModelProvider
import dev.abstra.pro.service.MusicPlaybackService

class MainActivity : ComponentActivity() {

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    override fun onStart() {
        super.onStart()
        val viewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        dev.abstra.pro.service.MusicPlaybackService.audioEngine?.setForegroundState(true)
        val intent = Intent(this, MusicPlaybackService::class.java)
        ContextCompat.startForegroundService(this, intent)
        bindService(intent, serviceConnection, android.content.Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        val viewModel = ViewModelProvider(this)[MusicViewModel::class.java]
        dev.abstra.pro.service.MusicPlaybackService.audioEngine?.setForegroundState(false)
        viewModel.persistNowPlaying()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dev.abstra.pro.data.prefs.AppPreferences.init(this.applicationContext)
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
            val playlistPickerForSongs by viewModel.playlistPickerForSongs.collectAsState()
            val isOutputRoutePickerOpen by viewModel.isOutputRoutePickerOpen.collectAsState()
            
            var showQueueSheet by remember { mutableStateOf(false) }

            // Playback state
            val currentSong by viewModel.currentSong.collectAsState()
            val pendingSong by viewModel.pendingSong.collectAsState()
            val isPlaying by viewModel.isPlaying.collectAsState()
            val currentPosition by viewModel.currentPosition.collectAsState()
            val duration by viewModel.duration.collectAsState()
            val repeatMode by viewModel.repeatMode.collectAsState()
            val isShuffle by viewModel.isShuffle.collectAsState()
            val playbackSpeed by viewModel.playbackSpeed.collectAsState()
            val sleepTimerRemaining by viewModel.sleepTimerRemainingSec.collectAsState()
            val currentDesignStyle by viewModel.currentDesignStyle.collectAsState()
            val visualizerEnabled by viewModel.visualizerEnabled.collectAsState()
            val fullPlayerBg by viewModel.fullPlayerBg.collectAsState()
            val showTabBar by viewModel.showTabBar.collectAsState()
            val backgroundPlayback by viewModel.backgroundPlayback.collectAsState()
            val shareAudioFocus by viewModel.shareAudioFocus.collectAsState()
            val appName by viewModel.customAppName.collectAsState()
            val gaplessEnabled by viewModel.gaplessEnabled.collectAsState()
            val minDurationFilterSec by viewModel.minDurationFilterSec.collectAsState()
            val writePhysicalTags by viewModel.writePhysicalTags.collectAsState()
            
            val userName by viewModel.userName.collectAsState()
            val greetingEnabled by viewModel.greetingEnabled.collectAsState()
            val hasAskedForName by viewModel.hasAskedForName.collectAsState()
            var showFirstRunNameDialog by remember { mutableStateOf(false) }

            LaunchedEffect(hasAskedForName) {
                if (!hasAskedForName) {
                    kotlinx.coroutines.delay(500)
                    showFirstRunNameDialog = true
                    viewModel.markAskedForName()
                }
            }

            // Library data
            val allSongs by viewModel.allSongs.collectAsState()
            val visibleSongs by viewModel.visibleSongs.collectAsState()
            val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
            val playlists by viewModel.playlists.collectAsState()
            val firstSongs by viewModel.firstSongsByPlaylist.collectAsState()
            
            LaunchedEffect(playlists) {
                viewModel.loadPlaylistFirstSongs(playlists)
            }
            
            val aiMixes by viewModel.aiMixes.collectAsState()
            val lyricsLines by viewModel.currentLyrics.collectAsState()
            val lyricsOffsetMs by viewModel.lyricsTimeOffsetMs.collectAsState()
            val lifetimeMs by viewModel.totalLifetimePlaytime.collectAsState()
            val availableRoutes by viewModel.availableRoutes.collectAsState()

            // Hoisted List States
            val songsListState = androidx.compose.foundation.lazy.rememberLazyListState()
            
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

            val folderPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    viewModel.setMusicFolderUri(uri.toString())
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

            val deleteSongLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                viewModel.onSongDeleteConfirmed(result.resultCode == android.app.Activity.RESULT_OK)
                viewModel.onBatchDeleteConfirmed(result.resultCode == android.app.Activity.RESULT_OK)
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

            val appFont by dev.abstra.pro.data.prefs.AppPreferences.fontFlow.collectAsStateWithLifecycle(initialValue = dev.abstra.pro.ui.theme.AppFont.DEFAULT)
            androidx.compose.runtime.CompositionLocalProvider(
                dev.abstra.pro.ui.theme.LocalAppFont provides appFont
            ) {
                AbstraTheme(appTheme = currentTheme) {
                    val colors = LocalAbstraColors.current
                var selectedPlaylist by remember { mutableStateOf<dev.abstra.pro.data.model.Playlist?>(null) }
                val coroutineScope = rememberCoroutineScope()
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                    initialPage = currentTab.ordinal,
                    pageCount = { MainNavigationTab.values().size }
                )
                val tabHistory = remember { mutableStateListOf(MainNavigationTab.HOME) }
                val switchTab: (MainNavigationTab) -> Unit = { tab ->
                    if (tab != currentTab) {
                        if (tab == MainNavigationTab.HOME) {
                            tabHistory.clear()
                            tabHistory.add(MainNavigationTab.HOME)
                        } else {
                            if (tabHistory.size >= 16) tabHistory.removeAt(0)
                            tabHistory.add(currentTab)
                        }
                        coroutineScope.launch { pagerState.animateScrollToPage(tab.ordinal) }
                    }
                }
                
                androidx.activity.compose.BackHandler(
                    enabled = isPlayerExpanded || isEqualizerOpen || isLyricsOpen || showQueueSheet || 
                              isSleepTimerDialogOpen || isCreatePlaylistDialogOpen || isOutputRoutePickerOpen || 
                              selectedPlaylist != null || playlistPickerForSongs != null || tabHistory.size > 1
                ) {
                    when {
                        isCreatePlaylistDialogOpen -> viewModel.setCreatePlaylistDialogOpen(false)
                        playlistPickerForSongs != null -> viewModel.closePlaylistPicker()
                        isSleepTimerDialogOpen -> viewModel.setSleepTimerDialogOpen(false)
                        isOutputRoutePickerOpen -> viewModel.setOutputRoutePickerOpen(false)
                        showQueueSheet -> showQueueSheet = false
                        isLyricsOpen -> viewModel.setLyricsOpen(false)
                        isEqualizerOpen -> viewModel.setEqualizerOpen(false)
                        selectedPlaylist != null -> selectedPlaylist = null
                        isPlayerExpanded -> viewModel.setPlayerExpanded(false)
                        tabHistory.size > 1 -> {
                            val previous = tabHistory.removeAt(tabHistory.lastIndex)
                            coroutineScope.launch { pagerState.animateScrollToPage(previous.ordinal) }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bgBackground)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Main Content Scaffold
                    Column(modifier = Modifier.fillMaxSize()) {
                        
                        if (showFirstRunNameDialog) {
                            var draft by remember { mutableStateOf(userName.orEmpty()) }
                            AlertDialog(
                                onDismissRequest = { showFirstRunNameDialog = false },
                                title = { Text("What's your name?") },
                                text = {
                                    Column {
                                        Text("Used in the greeting on the home tab.", color = colors.textSecondary, fontSize = 13.sp)
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = draft,
                                            onValueChange = { if (it.length <= 30) draft = it },
                                            singleLine = true,
                                            placeholder = { Text("e.g. Harsh") }
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.setUserName(draft.trim().ifBlank { null })
                                        showFirstRunNameDialog = false
                                    }) { Text("Save") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showFirstRunNameDialog = false }) { Text("Skip") }
                                }
                            )
                        }

                        // Single source of truth: pagerState drives the tab highlight.
                        LaunchedEffect(pagerState.currentPage) {
                            val tab = MainNavigationTab.values()[pagerState.currentPage]
                            if (currentTab != tab) {
                                viewModel.setTab(tab)
                            }
                        }

                        if (showTabBar) {
                            TopSegmentedTabs(
                                selectedTab = MainNavigationTab.values()[pagerState.currentPage],
                                onTabSelected = { tab -> switchTab(tab) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else {
                            AppHeader(
                                appName = appName,
                                currentPage = pagerState.currentPage,
                                pageCount = MainNavigationTab.values().size,
                                colors = colors,
                                onTabRequested = { page -> switchTab(MainNavigationTab.values()[page]) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 2. Active Screen Content
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) { page ->
                            key(MainNavigationTab.values()[page]) {
                                when (MainNavigationTab.values()[page]) {
                                MainNavigationTab.HOME -> {
                                    HomeScreen(
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        recentlyPlayed = recentlyPlayed,
                                        recentlyAdded = visibleSongs.sortedByDescending { it.dateAdded }.take(10),
                                        mostPlayed = viewModel.mostPlayed.collectAsState().value,
                                        playlists = playlists,
                                        aiMixes = aiMixes,
                                        topArtists = emptyList(),
                                        topGenres = emptyList(),
                                        totalSongs = allSongs.size,
                                        favoriteCount = allSongs.count { it.isFavorite },
                                        lifetimeListeningMs = lifetimeMs,
                                        onSongClick = { song, list -> viewModel.playSong(song, list) },
                                        onPlaylistClick = { pl ->
                                            viewModel.setTab(MainNavigationTab.PLAYLISTS)
                                        },
                                        onMixClick = { mix ->
                                            viewModel.playQueue(mix.songs)
                                        },
                                        onTogglePlay = { viewModel.togglePlayPause() },
                                        onExpandPlayer = { viewModel.setPlayerExpanded(true) },
                                        userName = userName,
                                        greetingEnabled = greetingEnabled
                                    )
                                }
                                MainNavigationTab.SONGS -> {
                                    SongsScreen(
                                        songs = visibleSongs,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onSongClick = { song, currentList -> viewModel.playSong(song, currentList) },
                                        onPlayAll = { viewModel.playQueue(it) },
                                        onShuffleAll = { viewModel.shuffleQueue(it) },
                                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                                        onAddToQueue = { viewModel.addToQueue(it) },
                                        onPlayNext = { viewModel.playNext(it) },
                                        onAddToPlaylist = { viewModel.openPlaylistPicker(it) },
                                        onEditMetadata = { viewModel.setEditingMetadataSong(it) },
                                        onDeleteSong = { song -> 
                                            coroutineScope.launch {
                                                val sender = viewModel.requestDeleteSong(song)
                                                sender?.let { s -> 
                                                    deleteSongLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(s).build()) 
                                                }
                                            }
                                        },
                                        onBatchDelete = { songs ->
                                            coroutineScope.launch {
                                                val sender = viewModel.requestBatchDelete(songs)
                                                sender?.let { s ->
                                                    deleteSongLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(s).build())
                                                }
                                            }
                                        },
                                        onBatchAddToPlaylist = { songs -> viewModel.openPlaylistPicker(songs) },
                                        onRescanLibrary = { viewModel.rescanLibrary() },
                                        sortOption = viewModel.sortOption.collectAsState().value,
                                        onSortOptionChange = { viewModel.setSortOption(it) },
                                        listState = songsListState
                                    )
                                }
                                MainNavigationTab.PLAYLISTS -> {
                                    PlaylistsScreen(
                                        playlists = playlists,
                                        firstSongs = firstSongs,
                                        favoriteSongs = allSongs.filter { it.isFavorite },
                                        recentlyAddedSongs = visibleSongs.sortedByDescending { it.dateAdded }.take(20),
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onPlaylistClick = { selectedPlaylist = it },
                                        onCreatePlaylistClick = { viewModel.setCreatePlaylistDialogOpen(true) },
                                        onImportPlaylistClick = { importLauncher.launch(arrayOf("application/json")) },
                                        onExportAllClick = { exportLauncher.launch("abstra_pro_backup.json") },
                                        onExportPlaylistClick = { pl -> exportLauncher.launch("${pl.name}.json") },
                                        onDeletePlaylistClick = { viewModel.deletePlaylist(it) },
                                        onSongClick = { song, ctx -> viewModel.playSong(song, ctx) },
                                        onPlayAll = { viewModel.playQueue(it) },
                                        onShuffleAll = { viewModel.shuffleQueue(it) }
                                    )
                                }
                                MainNavigationTab.FOLDERS -> {
                                    val excluded by viewModel.excludedFolderPaths.collectAsState()
                                    val allFolders = remember(allSongs) {
                                        allSongs.asSequence()
                                            .mapNotNull { java.io.File(it.filePath).parent }
                                            .distinct()
                                            .map { path ->
                                                val songs = allSongs.filter { java.io.File(it.filePath).parent == path }
                                                Folder(java.io.File(path).name, path, songs.size, songs)
                                            }
                                            .sortedBy { it.name }
                                            .toList()
                                    }
                                    val visibleFolders = remember(allFolders, excluded) {
                                        allFolders.filter { it.path !in excluded }
                                    }
                                    
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    val realVolumes = remember { getStorageVolumes(context) }

                                    FoldersScreen(
                                        allFolders = allFolders,
                                        folders = visibleFolders,
                                        storageVolumes = realVolumes,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                        onSongClick = { song, list -> viewModel.playSong(song, list) },
                                        onPlayFolder = { f -> viewModel.playQueue(f.songs) },
                                        onShuffleFolder = { f -> viewModel.shuffleQueue(f.songs) },
                                        onExcludeFolder = { folder -> viewModel.excludeFolder(folder.path) },
                                        onRestoreFolder = { folder -> viewModel.restoreFolder(folder.path) },
                                        onRescanLibrary = { viewModel.rescanLibrary() }
                                    )
                                }
                                MainNavigationTab.SETTINGS -> {
                                    SettingsScreen(
                                        currentTheme = currentTheme,
                                        onThemeChange = { viewModel.setTheme(it) },
                                        currentDesignStyle = currentDesignStyle,
                                        onDesignStyleChange = { viewModel.setDesignStyle(it) },
                                        backgroundPlayback = backgroundPlayback,
                                        onBackgroundPlaybackToggle = { viewModel.setBackgroundPlayback(it) },
                                        shareAudioFocus = shareAudioFocus,
                                        onShareAudioFocusToggle = { viewModel.setShareAudioFocus(it) },
                                        visualizerEnabled = visualizerEnabled,
                                        onVisualizerToggle = { viewModel.setVisualizerEnabled(it) },
                                        fullPlayerBg = fullPlayerBg,
                                        onFullPlayerBgChange = { viewModel.setFullPlayerBg(it) },
                                        showTabBar = showTabBar,
                                        onShowTabBarToggle = { viewModel.setShowTabBar(it) },
                                        appName = appName,
                                        onAppNameChange = { viewModel.setCustomAppName(it) },
                                        gaplessEnabled = gaplessEnabled,
                                        onGaplessToggle = { viewModel.setGapless(it) },
                                        minDurationFilterSec = minDurationFilterSec,
                                        onMinDurationChange = { viewModel.setMinDurationFilterSec(it) },
                                        writePhysicalTags = writePhysicalTags,
                                        onWritePhysicalTagsToggle = { viewModel.setWritePhysicalTags(it) },
                                        onExportBackup = { exportLauncher.launch("abstra_pro_backup.json") },
                                        onImportBackup = { importLauncher.launch(arrayOf("application/json")) },
                                        onRescanLibrary = { viewModel.rescanLibrary() },
                                        onSelectFolder = { folderPickerLauncher.launch(Uri.EMPTY) },
                                        userName = viewModel.userName.collectAsState().value,
                                        onUserNameChange = { viewModel.setUserName(it) },
                                        greetingEnabled = viewModel.greetingEnabled.collectAsState().value,
                                        onGreetingEnabledChange = { viewModel.setGreetingEnabled(it) }
                                    )
                                }
                                }
                            }
                        }
                    }

                    // 3. Floating MiniPlayer
                    AnimatedVisibility(
                        visible = currentSong != null && !isPlayerExpanded,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        MiniPlayer(
                            song = currentSong,
                            isPlaying = isPlaying,
                            progressFraction = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onNext = { viewModel.skipNext() },
                            onPrevious = { viewModel.skipPrevious() },
                            onClick = { viewModel.setPlayerExpanded(true) },
                            modifier = Modifier.pointerInput(isPlayerExpanded) {
                                detectVerticalSwipes(
                                    thresholdPx = 50.dp.toPx(),
                                    onSwipeUp = {
                                        if (!isPlayerExpanded) viewModel.setPlayerExpanded(true)
                                    },
                                    onSwipeDown = {
                                        if (isPlayerExpanded) viewModel.setPlayerExpanded(false)
                                    }
                                )
                            }
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
                                pendingSong = pendingSong,
                                isPlaying = isPlaying,
                                currentPosition = currentPosition,
                                duration = duration,
                                repeatMode = repeatMode,
                                isShuffle = isShuffle,
                                sleepTimerRemainingSec = sleepTimerRemaining,
                                designStyle = currentDesignStyle,
                                backgroundStyle = fullPlayerBg,
                                showVisualizer = visualizerEnabled,
                                onToggleVisualizer = { viewModel.setVisualizerEnabled(!visualizerEnabled) },
                                onTogglePlay = { viewModel.togglePlayPause() },
                                onPrevious = { viewModel.skipPrevious() },
                                onNext = { viewModel.skipNext() },
                                onSeekTo = { viewModel.seekTo(it) },
                                onToggleRepeat = { viewModel.toggleRepeat() },
                                onToggleShuffle = { viewModel.toggleShuffle() },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onOpenQueue = { showQueueSheet = true },
                                onOpenLyrics = { viewModel.setLyricsOpen(true) },
                                onOpenEqualizer = { viewModel.setEqualizerOpen(true) },

                                onOpenSleepTimer = { viewModel.setSleepTimerDialogOpen(true) },
                                onStyleChange = { viewModel.setDesignStyle(it) },
                                onCollapse = { viewModel.setPlayerExpanded(false) }
                            )
                        }

                    // 4.5. Playlist Detail Overlay
                    selectedPlaylist?.let { playlist ->
                        dev.abstra.pro.ui.screens.PlaylistDetailScreen(
                            playlistId = playlist.id,
                            playlistName = playlist.name,
                            viewModel = viewModel,
                            onBack = { selectedPlaylist = null },
                            onPlaySong = { song, queue -> 
                                viewModel.playSong(song, queue)
                            },
                            onPlayAll = { queue -> viewModel.playQueue(queue) },
                            onShuffleAll = { queue -> viewModel.shuffleQueue(queue) },
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(5f)
                        )
                    }

                    // 5. Equalizer Overlay Screen

                    AnimatedVisibility(
                        visible = isEqualizerOpen,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        dev.abstra.pro.service.MusicPlaybackService.audioEngine?.effectsManager?.let { effectsManager ->
                            EqualizerScreen(
                                effectsManager = effectsManager,
                                playbackSpeed = playbackSpeed,
                                onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                                onClose = { viewModel.setEqualizerOpen(false) }
                            )
                        }
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
                            onSelectLrcFile = { 
                                val lrcMime = arrayOf("text/plain", "application/octet-stream", "*/*")
                                lrcPickerLauncher.launch(lrcMime) 
                            },
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

                    if (showQueueSheet) {
                        key(showQueueSheet) {
                            val q by viewModel.queue.collectAsState()
                            val qIdx by viewModel.currentIndex.collectAsState()
                            QueueBottomSheet(
                                queue = q,
                                currentIndex = qIdx,
                                onDismiss = { showQueueSheet = false },
                                onReorder = { from, to -> viewModel.moveQueueItem(from, to) },
                                onRemove = { song -> viewModel.removeQueueItem(song) },
                                onRemoveAt = { idx -> viewModel.removeQueueItemAt(idx) },
                                onClearQueue = { viewModel.clearQueue() },
                                onClearEntireQueue = {
                                    viewModel.clearEntireQueue()
                                    showQueueSheet = false
                                },
                                onSongClick = { song -> 
                                    viewModel.playSong(song)
                                    showQueueSheet = false
                                }
                            )
                        }
                    }

                    playlistPickerForSongs?.let { songsToBatchAdd ->
                        AddToPlaylistDialog(
                            playlists = playlists,
                            onDismiss = { viewModel.closePlaylistPicker() },
                            onCreateNew = { name ->
                                viewModel.createPlaylist(name)
                                viewModel.closePlaylistPicker()
                            },
                            onSelectPlaylist = { pl ->
                                viewModel.addSongsToPlaylist(pl.id, songsToBatchAdd)
                                viewModel.closePlaylistPicker()
                            }
                        )
                    }

                    if (isOutputRoutePickerOpen) {
                        OutputRouteBottomSheet(
                            routes = availableRoutes,
                            onRouteSelected = { viewModel.selectRoute(it) },
                            onDismiss = { viewModel.setOutputRoutePickerOpen(false) }
                        )
                    }
                }
            }
        }
    }
}
}

fun getStorageVolumes(context: android.content.Context): List<StorageInfo> {
    val volumes = mutableListOf<StorageInfo>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val storageManager = context.getSystemService(android.content.Context.STORAGE_SERVICE) as android.os.storage.StorageManager
        for (volume in storageManager.storageVolumes) {
            val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                volume.directory
            } else {
                @Suppress("DEPRECATION")
                if (volume.isPrimary) {
                    android.os.Environment.getExternalStorageDirectory()
                } else {
                    try {
                        val method = volume.javaClass.getMethod("getPath")
                        java.io.File(method.invoke(volume) as String)
                    } catch (_: Exception) { null }
                }
            }
            dir?.let {
                val name = volume.getDescription(context)
                volumes.add(StorageInfo(name, it.absolutePath, volume.isPrimary, 0, 0))
            }
        }
    }
    return volumes
}


