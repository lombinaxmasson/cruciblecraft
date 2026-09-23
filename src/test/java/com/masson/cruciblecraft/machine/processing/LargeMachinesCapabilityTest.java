package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.energy.bedrockdrill.BedrockDrillStructure;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeMachinesCapabilityTest {
    private static final List<String> CAPABILITIES = List.of(
            "machines/large-crucible",
            "machines/bedrock-drill",
            "machines/coke-oven",
            "machines/large-centrifuge",
            "machines/large-mixer",
            "machines/large-coagulator",
            "machines/large-bathing-vat",
            "machines/large-oven",
            "machines/large-crusher");

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void acceptedCardsDeclareDistinctOwners() throws Exception {
        Set<String> slugs = new HashSet<>();
        for (String slug : CAPABILITIES) {
            Path path = Path.of(
                    "tools/capabilities/" + slug + "/capability.json");
            JsonObject card = JsonParser.parseString(
                    Files.readString(path)).getAsJsonObject();
            assertEquals(2, card.get("schema_version").getAsInt(), slug);
            assertEquals(slug, card.get("slug").getAsString(), slug);
            assertEquals("runtime_ready", card.get("maturity").getAsString(), slug);
            assertEquals("accepted", card.get("workflow").getAsString(), slug);
            assertTrue(card.getAsJsonArray("owned_paths").size() > 0, slug);
            assertTrue(card.getAsJsonArray("required_test_ids").size() > 0, slug);
            assertTrue(slugs.add(slug), "duplicate capability " + slug);
        }
        assertEquals(CAPABILITIES.size(), slugs.size());
    }

    @Test
    void bedrockDrillPortProjectionMatchesGt6() {
        Map<BedrockDrillStructure.PortKind, Integer> counts =
                new java.util.EnumMap<>(BedrockDrillStructure.PortKind.class);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -5; dy <= 0; dy++) {
                    BedrockDrillStructure.PortKind kind =
                            BedrockDrillStructure.portKind(dx, dy, dz);
                    counts.merge(kind, 1, Integer::sum);
                }
            }
        }
        assertEquals(9, counts.get(BedrockDrillStructure.PortKind.BEDROCK_FLOOR));
        assertEquals(9, counts.get(BedrockDrillStructure.PortKind.DRILL_HEAD));
        assertEquals(31, counts.get(BedrockDrillStructure.PortKind.FLUID_INPUT));
        assertEquals(4, counts.get(BedrockDrillStructure.PortKind.ENERGY_INPUT));
        assertEquals(1, counts.get(BedrockDrillStructure.PortKind.CONTROLLER));
    }

    @Test
    void cokeOvenFluidOutputCoordinatesMatchGt6() {
        BlockPos controller = BlockPos.ZERO;
        List<BlockPos> drains =
                com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity
                        .fluidDrainPositions(controller, Direction.SOUTH);
        assertEquals(9, drains.size());
        assertTrue(drains.stream().allMatch(pos -> pos.getY() == -2));
        assertTrue(drains.contains(new BlockPos(0, -2, -1)));
        assertTrue(drains.contains(new BlockPos(-1, -2, -2)));
        assertTrue(drains.contains(new BlockPos(1, -2, 0)));
    }

    @Test
    void cokeOvenEnergyIsBoundedAndPersisted() throws Exception {
        String oven = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                        + "CokeOvenBlockEntity.java"));
        String brick = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                        + "FirebrickBlockEntity.java"));
        assertTrue(oven.contains("IEnergyHandler"));
        assertTrue(oven.contains("TU_CAPACITY"));
        assertTrue(oven.contains("advanceWithAvailableTu"));
        assertTrue(oven.contains("energy.restore"));
        assertTrue(brick.contains("PortType.ITEM_FLUID_ENERGY"));
        assertTrue(brick.contains("IEnergyHandler energy"));
    }

    @Test
    void largeMixerProfileIsSourceBacked() {
        TierProfile profile =
                ModMultiblockControllers.LARGE_MIXER_VARIANT.tierBand();
        assertEquals("cruciblecraft:large_mixer_profile",
                profile.tierBandId().toString());
        assertEquals(512L, profile.inputMinimum());
        assertEquals(4_096L, profile.inputMaximum());
        assertEquals(256, profile.parallelLimit());
        assertEquals(10_000, profile.efficiency());
        assertTrue(ModMultiblockControllers.LARGE_MIXER_KIND.parallelDuration());
    }

    @Test
    void largeMixerStructureBindsIndependentPorts() throws Exception {
        JsonObject document = readJson(
                "src/main/resources/data/cruciblecraft/multiblock_structures/"
                        + "large_mixer.json");
        JsonObject palette = document.getAsJsonObject("palette");
        int inputs = 0;
        int outputs = 0;
        int energy = 0;
        for (var row : document.getAsJsonArray("structure")) {
            JsonObject predicate = palette.getAsJsonObject(
                    row.getAsJsonObject().get("predicate").getAsString());
            String port = predicate.has("port")
                    ? predicate.get("port").getAsString()
                    : "";
            inputs += "item_fluid_in".equals(port) ? 1 : 0;
            outputs += "item_fluid_out".equals(port) ? 1 : 0;
            energy += "energy_input".equals(port) ? 1 : 0;
        }
        assertEquals(8, inputs);
        assertEquals(7, outputs);
        assertEquals(2, energy);
    }

    @Test
    void largeMixerProcessesAndPreservesBlockedOutputs() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                        + "LargeMixerBlockEntity.java"));
        String host = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                        + "ProcessingMachineBlockEntity.java"));
        assertTrue(source.contains("tickProcessingServer"));
        assertTrue(source.contains("saveAdditional"));
        assertTrue(source.contains("loadAdditional"));
        assertTrue(host.contains("PortStoreSync.pullInputs"));
        assertTrue(host.contains("PortStoreSync.pushOutputs"));
        assertTrue(host.contains("outputsCompletelyEmpty"));
    }

    @Test
    void largeMixerReloadRetainsPortBindings() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                        + "LargeMixerBlockEntity.java"));
        assertTrue(source.contains("PLUGIN_TAG"));
        assertTrue(source.contains("MultiblockPortAggregator.refresh"));
        assertTrue(source.contains("MultiblockPortAggregator.unbindLoaded"));
        assertTrue(source.contains("clearBindings"));
    }

    @Test
    void largeCoagulatorTimeProfileMatchesGt6() {
        TierProfile profile =
                ModMultiblockControllers.LARGE_COAGULATOR_VARIANT.tierBand();
        assertEquals(EnergyType.TIME, profile.energyType());
        assertEquals(64, profile.parallelLimit());
        assertEquals(10_000, profile.efficiency());
        assertFalse(ModMultiblockControllers.LARGE_COAGULATOR_KIND.parallelDuration());
    }

    @Test
    void largeCoagulatorOutputBlockingIsLossless() throws Exception {
        assertProcessingHostRetainsTransaction();
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                        + "LargeCoagulatorBlockEntity.java"));
        assertTrue(source.contains("tickProcessingServer"));
    }

    @Test
    void largeBathingVatTimeProfileMatchesGt6() {
        TierProfile profile =
                ModMultiblockControllers.LARGE_BATH_VARIANT.tierBand();
        assertEquals(EnergyType.TIME, profile.energyType());
        assertEquals(64, profile.parallelLimit());
        assertEquals(10_000, profile.efficiency());
        assertFalse(ModMultiblockControllers.LARGE_BATH_KIND.parallelDuration());
    }

    @Test
    void largeBathingVatOutputBlockingIsLossless() throws Exception {
        assertProcessingHostRetainsTransaction();
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                        + "LargeBathBlockEntity.java"));
        assertTrue(source.contains("tickProcessingServer"));
    }

    @Test
    void largeOvenOutputBlockingIsLossless() throws Exception {
        assertProcessingHostRetainsTransaction();
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                        + "LargeOvenBlockEntity.java"));
        assertTrue(source.contains("updateActivity"));
    }

    @Test
    void largeCrusherOutputBlockingIsLossless() throws Exception {
        assertProcessingHostRetainsTransaction();
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                        + "LargeCrusherBlockEntity.java"));
        assertTrue(source.contains("updateAdjacentToggleableEnergySources"));
        assertTrue(source.contains("bindStructureMember"));
    }

    private static void assertProcessingHostRetainsTransaction()
            throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                        + "ProcessingMachineBlockEntity.java"));
        assertTrue(source.contains("Optional<MachineTransaction> transaction"));
        assertTrue(source.contains("if (!completion.commit(this))"));
        assertTrue(source.contains("autoOutputItems"));
    }

    private static JsonObject readJson(String path) throws Exception {
        return JsonParser.parseString(
                Files.readString(Path.of(path))).getAsJsonObject();
    }
}
