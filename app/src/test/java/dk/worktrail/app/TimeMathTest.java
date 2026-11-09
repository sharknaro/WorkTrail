package dk.worktrail.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimeMathTest {
    @Test public void dailyBalanceExample() {
        // 08:55–17:10 = 8h15, minus 30m break = 7h45.
        long elapsed = (8 * 60 + 15) * 60000L;
        assertEquals(465, TimeMath.creditedMinutes(elapsed, 30, 360));
        assertEquals("+0:21", TimeMath.signed(465 - 444));
    }

    @Test public void shortVisitsDoNotIncurBreak() {
        assertEquals(90, TimeMath.creditedMinutes(90 * 60000L, 30, 360));
    }

    @Test public void visitCanSpanMidnight() {
        long hour = 3600000L;
        assertEquals(2 * hour, TimeMath.overlap(22 * hour, 26 * hour, 24 * hour, 48 * hour));
    }

    @Test public void mondayFirstIndex() {
        assertEquals(0, TimeMath.mondayIndex(java.util.Calendar.MONDAY));
        assertEquals(6, TimeMath.mondayIndex(java.util.Calendar.SUNDAY));
    }

    @Test public void negativeBalance() {
        assertEquals("−0:14", TimeMath.signed(-14));
    }

    @Test public void homeHoursAreAddedWithoutDoubleBreak() {
        assertEquals(465 + 120, TimeMath.totalCreditedMinutes(495 * 60000L, 30, 360, 120));
        assertEquals(180, TimeMath.totalCreditedMinutes(0, 30, 360, 180));
    }
}
