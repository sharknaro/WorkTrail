package dk.worktrail.app;

/** Dependency-free math tests for environments without an Android SDK or JUnit. */
public final class SmokeTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        int credited = TimeMath.creditedMinutes((8 * 60 + 15) * 60000L, 30, 360);
        check(credited == 465, "8h15 minus 30m should be 7h45");
        check(credited - 444 == 21, "7h45 against 7h24 should give +21m");
        check(TimeMath.creditedMinutes(90 * 60000L, 30, 360) == 90, "No break for short visit");
        check(TimeMath.overlap(22 * 3600000L, 26 * 3600000L, 24 * 3600000L, 48 * 3600000L) == 2 * 3600000L,
                "Split overnight visits at midnight");
        check(TimeMath.mondayIndex(java.util.Calendar.SUNDAY) == 6, "Sunday belongs last");
        check("−0:14".equals(TimeMath.signed(-14)), "Signed negative balance");
        check(TimeMath.totalCreditedMinutes(495 * 60000L, 30, 360, 120) == 585,
                "Two hours of home work added without a second break");
        check(TimeMath.totalCreditedMinutes(0, 30, 360, 180) == 180,
                "Home-only day credits three hours exactly");
        System.out.println("PASS: WorkTrail business logic smoke tests");
    }
}
