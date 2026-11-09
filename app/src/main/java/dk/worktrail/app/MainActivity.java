package dk.worktrail.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG = 0xFF0C1526, PANEL = 0xFF18263B, MUTED = 0xFF9AACBF;
    private static final int WHITE = 0xFFF3F7FC, ACCENT = 0xFF79D8C4, NEGATIVE = 0xFFFF9A9A;
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private final SimpleDateFormat date = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final SimpleDateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private TrackerDb db;
    private Schedule schedule;
    private LinearLayout root, page;
    private String tab = "Today", exportType = "visits";
    private boolean initialSyncDone = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        db = new TrackerDb(this);
        schedule = new Schedule(this);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (Geofences.canMonitor(this) && !initialSyncDone) {
            initialSyncDone = true;
            Geofences.sync(this);
        }
        if (root != null) render();
    }

    private int px(float dp) { return (int) (dp * getResources().getDisplayMetrics().density + .5f); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(px(radius));
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout column() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        return c;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setPadding(px(17), px(16), px(17), px(16));
        c.setBackground(shape(PANEL, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 0, 0, px(12));
        page.addView(c, p);
        return c;
    }

    private void line(LinearLayout parent, String value, int size, int color, boolean bold) {
        parent.addView(text(value, size, color, bold));
    }

    private void smallGap(LinearLayout parent) {
        View space = new View(this);
        parent.addView(space, new LinearLayout.LayoutParams(1, px(8)));
    }

    private Button button(String label, Runnable click) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextColor(BG);
        b.setTextSize(14);
        b.setBackground(shape(ACCENT, 12));
        b.setOnClickListener(v -> click.run());
        return b;
    }

    private void addButton(LinearLayout parent, String label, Runnable click) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, px(47));
        params.topMargin = px(12);
        parent.addView(button(label, click), params);
    }

    private void render() {
        root = column();
        root.setBackgroundColor(BG);
        setContentView(root);
        LinearLayout heading = column();
        heading.setPadding(px(22), px(20), px(22), px(17));
        line(heading, "WORKTRAIL  /  PERSONAL TIMESHEET", 11, ACCENT, true);
        smallGap(heading);
        line(heading, tab, 29, WHITE, true);
        root.addView(heading);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = column();
        page.setPadding(px(18), 0, px(18), px(30));
        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        switch (tab) {
            case "History": renderHistory(); break;
            case "Places": renderPlaces(); break;
            case "Schedule": renderSchedule(); break;
            default: renderToday();
        }
        LinearLayout nav = row();
        nav.setPadding(px(8), px(9), px(8), px(12));
        nav.setBackgroundColor(PANEL);
        for (String item : new String[]{"Today", "History", "Places", "Schedule"}) {
            TextView n = text(item, 13, tab.equals(item) ? ACCENT : MUTED, tab.equals(item));
            n.setGravity(Gravity.CENTER);
            n.setPadding(px(2), px(8), px(2), px(8));
            n.setOnClickListener(v -> { tab = item; render(); });
            nav.addView(n, new LinearLayout.LayoutParams(0, -2, 1));
        }
        root.addView(nav);
    }

    private void renderToday() {
        long now = System.currentTimeMillis();
        Calendar today = Calendar.getInstance();
        Calendar start = (Calendar) today.clone();
        midnight(start);
        Calendar tomorrow = (Calendar) start.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        TrackerDb.Daily current = db.daily(start.getTimeInMillis(), tomorrow.getTimeInMillis(), now, schedule.targetForDay(start), schedule);
        TrackerDb.Session open = db.openSession();
        LinearLayout top = card();
        line(top, date.format(new Date(now)), 14, MUTED, false);
        smallGap(top);
        line(top, open == null ? "Away from work" : "At " + open.placeName, 22, open == null ? WHITE : ACCENT, true);
        if (open != null) {
            smallGap(top);
            line(top, "Entry: " + stamp.format(new Date(open.start)) + "  ·  " + open.source, 13, MUTED, false);
            addButton(top, "End this visit manually", () -> manuallyFinish(open));
        } else if (!db.places().isEmpty()) {
            addButton(top, "Start a visit manually", this::chooseManualCheckIn);
        }

        LinearLayout worked = card();
        line(worked, "CREDITED WORK TODAY", 12, MUTED, true);
        smallGap(worked);
        line(worked, TimeMath.hhmm(current.creditedMinutes) + " h", 36, WHITE, true);
        line(worked, "Elapsed " + TimeMath.hhmm(TimeMath.minutes(current.elapsedMillis)) + " h  ·  Unpaid breaks per schedule", 12, MUTED, false);
        if (schedule.configured()) {
            smallGap(worked);
            int balance = current.creditedMinutes - current.targetMinutes;
            line(worked, "Target " + TimeMath.hhmm(current.targetMinutes) + " h    Balance " + TimeMath.signed(balance) + " h", 14, balance >= 0 ? ACCENT : NEGATIVE, true);
        } else {
            addButton(worked, "Set your custom weekly schedule", () -> { tab = "Schedule"; render(); editSchedule(); });
        }

        if (!Geofences.canMonitor(this)) {
            LinearLayout permissions = card();
            line(permissions, "Automatic detection not yet enabled", 17, WHITE, true);
            smallGap(permissions);
            line(permissions, "Allow precise location and 'Allow all the time' so entries can be recorded while your phone is locked.", 13, MUTED, false);
            addButton(permissions, "Enable location tracking", this::requestTrackingPermissions);
        } else if (db.places().isEmpty()) {
            LinearLayout empty = card();
            line(empty, "Add your first work location", 17, WHITE, true);
            line(empty, "Register a building in Herlev, at DTU, or anywhere else.", 13, MUTED, false);
            addButton(empty, "Add a location", () -> { tab = "Places"; render(); editPlace(null); });
        }

        LinearLayout week = card();
        line(week, "THIS WEEK  ·  MON–SUN", 13, MUTED, true);
        smallGap(week);
        Calendar monday = TimeMath.monday(now);
        int totalCredited = 0, targetToDate = 0;
        for (int i = 0; i < 7; i++) {
            Calendar end = (Calendar) monday.clone();
            end.add(Calendar.DAY_OF_MONTH, 1);
            TrackerDb.Daily d = db.daily(monday.getTimeInMillis(), end.getTimeInMillis(), now, schedule.target(i), schedule);
            boolean future = monday.getTimeInMillis() > now;
            LinearLayout r = row();
            r.setPadding(0, px(8), 0, px(8));
            TextView day = text(DAYS[i], 14, future ? MUTED : WHITE, false);
            r.addView(day, new LinearLayout.LayoutParams(0, -2, 1));
            String summary = future ? "—" : TimeMath.hhmm(d.creditedMinutes) + " h";
            if (!future && schedule.configured()) summary += "   " + TimeMath.signed(d.creditedMinutes - d.targetMinutes);
            r.addView(text(summary, 14, future ? MUTED : (schedule.configured() && d.creditedMinutes < d.targetMinutes ? NEGATIVE : ACCENT), !future));
            week.addView(r);
            if (!future) { totalCredited += d.creditedMinutes; targetToDate += d.targetMinutes; }
            monday = end;
        }
        smallGap(week);
        line(week, "Week to date: " + TimeMath.hhmm(totalCredited) + " h", 19, WHITE, true);
        if (schedule.configured())
            line(week, "Balance to date: " + TimeMath.signed(totalCredited - targetToDate) + " h    ·    Full-week target: " + TimeMath.hhmm(schedule.weeklyTarget()) + " h", 13, ACCENT, true);
        line(week, "Automatic timestamps are estimates; review records when precision matters.", 12, MUTED, false);
    }

    private void renderHistory() {
        LinearLayout actions = card();
        line(actions, "All records stay on your phone until you choose to export them.", 13, MUTED, false);
        addButton(actions, "Add a missing visit", () -> editSession(null));
        addButton(actions, "Export visit records (.csv)", () -> beginExport("visits"));
        addButton(actions, "Export last 365 daily balances (.csv)", () -> beginExport("daily"));
        if (schedule.configured()) {
            LinearLayout weekly = card();
            line(weekly, "RECENT WEEKLY BALANCES", 13, MUTED, true);
            smallGap(weekly);
            Calendar monday = TimeMath.monday(System.currentTimeMillis());
            for (int week = 0; week < 8; week++) {
                Calendar day = (Calendar) monday.clone();
                day.add(Calendar.DAY_OF_MONTH, -7 * week);
                String label = new SimpleDateFormat("d MMM", Locale.getDefault()).format(day.getTime());
                long now = System.currentTimeMillis();
                int credited = 0, target = 0;
                for (int d = 0; d < 7; d++) {
                    Calendar end = (Calendar) day.clone();
                    end.add(Calendar.DAY_OF_MONTH, 1);
                    if (day.getTimeInMillis() <= now) {
                        TrackerDb.Daily stat = db.daily(day.getTimeInMillis(), end.getTimeInMillis(), now, schedule.target(d), schedule);
                        credited += stat.creditedMinutes;
                        target += stat.targetMinutes;
                    }
                    day = end;
                }
                // Do not display empty historical weeks preceding first usage.
                if (week > 0 && credited == 0) continue;
                int balance = credited - target;
                line(weekly, label + "  ·  " + TimeMath.hhmm(credited) + " / " + TimeMath.hhmm(target) + " h  ·  " + TimeMath.signed(balance) + " h",
                        14, balance >= 0 ? ACCENT : NEGATIVE, true);
                smallGap(weekly);
            }
            line(weekly, "Current week is compared through today; completed weeks use all seven target days.", 12, MUTED, false);
        }
        List<TrackerDb.Session> sessions = db.sessions(100);
        if (sessions.isEmpty()) line(card(), "No visits yet. A location entry or a manual visit will appear here.", 14, MUTED, false);
        for (TrackerDb.Session s : sessions) {
            LinearLayout c = card();
            line(c, s.placeName + (s.manual ? "   ·   edited" : ""), 18, WHITE, true);
            smallGap(c);
            line(c, stamp.format(new Date(s.start)) + " → " + (s.end == null ? "ongoing" : stamp.format(new Date(s.end))), 13, MUTED, false);
            if (s.end != null) line(c, TimeMath.hhmm(TimeMath.minutes(s.end - s.start)) + " elapsed hours", 13, ACCENT, true);
            line(c, "Method: " + s.source + "  ·  Wi-Fi: " + s.wifiResult, 12, MUTED, false);
            if (!s.note.isEmpty()) line(c, s.note, 12, MUTED, false);
            addButton(c, "Review / correct", () -> editSession(s));
        }
    }

    private void renderPlaces() {
        LinearLayout intro = card();
        line(intro, "Geofences around specific buildings", 18, WHITE, true);
        smallGap(intro);
        line(intro, "Use a location radius of about 100–200 metres. Wi-Fi is optional corroboration, not a required trigger.", 13, MUTED, false);
        addButton(intro, "+ Add workplace", () -> editPlace(null));
        for (TrackerDb.Place place : db.places()) {
            LinearLayout c = card();
            line(c, place.name, 19, WHITE, true);
            smallGap(c);
            line(c, String.format(Locale.US, "%.6f, %.6f   ·   %.0f m radius", place.latitude, place.longitude, place.radius), 13, MUTED, false);
            line(c, "Wi-Fi: " + (place.wifi.isEmpty() ? "not configured" : place.wifi), 12, ACCENT, false);
            addButton(c, "Edit workplace", () -> editPlace(place));
        }
    }

    private void renderSchedule() {
        LinearLayout c = card();
        line(c, schedule.configured() ? "Your weekly targets" : "Set your custom weekly hours", 19, WHITE, true);
        smallGap(c);
        line(c, schedule.configured() ? "Total: " + TimeMath.hhmm(schedule.weeklyTarget()) + " hours/week" : "No target configured yet", 22, ACCENT, true);
        for (int i = 0; i < 7; i++) line(c, DAYS[i] + ":   " + TimeMath.hhmm(schedule.target(i)) + " hours", 14, MUTED, false);
        smallGap(c);
        line(c, "Unpaid break: " + TimeMath.hhmm(schedule.breakMinutes()) + " h when daily elapsed time reaches " + TimeMath.hhmm(schedule.breakThresholdMinutes()) + " h.", 13, MUTED, false);
        addButton(c, "Edit schedule and break policy", this::editSchedule);
        LinearLayout privacy = card();
        line(privacy, "Tracking and privacy", 18, WHITE, true);
        smallGap(privacy);
        line(privacy, "Your visits are stored locally. No account or server is used. Geofences use Google Play services for location detection. Wi-Fi network names are compared on-device.", 13, MUTED, false);
        addButton(privacy, "Check location permission", this::requestTrackingPermissions);
        addButton(privacy, "Request notification permission", this::requestNotifications);
        line(privacy, "For reliable detection, keep device location and Wi-Fi scanning available. Some Android battery settings can delay triggers.", 12, MUTED, false);
    }

    private EditText field(LinearLayout parent, String label, String value) {
        TextView caption = text(label, 13, MUTED, false);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = px(11);
        parent.addView(caption, cp);
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setText(value);
        e.setTextColor(WHITE);
        e.setHintTextColor(MUTED);
        e.setTextSize(16);
        e.setPadding(px(10), px(9), px(10), px(9));
        e.setBackground(shape(PANEL, 8));
        parent.addView(e, new LinearLayout.LayoutParams(-1, px(43)));
        return e;
    }

    private ScrollView dialogContent(LinearLayout content) {
        content.setPadding(px(20), px(7), px(20), px(10));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        return scroll;
    }

    private void editPlace(TrackerDb.Place old) {
        LinearLayout content = column();
        EditText name = field(content, "Location name", old == null ? "" : old.name);
        EditText latitude = field(content, "Latitude (decimal degrees)", old == null ? "" : String.valueOf(old.latitude));
        EditText longitude = field(content, "Longitude (decimal degrees)", old == null ? "" : String.valueOf(old.longitude));
        EditText radius = field(content, "Radius (metres, 100–1000)", old == null ? "150" : String.valueOf((int) old.radius));
        EditText wifi = field(content, "Optional workplace Wi-Fi SSID", old == null ? "" : old.wifi);
        addButton(content, "Use my current GPS location", () -> {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestTrackingPermissions();
                return;
            }
            try {
                LocationServices.getFusedLocationProviderClient(this)
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener(loc -> {
                            if (loc == null) toast("Could not get a fix. Try outdoors or enter coordinates.");
                            else {
                                latitude.setText(String.format(Locale.US, "%.7f", loc.getLatitude()));
                                longitude.setText(String.format(Locale.US, "%.7f", loc.getLongitude()));
                            }
                        }).addOnFailureListener(e -> toast("Location unavailable: " + e.getMessage()));
            } catch (SecurityException e) { toast("Precise location permission is required"); }
        });
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(old == null ? "Add workplace" : "Edit workplace")
                .setView(dialogContent(content))
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        if (old != null) dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Remove", (d, w) -> confirmDeletePlace(old));
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String n = name.getText().toString().trim();
                double lat = Double.parseDouble(latitude.getText().toString().trim());
                double lon = Double.parseDouble(longitude.getText().toString().trim());
                float r = Float.parseFloat(radius.getText().toString().trim());
                if (n.isEmpty() || !Double.isFinite(lat) || !Double.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180 || r < 100 || r > 1000)
                    throw new IllegalArgumentException("Check name, coordinates and radius (100–1000 m)");
                TrackerDb.Place p = old == null ? new TrackerDb.Place() : old;
                p.name = n; p.latitude = lat; p.longitude = lon; p.radius = r;
                p.wifi = wifi.getText().toString().trim();
                if (old == null && db.places().size() >= 100)
                    throw new IllegalArgumentException("Android supports at most 100 active geofences for this app");
                db.savePlace(p);
                Geofences.sync(this);
                initialSyncDone = Geofences.canMonitor(this);
                dialog.dismiss();
                render();
            } catch (Exception error) { toast("Invalid workplace: " + error.getMessage()); }
        }));
        dialog.show();
    }

    private void confirmDeletePlace(TrackerDb.Place place) {
        new AlertDialog.Builder(this).setTitle("Remove " + place.name + "?")
                .setMessage("This stops future tracking at this place. Past visits are kept.")
                .setNegativeButton("Cancel", null).setPositiveButton("Remove", (d, w) -> {
                    db.removePlace(place.id);
                    Geofences.sync(this);
                    render();
                }).show();
    }

    private static int parseMinutes(String input) {
        String s = input.trim();
        if (!s.matches("\\d{1,3}:[0-5]\\d")) throw new IllegalArgumentException("Use hours:minutes, e.g. 7:24");
        String[] parts = s.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private void editSchedule() {
        LinearLayout content = column();
        EditText[] inputs = new EditText[7];
        for (int i = 0; i < 7; i++) inputs[i] = field(content, DAYS[i] + " target (h:mm)", TimeMath.hhmm(schedule.target(i)));
        EditText breaks = field(content, "Unpaid break per workday (h:mm)", TimeMath.hhmm(schedule.breakMinutes()));
        EditText threshold = field(content, "Deduct break only if elapsed time is at least (h:mm)", TimeMath.hhmm(schedule.breakThresholdMinutes()));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Working-hours schedule")
                .setView(dialogContent(content)).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                int[] targets = new int[7];
                for (int i = 0; i < 7; i++) targets[i] = parseMinutes(inputs[i].getText().toString());
                int breakMin = parseMinutes(breaks.getText().toString());
                int minThreshold = parseMinutes(threshold.getText().toString());
                if (breakMin > minThreshold) throw new IllegalArgumentException("Break cannot exceed the threshold");
                schedule.save(targets, breakMin, minThreshold);
                dialog.dismiss(); render();
            } catch (Exception error) { toast("Invalid schedule: " + error.getMessage()); }
        }));
        dialog.show();
    }

    private long parseStamp(String value) throws ParseException {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        parser.setLenient(false);
        Date d = parser.parse(value.trim());
        if (d == null) throw new ParseException("Invalid date", 0);
        return d.getTime();
    }

    private void chooseManualCheckIn() {
        List<TrackerDb.Place> places = db.places();
        String[] names = new String[places.size()];
        for (int i = 0; i < names.length; i++) names[i] = places.get(i).name;
        new AlertDialog.Builder(this).setTitle("Start visit at")
                .setItems(names, (dialog, which) -> {
                    TrackerDb.Session session = new TrackerDb.Session();
                    session.placeId = places.get(which).id;
                    session.placeName = places.get(which).name;
                    session.start = System.currentTimeMillis();
                    session.source = "manual"; session.note = "Manual check-in";
                    session.wifiResult = "not checked";
                    db.saveManualSession(session);
                    render();
                }).show();
    }

    private void manuallyFinish(TrackerDb.Session open) {
        new AlertDialog.Builder(this).setTitle("End work visit?")
                .setMessage("This records the current time as your departure. You can correct it later.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("End visit", (d, w) -> {
                    open.end = System.currentTimeMillis();
                    open.note = (open.note == null ? "" : open.note + " ") + "Manually ended";
                    db.saveManualSession(open);
                    render();
                }).show();
    }

    private void editSession(TrackerDb.Session old) {
        List<TrackerDb.Place> places = db.places();
        if (old == null && places.isEmpty()) { toast("Create a workplace first"); return; }
        LinearLayout content = column();
        EditText name = field(content, "Workplace name", old == null ? places.get(0).name : old.placeName);
        EditText entry = field(content, "Arrival, yyyy-MM-dd HH:mm", stamp.format(new Date(old == null ? System.currentTimeMillis() : old.start)));
        EditText exit = field(content, "Departure (blank if still present)", old == null || old.end == null ? "" : stamp.format(new Date(old.end)));
        EditText note = field(content, "Correction note (optional)", old == null ? "" : old.note);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(old == null ? "Add missing visit" : "Correct visit")
                .setView(dialogContent(content)).setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        if (old != null) dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Delete", (d, w) ->
                new AlertDialog.Builder(this).setTitle("Delete this visit?")
                        .setMessage("This removes the visit permanently from the local timesheet.")
                        .setNegativeButton("Cancel", null).setPositiveButton("Delete", (d2, w2) -> { db.deleteSession(old.id); render(); }).show());
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                long start = parseStamp(entry.getText().toString());
                String endString = exit.getText().toString().trim();
                Long end = endString.isEmpty() ? null : parseStamp(endString);
                if (end != null && end <= start) throw new IllegalArgumentException("Departure must be after arrival");
                if (name.getText().toString().trim().isEmpty()) throw new IllegalArgumentException("Workplace name required");
                TrackerDb.Session s = old == null ? new TrackerDb.Session() : old;
                s.start = start; s.end = end;
                s.placeName = name.getText().toString().trim();
                if (old == null) { s.placeId = places.get(0).id; s.wifiResult = "not checked"; }
                s.note = note.getText().toString();
                db.saveManualSession(s);
                dialog.dismiss(); render();
            } catch (Exception error) { toast("Invalid visit: " + error.getMessage()); }
        }));
        dialog.show();
    }

    private void requestTrackingPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
            return;
        }
        if (Build.VERSION.SDK_INT >= 29 && checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT == 29) requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 102);
            else new AlertDialog.Builder(this).setTitle("Allow location all the time")
                    .setMessage("On the next screen, open Permissions → Location and choose 'Allow all the time'. Then return to WorkTrail.")
                    .setNegativeButton("Later", null)
                    .setPositiveButton("Open app settings", (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    }).show();
            return;
        }
        initialSyncDone = true;
        Geofences.sync(this);
        toast("Automatic geofence tracking enabled");
        render();
    }

    private void requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 103);
        else toast("Notifications already allowed or not required");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        boolean granted = results.length > 0;
        for (int result : results) if (result != PackageManager.PERMISSION_GRANTED) granted = false;
        if ((requestCode == 101 || requestCode == 102) && granted) requestTrackingPermissions();
        else if ((requestCode == 101 || requestCode == 102) && !granted)
            toast("Permission not granted; automatic tracking remains off");
        render();
    }

    private void beginExport(String type) {
        exportType = type;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/csv");
        i.putExtra(Intent.EXTRA_TITLE, type.equals("visits") ? "worktrail-visits.csv" : "worktrail-daily.csv");
        startActivityForResult(i, 201);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 201 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try (OutputStream out = getContentResolver().openOutputStream(data.getData());
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write('\ufeff'); // Helps Excel recognize UTF-8.
            if (exportType.equals("visits")) exportVisits(writer); else exportDaily(writer);
            writer.flush();
            toast("CSV saved");
        } catch (Exception error) { toast("Export failed: " + error.getMessage()); }
    }

    private static String csv(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }

    private void exportVisits(OutputStreamWriter writer) throws Exception {
        writer.write("Place,Arrival local,Departure local,Arrival epoch ms,Departure epoch ms,Timezone at export,Elapsed minutes,Source,Wi-Fi confirmation,Correction note\n");
        for (TrackerDb.Session s : db.sessions(100000)) {
            writer.write(csv(s.placeName) + "," + csv(stamp.format(new Date(s.start))) + "," +
                    csv(s.end == null ? "" : stamp.format(new Date(s.end))) + "," + s.start + "," +
                    (s.end == null ? "" : s.end) + "," + csv(java.util.TimeZone.getDefault().getID()) + "," +
                    (s.end == null ? "" : TimeMath.minutes(s.end - s.start)) + "," +
                    csv(s.source) + "," + csv(s.wifiResult) + "," + csv(s.note) + "\n");
        }
    }

    private void exportDaily(OutputStreamWriter writer) throws Exception {
        writer.write("Date,Elapsed minutes,Credited minutes,Expected minutes,Balance minutes,Schedule configured\n");
        Calendar day = Calendar.getInstance();
        midnight(day);
        day.add(Calendar.DAY_OF_MONTH, -364);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 365; i++) {
            Calendar end = (Calendar) day.clone();
            end.add(Calendar.DAY_OF_MONTH, 1);
            int expected = schedule.targetForDay(day);
            TrackerDb.Daily d = db.daily(day.getTimeInMillis(), end.getTimeInMillis(), now, expected, schedule);
            writer.write(isoDate.format(new Date(day.getTimeInMillis())) + "," + TimeMath.minutes(d.elapsedMillis) + "," +
                    d.creditedMinutes + "," + (schedule.configured() ? expected : "") + "," +
                    (schedule.configured() ? d.creditedMinutes - expected : "") + "," + schedule.configured() + "\n");
            day = end;
        }
    }

    private static void midnight(Calendar day) {
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
