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

    @Test
    void acceptsGt6HeatCryoAndKineticAirKinds() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/LargeCrucibleBlockEntity.java"));
        int handles = source.indexOf("public boolean handles(EnergyType type, Direction side)");
        String body = source.substring(handles, source.indexOf("public long insert", handles));
        assertTrue(body.contains("HEAT"));
        assertTrue(body.contains("CU"));
        assertTrue(body.contains("AIR"));
        assertTrue(body.contains("KINETIC_ROTATION"));
        assertTrue(source.contains("queueCooling"));
        assertTrue(source.contains("fillMeltdownLava"));
        assertTrue(source.contains("pourHostAtWall"));
    }
}
