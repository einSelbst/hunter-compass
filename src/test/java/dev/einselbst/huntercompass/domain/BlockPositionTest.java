package dev.einselbst.huntercompass.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockPositionTest {
    @Test
    void floorsPositiveAndNegativeCoordinatesLikeMinecraftBlocks() {
        assertEquals(new BlockPosition(12, 64, -1), BlockPosition.from(12.99, 64.0, -0.01));
    }
}
