package dev.einselbst.huntercompass.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DimensionNamesTest {
    private final DimensionNames names = new DimensionNames("World", "Hell", "Sky", "Unknown");

    @Test
    void mapsVanillaDimensionKeys() {
        assertEquals(DimensionId.OVERWORLD, DimensionId.fromKey("minecraft:overworld"));
        assertEquals(DimensionId.NETHER, DimensionId.fromKey("minecraft:the_nether"));
        assertEquals(DimensionId.END, DimensionId.fromKey("minecraft:the_end"));
    }

    @Test
    void usesConfiguredNamesAndOtherFallback() {
        assertEquals("World", names.displayName(DimensionId.OVERWORLD));
        assertEquals("Unknown", names.displayName(DimensionId.OTHER));
        assertEquals(DimensionId.OTHER, DimensionId.fromKey("custom:moon"));
    }
}
