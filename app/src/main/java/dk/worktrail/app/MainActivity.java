package dk.worktrail.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.widget.CheckBox;
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
    private int BG, PANEL, MUTED, WHITE, ACCENT, NEGATIVE, BORDER, NAV, FIELD;
    private boolean dark;
    private static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private final SimpleDateFormat date = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final SimpleDateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private TrackerDb db;
    private Schedule schedule;
    private LinearLayout root, page;
    private String tab = "Today", exportType = "visits";
    private boolean initialSyncDone = false;
    private Calendar historyDay = Calendar.getInstance();
    private Calendar displayedMonth = Calendar.getInstance();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
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

    private void applyPalette() {
        dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        BG = dark ? 0xFF0B1524 : 0xFFF3F6FB;
        PANEL = dark ? 0xFF18263B : 0xFFFFFFFF;
        MUTED = dark ? 0xFFADBCD0 : 0xFF607188;
        WHITE = dark ? 0xFFF3F7FC : 0xFF172438;
        ACCENT = dark ? 0xFF68E0BC : 0xFF087D70;
        NEGATIVE = dark ? 0xFFFF95A2 : 0xFFBE4054;
        BORDER = dark ? 0xFF35485F : 0xFFDAE4EC;
        NAV = dark ? 0xFF101D30 : 0xFFFFFFFF;
        FIELD = dark ? 0xFF25364A : 0xFFEDF2F6;
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(NAV);
        int flags = dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (!dark && Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

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
        GradientDrawable bg = shape(PANEL, 18);
        bg.setStroke(px(1), BORDER);
        c.setBackground(bg);
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
        b.setTextColor(HeatmapColors.readableText(ACCENT));
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(shape(ACCENT, 12));
        b.setOnClickListener(v -> click.run());
        return b;
    }

    private TextView quietButton(String label, Runnable click) {
        TextView b = text(label, 13, ACCENT, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(px(12), px(9), px(12), px(9));
        GradientDrawable surface = shape(FIELD, 10);
        surface.setStroke(px(1), BORDER);
        b.setBackground(surface);
        b.setOnClickListener(v -> click.run());
        return b;
    }

    private void addButton(LinearLayout parent, String label, Runnable click) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, px(47));
        params.topMargin = px(12);
        parent.addView(button(label, click), params);
    }

    private void render() {
        applyPalette();
        root = column();
        root.setBackgroundColor(BG);
        setContentView(root);
        LinearLayout heading = column();
        heading.setPadding(px(22), px(21), px(22), px(18));
        line(heading, "●  WORKTRAIL    /    YOUR TIME, IN FOCUS", 11, ACCENT, true);
        smallGap(heading);
        line(heading, tab.equals("History") ? "Your activity" : tab.equals("Today") ? "Good to see you" : tab,
                29, WHITE, true);
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
        nav.setBackgroundColor(NAV);
        for (String item : new String[]{"Today", "History", "Places", "Schedule"}) {
            String symbol = item.equals("Today") ? "◉" : item.equals("History") ? "▦" : item.equals("Places") ? "⌖" : "⚙";
            TextView n = text(symbol + "  " + item, 13, tab.equals(item) ? ACCENT : MUTED, tab.equals(item));
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
        GradientDrawable hero = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{dark ? 0xFF193D63 : 0xFF17446A, dark ? 0xFF13564B : 0xFF087B69});
        hero.setCornerRadius(px(18));
        top.setBackground(hero);
        line(top, open == null ? "●  LOCATION STATUS" : "●  LIVE WORK SESSION", 12, 0xFFC3EFE5, true);
        smallGap(top);
        line(top, date.format(new Date(now)), 13, 0xFFD5E5ED, false);
        smallGap(top);
        line(top, open == null ? "Away from work" : "At " + open.placeName, 23, 0xFFFFFFFF, true);
        if (open != null) {
            smallGap(top);
            line(top, "Entry: " + stamp.format(new Date(open.start)) + "  ·  " + open.source, 13, 0xFFD5E5ED, false);
            addButton(top, "End this visit manually", () -> manuallyFinish(open));
        } else if (!db.places().isEmpty()) {
            addButton(top, "Start a visit manually", this::chooseManualCheckIn);
        }
        addButton(top, "+ Add work from home / manual hours", () -> editManualWork(null));

        LinearLayout worked = card();
        line(worked, "CREDITED WORK TODAY", 12, MUTED, true);
        smallGap(worked);
        line(worked, TimeMath.hhmm(current.creditedMinutes) + " h", 36, WHITE, true);
        line(worked, "On-site elapsed: " + TimeMath.hhmm(TimeMath.minutes(current.elapsedMillis)) + " h", 13, MUTED, false);
        line(worked, "On-site credited: " + TimeMath.hhmm(current.onsiteCreditedMinutes) + " h  ·  Manually added: " + TimeMath.hhmm(current.manualMinutes) + " h", 13, MUTED, false);
        if (schedule.configured()) {
            smallGap(worked);
            int balance = current.creditedMinutes - current.targetMinutes;
            line(worked, "Target " + TimeMath.hhmm(current.targetMinutes) + " h    Balance " + TimeMath.signed(balance) + " h", 14, balance >= 0 ? ACCENT : NEGATIVE, true);
        } else {
            addButton(worked, "Set your custom weekly schedule", () -> { tab = "Schedule"; render(); editSchedule(); });
        }

        renderMonthHeatmap();
        renderDayDetails(start, tomorrow, now);

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
        line(week, "THIS WEEK  ·  MON–SUN", 12, MUTED, true);
        smallGap(week);
        Calendar monday = TimeMath.monday(now);
        int totalCredited = 0, targetToDate = 0;
        for (int i = 0; i < 7; i++) {
            Calendar end = (Calendar) monday.clone();
            end.add(Calendar.DAY_OF_MONTH, 1);
            TrackerDb.Daily d = db.daily(monday.getTimeInMillis(), end.getTimeInMillis(), now, schedule.target(i), schedule);
            if (monday.getTimeInMillis() <= now) {
                totalCredited += d.creditedMinutes;
                targetToDate += d.targetMinutes;
            }
            monday = end;
        }
        line(week, TimeMath.hhmm(totalCredited) + " h worked", 24, WHITE, true);
        if (schedule.configured())
            line(week, TimeMath.signed(totalCredited - targetToDate) + " h balance so far  ·  " +
                    TimeMath.hhmm(schedule.weeklyTarget()) + " h weekly target", 13,
                    totalCredited >= targetToDate ? ACCENT : NEGATIVE, true);
        line(week, "Automatic timestamps are estimates; review records when precision matters.", 12, MUTED, false);
    }

    private void renderHistory() {
        renderMonthHeatmap();

        Calendar selected = (Calendar) historyDay.clone();
        midnight(selected);
        Calendar next = (Calendar) selected.clone();
        next.add(Calendar.DAY_OF_MONTH, 1);
        LinearLayout picker = card();
        line(picker, "SELECTED DAY", 12, MUTED, true);
        smallGap(picker);
        line(picker, date.format(selected.getTime()), 21, WHITE, true);
        TrackerDb.Daily selectedStats = db.daily(selected.getTimeInMillis(), next.getTimeInMillis(), System.currentTimeMillis(), schedule.targetForDay(selected), schedule);
        smallGap(picker);
        line(picker, TimeMath.hhmm(selectedStats.creditedMinutes) + " h credited" +
                (schedule.configured() ? "   ·   " + TimeMath.signed(selectedStats.creditedMinutes - selectedStats.targetMinutes) + " h balance" : ""),
                15, selectedStats.creditedMinutes >= selectedStats.targetMinutes ? ACCENT : NEGATIVE, true);
        LinearLayout controls = row();
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0, px(42), 1f);
        actionParams.setMargins(0, px(12), px(8), 0);
        controls.addView(quietButton("Choose date", () -> {
            Calendar h = historyDay;
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                historyDay.set(year, month, dayOfMonth);
                displayedMonth.set(year, month, 1);
                render();
            }, h.get(Calendar.YEAR), h.get(Calendar.MONTH), h.get(Calendar.DAY_OF_MONTH)).show();
        }), actionParams);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(0, px(42), 1f);
        addParams.topMargin = px(12);
        controls.addView(quietButton("+ Log hours", () -> editManualWork(null)), addParams);
        picker.addView(controls);

        renderDayDetails(selected, next, System.currentTimeMillis());

        LinearLayout actions = card();
        line(actions, "Need to correct a missing check-in? Add a visit, then tap any day to review it.", 13, MUTED, false);
        addButton(actions, "+ Add a missing visit", () -> editSession(null));
        addButton(actions, "Export timesheets (.csv)", () -> new AlertDialog.Builder(this)
                .setTitle("Export timesheets")
                .setItems(new String[]{"Check-in / check-out visits", "Work from home / manual hours", "Last 365 days: totals and balances"},
                        (dialog, which) -> beginExport(new String[]{"visits", "manual", "daily"}[which]))
                .show());
        line(actions, "Your records stay on this phone until you choose to export them.", 12, MUTED, false);
    }

    private void renderMonthHeatmap() {
        LinearLayout board = card();
        line(board, "YOUR MONTH AT A GLANCE", 12, MUTED, true);
        smallGap(board);
        Calendar first = (Calendar) displayedMonth.clone();
        midnight(first);
        first.set(Calendar.DAY_OF_MONTH, 1);
        String label = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(first.getTime());
        LinearLayout header = row();
        header.addView(quietButton("‹", () -> {
            displayedMonth.add(Calendar.MONTH, -1);
            render();
        }), new LinearLayout.LayoutParams(px(42), px(40)));
        TextView month = text(label, 21, WHITE, true);
        month.setGravity(Gravity.CENTER);
        header.addView(month, new LinearLayout.LayoutParams(0, px(40), 1));
        header.addView(quietButton("›", () -> {
            displayedMonth.add(Calendar.MONTH, 1);
            render();
        }), new LinearLayout.LayoutParams(px(42), px(40)));
        board.addView(header);
        smallGap(board);

        LinearLayout weekdays = row();
        for (String weekday : new String[]{"M", "T", "W", "T", "F", "S", "S"}) {
            TextView title = text(weekday, 11, MUTED, true);
            title.setGravity(Gravity.CENTER);
            weekdays.addView(title, new LinearLayout.LayoutParams(0, px(25), 1));
        }
        board.addView(weekdays);

        int leading = TimeMath.mondayIndex(first.get(Calendar.DAY_OF_WEEK));
        int dayCount = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int rows = (leading + dayCount + 6) / 7;
        long now = System.currentTimeMillis();
        int monthCredited = 0, monthExpected = 0;
        for (int week = 0; week < rows; week++) {
            LinearLayout gridRow = row();
            for (int weekday = 0; weekday < 7; weekday++) {
                int dayNumber = week * 7 + weekday - leading + 1;
                LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, px(53), 1);
                cell.setMargins(px(2), px(2), px(2), px(2));
                if (dayNumber < 1 || dayNumber > dayCount) {
                    gridRow.addView(new View(this), cell);
                    continue;
                }
                Calendar day = (Calendar) first.clone();
                day.set(Calendar.DAY_OF_MONTH, dayNumber);
                Calendar end = (Calendar) day.clone();
                end.add(Calendar.DAY_OF_MONTH, 1);
                boolean future = day.getTimeInMillis() > now;
                TrackerDb.Daily stats = db.daily(day.getTimeInMillis(), end.getTimeInMillis(),
                        now, schedule.targetForDay(day), schedule);
                if (!future) {
                    monthCredited += stats.creditedMinutes;
                    monthExpected += stats.targetMinutes;
                }
                int tileColor = HeatmapColors.tileColor(stats.creditedMinutes, stats.targetMinutes,
                        schedule.configured(), future, dark);
                String hours = (future || (stats.creditedMinutes == 0 && stats.targetMinutes == 0))
                        ? "—" : TimeMath.hhmm(stats.creditedMinutes);
                TextView tile = text(dayNumber + "\n" + hours, 11, HeatmapColors.readableText(tileColor), true);
                tile.setGravity(Gravity.CENTER);
                tile.setLineSpacing(px(2), 1f);
                GradientDrawable surface = shape(tileColor, 9);
                Calendar selected = tab.equals("History") ? historyDay : Calendar.getInstance();
                boolean selectedDay = day.get(Calendar.YEAR) == selected.get(Calendar.YEAR)
                        && day.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR);
                surface.setStroke(px(selectedDay ? 2 : 1), selectedDay ? ACCENT : BORDER);
                tile.setBackground(surface);
                tile.setContentDescription(date.format(day.getTime()) + ", " +
                        (future ? "future" : TimeMath.hhmm(stats.creditedMinutes) + " hours credited") +
                        ". Tap for details.");
                tile.setOnClickListener(v -> showDayPopup(day));
                gridRow.addView(tile, cell);
            }
            board.addView(gridRow);
        }
        smallGap(board);
        if (schedule.configured()) {
            line(board, TimeMath.hhmm(monthCredited) + " h logged  ·  " +
                    TimeMath.signed(monthCredited - monthExpected) + " h vs target", 15, WHITE, true);
            smallGap(board);
        } else {
            line(board, "Set your weekly targets to enable blue / green / red comparisons.", 13, MUTED, false);
            addButton(board, "Configure targets", () -> { tab = "Schedule"; render(); editSchedule(); });
        }
        LinearLayout legend = row();
        legend.addView(legendItem(HeatmapColors.BLUE, "Below"), new LinearLayout.LayoutParams(0, px(28), 1));
        legend.addView(legendItem(HeatmapColors.GREEN, "On target"), new LinearLayout.LayoutParams(0, px(28), 1));
        legend.addView(legendItem(HeatmapColors.RED, "Above"), new LinearLayout.LayoutParams(0, px(28), 1));
        board.addView(legend);
        line(board, "Blue: less work  ·  Green: within 15 minutes  ·  Red: extra hours. Tap a square for details.", 12, MUTED, false);
    }

    private LinearLayout legendItem(int color, String label) {
        LinearLayout item = row();
        View swatch = new View(this);
        swatch.setBackground(shape(color, 4));
        LinearLayout.LayoutParams icon = new LinearLayout.LayoutParams(px(12), px(12));
        icon.rightMargin = px(5);
        item.addView(swatch, icon);
        item.addView(text(label, 11, MUTED, false));
        return item;
    }

    private void showDayPopup(Calendar selected) {
        Calendar day = (Calendar) selected.clone();
        midnight(day);
        Calendar end = (Calendar) day.clone();
        end.add(Calendar.DAY_OF_MONTH, 1);
        long now = System.currentTimeMillis();
        TrackerDb.Daily stats = db.daily(day.getTimeInMillis(), end.getTimeInMillis(),
                now, schedule.targetForDay(day), schedule);
        LinearLayout content = column();
        line(content, "CREDITED WORK", 12, MUTED, true);
        smallGap(content);
        line(content, TimeMath.hhmm(stats.creditedMinutes) + " h", 30, WHITE, true);
        line(content, "On-site: " + TimeMath.hhmm(stats.onsiteCreditedMinutes) + " h  ·  Manual: " +
                TimeMath.hhmm(stats.manualMinutes) + " h", 13, MUTED, false);
        if (schedule.configured()) {
            smallGap(content);
            line(content, "Target " + TimeMath.hhmm(stats.targetMinutes) + " h   ·   Balance " +
                    TimeMath.signed(stats.creditedMinutes - stats.targetMinutes) + " h", 14,
                    stats.creditedMinutes >= stats.targetMinutes ? ACCENT : NEGATIVE, true);
        }
        smallGap(content);
        List<TrackerDb.Session> visits = db.sessionsForDay(day.getTimeInMillis(), end.getTimeInMillis());
        if (visits.isEmpty()) line(content, "No workplace check-ins recorded.", 13, MUTED, false);
        for (TrackerDb.Session visit : visits) {
            line(content, visit.placeName, 15, WHITE, true);
            line(content, "In  " + stamp.format(new Date(visit.start)), 13, MUTED, false);
            line(content, "Out  " + (visit.end == null ? "Still checked in" : stamp.format(new Date(visit.end))), 13, MUTED, false);
            if (visit.note != null && !visit.note.isEmpty()) line(content, visit.note, 12, MUTED, false);
            smallGap(content);
        }
        for (TrackerDb.ManualWork manual : db.manualWorkForDay(isoDate.format(day.getTime()))) {
            line(content, ("home".equals(manual.type) ? "Work from home" : "Other manual work") +
                    "  ·  " + TimeMath.hhmm(manual.minutes) + " h", 14, ACCENT, true);
            if (!manual.note.isEmpty()) line(content, manual.note, 13, MUTED, false);
            smallGap(content);
        }
        new AlertDialog.Builder(this)
                .setTitle(date.format(day.getTime()))
                .setView(dialogContent(content))
                .setNegativeButton("Close", null)
                .setPositiveButton("View / edit day", (dialog, which) -> {
                    historyDay = (Calendar) day.clone();
                    displayedMonth = (Calendar) day.clone();
                    tab = "History";
                    render();
                }).show();
    }

    private void renderDayDetails(Calendar start, Calendar end, long now) {
        long from = start.getTimeInMillis(), until = end.getTimeInMillis();
        LinearLayout details = card();
        line(details, "CHECK-IN / CHECK-OUT", 13, MUTED, true);
        smallGap(details);
        List<TrackerDb.Session> visits = db.sessionsForDay(from, until);
        if (visits.isEmpty()) line(details, "No recorded workplace visits for this day.", 13, MUTED, false);
        for (TrackerDb.Session s : visits) {
            line(details, s.placeName, 16, WHITE, true);
            line(details, "Check-in: " + stamp.format(new Date(s.start)), 14, MUTED, false);
            line(details, "Check-out: " + (s.end == null ? "Not yet recorded" : stamp.format(new Date(s.end))), 14, MUTED, false);
            long sessionEnd = s.end == null ? now : s.end;
            long dayElapsed = TimeMath.overlap(s.start, sessionEnd, from, Math.min(until, now));
            line(details, "Time on this day: " + TimeMath.hhmm(TimeMath.minutes(dayElapsed)) + " h", 14, ACCENT, true);
            if (s.note != null && !s.note.isEmpty()) line(details, "Note: " + s.note, 12, MUTED, false);
            addButton(details, "Correct these times", () -> editSession(s));
            smallGap(details);
        }
        String dayKey = isoDate.format(start.getTime());
        List<TrackerDb.ManualWork> manual = db.manualWorkForDay(dayKey);
        if (!manual.isEmpty()) {
            line(details, "MANUALLY ENTERED WORK", 13, MUTED, true);
            smallGap(details);
        }
        for (TrackerDb.ManualWork m : manual) {
            line(details, ("home".equals(m.type) ? "Work from home" : "Other work") + "  ·  " + TimeMath.hhmm(m.minutes) + " h", 15, ACCENT, true);
            line(details, "Duration only — no check-in/out timestamps", 12, MUTED, false);
            if (!m.note.isEmpty()) line(details, "Comment: " + m.note, 13, WHITE, false);
            addButton(details, "Edit manual hours", () -> editManualWork(m));
            smallGap(details);
        }
        if (tab.equals("Today")) addButton(details, "+ Add work from home hours", () -> editManualWork(null));
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
        GradientDrawable input = shape(FIELD, 8);
        input.setStroke(px(1), BORDER);
        e.setBackground(input);
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

    private void editManualWork(TrackerDb.ManualWork old) {
        LinearLayout content = column();
        String defaultDay = tab.equals("History") ? isoDate.format(historyDay.getTime()) : isoDate.format(new Date());
        EditText day = field(content, "Date (yyyy-MM-dd)", old == null ? defaultDay : old.day);
        EditText duration = field(content, "Hours actually worked (h:mm, excluding breaks)", old == null ? "0:00" : TimeMath.hhmm(old.minutes));
        CheckBox home = new CheckBox(this);
        home.setText("Mark as work from home");
        home.setTextColor(WHITE);
        home.setChecked(old == null || "home".equals(old.type));
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(-1, -2);
        checkParams.topMargin = px(12);
        content.addView(home, checkParams);
        EditText note = field(content, "Comment / activity (optional)", old == null ? "" : old.note);
        line(content, "These hours are counted directly. No fictitious check-in or check-out time is generated, and no additional break is deducted.", 12, MUTED, false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(old == null ? "Add manual working hours" : "Edit manual working hours")
                .setView(dialogContent(content))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        if (old != null) dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Delete", (d, w) ->
                new AlertDialog.Builder(this).setTitle("Delete these manual hours?")
                        .setNegativeButton("Cancel", null).setPositiveButton("Delete", (d2, w2) -> {
                            db.deleteManualWork(old.id);
                            render();
                        }).show());
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String normalizedDate = isoDate.format(new Date(parseStamp(day.getText().toString().trim() + " 12:00")));
                int worked = parseMinutes(duration.getText().toString());
                if (worked <= 0 || worked > 24 * 60) throw new IllegalArgumentException("Enter between 0:01 and 24:00 hours");
                TrackerDb.ManualWork m = old == null ? new TrackerDb.ManualWork() : old;
                m.day = normalizedDate;
                m.minutes = worked;
                m.type = home.isChecked() ? "home" : "other";
                m.note = note.getText().toString().trim();
                db.saveManualWork(m);
                dialog.dismiss();
                render();
            } catch (Exception error) { toast("Invalid manual entry: " + error.getMessage()); }
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
        i.putExtra(Intent.EXTRA_TITLE, type.equals("visits") ? "worktrail-visits.csv" :
                (type.equals("manual") ? "worktrail-manual-hours.csv" : "worktrail-daily.csv"));
        startActivityForResult(i, 201);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 201 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try (OutputStream out = getContentResolver().openOutputStream(data.getData());
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write('\ufeff'); // Helps Excel recognize UTF-8.
            if (exportType.equals("visits")) exportVisits(writer);
            else if (exportType.equals("manual")) exportManual(writer);
            else exportDaily(writer);
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

    private void exportManual(OutputStreamWriter writer) throws Exception {
        writer.write("Date,Category,Credited minutes,Comment\n");
        for (TrackerDb.ManualWork m : db.allManualWork())
            writer.write(csv(m.day) + "," + csv("home".equals(m.type) ? "Work from home" : "Other manual work") +
                    "," + m.minutes + "," + csv(m.note) + "\n");
    }

    private void exportDaily(OutputStreamWriter writer) throws Exception {
        writer.write("Date,On-site elapsed minutes,On-site credited minutes,Manual credited minutes,Total credited minutes,Expected minutes,Balance minutes,Schedule configured\n");
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
                    d.onsiteCreditedMinutes + "," + d.manualMinutes + "," + d.creditedMinutes + "," +
                    (schedule.configured() ? expected : "") + "," +
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
