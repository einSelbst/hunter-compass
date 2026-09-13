package dev.einselbst.huntercompass.domain;

public record CompassGuidance(
        GuidanceMode mode,
        DimensionId needleDimension,
        BlockPosition needleTarget
) {
}
