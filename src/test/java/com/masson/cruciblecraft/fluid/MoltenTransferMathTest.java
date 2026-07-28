package com.masson.cruciblecraft.fluid;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoltenTransferMathTest {
    @Test
    void convertsCelsiusMeltingPointsToKelvin() {
        assertEquals(505, MoltenTransferMath.celsiusToKelvin(232));
        assertEquals(1_223, MoltenTransferMath.celsiusToKelvin(950));
        assertEquals(1_811, MoltenTransferMath.celsiusToKelvin(1_538));
    }

    @Test
    void oneIngotIsExactly144MillibucketsAndMaterialUnits() {
        assertEquals(144, MoltenTransferMath.MILLIBUCKETS_PER_INGOT);
    }

    @Test
    void fillPlanHonorsCapacityAndIntegralCompositionQuantum() {
        assertEquals(100, MoltenTransferMath.planFill(101, 200, 4));
        assertEquals(48, MoltenTransferMath.planFill(100, 50, 4));
        assertEquals(0, MoltenTransferMath.planFill(3, 100, 4));
    }

    @Test
    void bronzeDrainPlanPreservesThreeToOneRatio() {
        var plan = MoltenTransferMath.planDrain(
                Map.of("copper", 108, "tin", 36),
                Map.of("copper", 3, "tin", 1),
                101).orElseThrow();

        assertEquals(100, plan.amount());
        assertEquals(Map.of("copper", 75, "tin", 25), plan.removals());
    }

    @Test
    void plansAreImmutableAndDoNotMutateSimulatedContents() {
        Map<String, Integer> contents = Map.of("copper", 108, "tin", 36);
        var plan = MoltenTransferMath.planDrain(
                contents,
                Map.of("copper", 108, "tin", 36),
                144).orElseThrow();

        assertEquals(Map.of("copper", 108, "tin", 36), contents);
        assertEquals(144, plan.amount());
        assertTrue(MoltenTransferMath.planDrain(
                Map.of("copper", 109, "tin", 36),
                Map.of("copper", 3, "tin", 1),
                144).isEmpty());
    }
}
