package com.masson.cruciblecraft.recipe.gt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GTRecipePublicationMetricsTest {
    @BeforeEach
    void resetLog() {
        RecipeLoadLog.resetForTest();
    }

    @Test
    void emptyMetricsDoNotZeroFillAllocation() {
        GTRecipeMapLoader.PublicationAllocationReport allocation =
                GTRecipeMapLoader.PublicationAllocationReport.pendingMeasurement();
        assertEquals("PENDING_MEASUREMENT", allocation.reloadTransientAllocation());
        assertEquals("PENDING_MEASUREMENT", allocation.retainedMemory());
        assertEquals(
                0L,
                GTRecipeMapLoader.PublicationPhaseTimings.zero()
                        .temporaryIndexMillis());
        assertEquals(
                "none",
                GTRecipeMapLoader.PublicationControlMetrics.empty()
                        .requestCause());
    }

    @Test
    void parseFailuresAggregateByFingerprint() {
        ResourceLocation map = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "fuels_fluidbed");
        ResourceLocation first = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "energy/fuels_fluidbed/a");
        ResourceLocation second = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "energy/fuels_fluidbed/b");
        RecipeLoadLog.parseFailure(
                7L,
                "IllegalArgumentException",
                map,
                first,
                "output_chances",
                "a.json");
        RecipeLoadLog.parseFailure(
                7L,
                "IllegalArgumentException",
                map,
                second,
                "output_chances",
                "b.json");
        RecipeLoadLog.flushEpoch(7L, 0, "TAGS_UPDATED", 1);
        RecipeLoadLog.flushEpoch(7L, 0, "TAGS_UPDATED", 1);
    }

    @Test
    void phaseTimingsKeepTemporaryIndexDistinctFromFinalIndex() {
        var timings = new GTRecipeMapLoader.PublicationPhaseTimings(
                1, 2, 3, 4, 5, 0, 6, 7);
        assertEquals(0L, timings.temporaryIndexMillis());
        assertEquals(6L, timings.finalIndexMillis());
        assertEquals(7L, timings.emiProjectionMillis());
        assertNotEquals(
                timings.finalIndexMillis(),
                timings.temporaryIndexMillis());
    }
}
