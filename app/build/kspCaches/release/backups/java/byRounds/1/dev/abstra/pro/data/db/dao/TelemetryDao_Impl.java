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
import dev.abstra.pro.data.db.entities.MarkovTransitionEntity;
import dev.abstra.pro.data.db.entities.PlaybackTelemetryEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
public final class TelemetryDao_Impl implements TelemetryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PlaybackTelemetryEntity> __insertionAdapterOfPlaybackTelemetryEntity;

  private final EntityInsertionAdapter<MarkovTransitionEntity> __insertionAdapterOfMarkovTransitionEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTelemetry;

  public TelemetryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlaybackTelemetryEntity = new EntityInsertionAdapter<PlaybackTelemetryEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `playback_telemetry` (`songId`,`playCount`,`fullCompletions`,`fastSkips`,`consecutiveSkips`,`repeatCount`,`morningPlays`,`afternoonPlays`,`eveningPlays`,`nightPlays`,`weekdayPlays`,`weekendPlays`,`lastPlayedTimestamp`,`totalPlayedMs`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlaybackTelemetryEntity entity) {
        statement.bindString(1, entity.getSongId());
        statement.bindLong(2, entity.getPlayCount());
        statement.bindLong(3, entity.getFullCompletions());
        statement.bindLong(4, entity.getFastSkips());
        statement.bindLong(5, entity.getConsecutiveSkips());
        statement.bindLong(6, entity.getRepeatCount());
        statement.bindLong(7, entity.getMorningPlays());
        statement.bindLong(8, entity.getAfternoonPlays());
        statement.bindLong(9, entity.getEveningPlays());
        statement.bindLong(10, entity.getNightPlays());
        statement.bindLong(11, entity.getWeekdayPlays());
        statement.bindLong(12, entity.getWeekendPlays());
        statement.bindLong(13, entity.getLastPlayedTimestamp());
        statement.bindLong(14, entity.getTotalPlayedMs());
      }
    };
    this.__insertionAdapterOfMarkovTransitionEntity = new EntityInsertionAdapter<MarkovTransitionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `markov_transitions` (`fromSongId`,`toSongId`,`transitionCount`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MarkovTransitionEntity entity) {
        statement.bindString(1, entity.getFromSongId());
        statement.bindString(2, entity.getToSongId());
        statement.bindLong(3, entity.getTransitionCount());
      }
    };
    this.__preparedStmtOfDeleteTelemetry = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM playback_telemetry WHERE songId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object saveTelemetry(final PlaybackTelemetryEntity telemetry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlaybackTelemetryEntity.insert(telemetry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object recordTransition(final MarkovTransitionEntity transition,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMarkovTransitionEntity.insert(transition);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTelemetry(final String songId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTelemetry.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, songId);
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
          __preparedStmtOfDeleteTelemetry.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MostPlayedRow>> observeMostPlayed() {
    final String _sql = "\n"
            + "        SELECT songId, playCount, lastPlayedTimestamp, totalPlayedMs \n"
            + "        FROM playback_telemetry \n"
            + "        WHERE playCount > 0 \n"
            + "        ORDER BY playCount DESC, lastPlayedTimestamp DESC \n"
            + "        LIMIT 20\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playback_telemetry"}, new Callable<List<MostPlayedRow>>() {
      @Override
      @NonNull
      public List<MostPlayedRow> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSongId = 0;
          final int _cursorIndexOfPlayCount = 1;
          final int _cursorIndexOfLastPlayedTimestamp = 2;
          final int _cursorIndexOfTotalPlayedMs = 3;
          final List<MostPlayedRow> _result = new ArrayList<MostPlayedRow>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MostPlayedRow _item;
            final String _tmpSongId;
            _tmpSongId = _cursor.getString(_cursorIndexOfSongId);
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final long _tmpLastPlayedTimestamp;
            _tmpLastPlayedTimestamp = _cursor.getLong(_cursorIndexOfLastPlayedTimestamp);
            final long _tmpTotalPlayedMs;
            _tmpTotalPlayedMs = _cursor.getLong(_cursorIndexOfTotalPlayedMs);
            _item = new MostPlayedRow(_tmpSongId,_tmpPlayCount,_tmpLastPlayedTimestamp,_tmpTotalPlayedMs);
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
  public Flow<List<PlaybackTelemetryEntity>> getAllTelemetry() {
    final String _sql = "SELECT * FROM playback_telemetry";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playback_telemetry"}, new Callable<List<PlaybackTelemetryEntity>>() {
      @Override
      @NonNull
      public List<PlaybackTelemetryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "songId");
          final int _cursorIndexOfPlayCount = CursorUtil.getColumnIndexOrThrow(_cursor, "playCount");
          final int _cursorIndexOfFullCompletions = CursorUtil.getColumnIndexOrThrow(_cursor, "fullCompletions");
          final int _cursorIndexOfFastSkips = CursorUtil.getColumnIndexOrThrow(_cursor, "fastSkips");
          final int _cursorIndexOfConsecutiveSkips = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveSkips");
          final int _cursorIndexOfRepeatCount = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatCount");
          final int _cursorIndexOfMorningPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "morningPlays");
          final int _cursorIndexOfAfternoonPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "afternoonPlays");
          final int _cursorIndexOfEveningPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningPlays");
          final int _cursorIndexOfNightPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "nightPlays");
          final int _cursorIndexOfWeekdayPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdayPlays");
          final int _cursorIndexOfWeekendPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekendPlays");
          final int _cursorIndexOfLastPlayedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayedTimestamp");
          final int _cursorIndexOfTotalPlayedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPlayedMs");
          final List<PlaybackTelemetryEntity> _result = new ArrayList<PlaybackTelemetryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlaybackTelemetryEntity _item;
            final String _tmpSongId;
            _tmpSongId = _cursor.getString(_cursorIndexOfSongId);
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final int _tmpFullCompletions;
            _tmpFullCompletions = _cursor.getInt(_cursorIndexOfFullCompletions);
            final int _tmpFastSkips;
            _tmpFastSkips = _cursor.getInt(_cursorIndexOfFastSkips);
            final int _tmpConsecutiveSkips;
            _tmpConsecutiveSkips = _cursor.getInt(_cursorIndexOfConsecutiveSkips);
            final int _tmpRepeatCount;
            _tmpRepeatCount = _cursor.getInt(_cursorIndexOfRepeatCount);
            final int _tmpMorningPlays;
            _tmpMorningPlays = _cursor.getInt(_cursorIndexOfMorningPlays);
            final int _tmpAfternoonPlays;
            _tmpAfternoonPlays = _cursor.getInt(_cursorIndexOfAfternoonPlays);
            final int _tmpEveningPlays;
            _tmpEveningPlays = _cursor.getInt(_cursorIndexOfEveningPlays);
            final int _tmpNightPlays;
            _tmpNightPlays = _cursor.getInt(_cursorIndexOfNightPlays);
            final int _tmpWeekdayPlays;
            _tmpWeekdayPlays = _cursor.getInt(_cursorIndexOfWeekdayPlays);
            final int _tmpWeekendPlays;
            _tmpWeekendPlays = _cursor.getInt(_cursorIndexOfWeekendPlays);
            final long _tmpLastPlayedTimestamp;
            _tmpLastPlayedTimestamp = _cursor.getLong(_cursorIndexOfLastPlayedTimestamp);
            final long _tmpTotalPlayedMs;
            _tmpTotalPlayedMs = _cursor.getLong(_cursorIndexOfTotalPlayedMs);
            _item = new PlaybackTelemetryEntity(_tmpSongId,_tmpPlayCount,_tmpFullCompletions,_tmpFastSkips,_tmpConsecutiveSkips,_tmpRepeatCount,_tmpMorningPlays,_tmpAfternoonPlays,_tmpEveningPlays,_tmpNightPlays,_tmpWeekdayPlays,_tmpWeekendPlays,_tmpLastPlayedTimestamp,_tmpTotalPlayedMs);
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
  public Object getAllTelemetrySync(
      final Continuation<? super List<PlaybackTelemetryEntity>> $completion) {
    final String _sql = "SELECT * FROM playback_telemetry";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PlaybackTelemetryEntity>>() {
      @Override
      @NonNull
      public List<PlaybackTelemetryEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "songId");
          final int _cursorIndexOfPlayCount = CursorUtil.getColumnIndexOrThrow(_cursor, "playCount");
          final int _cursorIndexOfFullCompletions = CursorUtil.getColumnIndexOrThrow(_cursor, "fullCompletions");
          final int _cursorIndexOfFastSkips = CursorUtil.getColumnIndexOrThrow(_cursor, "fastSkips");
          final int _cursorIndexOfConsecutiveSkips = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveSkips");
          final int _cursorIndexOfRepeatCount = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatCount");
          final int _cursorIndexOfMorningPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "morningPlays");
          final int _cursorIndexOfAfternoonPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "afternoonPlays");
          final int _cursorIndexOfEveningPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningPlays");
          final int _cursorIndexOfNightPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "nightPlays");
          final int _cursorIndexOfWeekdayPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdayPlays");
          final int _cursorIndexOfWeekendPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekendPlays");
          final int _cursorIndexOfLastPlayedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayedTimestamp");
          final int _cursorIndexOfTotalPlayedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPlayedMs");
          final List<PlaybackTelemetryEntity> _result = new ArrayList<PlaybackTelemetryEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlaybackTelemetryEntity _item;
            final String _tmpSongId;
            _tmpSongId = _cursor.getString(_cursorIndexOfSongId);
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final int _tmpFullCompletions;
            _tmpFullCompletions = _cursor.getInt(_cursorIndexOfFullCompletions);
            final int _tmpFastSkips;
            _tmpFastSkips = _cursor.getInt(_cursorIndexOfFastSkips);
            final int _tmpConsecutiveSkips;
            _tmpConsecutiveSkips = _cursor.getInt(_cursorIndexOfConsecutiveSkips);
            final int _tmpRepeatCount;
            _tmpRepeatCount = _cursor.getInt(_cursorIndexOfRepeatCount);
            final int _tmpMorningPlays;
            _tmpMorningPlays = _cursor.getInt(_cursorIndexOfMorningPlays);
            final int _tmpAfternoonPlays;
            _tmpAfternoonPlays = _cursor.getInt(_cursorIndexOfAfternoonPlays);
            final int _tmpEveningPlays;
            _tmpEveningPlays = _cursor.getInt(_cursorIndexOfEveningPlays);
            final int _tmpNightPlays;
            _tmpNightPlays = _cursor.getInt(_cursorIndexOfNightPlays);
            final int _tmpWeekdayPlays;
            _tmpWeekdayPlays = _cursor.getInt(_cursorIndexOfWeekdayPlays);
            final int _tmpWeekendPlays;
            _tmpWeekendPlays = _cursor.getInt(_cursorIndexOfWeekendPlays);
            final long _tmpLastPlayedTimestamp;
            _tmpLastPlayedTimestamp = _cursor.getLong(_cursorIndexOfLastPlayedTimestamp);
            final long _tmpTotalPlayedMs;
            _tmpTotalPlayedMs = _cursor.getLong(_cursorIndexOfTotalPlayedMs);
            _item = new PlaybackTelemetryEntity(_tmpSongId,_tmpPlayCount,_tmpFullCompletions,_tmpFastSkips,_tmpConsecutiveSkips,_tmpRepeatCount,_tmpMorningPlays,_tmpAfternoonPlays,_tmpEveningPlays,_tmpNightPlays,_tmpWeekdayPlays,_tmpWeekendPlays,_tmpLastPlayedTimestamp,_tmpTotalPlayedMs);
            _result.add(_item);
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
  public Object getTelemetry(final String songId,
      final Continuation<? super PlaybackTelemetryEntity> $completion) {
    final String _sql = "SELECT * FROM playback_telemetry WHERE songId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, songId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PlaybackTelemetryEntity>() {
      @Override
      @Nullable
      public PlaybackTelemetryEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "songId");
          final int _cursorIndexOfPlayCount = CursorUtil.getColumnIndexOrThrow(_cursor, "playCount");
          final int _cursorIndexOfFullCompletions = CursorUtil.getColumnIndexOrThrow(_cursor, "fullCompletions");
          final int _cursorIndexOfFastSkips = CursorUtil.getColumnIndexOrThrow(_cursor, "fastSkips");
          final int _cursorIndexOfConsecutiveSkips = CursorUtil.getColumnIndexOrThrow(_cursor, "consecutiveSkips");
          final int _cursorIndexOfRepeatCount = CursorUtil.getColumnIndexOrThrow(_cursor, "repeatCount");
          final int _cursorIndexOfMorningPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "morningPlays");
          final int _cursorIndexOfAfternoonPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "afternoonPlays");
          final int _cursorIndexOfEveningPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "eveningPlays");
          final int _cursorIndexOfNightPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "nightPlays");
          final int _cursorIndexOfWeekdayPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekdayPlays");
          final int _cursorIndexOfWeekendPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "weekendPlays");
          final int _cursorIndexOfLastPlayedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "lastPlayedTimestamp");
          final int _cursorIndexOfTotalPlayedMs = CursorUtil.getColumnIndexOrThrow(_cursor, "totalPlayedMs");
          final PlaybackTelemetryEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpSongId;
            _tmpSongId = _cursor.getString(_cursorIndexOfSongId);
            final int _tmpPlayCount;
            _tmpPlayCount = _cursor.getInt(_cursorIndexOfPlayCount);
            final int _tmpFullCompletions;
            _tmpFullCompletions = _cursor.getInt(_cursorIndexOfFullCompletions);
            final int _tmpFastSkips;
            _tmpFastSkips = _cursor.getInt(_cursorIndexOfFastSkips);
            final int _tmpConsecutiveSkips;
            _tmpConsecutiveSkips = _cursor.getInt(_cursorIndexOfConsecutiveSkips);
            final int _tmpRepeatCount;
            _tmpRepeatCount = _cursor.getInt(_cursorIndexOfRepeatCount);
            final int _tmpMorningPlays;
            _tmpMorningPlays = _cursor.getInt(_cursorIndexOfMorningPlays);
            final int _tmpAfternoonPlays;
            _tmpAfternoonPlays = _cursor.getInt(_cursorIndexOfAfternoonPlays);
            final int _tmpEveningPlays;
            _tmpEveningPlays = _cursor.getInt(_cursorIndexOfEveningPlays);
            final int _tmpNightPlays;
            _tmpNightPlays = _cursor.getInt(_cursorIndexOfNightPlays);
            final int _tmpWeekdayPlays;
            _tmpWeekdayPlays = _cursor.getInt(_cursorIndexOfWeekdayPlays);
            final int _tmpWeekendPlays;
            _tmpWeekendPlays = _cursor.getInt(_cursorIndexOfWeekendPlays);
            final long _tmpLastPlayedTimestamp;
            _tmpLastPlayedTimestamp = _cursor.getLong(_cursorIndexOfLastPlayedTimestamp);
            final long _tmpTotalPlayedMs;
            _tmpTotalPlayedMs = _cursor.getLong(_cursorIndexOfTotalPlayedMs);
            _result = new PlaybackTelemetryEntity(_tmpSongId,_tmpPlayCount,_tmpFullCompletions,_tmpFastSkips,_tmpConsecutiveSkips,_tmpRepeatCount,_tmpMorningPlays,_tmpAfternoonPlays,_tmpEveningPlays,_tmpNightPlays,_tmpWeekdayPlays,_tmpWeekendPlays,_tmpLastPlayedTimestamp,_tmpTotalPlayedMs);
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
  public Object getTransitionsFrom(final String fromSongId,
      final Continuation<? super List<MarkovTransitionEntity>> $completion) {
    final String _sql = "SELECT * FROM markov_transitions WHERE fromSongId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, fromSongId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<MarkovTransitionEntity>>() {
      @Override
      @NonNull
      public List<MarkovTransitionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfFromSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "fromSongId");
          final int _cursorIndexOfToSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "toSongId");
          final int _cursorIndexOfTransitionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "transitionCount");
          final List<MarkovTransitionEntity> _result = new ArrayList<MarkovTransitionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MarkovTransitionEntity _item;
            final String _tmpFromSongId;
            _tmpFromSongId = _cursor.getString(_cursorIndexOfFromSongId);
            final String _tmpToSongId;
            _tmpToSongId = _cursor.getString(_cursorIndexOfToSongId);
            final int _tmpTransitionCount;
            _tmpTransitionCount = _cursor.getInt(_cursorIndexOfTransitionCount);
            _item = new MarkovTransitionEntity(_tmpFromSongId,_tmpToSongId,_tmpTransitionCount);
            _result.add(_item);
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
  public Object getTransition(final String fromSongId, final String toSongId,
      final Continuation<? super MarkovTransitionEntity> $completion) {
    final String _sql = "SELECT * FROM markov_transitions WHERE fromSongId = ? AND toSongId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, fromSongId);
    _argIndex = 2;
    _statement.bindString(_argIndex, toSongId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MarkovTransitionEntity>() {
      @Override
      @Nullable
      public MarkovTransitionEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfFromSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "fromSongId");
          final int _cursorIndexOfToSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "toSongId");
          final int _cursorIndexOfTransitionCount = CursorUtil.getColumnIndexOrThrow(_cursor, "transitionCount");
          final MarkovTransitionEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpFromSongId;
            _tmpFromSongId = _cursor.getString(_cursorIndexOfFromSongId);
            final String _tmpToSongId;
            _tmpToSongId = _cursor.getString(_cursorIndexOfToSongId);
            final int _tmpTransitionCount;
            _tmpTransitionCount = _cursor.getInt(_cursorIndexOfTransitionCount);
            _result = new MarkovTransitionEntity(_tmpFromSongId,_tmpToSongId,_tmpTransitionCount);
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
  public Flow<Long> getTotalLifetimePlaytime() {
    final String _sql = "SELECT SUM(totalPlayedMs) FROM playback_telemetry";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playback_telemetry"}, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final Long _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(0);
            }
            _result = _tmp;
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
  public Flow<Integer> getTotalPlayCount() {
    final String _sql = "SELECT SUM(playCount) FROM playback_telemetry";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"playback_telemetry"}, new Callable<Integer>() {
      @Override
      @Nullable
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final Integer _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getInt(0);
            }
            _result = _tmp;
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
