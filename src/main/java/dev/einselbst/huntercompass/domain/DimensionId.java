package dev.einselbst.huntercompass.domain;

import java.util.Locale;

public enum DimensionId {
    OVERWORLD,
    NETHER,
    END,
    OTHER;

    public static DimensionId fromKey(String key) {
        if (key == null) {
            return OTHER;
        }
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "minecraft:overworld", "overworld" -> OVERWORLD;
            case "minecraft:the_nether", "the_nether", "nether" -> NETHER;
            case "minecraft:the_end", "the_end", "end" -> END;
            default -> OTHER;
        };
    }
}
