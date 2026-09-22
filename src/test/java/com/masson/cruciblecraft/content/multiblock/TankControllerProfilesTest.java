package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TankControllerProfilesTest {
    @Test
    void includesEverySmallTankValveAndKeepsDenseCapacityTiers() {
        assertEquals(13, TankControllerProfiles.values().size());
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
    }
}
