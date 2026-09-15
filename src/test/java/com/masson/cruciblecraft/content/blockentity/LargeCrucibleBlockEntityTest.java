package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore;
import com.masson.cruciblecraft.registry.ModMultiblockPlugins;

import org.junit.jupiter.api.Test;

class LargeCrucibleBlockEntityTest {
    @Test
    void pluginSetIsThermalSteelmakingNotProcessingHost() {
        assertEquals(
                List.of(
                        ModMultiblockPlugins.THERMAL_STEELMAKING_HOST,
                        ModMultiblockPlugins.HEAT_ENERGY_INPUT,
                        ModMultiblockPlugins.SHARED_PORT_SUPPLY),
                ModMultiblockPlugins.LARGE_CRUCIBLE_PLUGINS);
        assertFalse(
                ModMultiblockPlugins.LARGE_CRUCIBLE_PLUGINS.contains(
                        ModMultiblockPlugins.PROCESSING_HOST));
        assertFalse(
                ModMultiblockPlugins.LARGE_CRUCIBLE_PLUGINS.contains(
                        ModMultiblockPlugins.STORAGE_HOST));
        assertFalse(
                ModMultiblockPlugins.LARGE_CRUCIBLE_PLUGINS.contains(
                        ModMultiblockPlugins.STEAM_CONVERSION));
    }

    @Test
    void capacityStaysDistinctFromSingleBlock() {
        assertEquals(16, CrucibleBlockEntity.MAX_INGOTS);
        assertEquals(432, CrucibleProcessCore.LARGE_MAX_INGOTS);
        assertEquals(
                CrucibleProcessCore.SINGLE_BLOCK_MAX_INGOTS,
                CrucibleBlockEntity.MAX_INGOTS);
    }

    @Test
    void mismatchedPluginIdQuarantinesWithoutAdopting() {
        PluginQuarantinePolicy.Decision decision =
                PluginQuarantinePolicy.resolve(
                        "cruciblecraft:not_a_plugin",
                        ModMultiblockPlugins.THERMAL_STEELMAKING_HOST
                                .toString());
        assertEquals(
                PluginQuarantinePolicy.Resolution.QUARANTINED,
                decision.resolution());
        assertTrue(decision.quarantineReason().orElseThrow()
                .contains("does not match"));
    }

    @Test
    void controllerIdDoesNotCollideWithSingleBlockCrucible() {
        assertEquals(
                "cruciblecraft:large_crucible",
                LargeCrucibleBlockEntity.STRUCTURE_ID.toString());
        assertFalse(
                LargeCrucibleBlockEntity.STRUCTURE_ID
                        .getPath()
                        .equals("crucible"));
    }
}
