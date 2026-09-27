package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TankControllerProfilesTest {
    @Test
    void includesEveryTankValveAndKeepsDenseCapacityTiers() {
        assertEquals(25, TankControllerProfiles.values().size());
        assertEquals(
                1_728_000,
                TankControllerProfiles.findControllerMeta(17002)
                        .orElseThrow()
                        .capacityMb());
        assertEquals(
                6_912_000,
                TankControllerProfiles.findControllerMeta(17022)
                        .orElseThrow()
                        .capacityMb());
        assertEquals(
                442_368_000,
                TankControllerProfiles.findControllerMeta(17025)
                        .orElseThrow()
                        .capacityMb());
    }

    @Test
    void proofAndWallIdentityAreMaterialSpecific() {
        TankControllerProfiles.Profile stainless =
                TankControllerProfiles.findControllerMeta(17002)
                        .orElseThrow();
        TankControllerProfiles.Profile adamantium =
                TankControllerProfiles.findControllerMeta(17005)
                        .orElseThrow();
        TankControllerProfiles.Profile denseAdamantium =
                TankControllerProfiles.findControllerMeta(17025)
                        .orElseThrow();
        assertTrue(stainless.gasProof());
        assertTrue(stainless.acidProof());
        assertFalse(stainless.plasmaProof());
        assertTrue(adamantium.gasProof());
        assertTrue(adamantium.acidProof());
        assertTrue(adamantium.plasmaProof());
        assertEquals(18002, stainless.wallMeta());
        assertFalse(stainless.wallMeta() == 18003);
        assertEquals(
                "cruciblecraft:multiblock/small_stainless_steel_tank_main_valve",
                stainless.controllerId().toString());
        assertEquals(
                "cruciblecraft:stainless_steel/wall",
                stainless.wallId().toString());
        assertEquals(
                "cruciblecraft:multiblock/dense_adamantium_wall",
                denseAdamantium.wallId().toString());
        assertEquals(3, stainless.span());
        assertEquals(25, stainless.wallPorts());
        assertEquals("cruciblecraft:tank_3x3x3", stainless.structureId().toString());
    }

    @Test
    void largeValvesUseTheFiveCubeAndKeepLoaderCapacity() {
        TankControllerProfiles.Profile stainless =
                TankControllerProfiles.findControllerMeta(17042)
                        .orElseThrow();
        TankControllerProfiles.Profile denseAdamantium =
                TankControllerProfiles.findControllerMeta(17065)
                        .orElseThrow();
        assertEquals(5, stainless.span());
        assertEquals(2, stainless.radius());
        assertEquals(97, stainless.wallPorts());
        assertEquals(8_000_000, stainless.capacityMb());
        assertEquals(18002, stainless.wallMeta());
        assertTrue(stainless.gasProof());
        assertTrue(stainless.acidProof());
        assertFalse(stainless.magicProof());
        assertEquals(
                "cruciblecraft:tank_5x5x5",
                stainless.structureId().toString());
        assertEquals(
                "cruciblecraft:multiblock/large_stainless_steel_tank_main_valve",
                stainless.controllerId().toString());
        assertEquals(
                "cruciblecraft:stainless_steel/wall",
                stainless.wallId().toString());
        assertEquals(2_048_000_000, denseAdamantium.capacityMb());
        assertEquals(18025, denseAdamantium.wallMeta());
        assertTrue(denseAdamantium.plasmaProof());
        assertEquals(
                "cruciblecraft:multiblock/large_dense_adamantium_tank_main_valve",
                denseAdamantium.controllerId().toString());
        assertEquals(
                "cruciblecraft:multiblock/dense_adamantium_wall",
                denseAdamantium.wallId().toString());
    }
}
