package dev.einselbst.huntercompass.domain;

import java.util.Map;

public final class DimensionNames {
    private final Map<DimensionId, String> names;

    public DimensionNames(String overworld, String nether, String end, String other) {
        this.names = Map.of(
                DimensionId.OVERWORLD, overworld,
                DimensionId.NETHER, nether,
                DimensionId.END, end,
                DimensionId.OTHER, other
        );
    }

    public String displayName(DimensionId dimension) {
        return names.getOrDefault(dimension, names.get(DimensionId.OTHER));
    }
}
