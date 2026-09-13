package dev.einselbst.huntercompass.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HuntClockTest {
    private static final long START = 1_700_000_000_000L;
    private static final long DEADLINE = HuntClock.deadline(START, 7);

    @Test
    void createsASevenRealDayDeadline() {
        assertEquals(Duration.ofDays(7).toMillis(), DEADLINE - START);
    }

    @Test
    void advancesDisplayDayAtRealDayBoundaries() {
        assertEquals(1, HuntClock.displayDay(START, DEADLINE, START));
        assertEquals(1, HuntClock.displayDay(START, DEADLINE, START + Duration.ofDays(1).toMillis() - 1));
        assertEquals(2, HuntClock.displayDay(START, DEADLINE, START + Duration.ofDays(1).toMillis()));
        assertEquals(7, HuntClock.displayDay(START, DEADLINE, DEADLINE - 1));
    }

    @Test
    void expiresAtThePersistedDeadline() {
        assertFalse(HuntClock.isExpired(DEADLINE, DEADLINE - 1));
        assertTrue(HuntClock.isExpired(DEADLINE, DEADLINE));
        assertTrue(HuntClock.isExpired(DEADLINE, DEADLINE + Duration.ofHours(3).toMillis()));
    }

    @Test
    void clampsBossBarProgress() {
        assertEquals(1.0f, HuntClock.remainingFraction(START, DEADLINE, START - 1));
        assertEquals(0.5f, HuntClock.remainingFraction(START, DEADLINE, START + Duration.ofDays(3).plusHours(12).toMillis()));
        assertEquals(0.0f, HuntClock.remainingFraction(START, DEADLINE, DEADLINE + 1));
    }
}
