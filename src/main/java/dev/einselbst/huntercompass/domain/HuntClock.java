package dev.einselbst.huntercompass.domain;

import java.time.Duration;

public final class HuntClock {
    public static final long MILLIS_PER_DAY = Duration.ofDays(1).toMillis();

    private HuntClock() {
    }

    public static long deadline(long startedAtMillis, int durationDays) {
        return Math.addExact(startedAtMillis, Duration.ofDays(sanitizedDurationDays(durationDays)).toMillis());
    }

    public static boolean isExpired(long deadlineMillis, long nowMillis) {
        return nowMillis >= deadlineMillis;
    }

    public static int displayDay(long startedAtMillis, long deadlineMillis, long nowMillis) {
        int totalDays = totalDays(startedAtMillis, deadlineMillis);
        long elapsedMillis = Math.max(0L, nowMillis - startedAtMillis);
        long elapsedDays = elapsedMillis / MILLIS_PER_DAY;
        return (int) Math.min(totalDays, elapsedDays + 1L);
    }

    public static int totalDays(long startedAtMillis, long deadlineMillis) {
        long durationMillis = Math.max(1L, deadlineMillis - startedAtMillis);
        return (int) Math.max(1L, Math.ceilDiv(durationMillis, MILLIS_PER_DAY));
    }

    public static float remainingFraction(long startedAtMillis, long deadlineMillis, long nowMillis) {
        long durationMillis = Math.max(1L, deadlineMillis - startedAtMillis);
        long remainingMillis = Math.clamp(deadlineMillis - nowMillis, 0L, durationMillis);
        return (float) ((double) remainingMillis / durationMillis);
    }

    private static int sanitizedDurationDays(int durationDays) {
        return Math.max(1, durationDays);
    }
}
