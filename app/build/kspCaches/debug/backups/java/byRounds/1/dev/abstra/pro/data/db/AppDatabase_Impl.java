package dev.abstra.pro.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import dev.abstra.pro.data.db.dao.LibraryDao;
import dev.abstra.pro.data.db.dao.LibraryDao_Impl;
import dev.abstra.pro.data.db.dao.PlaylistDao;
import dev.abstra.pro.data.db.dao.PlaylistDao_Impl;
import dev.abstra.pro.data.db.dao.SettingsDao;
import dev.abstra.pro.data.db.dao.SettingsDao_Impl;
import dev.abstra.pro.data.db.dao.TelemetryDao;
import dev.abstra.pro.data.db.dao.TelemetryDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile LibraryDao _libraryDao;

  private volatile PlaylistDao _playlistDao;

  private volatile TelemetryDao _telemetryDao;

  private volatile SettingsDao _settingsDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `songs` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, `uriString` TEXT NOT NULL, `folderName` TEXT NOT NULL, `filePath` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, `genre` TEXT, `albumId` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `lyrics` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `song_overrides` (`songId` TEXT NOT NULL, `customTitle` TEXT, `customArtist` TEXT, `customAlbum` TEXT, `customGenre` TEXT, `customCoverUri` TEXT, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`songId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist_songs` (`playlistId` INTEGER NOT NULL, `songId` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`playlistId`, `songId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `playback_telemetry` (`songId` TEXT NOT NULL, `playCount` INTEGER NOT NULL, `fullCompletions` INTEGER NOT NULL, `fastSkips` INTEGER NOT NULL, `consecutiveSkips` INTEGER NOT NULL, `repeatCount` INTEGER NOT NULL, `morningPlays` INTEGER NOT NULL, `afternoonPlays` INTEGER NOT NULL, `eveningPlays` INTEGER NOT NULL, `nightPlays` INTEGER NOT NULL, `weekdayPlays` INTEGER NOT NULL, `weekendPlays` INTEGER NOT NULL, `lastPlayedTimestamp` INTEGER NOT NULL, `totalPlayedMs` INTEGER NOT NULL, PRIMARY KEY(`songId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `markov_transitions` (`fromSongId` TEXT NOT NULL, `toSongId` TEXT NOT NULL, `transitionCount` INTEGER NOT NULL, PRIMARY KEY(`fromSongId`, `toSongId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `effect_settings` (`id` INTEGER NOT NULL, `isEqEnabled` INTEGER NOT NULL, `activePresetName` TEXT NOT NULL, `eqBandsString` TEXT NOT NULL, `bassBoost` INTEGER NOT NULL, `virtualizer` INTEGER NOT NULL, `loudnessGain` INTEGER NOT NULL, `balancePan` REAL NOT NULL, `reverbPreset` INTEGER NOT NULL, `dynamicsPreset` INTEGER NOT NULL, `playbackSpeed` REAL NOT NULL, `crossfadeSeconds` INTEGER NOT NULL, `gaplessEnabled` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `custom_eq_presets` (`name` TEXT NOT NULL, `bandLevelsString` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`name`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `lyrics_cache` (`songId` TEXT NOT NULL, `rawLrc` TEXT NOT NULL, `userOffsetMs` INTEGER NOT NULL, `cachedTimestamp` INTEGER NOT NULL, PRIMARY KEY(`songId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `widget_prefs` (`id` INTEGER NOT NULL, `coverShape` TEXT NOT NULL, `blurStrength` REAL NOT NULL, `useSolidBg` INTEGER NOT NULL, `accentColor` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'cf15099e68cd48e729dc9a60a71663fa')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `songs`");
        db.execSQL("DROP TABLE IF EXISTS `song_overrides`");
        db.execSQL("DROP TABLE IF EXISTS `playlists`");
        db.execSQL("DROP TABLE IF EXISTS `playlist_songs`");
        db.execSQL("DROP TABLE IF EXISTS `playback_telemetry`");
        db.execSQL("DROP TABLE IF EXISTS `markov_transitions`");
        db.execSQL("DROP TABLE IF EXISTS `effect_settings`");
        db.execSQL("DROP TABLE IF EXISTS `custom_eq_presets`");
        db.execSQL("DROP TABLE IF EXISTS `lyrics_cache`");
        db.execSQL("DROP TABLE IF EXISTS `widget_prefs`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsSongs = new HashMap<String, TableInfo.Column>(13);
        _columnsSongs.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("artist", new TableInfo.Column("artist", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("album", new TableInfo.Column("album", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("durationMs", new TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("uriString", new TableInfo.Column("uriString", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("folderName", new TableInfo.Column("folderName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("filePath", new TableInfo.Column("filePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("dateAdded", new TableInfo.Column("dateAdded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("genre", new TableInfo.Column("genre", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("albumId", new TableInfo.Column("albumId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("lyrics", new TableInfo.Column("lyrics", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSongs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSongs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSongs = new TableInfo("songs", _columnsSongs, _foreignKeysSongs, _indicesSongs);
        final TableInfo _existingSongs = TableInfo.read(db, "songs");
        if (!_infoSongs.equals(_existingSongs)) {
          return new RoomOpenHelper.ValidationResult(false, "songs(dev.abstra.pro.data.db.entities.SongEntity).\n"
                  + " Expected:\n" + _infoSongs + "\n"
                  + " Found:\n" + _existingSongs);
        }
        final HashMap<String, TableInfo.Column> _columnsSongOverrides = new HashMap<String, TableInfo.Column>(7);
        _columnsSongOverrides.put("songId", new TableInfo.Column("songId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongOverrides.put("customTitle", new TableInfo.Column("customTitle", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongOverrides.put("customArtist", new TableInfo.Column("customArtist", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongOverrides.put("customAlbum", new TableInfo.Column("customAlbum", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongOverrides.put("customGenre", new TableInfo.Column("customGenre", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongOverrides.put("customCoverUri", new TableInfo.Column("customCoverUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongOverrides.put("isFavorite", new TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSongOverrides = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSongOverrides = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSongOverrides = new TableInfo("song_overrides", _columnsSongOverrides, _foreignKeysSongOverrides, _indicesSongOverrides);
        final TableInfo _existingSongOverrides = TableInfo.read(db, "song_overrides");
        if (!_infoSongOverrides.equals(_existingSongOverrides)) {
          return new RoomOpenHelper.ValidationResult(false, "song_overrides(dev.abstra.pro.data.db.entities.SongOverrideEntity).\n"
                  + " Expected:\n" + _infoSongOverrides + "\n"
                  + " Found:\n" + _existingSongOverrides);
        }
        final HashMap<String, TableInfo.Column> _columnsPlaylists = new HashMap<String, TableInfo.Column>(4);
        _columnsPlaylists.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylists = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaylists = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlaylists = new TableInfo("playlists", _columnsPlaylists, _foreignKeysPlaylists, _indicesPlaylists);
        final TableInfo _existingPlaylists = TableInfo.read(db, "playlists");
        if (!_infoPlaylists.equals(_existingPlaylists)) {
          return new RoomOpenHelper.ValidationResult(false, "playlists(dev.abstra.pro.data.db.entities.PlaylistEntity).\n"
                  + " Expected:\n" + _infoPlaylists + "\n"
                  + " Found:\n" + _existingPlaylists);
        }
        final HashMap<String, TableInfo.Column> _columnsPlaylistSongs = new HashMap<String, TableInfo.Column>(3);
        _columnsPlaylistSongs.put("playlistId", new TableInfo.Column("playlistId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistSongs.put("songId", new TableInfo.Column("songId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistSongs.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaylistSongs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaylistSongs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlaylistSongs = new TableInfo("playlist_songs", _columnsPlaylistSongs, _foreignKeysPlaylistSongs, _indicesPlaylistSongs);
        final TableInfo _existingPlaylistSongs = TableInfo.read(db, "playlist_songs");
        if (!_infoPlaylistSongs.equals(_existingPlaylistSongs)) {
          return new RoomOpenHelper.ValidationResult(false, "playlist_songs(dev.abstra.pro.data.db.entities.PlaylistSongEntity).\n"
                  + " Expected:\n" + _infoPlaylistSongs + "\n"
                  + " Found:\n" + _existingPlaylistSongs);
        }
        final HashMap<String, TableInfo.Column> _columnsPlaybackTelemetry = new HashMap<String, TableInfo.Column>(14);
        _columnsPlaybackTelemetry.put("songId", new TableInfo.Column("songId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("playCount", new TableInfo.Column("playCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("fullCompletions", new TableInfo.Column("fullCompletions", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("fastSkips", new TableInfo.Column("fastSkips", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("consecutiveSkips", new TableInfo.Column("consecutiveSkips", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("repeatCount", new TableInfo.Column("repeatCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("morningPlays", new TableInfo.Column("morningPlays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("afternoonPlays", new TableInfo.Column("afternoonPlays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("eveningPlays", new TableInfo.Column("eveningPlays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("nightPlays", new TableInfo.Column("nightPlays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("weekdayPlays", new TableInfo.Column("weekdayPlays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("weekendPlays", new TableInfo.Column("weekendPlays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("lastPlayedTimestamp", new TableInfo.Column("lastPlayedTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaybackTelemetry.put("totalPlayedMs", new TableInfo.Column("totalPlayedMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlaybackTelemetry = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlaybackTelemetry = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlaybackTelemetry = new TableInfo("playback_telemetry", _columnsPlaybackTelemetry, _foreignKeysPlaybackTelemetry, _indicesPlaybackTelemetry);
        final TableInfo _existingPlaybackTelemetry = TableInfo.read(db, "playback_telemetry");
        if (!_infoPlaybackTelemetry.equals(_existingPlaybackTelemetry)) {
          return new RoomOpenHelper.ValidationResult(false, "playback_telemetry(dev.abstra.pro.data.db.entities.PlaybackTelemetryEntity).\n"
                  + " Expected:\n" + _infoPlaybackTelemetry + "\n"
                  + " Found:\n" + _existingPlaybackTelemetry);
        }
        final HashMap<String, TableInfo.Column> _columnsMarkovTransitions = new HashMap<String, TableInfo.Column>(3);
        _columnsMarkovTransitions.put("fromSongId", new TableInfo.Column("fromSongId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarkovTransitions.put("toSongId", new TableInfo.Column("toSongId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMarkovTransitions.put("transitionCount", new TableInfo.Column("transitionCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMarkovTransitions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMarkovTransitions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMarkovTransitions = new TableInfo("markov_transitions", _columnsMarkovTransitions, _foreignKeysMarkovTransitions, _indicesMarkovTransitions);
        final TableInfo _existingMarkovTransitions = TableInfo.read(db, "markov_transitions");
        if (!_infoMarkovTransitions.equals(_existingMarkovTransitions)) {
          return new RoomOpenHelper.ValidationResult(false, "markov_transitions(dev.abstra.pro.data.db.entities.MarkovTransitionEntity).\n"
                  + " Expected:\n" + _infoMarkovTransitions + "\n"
                  + " Found:\n" + _existingMarkovTransitions);
        }
        final HashMap<String, TableInfo.Column> _columnsEffectSettings = new HashMap<String, TableInfo.Column>(13);
        _columnsEffectSettings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("isEqEnabled", new TableInfo.Column("isEqEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("activePresetName", new TableInfo.Column("activePresetName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("eqBandsString", new TableInfo.Column("eqBandsString", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("bassBoost", new TableInfo.Column("bassBoost", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("virtualizer", new TableInfo.Column("virtualizer", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("loudnessGain", new TableInfo.Column("loudnessGain", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("balancePan", new TableInfo.Column("balancePan", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("reverbPreset", new TableInfo.Column("reverbPreset", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("dynamicsPreset", new TableInfo.Column("dynamicsPreset", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("playbackSpeed", new TableInfo.Column("playbackSpeed", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("crossfadeSeconds", new TableInfo.Column("crossfadeSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEffectSettings.put("gaplessEnabled", new TableInfo.Column("gaplessEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEffectSettings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEffectSettings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEffectSettings = new TableInfo("effect_settings", _columnsEffectSettings, _foreignKeysEffectSettings, _indicesEffectSettings);
        final TableInfo _existingEffectSettings = TableInfo.read(db, "effect_settings");
        if (!_infoEffectSettings.equals(_existingEffectSettings)) {
          return new RoomOpenHelper.ValidationResult(false, "effect_settings(dev.abstra.pro.data.db.entities.EffectSettingsEntity).\n"
                  + " Expected:\n" + _infoEffectSettings + "\n"
                  + " Found:\n" + _existingEffectSettings);
        }
        final HashMap<String, TableInfo.Column> _columnsCustomEqPresets = new HashMap<String, TableInfo.Column>(3);
        _columnsCustomEqPresets.put("name", new TableInfo.Column("name", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomEqPresets.put("bandLevelsString", new TableInfo.Column("bandLevelsString", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomEqPresets.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCustomEqPresets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCustomEqPresets = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCustomEqPresets = new TableInfo("custom_eq_presets", _columnsCustomEqPresets, _foreignKeysCustomEqPresets, _indicesCustomEqPresets);
        final TableInfo _existingCustomEqPresets = TableInfo.read(db, "custom_eq_presets");
        if (!_infoCustomEqPresets.equals(_existingCustomEqPresets)) {
          return new RoomOpenHelper.ValidationResult(false, "custom_eq_presets(dev.abstra.pro.data.db.entities.CustomEqPresetEntity).\n"
                  + " Expected:\n" + _infoCustomEqPresets + "\n"
                  + " Found:\n" + _existingCustomEqPresets);
        }
        final HashMap<String, TableInfo.Column> _columnsLyricsCache = new HashMap<String, TableInfo.Column>(4);
        _columnsLyricsCache.put("songId", new TableInfo.Column("songId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLyricsCache.put("rawLrc", new TableInfo.Column("rawLrc", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLyricsCache.put("userOffsetMs", new TableInfo.Column("userOffsetMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLyricsCache.put("cachedTimestamp", new TableInfo.Column("cachedTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLyricsCache = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLyricsCache = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLyricsCache = new TableInfo("lyrics_cache", _columnsLyricsCache, _foreignKeysLyricsCache, _indicesLyricsCache);
        final TableInfo _existingLyricsCache = TableInfo.read(db, "lyrics_cache");
        if (!_infoLyricsCache.equals(_existingLyricsCache)) {
          return new RoomOpenHelper.ValidationResult(false, "lyrics_cache(dev.abstra.pro.data.db.entities.LyricsCacheEntity).\n"
                  + " Expected:\n" + _infoLyricsCache + "\n"
                  + " Found:\n" + _existingLyricsCache);
        }
        final HashMap<String, TableInfo.Column> _columnsWidgetPrefs = new HashMap<String, TableInfo.Column>(5);
        _columnsWidgetPrefs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWidgetPrefs.put("coverShape", new TableInfo.Column("coverShape", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWidgetPrefs.put("blurStrength", new TableInfo.Column("blurStrength", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWidgetPrefs.put("useSolidBg", new TableInfo.Column("useSolidBg", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWidgetPrefs.put("accentColor", new TableInfo.Column("accentColor", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWidgetPrefs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWidgetPrefs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWidgetPrefs = new TableInfo("widget_prefs", _columnsWidgetPrefs, _foreignKeysWidgetPrefs, _indicesWidgetPrefs);
        final TableInfo _existingWidgetPrefs = TableInfo.read(db, "widget_prefs");
        if (!_infoWidgetPrefs.equals(_existingWidgetPrefs)) {
          return new RoomOpenHelper.ValidationResult(false, "widget_prefs(dev.abstra.pro.data.db.entities.WidgetPrefsEntity).\n"
                  + " Expected:\n" + _infoWidgetPrefs + "\n"
                  + " Found:\n" + _existingWidgetPrefs);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "cf15099e68cd48e729dc9a60a71663fa", "6629ebbff71c2d06ad991127c1472a59");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "songs","song_overrides","playlists","playlist_songs","playback_telemetry","markov_transitions","effect_settings","custom_eq_presets","lyrics_cache","widget_prefs");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `songs`");
      _db.execSQL("DELETE FROM `song_overrides`");
      _db.execSQL("DELETE FROM `playlists`");
      _db.execSQL("DELETE FROM `playlist_songs`");
      _db.execSQL("DELETE FROM `playback_telemetry`");
      _db.execSQL("DELETE FROM `markov_transitions`");
      _db.execSQL("DELETE FROM `effect_settings`");
      _db.execSQL("DELETE FROM `custom_eq_presets`");
      _db.execSQL("DELETE FROM `lyrics_cache`");
      _db.execSQL("DELETE FROM `widget_prefs`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LibraryDao.class, LibraryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PlaylistDao.class, PlaylistDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TelemetryDao.class, TelemetryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SettingsDao.class, SettingsDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LibraryDao libraryDao() {
    if (_libraryDao != null) {
      return _libraryDao;
    } else {
      synchronized(this) {
        if(_libraryDao == null) {
          _libraryDao = new LibraryDao_Impl(this);
        }
        return _libraryDao;
      }
    }
  }

  @Override
  public PlaylistDao playlistDao() {
    if (_playlistDao != null) {
      return _playlistDao;
    } else {
      synchronized(this) {
        if(_playlistDao == null) {
          _playlistDao = new PlaylistDao_Impl(this);
        }
        return _playlistDao;
      }
    }
  }

  @Override
  public TelemetryDao telemetryDao() {
    if (_telemetryDao != null) {
      return _telemetryDao;
    } else {
      synchronized(this) {
        if(_telemetryDao == null) {
          _telemetryDao = new TelemetryDao_Impl(this);
        }
        return _telemetryDao;
      }
    }
  }

  @Override
  public SettingsDao settingsDao() {
    if (_settingsDao != null) {
      return _settingsDao;
    } else {
      synchronized(this) {
        if(_settingsDao == null) {
          _settingsDao = new SettingsDao_Impl(this);
        }
        return _settingsDao;
      }
    }
  }
}
