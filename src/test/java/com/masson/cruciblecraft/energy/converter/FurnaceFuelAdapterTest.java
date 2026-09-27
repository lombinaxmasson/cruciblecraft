package com.masson.cruciblecraft.energy.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FurnaceFuelAdapterTest {
    @BeforeAll
    static void bootstrapPrefixes() {
        if (!MaterialPrefixCatalog.isBootstrapped()) {
            MaterialPrefixCatalog.bootstrap(null);
        }
    }

    @Test
    void cokeFamilyUsesGt6FurnaceTicks() {
        assertEquals(3_200, FurnaceFuelTicks.ticksPerUnit("coal_coke"));
        assertEquals(1_600, FurnaceFuelTicks.ticksPerUnit("lignite_coke"));
        assertEquals(6_400, FurnaceFuelTicks.ticksPerUnit("petroleum_coke"));
        assertEquals(
                3_200,
                FurnaceFuelTicks.ticks("coal_coke", MaterialPrefixes.GEM));
        assertEquals(
                3_200,
                FurnaceFuelTicks.ticks("coal_coke", MaterialPrefixes.DUST));
        assertEquals(
                800,
                FurnaceFuelTicks.ticks(
                        "coal_coke", MaterialPrefixes.SMALL_DUST));
        assertEquals(
                355,
                FurnaceFuelTicks.ticks(
                        "coal_coke", MaterialPrefixes.TINY_DUST));
        assertEquals(
                28_800,
                FurnaceFuelTicks.ticks("coal_coke", MaterialPrefixes.BLOCK));
        assertEquals(
                28_800,
                FurnaceFuelTicks.ticks(
                        "coal_coke", MaterialPrefixes.STORAGE_DUST));
        assertEquals(
                6_400,
                FurnaceFuelTicks.ticks(
                        "petroleum_coke", MaterialPrefixes.GEM));
    }

    @Test
    void burningBoxHeatIsFurnaceTicksTimesTwentyFive() {
        assertEquals(40_000L, FurnaceFuelAdapter.heatUnits(1_600, 10_000));
        assertEquals(30_000L, FurnaceFuelAdapter.heatUnits(1_600, 7_500));
        assertEquals(60_000L, FurnaceFuelAdapter.heatUnits(3_200, 7_500));
    }

    @Test
    void solidBoxCatalogDoesNotInventAConsumeCap() {
        assertEquals(
                0,
                EnergyConverterCatalog.require(
                                "cruciblecraft:bronze_burning_box_solid")
                        .outputCapacity());
        assertEquals(
                0,
                EnergyConverterCatalog.require(
                                "cruciblecraft:bronze_burning_box_solid_dense")
                        .outputCapacity());
        assertTrue(
                EnergyConverterCatalog.require(
                                "cruciblecraft:bronze_burning_box_gas")
                        .outputCapacity()
                        >= 288_000);
    }
}
