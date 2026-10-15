package dev.abstra.pro.data.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import dev.abstra.pro.data.db.entities.CustomEqPresetEntity;
import dev.abstra.pro.data.db.entities.EffectSettingsEntity;
import dev.abstra.pro.data.db.entities.SettingsEntity;
import dev.abstra.pro.data.db.entities.WidgetPrefsEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SettingsDao_Impl implements SettingsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SettingsEntity> __insertionAdapterOfSettingsEntity;

  private final EntityInsertionAdapter<EffectSettingsEntity> __insertionAdapterOfEffectSettingsEntity;

  private final EntityInsertionAdapter<CustomEqPresetEntity> __insertionAdapterOfCustomEqPresetEntity;

  private final EntityInsertionAdapter<WidgetPrefsEntity> __insertionAdapterOfWidgetPrefsEntity;

  private final SharedSQLiteStatement __preparedStmtOfSetUserName;

  private final SharedSQLiteStatement __preparedStmtOfSetGreetingEnabled;

  private final SharedSQLiteStatement __preparedStmtOfMarkAskedForName;

  private final SharedSQLiteStatement __preparedStmtOfSetSelectedFont;

  private final SharedSQLiteStatement __preparedStmtOfSetGreetingStyle;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCustomPreset;

  public SettingsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSettingsEntity = new EntityInsertionAdapter<SettingsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `app_settings` (`id`,`userName`,`greetingEnabled`,`hasAskedForName`,`excludedFolderPaths`,`themeMode`,`accentColor`,`crossfadeSeconds`,`gaplessEnabled`,`defaultSleepTimerMin`,`keepScreenOnInPlayer`,`sortOption`,`showQueueOnSongClick`,`playlistOrder`,`selectedFont`,`greetingStyle`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SettingsEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getUserName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getUserName());
        }
        final int _tmp = entity.getGreetingEnabled() ? 1 : 0;
        statement.bindLong(3, _tmp);
        final int _tmp_1 = entity.getHasAskedForName() ? 1 : 0;
        statement.bindLong(4, _tmp_1);
        statement.bindString(5, entity.getExcludedFolderPaths());
        statement.bindString(6, entity.getThemeMode());
        statement.bindString(7, entity.getAccentColor());
        statement.bindLong(8, entity.getCrossfadeSeconds());
        final int _tmp_2 = entity.getGaplessEnabled() ? 1 : 0;
        statement.bindLong(9, _tmp_2);
        statement.bindLong(10, entity.getDefaultSleepTimerMin());
        final int _tmp_3 = entity.getKeepScreenOnInPlayer() ? 1 : 0;
        statement.bindLong(11, _tmp_3);
        statement.bindString(12, entity.getSortOption());
        final int _tmp_4 = entity.getShowQueueOnSongClick() ? 1 : 0;
        statement.bindLong(13, _tmp_4);
        statement.bindString(14, entity.getPlaylistOrder());
        statement.bindString(15, entity.getSelectedFont());
        statement.bindString(16, entity.getGreetingStyle());
      }
    };
    this.__insertionAdapterOfEffectSettingsEntity = new EntityInsertionAdapter<EffectSettingsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `effect_settings` (`id`,`isEqEnabled`,`activePresetName`,`eqBandsString`,`bassBoost`,`virtualizer`,`loudnessGain`,`balancePan`,`reverbPreset`,`dynamicsPreset`,`playbackSpeed`,`crossfadeSeconds`,`gaplessEnabled`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EffectSettingsEntity entity) {
        statement.bindLong(1, entity.getId());
        final int _tmp = entity.isEqEnabled() ? 1 : 0;
        statement.bindLong(2, _tmp);
        statement.bindString(3, entity.getActivePresetName());
        statement.bindString(4, entity.getEqBandsString());
        statement.bindLong(5, entity.getBassBoost());
        statement.bindLong(6, entity.getVirtualizer());
        statement.bindLong(7, entity.getLoudnessGain());
        statement.bindDouble(8, entity.getBalancePan());
        statement.bindLong(9, entity.getReverbPreset());
        statement.bindLong(10, entity.getDynamicsPreset());
        statement.bindDouble(11, entity.getPlaybackSpeed());
        statement.bindLong(12, entity.getCrossfadeSeconds());
        final int _tmp_1 = entity.getGaplessEnabled() ? 1 : 0;
        statement.bindLong(13, _tmp_1);
      }
    };
    this.__insertionAdapterOfCustomEqPresetEntity = new EntityInsertionAdapter<CustomEqPresetEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `custom_eq_presets` (`name`,`bandLevelsString`,`createdAt`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CustomEqPresetEntity entity) {
        statement.bindString(1, entity.getName());
        statement.bindString(2, entity.getBandLevelsString());
        statement.bindLong(3, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfWidgetPrefsEntity = new EntityInsertionAdapter<WidgetPrefsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `widget_prefs` (`id`,`coverShape`,`blurStrength`,`useSolidBg`,`accentColor`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WidgetPrefsEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCoverShape());
        statement.bindDouble(3, entity.getBlurStrength());
        final int _tmp = entity.getUseSolidBg() ? 1 : 0;
        statement.bindLong(4, _tmp);
        statement.bindLong(5, entity.getAccentColor());
      }
    };
    this.__preparedStmtOfSetUserName = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE app_settings SET userName = ? WHERE id = 0";
        return _query;
      }
    };
    this.__preparedStmtOfSetGreetingEnabled = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE app_settings SET greetingEnabled = ? WHERE id = 0";
        return _query;
      }
    };
    this.__preparedStmtOfMarkAskedForName = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE app_settings SET hasAskedForName = 1 WHERE id = 0";
        return _query;
      }
    };
    this.__preparedStmtOfSetSelectedFont = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE app_settings SET selectedFont = ? WHERE id = 0";
        return _query;
      }
    };
    this.__preparedStmtOfSetGreetingStyle = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE app_settings SET greetingStyle = ? WHERE id = 0";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteCustomPreset = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM custom_eq_presets WHERE name = ?";
        return _query;
      }
    };
  }

  @Override
  public Object saveSettings(final SettingsEntity settings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSettingsEntity.insert(settings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveEffectSettings(final EffectSettingsEntity settings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEffectSettingsEntity.insert(settings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCustomPreset(final CustomEqPresetEntity preset,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCustomEqPresetEntity.insert(preset);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object saveWidgetPrefs(final WidgetPrefsEntity prefs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWidgetPrefsEntity.insert(prefs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object setUserName(final String name, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetUserName.acquire();
        int _argIndex = 1;
        if (name == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, name);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetUserName.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setGreetingEnabled(final boolean enabled,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetGreetingEnabled.acquire();
        int _argIndex = 1;
        final int _tmp = enabled ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetGreetingEnabled.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markAskedForName(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAskedForName.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkAskedForName.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setSelectedFont(final String font, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetSelectedFont.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, font);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetSelectedFont.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setGreetingStyle(final String style, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetGreetingStyle.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, style);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetGreetingStyle.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCustomPreset(final String name,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCustomPreset.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, name);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteCustomPreset.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<SettingsEntity> observeSettings() {
    final String _sql = "SELECT * FROM app_settings WHERE id = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"app_settings"}, new Callable<SettingsEntity>() {
      @Override
      @Nullable
      public SettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfGreetingEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "greetingEnabled");
          final int _cursorIndexOfHasAskedForName = CursorUtil.getColumnIndexOrThrow(_cursor, "hasAskedForName");
          final int _cursorIndexOfExcludedFolderPaths = CursorUtil.getColumnIndexOrThrow(_cursor, "excludedFolderPaths");
          final int _cursorIndexOfThemeMode = CursorUtil.getColumnIndexOrThrow(_cursor, "themeMode");
          final int _cursorIndexOfAccentColor = CursorUtil.getColumnIndexOrThrow(_cursor, "accentColor");
          final int _cursorIndexOfCrossfadeSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "crossfadeSeconds");
          final int _cursorIndexOfGaplessEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "gaplessEnabled");
          final int _cursorIndexOfDefaultSleepTimerMin = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultSleepTimerMin");
          final int _cursorIndexOfKeepScreenOnInPlayer = CursorUtil.getColumnIndexOrThrow(_cursor, "keepScreenOnInPlayer");
          final int _cursorIndexOfSortOption = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOption");
          final int _cursorIndexOfShowQueueOnSongClick = CursorUtil.getColumnIndexOrThrow(_cursor, "showQueueOnSongClick");
          final int _cursorIndexOfPlaylistOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistOrder");
          final int _cursorIndexOfSelectedFont = CursorUtil.getColumnIndexOrThrow(_cursor, "selectedFont");
          final int _cursorIndexOfGreetingStyle = CursorUtil.getColumnIndexOrThrow(_cursor, "greetingStyle");
          final SettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpUserName;
            if (_cursor.isNull(_cursorIndexOfUserName)) {
              _tmpUserName = null;
            } else {
              _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            }
            final boolean _tmpGreetingEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfGreetingEnabled);
            _tmpGreetingEnabled = _tmp != 0;
            final boolean _tmpHasAskedForName;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasAskedForName);
            _tmpHasAskedForName = _tmp_1 != 0;
            final String _tmpExcludedFolderPaths;
            _tmpExcludedFolderPaths = _cursor.getString(_cursorIndexOfExcludedFolderPaths);
            final String _tmpThemeMode;
            _tmpThemeMode = _cursor.getString(_cursorIndexOfThemeMode);
            final String _tmpAccentColor;
            _tmpAccentColor = _cursor.getString(_cursorIndexOfAccentColor);
            final int _tmpCrossfadeSeconds;
            _tmpCrossfadeSeconds = _cursor.getInt(_cursorIndexOfCrossfadeSeconds);
            final boolean _tmpGaplessEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfGaplessEnabled);
            _tmpGaplessEnabled = _tmp_2 != 0;
            final int _tmpDefaultSleepTimerMin;
            _tmpDefaultSleepTimerMin = _cursor.getInt(_cursorIndexOfDefaultSleepTimerMin);
            final boolean _tmpKeepScreenOnInPlayer;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfKeepScreenOnInPlayer);
            _tmpKeepScreenOnInPlayer = _tmp_3 != 0;
            final String _tmpSortOption;
            _tmpSortOption = _cursor.getString(_cursorIndexOfSortOption);
            final boolean _tmpShowQueueOnSongClick;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfShowQueueOnSongClick);
            _tmpShowQueueOnSongClick = _tmp_4 != 0;
            final String _tmpPlaylistOrder;
            _tmpPlaylistOrder = _cursor.getString(_cursorIndexOfPlaylistOrder);
            final String _tmpSelectedFont;
            _tmpSelectedFont = _cursor.getString(_cursorIndexOfSelectedFont);
            final String _tmpGreetingStyle;
            _tmpGreetingStyle = _cursor.getString(_cursorIndexOfGreetingStyle);
            _result = new SettingsEntity(_tmpId,_tmpUserName,_tmpGreetingEnabled,_tmpHasAskedForName,_tmpExcludedFolderPaths,_tmpThemeMode,_tmpAccentColor,_tmpCrossfadeSeconds,_tmpGaplessEnabled,_tmpDefaultSleepTimerMin,_tmpKeepScreenOnInPlayer,_tmpSortOption,_tmpShowQueueOnSongClick,_tmpPlaylistOrder,_tmpSelectedFont,_tmpGreetingStyle);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSettingsSync(final Continuation<? super SettingsEntity> $completion) {
    final String _sql = "SELECT * FROM app_settings WHERE id = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SettingsEntity>() {
      @Override
      @Nullable
      public SettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfGreetingEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "greetingEnabled");
          final int _cursorIndexOfHasAskedForName = CursorUtil.getColumnIndexOrThrow(_cursor, "hasAskedForName");
          final int _cursorIndexOfExcludedFolderPaths = CursorUtil.getColumnIndexOrThrow(_cursor, "excludedFolderPaths");
          final int _cursorIndexOfThemeMode = CursorUtil.getColumnIndexOrThrow(_cursor, "themeMode");
          final int _cursorIndexOfAccentColor = CursorUtil.getColumnIndexOrThrow(_cursor, "accentColor");
          final int _cursorIndexOfCrossfadeSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "crossfadeSeconds");
          final int _cursorIndexOfGaplessEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "gaplessEnabled");
          final int _cursorIndexOfDefaultSleepTimerMin = CursorUtil.getColumnIndexOrThrow(_cursor, "defaultSleepTimerMin");
          final int _cursorIndexOfKeepScreenOnInPlayer = CursorUtil.getColumnIndexOrThrow(_cursor, "keepScreenOnInPlayer");
          final int _cursorIndexOfSortOption = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOption");
          final int _cursorIndexOfShowQueueOnSongClick = CursorUtil.getColumnIndexOrThrow(_cursor, "showQueueOnSongClick");
          final int _cursorIndexOfPlaylistOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "playlistOrder");
          final int _cursorIndexOfSelectedFont = CursorUtil.getColumnIndexOrThrow(_cursor, "selectedFont");
          final int _cursorIndexOfGreetingStyle = CursorUtil.getColumnIndexOrThrow(_cursor, "greetingStyle");
          final SettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpUserName;
            if (_cursor.isNull(_cursorIndexOfUserName)) {
              _tmpUserName = null;
            } else {
              _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            }
            final boolean _tmpGreetingEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfGreetingEnabled);
            _tmpGreetingEnabled = _tmp != 0;
            final boolean _tmpHasAskedForName;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfHasAskedForName);
            _tmpHasAskedForName = _tmp_1 != 0;
            final String _tmpExcludedFolderPaths;
            _tmpExcludedFolderPaths = _cursor.getString(_cursorIndexOfExcludedFolderPaths);
            final String _tmpThemeMode;
            _tmpThemeMode = _cursor.getString(_cursorIndexOfThemeMode);
            final String _tmpAccentColor;
            _tmpAccentColor = _cursor.getString(_cursorIndexOfAccentColor);
            final int _tmpCrossfadeSeconds;
            _tmpCrossfadeSeconds = _cursor.getInt(_cursorIndexOfCrossfadeSeconds);
            final boolean _tmpGaplessEnabled;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfGaplessEnabled);
            _tmpGaplessEnabled = _tmp_2 != 0;
            final int _tmpDefaultSleepTimerMin;
            _tmpDefaultSleepTimerMin = _cursor.getInt(_cursorIndexOfDefaultSleepTimerMin);
            final boolean _tmpKeepScreenOnInPlayer;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfKeepScreenOnInPlayer);
            _tmpKeepScreenOnInPlayer = _tmp_3 != 0;
            final String _tmpSortOption;
            _tmpSortOption = _cursor.getString(_cursorIndexOfSortOption);
            final boolean _tmpShowQueueOnSongClick;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfShowQueueOnSongClick);
            _tmpShowQueueOnSongClick = _tmp_4 != 0;
            final String _tmpPlaylistOrder;
            _tmpPlaylistOrder = _cursor.getString(_cursorIndexOfPlaylistOrder);
            final String _tmpSelectedFont;
            _tmpSelectedFont = _cursor.getString(_cursorIndexOfSelectedFont);
            final String _tmpGreetingStyle;
            _tmpGreetingStyle = _cursor.getString(_cursorIndexOfGreetingStyle);
            _result = new SettingsEntity(_tmpId,_tmpUserName,_tmpGreetingEnabled,_tmpHasAskedForName,_tmpExcludedFolderPaths,_tmpThemeMode,_tmpAccentColor,_tmpCrossfadeSeconds,_tmpGaplessEnabled,_tmpDefaultSleepTimerMin,_tmpKeepScreenOnInPlayer,_tmpSortOption,_tmpShowQueueOnSongClick,_tmpPlaylistOrder,_tmpSelectedFont,_tmpGreetingStyle);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<EffectSettingsEntity> getEffectSettings() {
    final String _sql = "SELECT * FROM effect_settings WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"effect_settings"}, new Callable<EffectSettingsEntity>() {
      @Override
      @Nullable
      public EffectSettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfIsEqEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEqEnabled");
          final int _cursorIndexOfActivePresetName = CursorUtil.getColumnIndexOrThrow(_cursor, "activePresetName");
          final int _cursorIndexOfEqBandsString = CursorUtil.getColumnIndexOrThrow(_cursor, "eqBandsString");
          final int _cursorIndexOfBassBoost = CursorUtil.getColumnIndexOrThrow(_cursor, "bassBoost");
          final int _cursorIndexOfVirtualizer = CursorUtil.getColumnIndexOrThrow(_cursor, "virtualizer");
          final int _cursorIndexOfLoudnessGain = CursorUtil.getColumnIndexOrThrow(_cursor, "loudnessGain");
          final int _cursorIndexOfBalancePan = CursorUtil.getColumnIndexOrThrow(_cursor, "balancePan");
          final int _cursorIndexOfReverbPreset = CursorUtil.getColumnIndexOrThrow(_cursor, "reverbPreset");
          final int _cursorIndexOfDynamicsPreset = CursorUtil.getColumnIndexOrThrow(_cursor, "dynamicsPreset");
          final int _cursorIndexOfPlaybackSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "playbackSpeed");
          final int _cursorIndexOfCrossfadeSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "crossfadeSeconds");
          final int _cursorIndexOfGaplessEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "gaplessEnabled");
          final EffectSettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final boolean _tmpIsEqEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEqEnabled);
            _tmpIsEqEnabled = _tmp != 0;
            final String _tmpActivePresetName;
            _tmpActivePresetName = _cursor.getString(_cursorIndexOfActivePresetName);
            final String _tmpEqBandsString;
            _tmpEqBandsString = _cursor.getString(_cursorIndexOfEqBandsString);
            final int _tmpBassBoost;
            _tmpBassBoost = _cursor.getInt(_cursorIndexOfBassBoost);
            final int _tmpVirtualizer;
            _tmpVirtualizer = _cursor.getInt(_cursorIndexOfVirtualizer);
            final int _tmpLoudnessGain;
            _tmpLoudnessGain = _cursor.getInt(_cursorIndexOfLoudnessGain);
            final float _tmpBalancePan;
            _tmpBalancePan = _cursor.getFloat(_cursorIndexOfBalancePan);
            final int _tmpReverbPreset;
            _tmpReverbPreset = _cursor.getInt(_cursorIndexOfReverbPreset);
            final int _tmpDynamicsPreset;
            _tmpDynamicsPreset = _cursor.getInt(_cursorIndexOfDynamicsPreset);
            final float _tmpPlaybackSpeed;
            _tmpPlaybackSpeed = _cursor.getFloat(_cursorIndexOfPlaybackSpeed);
            final int _tmpCrossfadeSeconds;
            _tmpCrossfadeSeconds = _cursor.getInt(_cursorIndexOfCrossfadeSeconds);
            final boolean _tmpGaplessEnabled;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfGaplessEnabled);
            _tmpGaplessEnabled = _tmp_1 != 0;
            _result = new EffectSettingsEntity(_tmpId,_tmpIsEqEnabled,_tmpActivePresetName,_tmpEqBandsString,_tmpBassBoost,_tmpVirtualizer,_tmpLoudnessGain,_tmpBalancePan,_tmpReverbPreset,_tmpDynamicsPreset,_tmpPlaybackSpeed,_tmpCrossfadeSeconds,_tmpGaplessEnabled);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<CustomEqPresetEntity>> getCustomPresets() {
    final String _sql = "SELECT * FROM custom_eq_presets ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"custom_eq_presets"}, new Callable<List<CustomEqPresetEntity>>() {
      @Override
      @NonNull
      public List<CustomEqPresetEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBandLevelsString = CursorUtil.getColumnIndexOrThrow(_cursor, "bandLevelsString");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CustomEqPresetEntity> _result = new ArrayList<CustomEqPresetEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CustomEqPresetEntity _item;
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBandLevelsString;
            _tmpBandLevelsString = _cursor.getString(_cursorIndexOfBandLevelsString);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new CustomEqPresetEntity(_tmpName,_tmpBandLevelsString,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<WidgetPrefsEntity> getWidgetPrefs() {
    final String _sql = "SELECT * FROM widget_prefs WHERE id = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"widget_prefs"}, new Callable<WidgetPrefsEntity>() {
      @Override
      @Nullable
      public WidgetPrefsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCoverShape = CursorUtil.getColumnIndexOrThrow(_cursor, "coverShape");
          final int _cursorIndexOfBlurStrength = CursorUtil.getColumnIndexOrThrow(_cursor, "blurStrength");
          final int _cursorIndexOfUseSolidBg = CursorUtil.getColumnIndexOrThrow(_cursor, "useSolidBg");
          final int _cursorIndexOfAccentColor = CursorUtil.getColumnIndexOrThrow(_cursor, "accentColor");
          final WidgetPrefsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpCoverShape;
            _tmpCoverShape = _cursor.getString(_cursorIndexOfCoverShape);
            final float _tmpBlurStrength;
            _tmpBlurStrength = _cursor.getFloat(_cursorIndexOfBlurStrength);
            final boolean _tmpUseSolidBg;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfUseSolidBg);
            _tmpUseSolidBg = _tmp != 0;
            final int _tmpAccentColor;
            _tmpAccentColor = _cursor.getInt(_cursorIndexOfAccentColor);
            _result = new WidgetPrefsEntity(_tmpId,_tmpCoverShape,_tmpBlurStrength,_tmpUseSolidBg,_tmpAccentColor);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
