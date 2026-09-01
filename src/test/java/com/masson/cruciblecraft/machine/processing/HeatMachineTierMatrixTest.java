package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HeatMachineTierMatrixTest {
    private static JsonObject expected;

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        expected = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(
                        HeatMachineTierMatrixTest.class.getResourceAsStream(
                                "/heat_machine_tier_expected.json")),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void independentHeatExpectedMatchesCatalogAndRuntimeVariants() {
        Map<ResourceLocation, MachineTierCatalog.Entry> entries =
                new HashMap<>();
        MachineTierCatalog.entries().forEach(
                entry -> entries.put(entry.variantId(), entry));
        assertTrue(entries.size() > 33);
        assertTrue(ModMachineVariants.ALL.size() > 33);
        assertEquals(9, ModMachineVariants.SELECTED_HEAT_VARIANTS.size());

        expected.getAsJsonArray("heat_selected").forEach(element -> {
            JsonObject row = element.getAsJsonObject();
            ResourceLocation id = ResourceLocation.parse(
                    row.get("id").getAsString());
            MachineTierCatalog.Entry entry = entries.get(id);
            MachineVariant variant = ModMachineVariants.require(id);
            long nominal = row.get("nominal").getAsLong();

            assertEquals(
                    row.get("kind").getAsString(),
                    entry.kindId().toString(),
                    id.toString());
            assertEquals(row.get("source_id").getAsInt(), entry.sourceId());
            assertEquals(
                    row.get("source_tier").getAsInt(),
                    entry.sourceTier());
            assertEquals(
                    row.get("source_material").getAsString(),
                    entry.sourceMaterial());
            assertTrue(entry.materialRegistered());
            assertNull(entry.acquisitionBlocker());
            assertEquals(
                    MachineKindSpec.OverclockPolicy.CHEAP,
                    entry.overclockPolicy());
            assertTrue(entry.parallelDuration());
            assertEquals(row.get("tier_band").getAsString(),
                    entry.tierBand().tierBandId().toString());
            assertEquals(row.get("material").getAsString(),
                    entry.tierBand().materialId());
            assertEquals(EnergyType.HEAT,
                    entry.tierBand().energyType());
            assertEquals(nominal / 2L,
                    entry.tierBand().inputMinimum());
            assertEquals(nominal, entry.tierBand().inputNominal());
            assertEquals(nominal * 2L,
                    entry.tierBand().inputMaximum());
            assertEquals(nominal * 2L,
                    entry.tierBand().energyCapacity());
            assertEquals(
                    row.get("parallel").getAsInt(),
                    entry.tierBand().parallelLimit());
            assertEquals(10_000, entry.tierBand().efficiency());

            assertSame(entry.tierBand(), variant.tierBand());
            assertEquals(entry.kindId(), variant.kind().id());
            assertEquals(
                    ProcessingMachineSpec.EnergyMode.ADJACENT,
                    variant.runtimeSpec().energy().mode());
            assertEquals(EnergyType.HEAT,
                    variant.runtimeSpec().energy().type());
            assertSame(
                    variant.kind().behavior().items(),
                    variant.runtimeSpec().items());
            assertSame(
                    variant.kind().behavior().fluids(),
                    variant.runtimeSpec().fluids());
            assertSame(
                    variant.kind().behavior().validator(),
                    variant.runtimeSpec().validator());
            assertSame(variant, ModBlocks.configuredProcessingVariant(id));
        });
    }

    @Test
    void selectedKindsLockSourceParallelPolicyAndElectricReference() {
        Map<ResourceLocation, Integer> expectedParallel = Map.of(
                ModMachineVariants.DISTILLERY.id(), 8,
                ModMachineVariants.DRYING.id(), 8,
                ModMachineVariants.SMELTER.id(), 1_000);
        for (var row : expectedParallel.entrySet()) {
            List<MachineVariant> variants =
                    ModMachineVariants.SELECTED_HEAT_VARIANTS.stream()
                            .filter(variant -> variant.kind().id().equals(
                                    row.getKey()))
                            .toList();
            assertEquals(3, variants.size(), row.getKey().toString());
            for (int index = 0; index < variants.size(); index++) {
                MachineVariant variant = variants.get(index);
                long nominal = 32L << (index * 2);
                int parallel = row.getKey().equals(
                        ModMachineVariants.SMELTER.id())
                                ? row.getValue()
                                : row.getValue() << index;
                assertEquals(nominal / 2L,
                        variant.tierBand().inputMinimum());
                assertEquals(
                        nominal, variant.tierBand().inputNominal());
                assertEquals(nominal * 2L,
                        variant.tierBand().inputMaximum());
                assertEquals(
                        parallel, variant.tierBand().parallelLimit());
                assertEquals(
                        MachineKindSpec.OverclockPolicy.CHEAP,
                        variant.kind().overclockPolicy());
                assertTrue(variant.kind().parallelDuration());
            }
        }

        expected.getAsJsonArray("electric_reference").forEach(element -> {
            JsonObject row = element.getAsJsonObject();
            MachineTierCatalog.Entry entry =
                    MachineTierCatalog.entries().stream()
                            .filter(candidate -> candidate.variantId().equals(
                                    ResourceLocation.parse(
                                            row.get("id").getAsString())))
                            .findFirst()
                            .orElseThrow();
            assertEquals(row.get("source_id").getAsInt(), entry.sourceId());
            assertEquals(
                    row.get("source_tier").getAsInt(),
                    entry.sourceTier());
            assertEquals(
                    EnergyType.ELECTRIC,
                    entry.tierBand().energyType());
            assertEquals(
                    MachineKindSpec.OverclockPolicy.STANDARD,
                    entry.overclockPolicy());
        });
    }

    @Test
    void tierOneBindingsAndSixHighTierBlocksAndItemsAreExact() {
        Set<String> selected = ModMachineVariants.SELECTED_HEAT_VARIANTS.stream()
                .map(variant -> variant.id().toString())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(9, selected.size());
        assertSame(
                ModMachineVariants.require(ResourceLocation.parse(
                        "cruciblecraft:distillery")),
                ModBlocks.configuredProcessingVariant(
                        ResourceLocation.parse("cruciblecraft:distillery")));
        assertSame(
                ModMachineVariants.require(ResourceLocation.parse(
                        "cruciblecraft:drying")),
                ModBlocks.configuredProcessingVariant(
                        ResourceLocation.parse("cruciblecraft:drying")));
        assertSame(
                ModMachineVariants.require(ResourceLocation.parse(
                        "cruciblecraft:smelter")),
                ModBlocks.configuredProcessingVariant(
                        ResourceLocation.parse("cruciblecraft:smelter")));

        Set<String> highTierBlocks = Set.of(
                ModBlocks.INVAR_DISTILLERY.getId().toString(),
                ModBlocks.TITANIUM_DISTILLERY.getId().toString(),
                ModBlocks.INVAR_DRYING.getId().toString(),
                ModBlocks.TITANIUM_DRYING.getId().toString(),
                ModBlocks.INVAR_SMELTER.getId().toString(),
                ModBlocks.TITANIUM_SMELTER.getId().toString());
        Set<String> highTierItems = Set.of(
                ModItems.INVAR_DISTILLERY.getId().toString(),
                ModItems.TITANIUM_DISTILLERY.getId().toString(),
                ModItems.INVAR_DRYING.getId().toString(),
                ModItems.TITANIUM_DRYING.getId().toString(),
                ModItems.INVAR_SMELTER.getId().toString(),
                ModItems.TITANIUM_SMELTER.getId().toString());
        assertEquals(6, highTierBlocks.size());
        assertEquals(highTierBlocks, highTierItems);
        assertTrue(selected.containsAll(highTierBlocks));
    }

    @Test
    void distilleryDryerAndSmelterUseAdjacentHeatBehavior() {
        for (ProcessingMachineSpec spec : List.of(
                ModProcessingMachines.DISTILLERY,
                ModProcessingMachines.DRYING,
                ModProcessingMachines.SMELTER)) {
            assertEquals(EnergyType.HEAT, spec.energy().type());
            assertEquals(
                    ProcessingMachineSpec.EnergyMode.ADJACENT,
                    spec.energy().mode());
            assertEquals(0L, spec.energy().capacity());
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.NONE,
                    spec.sidedIo().energy().resolve(
                            Direction.NORTH, Direction.SOUTH));
        }
    }

    @Test
    void huExecutionMatrixCoversThreeKindsThreeTiersAndCheapParallelDuration() {
        Map<ResourceLocation, List<Integer>> expectedParallel = Map.of(
                ModMachineVariants.DISTILLERY.id(), List.of(8, 16, 32),
                ModMachineVariants.DRYING.id(), List.of(8, 16, 32),
                ModMachineVariants.SMELTER.id(), List.of(1_000, 1_000, 1_000));
        for (var kindRow : expectedParallel.entrySet()) {
            List<MachineVariant> variants =
                    ModMachineVariants.SELECTED_HEAT_VARIANTS.stream()
                            .filter(variant -> variant.kind().id().equals(
                                    kindRow.getKey()))
                            .toList();
            assertEquals(3, variants.size(), kindRow.getKey().toString());
            for (int tierIndex = 0; tierIndex < variants.size(); tierIndex++) {
                MachineVariant variant = variants.get(tierIndex);
                TierProfile tier = variant.tierBand();
                assertEquals(EnergyType.HEAT, tier.energyType());
                assertEquals(
                        ProcessingMachineSpec.EnergyMode.ADJACENT,
                        variant.runtimeSpec().energy().mode());
                assertEquals(
                        MachineKindSpec.OverclockPolicy.CHEAP,
                        variant.kind().overclockPolicy());
                assertTrue(variant.kind().parallelDuration());
                assertEquals(
                        kindRow.getValue().get(tierIndex),
                        tier.parallelLimit());

                var placement = ProcessingMachineEnergyPlacement.connection(
                        variant.runtimeSpec(), Direction.EAST);
                assertEquals(Direction.DOWN, placement.providerOffset());
                assertEquals(Direction.UP, placement.providerFace());

                for (long input : List.of(
                        tier.inputMinimum(),
                        tier.inputNominal(),
                        tier.inputMaximum())) {
                    MachineExecutionPlan plan = MachineExecutionPlan.create(
                            recipe(input, 40),
                            variant.kind(),
                            tier,
                            1).orElseThrow();
                    assertEquals(input, plan.minimumPower());
                    assertEquals(
                            Math.max(input, tier.inputNominal()),
                            plan.nominalPower());
                    assertEquals(Math.multiplyExact(input, 40L),
                            plan.totalWork());
                    assertEquals(0, plan.overclockSteps());
                }

                MachineExecutionPlan cheap = MachineExecutionPlan.create(
                        recipe(4L, 40),
                        variant.kind(),
                        tier,
                        1).orElseThrow();
                assertEquals(4L, cheap.minimumPower());
                assertEquals(0, cheap.overclockSteps());

                MachineExecutionPlan parallel = MachineExecutionPlan.create(
                        recipe(16L, 20),
                        variant.kind(),
                        tier,
                        tier.parallelLimit()).orElseThrow();
                assertEquals(16L, parallel.minimumPower());
                assertEquals(tier.parallelLimit(), parallel.operations());
                assertEquals(
                        16L * 20L * tier.parallelLimit(),
                        parallel.totalWork());
            }
        }
    }

    @Test
    void smelterParallelThousandBoundaryRejectsOverflowAndCompactsOutputs() {
        MachineVariant smelter = ModMachineVariants.require(
                ResourceLocation.parse("cruciblecraft:smelter"));
        MachineExecutionPlan boundary = MachineExecutionPlan.create(
                recipe(16L, 20),
                smelter.kind(),
                smelter.tierBand(),
                1_000).orElseThrow();
        assertEquals(1_000, boundary.operations());
        assertFalse(MachineExecutionPlan.create(
                recipe(16L, 20),
                smelter.kind(),
                smelter.tierBand(),
                1_001).isPresent());
        assertFalse(MachineExecutionPlan.create(
                recipe(
                        smelter.tierBand().inputMaximum(),
                        Integer.MAX_VALUE),
                smelter.kind(),
                smelter.tierBand(),
                1_000).isPresent());

        List<ItemStack> compacted =
                ParallelRecipeOperations.maximumItemOutputs(
                        recipe(16L, 20), 1_000);
        assertEquals(16, compacted.size());
        assertEquals(
                1_000,
                compacted.stream().mapToInt(ItemStack::getCount).sum());
    }

    @Test
    void electrolyzerReferenceCoversThreeBufferedElectricTierEndpoints() {
        List<MachineVariant> variants = ModMachineVariants.forKind(
                ModMachineVariants.ELECTROLYZER.id()).stream()
                .filter(variant -> ModMachineVariants.isOpening(variant.id()))
                .toList();
        assertEquals(3, variants.size());
        for (int index = 0; index < variants.size(); index++) {
            MachineVariant variant = variants.get(index);
            TierProfile tier = variant.tierBand();
            long nominal = 32L << (index * 2);
            assertEquals(nominal / 2L, tier.inputMinimum());
            assertEquals(nominal, tier.inputNominal());
            assertEquals(nominal * 2L, tier.inputMaximum());
            assertEquals(nominal * 2L, tier.energyCapacity());
            assertEquals(1 << index, tier.parallelLimit());
            assertEquals(EnergyType.ELECTRIC, tier.energyType());
            assertEquals(
                    MachineKindSpec.OverclockPolicy.STANDARD,
                    variant.kind().overclockPolicy());
            assertTrue(variant.kind().parallelDuration());
            assertEquals(
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    variant.runtimeSpec().energy().mode());
            assertEquals(tier.energyCapacity(),
                    variant.runtimeSpec().energy().capacity());
            assertEquals(tier.inputMaximum(),
                    variant.runtimeSpec().energy().maxPacket());

            for (long input : List.of(
                    tier.inputMinimum(),
                    tier.inputNominal(),
                    tier.inputMaximum())) {
                MachineExecutionPlan plan = MachineExecutionPlan.create(
                        recipe(input, 40),
                        variant.kind(),
                        tier,
                        1).orElseThrow();
                assertEquals(input, plan.minimumPower());
                assertEquals(
                        Math.max(input, tier.inputNominal()),
                        plan.nominalPower());
            }
            assertEquals(
                    tier.parallelLimit(),
                    MachineExecutionPlan.create(
                            recipe(tier.inputMinimum(), 40),
                            variant.kind(),
                            tier,
                            tier.parallelLimit())
                            .orElseThrow()
                            .operations());
        }
        assertTrue(ModMachineVariants.SELECTED_HEAT_VARIANTS.stream().noneMatch(
                        variant -> variant.kind().id().equals(
                                ModProcessingMachines.MIXER.id())),
                "Heat selected set must not include mixer");
    }

    @Test
    void heatFailureStatesRemainObservableInTheSharedRuntime() {
        MachineVariant hu = ModMachineVariants.require(
                ResourceLocation.parse("cruciblecraft:distillery"));
        MachineVariant eu = ModMachineVariants.require(
                ResourceLocation.parse("cruciblecraft:electrolyzer"));
        for (MachineVariant variant : List.of(hu, eu)) {
            ProcessingRuntime runtime = new ProcessingRuntime();
            assertEquals(
                    ProcessingRuntime.Result.UNDERPOWERED,
                    runtime.tick(
                            "test:heat",
                            20,
                            true,
                            true,
                            false,
                            variant.runtimeSpec().buffering()));
            assertEquals("underpowered", runtime.status());
            assertEquals(
                    ProcessingRuntime.Result.OUTPUT_BLOCKED,
                    runtime.tick(
                            "test:heat",
                            20,
                            true,
                            false,
                            true,
                            variant.runtimeSpec().buffering()));
            assertEquals("output_blocked", runtime.status());
            runtime.overcharged();
            assertEquals("overcharged", runtime.status());
            assertFalse(MachineExecutionPlan.create(
                    recipe(
                            variant.tierBand().inputMaximum() + 1L,
                            20),
                    variant.kind(),
                    variant.tierBand(),
                    1).isPresent());
            runtime.recipePowerExceeded("test:heat", 20);
            assertEquals("recipe_power_exceeded", runtime.status());
        }
    }

    @Test
    void variantProjectionDoesNotMutateAnyGtRecipeRow() {
        Map<ResourceLocation, List<ResourceLocation>> entriesBefore =
                new LinkedHashMap<>();
        Map<ResourceLocation, Long> revisionsBefore =
                new LinkedHashMap<>();
        ModRecipeMaps.ALL.forEach(map -> {
            entriesBefore.put(
                    map.id(),
                    map.entries().stream()
                            .map(com.masson.cruciblecraft.recipe.gt
                                    .RecipeMap.Entry::id)
                            .toList());
            revisionsBefore.put(map.id(), map.revision());
        });

        ModMachineVariants.SELECTED_HEAT_VARIANTS.forEach(variant ->
                assertSame(
                        variant.kind().requireRecipeMap(),
                        variant.runtimeSpec().requireRecipeMap()));

        Map<ResourceLocation, List<ResourceLocation>> entriesAfter =
                new LinkedHashMap<>();
        Map<ResourceLocation, Long> revisionsAfter =
                new LinkedHashMap<>();
        ModRecipeMaps.ALL.forEach(map -> {
            entriesAfter.put(
                    map.id(),
                    map.entries().stream()
                            .map(com.masson.cruciblecraft.recipe.gt
                                    .RecipeMap.Entry::id)
                            .toList());
            revisionsAfter.put(map.id(), map.revision());
        });
        assertEquals(entriesBefore, entriesAfter);
        assertEquals(revisionsBefore, revisionsAfter);
    }

    private static GTRecipe recipe(long eut, int duration) {
        return new GTRecipe(
                List.of(Ingredient.of(Items.COBBLESTONE)),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(new ItemStack(Items.GRAVEL)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                duration,
                eut,
                0L,
                true,
                Optional.empty());
    }
}
