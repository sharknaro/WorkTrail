package dk.worktrail.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public final class TrackerDb extends SQLiteOpenHelper {
    public static final class Place {
        public long id;
        public String name;
        public double latitude, longitude;
        public float radius;
        public String wifi;
    }
    public static final class Session {
        public long id, placeId, start;
        public Long end;
        public String placeName, source, wifiResult, note;
        public boolean manual;
    }
    /** Duration-only work without invented arrival/departure timestamps. */
    public static final class ManualWork {
        public long id;
        public String day, type, note;
        public int minutes;
    }
    public static final class Daily {
        public long elapsedMillis;
        public int onsiteCreditedMinutes, manualMinutes, creditedMinutes, targetMinutes;
    }

    public TrackerDb(Context context) { super(context, "worktrail.db", null, 2); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE places (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, lat REAL NOT NULL, lon REAL NOT NULL, radius REAL NOT NULL, wifi TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE sessions (_id INTEGER PRIMARY KEY AUTOINCREMENT, place_id INTEGER NOT NULL, place_name TEXT NOT NULL, start_ms INTEGER NOT NULL, end_ms INTEGER, source TEXT NOT NULL, wifi_result TEXT NOT NULL DEFAULT '', note TEXT NOT NULL DEFAULT '', manual INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX sessions_time ON sessions(start_ms,end_ms)");
        db.execSQL("CREATE TABLE transitions (_id INTEGER PRIMARY KEY AUTOINCREMENT, place_id INTEGER NOT NULL, kind TEXT NOT NULL, at_ms INTEGER NOT NULL, wifi_result TEXT NOT NULL)");
        createManualWorkTable(db);
    }

    private static void createManualWorkTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS manual_work (_id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, type TEXT NOT NULL, minutes INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX IF NOT EXISTS manual_work_day ON manual_work(day)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createManualWorkTable(db); // Keep all existing geofence visits.
    }

    private static Place readPlace(Cursor c) {
        Place p = new Place();
        p.id = c.getLong(0);
        p.name = c.getString(1);
        p.latitude = c.getDouble(2);
        p.longitude = c.getDouble(3);
        p.radius = c.getFloat(4);
        p.wifi = c.getString(5);
        return p;
    }

    public List<Place> places() {
        List<Place> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT _id,name,lat,lon,radius,wifi FROM places ORDER BY name", null)) {
            while (c.moveToNext()) list.add(readPlace(c));
        }
        return list;
    }

    public Place place(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT _id,name,lat,lon,radius,wifi FROM places WHERE _id=?", new String[]{String.valueOf(id)})) {
            return c.moveToFirst() ? readPlace(c) : null;
        }
    }

    public long savePlace(Place p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name);
        v.put("lat", p.latitude);
        v.put("lon", p.longitude);
        v.put("radius", p.radius);
        v.put("wifi", p.wifi == null ? "" : p.wifi);
        if (p.id > 0) {
            getWritableDatabase().update("places", v, "_id=?", new String[]{String.valueOf(p.id)});
            return p.id;
        }
        return getWritableDatabase().insertOrThrow("places", null, v);
    }

    public void removePlace(long id) {
        // Existing sessions retain their location name for historical records.
        getWritableDatabase().delete("places", "_id=?", new String[]{String.valueOf(id)});
    }

    private static Session readSession(Cursor c) {
        Session s = new Session();
        s.id = c.getLong(0);
        s.placeId = c.getLong(1);
        s.placeName = c.getString(2);
        s.start = c.getLong(3);
        s.end = c.isNull(4) ? null : c.getLong(4);
        s.source = c.getString(5);
        s.wifiResult = c.getString(6);
        s.note = c.getString(7);
        s.manual = c.getInt(8) != 0;
        return s;
    }

    public List<Session> sessions(int maxResults) {
        List<Session> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,place_id,place_name,start_ms,end_ms,source,wifi_result,note,manual FROM sessions ORDER BY start_ms DESC LIMIT " + Math.max(1, maxResults), null)) {
            while (c.moveToNext()) result.add(readSession(c));
        }
        return result;
    }

    public List<Session> sessionsForDay(long dayStart, long dayEnd) {
        List<Session> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,place_id,place_name,start_ms,end_ms,source,wifi_result,note,manual FROM sessions " +
                "WHERE start_ms<? AND (end_ms IS NULL OR end_ms>?) ORDER BY start_ms",
                new String[]{String.valueOf(dayEnd), String.valueOf(dayStart)})) {
            while (c.moveToNext()) result.add(readSession(c));
        }
        return result;
    }

    private static ManualWork readManualWork(Cursor c) {
        ManualWork m = new ManualWork();
        m.id = c.getLong(0);
        m.day = c.getString(1);
        m.type = c.getString(2);
        m.minutes = c.getInt(3);
        m.note = c.getString(4);
        return m;
    }

    public List<ManualWork> manualWorkForDay(String day) {
        List<ManualWork> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,day,type,minutes,note FROM manual_work WHERE day=? ORDER BY _id",
                new String[]{day})) {
            while (c.moveToNext()) list.add(readManualWork(c));
        }
        return list;
    }

    public List<ManualWork> allManualWork() {
        List<ManualWork> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,day,type,minutes,note FROM manual_work ORDER BY day DESC,_id DESC", null)) {
            while (c.moveToNext()) list.add(readManualWork(c));
        }
        return list;
    }

    public void saveManualWork(ManualWork m) {
        ContentValues v = new ContentValues();
        v.put("day", m.day);
        v.put("type", m.type);
        v.put("minutes", m.minutes);
        v.put("note", m.note == null ? "" : m.note);
        if (m.id > 0)
            getWritableDatabase().update("manual_work", v, "_id=?", new String[]{String.valueOf(m.id)});
        else
            getWritableDatabase().insertOrThrow("manual_work", null, v);
    }

    public void deleteManualWork(long id) {
        getWritableDatabase().delete("manual_work", "_id=?", new String[]{String.valueOf(id)});
    }

    public int manualMinutesForDay(String day) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(minutes),0) FROM manual_work WHERE day=?", new String[]{day})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public Session openSession() {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,place_id,place_name,start_ms,end_ms,source,wifi_result,note,manual FROM sessions WHERE end_ms IS NULL ORDER BY start_ms DESC LIMIT 1", null)) {
            return c.moveToFirst() ? readSession(c) : null;
        }
    }

    public void recordTransition(long placeId, boolean entered, long timestamp, String wifiResult) {
        Place p = place(placeId);
        if (p == null) return;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues raw = new ContentValues();
            raw.put("place_id", placeId);
            raw.put("kind", entered ? "enter" : "exit");
            raw.put("at_ms", timestamp);
            raw.put("wifi_result", wifiResult);
            db.insertOrThrow("transitions", null, raw);

            if (entered) {
                // A transition to a different workplace closes the previous one.
                ContentValues closing = new ContentValues();
                closing.put("end_ms", timestamp);
                db.update("sessions", closing, "end_ms IS NULL AND place_id<>?", new String[]{String.valueOf(placeId)});
                long existing = -1;
                try (Cursor c = db.rawQuery("SELECT _id FROM sessions WHERE end_ms IS NULL AND place_id=? LIMIT 1", new String[]{String.valueOf(placeId)})) {
                    if (c.moveToFirst()) existing = c.getLong(0);
                }
                if (existing == -1) {
                    // Merge brief boundary oscillations without losing the original arrival.
                    try (Cursor c = db.rawQuery("SELECT _id,end_ms FROM sessions WHERE place_id=? AND end_ms IS NOT NULL ORDER BY end_ms DESC LIMIT 1", new String[]{String.valueOf(placeId)})) {
                        if (c.moveToFirst() && timestamp >= c.getLong(1) && timestamp - c.getLong(1) <= 10 * 60000L)
                            existing = c.getLong(0);
                    }
                    if (existing != -1) {
                        ContentValues reopen = new ContentValues();
                        reopen.putNull("end_ms");
                        reopen.put("wifi_result", wifiResult);
                        db.update("sessions", reopen, "_id=?", new String[]{String.valueOf(existing)});
                    } else {
                        ContentValues session = new ContentValues();
                        session.put("place_id", placeId);
                        session.put("place_name", p.name);
                        session.put("start_ms", timestamp);
                        session.putNull("end_ms");
                        session.put("source", "geofence");
                        session.put("wifi_result", wifiResult);
                        db.insertOrThrow("sessions", null, session);
                    }
                }
            } else {
                ContentValues closing = new ContentValues();
                closing.put("end_ms", timestamp);
                db.update("sessions", closing, "end_ms IS NULL AND place_id=? AND start_ms<=?",
                        new String[]{String.valueOf(placeId), String.valueOf(timestamp)});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void saveManualSession(Session s) {
        ContentValues v = new ContentValues();
        v.put("place_id", s.placeId);
        v.put("place_name", s.placeName);
        v.put("start_ms", s.start);
        if (s.end == null) v.putNull("end_ms"); else v.put("end_ms", s.end);
        v.put("source", "manual");
        v.put("wifi_result", s.wifiResult == null ? "" : s.wifiResult);
        v.put("note", s.note == null ? "" : s.note);
        v.put("manual", 1);
        if (s.id > 0)
            getWritableDatabase().update("sessions", v, "_id=?", new String[]{String.valueOf(s.id)});
        else
            getWritableDatabase().insertOrThrow("sessions", null, v);
    }

    public void deleteSession(long id) {
        getWritableDatabase().delete("sessions", "_id=?", new String[]{String.valueOf(id)});
    }

    /** Includes sessions crossing midnight, including an active session up to now. */
    public long elapsedInDay(long dayStart, long dayEnd, long now) {
        long elapsed = 0L;
        long coveredUntil = dayStart;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT start_ms,end_ms FROM sessions WHERE start_ms<? AND (end_ms IS NULL OR end_ms>?) ORDER BY start_ms",
                new String[]{String.valueOf(dayEnd), String.valueOf(dayStart)})) {
            while (c.moveToNext()) {
                long start = c.getLong(0);
                long end = c.isNull(1) ? now : c.getLong(1);
                // Union of visit intervals: accidental overlapping records must not double-count time.
                long effectiveStart = Math.max(Math.max(start, dayStart), coveredUntil);
                long effectiveEnd = Math.min(end, Math.min(dayEnd, now));
                if (effectiveEnd > effectiveStart) elapsed += effectiveEnd - effectiveStart;
                coveredUntil = Math.max(coveredUntil, Math.max(dayStart, effectiveEnd));
            }
        }
        return elapsed;
    }

    public Daily daily(long start, long end, long now, int target, Schedule schedule) {
        Daily day = new Daily();
        day.elapsedMillis = elapsedInDay(start, end, now);
        day.onsiteCreditedMinutes = TimeMath.creditedMinutes(day.elapsedMillis, schedule.breakMinutes(), schedule.breakThresholdMinutes());
        day.manualMinutes = manualMinutesForDay(new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date(start)));
        day.creditedMinutes = TimeMath.totalCreditedMinutes(day.elapsedMillis, schedule.breakMinutes(), schedule.breakThresholdMinutes(), day.manualMinutes);
        day.targetMinutes = target;
        return day;
    }
}
