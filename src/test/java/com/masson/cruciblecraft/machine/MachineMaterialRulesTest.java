package com.masson.cruciblecraft.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;

class MachineMaterialRulesTest {
    @Test
    void defaultsAreExplicitAndInvalidMaterialsFailLoudly() {
        assertEquals("ceramic", MachineMaterialRules.defaultMaterial(Device.CRUCIBLE));
        assertEquals("stone", MachineMaterialRules.defaultMaterial(Device.ANVIL));
        assertEquals("iron", MachineMaterialRules.defaultMaterial(Device.HAMMER));
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> MachineMaterialRules.requireAllowed(Device.CRUCIBLE, "invalid"));
        assertTrue(failure.getMessage().contains("crucible"));
        assertTrue(failure.getMessage().contains("invalid"));

        var external = MachineMaterialRules.resolveExternal(Device.ANVIL, "gold");
        assertEquals("stone", external.effectiveMaterial());
        assertEquals("gold", external.quarantinedMaterial().orElseThrow());
        assertTrue(external.quarantined());
    }

    @Test
    void onlyPlayableCrucibleCasingsAreAccepted() {
        assertTrue(MachineMaterialRules.isAllowed(Device.CRUCIBLE, "ceramic"));
        assertTrue(MachineMaterialRules.isAllowed(Device.CRUCIBLE, "bronze"));
        assertTrue(MachineMaterialRules.isAllowed(Device.CRUCIBLE, "steel"));
        assertFalse(MachineMaterialRules.isAllowed(Device.CRUCIBLE, "iron"));
    }

    @Test
    void explicitCapabilityPreservesCeramicProgression() {
        assertTrue(MachineMaterialRules.canCrucibleProcessTier("ceramic", 1));
        assertTrue(MachineMaterialRules.canCrucibleProcessTier("ceramic", 2));
        assertFalse(MachineMaterialRules.canCrucibleProcessTier("ceramic", 3));
        assertTrue(MachineMaterialRules.canCrucibleProcessTier("steel", 3));
    }

    @Test
    void maxTemperatureUsesGt6TwentyFivePercentFloor() {
        assertEquals(2227.0f, MachineMaterialRules.maxTemperature(1727.0), 0.001f);
        assertEquals(1255.0f, MachineMaterialRules.maxTemperature(950.0), 0.001f);
        assertEquals(2284.0f, MachineMaterialRules.maxTemperature(1773.0), 0.001f);
        assertEquals(
                1927.0f,
                MachineMaterialRules.maxTemperature(1727.0, 1.10),
                0.001f);
    }

    @Test
    void tieredAnvilAndHammerDurabilityUsesDocumentedProgression() {
        assertEquals(10_000L, MachineMaterialRules.anvilMaxDurability("stone"));
        assertEquals(1_000_000L, MachineMaterialRules.anvilMaxDurability("bronze"));
        assertEquals(5_000_000L, MachineMaterialRules.anvilMaxDurability("iron"));
        assertEquals(10_000_000L, MachineMaterialRules.anvilMaxDurability("steel"));
        assertEquals(44_800, MachineMaterialRules.hammerMaxDurability("bronze"));
        assertEquals(48_000, MachineMaterialRules.hammerMaxDurability("iron"));
        assertEquals(51_200, MachineMaterialRules.hammerMaxDurability("steel"));
    }

    @Test
    void gt6WearIsScaledIntoStoredInternalDurability() {
        assertEquals(1L, MachineMaterialRules.anvilWear(1L));
        assertEquals(1L, MachineMaterialRules.anvilWear(40_000L));
        assertEquals(2L, MachineMaterialRules.anvilWear(40_001L));
        assertTrue(MachineMaterialRules.STONE_ANVIL_DURABILITY
                / MachineMaterialRules.anvilWear(10_000L) > 1);
    }

    @Test
    void deviceTierGatesRejectHigherTierWorkpieces() {
        assertTrue(MachineMaterialRules.supportsTier(Device.ANVIL, "stone", 0));
        assertFalse(MachineMaterialRules.supportsTier(Device.ANVIL, "stone", 1));
        assertTrue(MachineMaterialRules.supportsTier(Device.HAMMER, "bronze", 1));
        assertFalse(MachineMaterialRules.supportsTier(Device.HAMMER, "bronze", 2));
        assertTrue(MachineMaterialRules.supportsTier(Device.ANVIL, "steel", 3));
    }
}
