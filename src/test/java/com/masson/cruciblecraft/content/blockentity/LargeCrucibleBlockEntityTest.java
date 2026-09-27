package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.masson.cruciblecraft.content.block.LargeCrucibleHosts;
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
        assertTrue(source.contains("applyFormedVisuals"));
        assertTrue(source.contains("LargeCrucibleHosts.FORMED"));
    }

    @Test
    void largeCrucibleMaterialsMatchGt6CatalogedWalls() {
        assertEquals(7, LargeCrucibleHosts.MATERIALS.size());
        assertTrue(LargeCrucibleHosts.isAllowed("steel"));
        assertTrue(LargeCrucibleHosts.isAllowed("stainless_steel"));
        assertTrue(LargeCrucibleHosts.acidProof("stainless_steel"));
        assertFalse(LargeCrucibleHosts.acidProof("steel"));
        assertFalse(LargeCrucibleHosts.isAllowed("ceramic"));
    }

    @Test
    void controllerColorAndClientSyncFollowCasingMaterial() throws Exception {
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/LargeCrucibleBlockEntity.java"));
        assertTrue(source.contains("getUpdateTag"));
        assertTrue(source.contains("syncToClient"));
        String color = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/masson/cruciblecraft/client/color/LargeCrucibleBlockColor.java"));
        assertTrue(color.contains("MACHINE_MATERIAL"));
        assertTrue(color.contains("isController"));
        assertTrue(color.contains("isCatalogWall"));
        String machineColor = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/masson/cruciblecraft/client/color/MachineBlockColor.java"));
        assertFalse(machineColor.contains("LARGE_CRUCIBLE"));
        String renderer = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/masson/cruciblecraft/client/render/LargeCrucibleRenderer.java"));
        assertTrue(renderer.contains("LargeCrucibleHullRenderer.render"));
        String hull = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/masson/cruciblecraft/client/render/LargeCrucibleHullRenderer.java"));
        assertTrue(hull.contains("-0.999f"));
        assertTrue(hull.contains("1.500f"));
        String interaction = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/masson/cruciblecraft/content/block/MteInPlaceBlock.java"));
        assertTrue(interaction.contains("!spec.kind().attachment()"));
        assertTrue(interaction.contains("return InteractionResult.PASS"));
        assertTrue(interaction.contains("CoinageMoldHosts.isCoinage"));
        String inventory = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/LargeCrucibleBlockEntity.java"));
        assertTrue(inventory.contains("CastingMolds.isMoldItem"));
        assertTrue(inventory.contains("implements MultiblockControllerBinding, MultiblockPortHost, CruciblePour, MoldHost"));
        String model = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/assets/cruciblecraft/models/block/large_crucible.json"));
        assertTrue(model.contains("gt6_import/multiblockmains/crucible"));
        assertFalse(model.contains("coke_oven"));
        String delivery = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft/machine_delivery.json"));
        assertFalse(delivery.contains("\"large_crucible\": \"coke_oven\""));
        String tabs = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/masson/cruciblecraft/registry/ModCreativeTabs.java"));
        int building = tabs.indexOf("private static void fillBuilding");
        int machinesFill = tabs.indexOf("private static void fillMachines");
        String machinesBody = tabs.substring(machinesFill, building);
        String buildingBody = tabs.substring(building);
        assertFalse(machinesBody.contains("STAINLESS_STEEL_WALL"));
        assertTrue(tabs.contains("CRUCIBLE_FOUNDRY"));
        assertFalse(buildingBody.contains("STAINLESS_STEEL_WALL"));
        assertFalse(buildingBody.contains("GALVANIZED_STEEL_WALL"));
    }
}
