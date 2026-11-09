package dk.worktrail.app;

import java.util.Calendar;

/** Pure business logic. Times are epoch milliseconds; duration uses elapsed time, not wall clock. */
public final class TimeMath {
    private TimeMath() {}

    public static int minutes(long elapsedMillis) {
        return (int) Math.max(0L, Math.round(elapsedMillis / 60000.0));
    }

    public static long overlap(long in, long out, long dayStart, long dayEnd) {
        return Math.max(0L, Math.min(out, dayEnd) - Math.max(in, dayStart));
    }

    public static int creditedMinutes(long elapsedMs, int breakMinutes, int thresholdMinutes) {
        int elapsed = minutes(elapsedMs);
        return Math.max(0, elapsed - (elapsed >= thresholdMinutes ? breakMinutes : 0));
    }

    /** Manual duration is already net work and never receives a second break deduction. */
    public static int totalCreditedMinutes(long onsiteElapsedMs, int breakMinutes, int thresholdMinutes, int manualNetMinutes) {
        return creditedMinutes(onsiteElapsedMs, breakMinutes, thresholdMinutes) + Math.max(0, manualNetMinutes);
    }

    public static int mondayIndex(int calendarDayOfWeek) {
        return (calendarDayOfWeek + 5) % 7;
    }

    public static String hhmm(int totalMinutes) {
        int absolute = Math.abs(totalMinutes);
        return String.format(java.util.Locale.US, "%d:%02d", absolute / 60, absolute % 60);
    }

    public static String signed(int differenceMinutes) {
        return (differenceMinutes >= 0 ? "+" : "−") + hhmm(differenceMinutes);
    }

    public static Calendar monday(long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        int back = mondayIndex(c.get(Calendar.DAY_OF_WEEK));
        c.add(Calendar.DAY_OF_MONTH, -back);
        return c;
    }
}
