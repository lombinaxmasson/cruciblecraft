package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

class HydrocarbonRuntimePolicyTest {
    @Test
    void productionAndMigrationAreIndependentFromLegacyReserve() {
        var methane = id("methane");
        var naturalGas = id("natural_gas");
        assertEquals(
                naturalGas,
                HydrocarbonRuntimePolicy.migrate(methane));

        var oil = HydrocarbonRuntimePolicy.production(
                id("crude_oil"));
        assertEquals(25, oil.amountMb());
        assertEquals(20, oil.intervalTicks());
        assertEquals(1_000, oil.accumulationCapMb());
        assertFalse(oil.ventOverflow());

        var gas = HydrocarbonRuntimePolicy.production(naturalGas);
        assertEquals(5, gas.amountMb());
        assertEquals(20, gas.intervalTicks());
        assertEquals(1_000, gas.accumulationCapMb());
        assertTrue(gas.ventOverflow());
        assertTrue(HydrocarbonRuntimePolicy.isFlammable(naturalGas));
        assertTrue(HydrocarbonRuntimePolicy.isFlammable(id("methane")));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", path);
    }
}
