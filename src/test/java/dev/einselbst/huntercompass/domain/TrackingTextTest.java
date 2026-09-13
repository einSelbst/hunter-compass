package dev.einselbst.huntercompass.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackingTextTest {
    @Test
    void rendersAllTrackingPlaceholders() {
        String result = TrackingText.format(
                "{target}: {x}/{y}/{z} {dimension} ({direction})",
                "Luis",
                new BlockPosition(-12, 66, 104),
                "Nether",
                "scaled portal direction"
        );

        assertEquals("Luis: -12/66/104 Nether (scaled portal direction)", result);
    }

    @Test
    void rendersOfflineStateWithoutCoordinates() {
        assertEquals(
                "Luis: target offline",
                TrackingText.formatOffline("{target}: {offline}", "Luis", "target offline")
        );
    }
}
