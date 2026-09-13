package dev.einselbst.huntercompass.domain;

public final class CompassTargetCalculator {
    private CompassTargetCalculator() {
    }

    public static CompassGuidance calculate(
            DimensionId hunterDimension,
            BlockPosition hunterPosition,
            DimensionId targetDimension,
            BlockPosition targetPosition
    ) {
        if (hunterDimension == targetDimension) {
            return new CompassGuidance(GuidanceMode.EXACT, hunterDimension, targetPosition);
        }

        if (hunterDimension == DimensionId.OVERWORLD && targetDimension == DimensionId.NETHER) {
            return new CompassGuidance(
                    GuidanceMode.SCALED_PORTAL,
                    hunterDimension,
                    new BlockPosition(targetPosition.x() * 8, hunterPosition.y(), targetPosition.z() * 8)
            );
        }

        if (hunterDimension == DimensionId.NETHER && targetDimension == DimensionId.OVERWORLD) {
            return new CompassGuidance(
                    GuidanceMode.SCALED_PORTAL,
                    hunterDimension,
                    new BlockPosition(
                            floorDividedByEight(targetPosition.x()),
                            hunterPosition.y(),
                            floorDividedByEight(targetPosition.z())
                    )
            );
        }

        return new CompassGuidance(GuidanceMode.UNAVAILABLE, targetDimension, targetPosition);
    }

    private static int floorDividedByEight(int value) {
        return Math.floorDiv(value, 8);
    }
}
