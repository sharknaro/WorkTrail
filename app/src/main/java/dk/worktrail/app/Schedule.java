package dk.worktrail.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

/** Seven editable day targets; no assumed contract hours until the user saves a schedule. */
public final class Schedule {
    private final SharedPreferences prefs;

    public Schedule(Context context) {
        prefs = context.getSharedPreferences("schedule", Context.MODE_PRIVATE);
    }

    public boolean configured() { return prefs.getBoolean("configured", false); }
    public int target(int mondayIndex) { return prefs.getInt("day_" + mondayIndex, 0); }
    public int breakMinutes() { return prefs.getInt("break", 30); }
    public int breakThresholdMinutes() { return prefs.getInt("threshold", 360); }
    public int weeklyTarget() {
        int sum = 0;
        for (int i = 0; i < 7; i++) sum += target(i);
        return sum;
    }
    public void save(int[] targets, int breakMinutes, int thresholdMinutes) {
        SharedPreferences.Editor e = prefs.edit();
        for (int i = 0; i < 7; i++) e.putInt("day_" + i, targets[i]);
        e.putInt("break", breakMinutes);
        e.putInt("threshold", thresholdMinutes);
        e.putBoolean("configured", true);
        e.apply();
    }
    public int targetForDay(Calendar date) {
        return target(TimeMath.mondayIndex(date.get(Calendar.DAY_OF_WEEK)));
    }
}
