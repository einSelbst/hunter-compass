package dev.einselbst.huntercompass.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompassTargetCalculatorTest {
    @Test
    void pointsExactlyInsideTheSameDimension() {
        CompassGuidance result = CompassTargetCalculator.calculate(
                DimensionId.OVERWORLD,
                new BlockPosition(0, 64, 0),
                DimensionId.OVERWORLD,
                new BlockPosition(120, 71, -33)
        );

        assertEquals(GuidanceMode.EXACT, result.mode());
        assertEquals(DimensionId.OVERWORLD, result.needleDimension());
        assertEquals(new BlockPosition(120, 71, -33), result.needleTarget());
    }

    @Test
    void projectsNetherTargetIntoOverworldPortalCoordinates() {
        CompassGuidance result = CompassTargetCalculator.calculate(
                DimensionId.OVERWORLD,
                new BlockPosition(10, 70, 20),
                DimensionId.NETHER,
                new BlockPosition(12, 45, -7)
        );

        assertEquals(GuidanceMode.SCALED_PORTAL, result.mode());
        assertEquals(new BlockPosition(96, 70, -56), result.needleTarget());
    }

    @Test
    void projectsOverworldTargetIntoNetherWithFloorDivision() {
        CompassGuidance result = CompassTargetCalculator.calculate(
                DimensionId.NETHER,
                new BlockPosition(0, 50, 0),
                DimensionId.OVERWORLD,
                new BlockPosition(-1, 80, -9)
        );

        assertEquals(GuidanceMode.SCALED_PORTAL, result.mode());
        assertEquals(new BlockPosition(-1, 50, -2), result.needleTarget());
    }

    @Test
    void refusesToInventDirectionForEndTransitions() {
        CompassGuidance result = CompassTargetCalculator.calculate(
                DimensionId.OVERWORLD,
                new BlockPosition(0, 64, 0),
                DimensionId.END,
                new BlockPosition(50, 70, 50)
        );

        assertEquals(GuidanceMode.UNAVAILABLE, result.mode());
        assertEquals(DimensionId.END, result.needleDimension());
    }
}
