package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class T16MachineTierMatrixTest {
    private static JsonObject expected;

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        expected = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(
                        T16MachineTierMatrixTest.class.getResourceAsStream(
                                "/t16_machine_tier_expected.json")),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void independentSourceExpectedMatchesCatalogAndRuntimeVariants() {
        Map<ResourceLocation, MachineTierCatalog.Entry> entries =
                new HashMap<>();
        MachineTierCatalog.entries().forEach(
                entry -> entries.put(entry.variantId(), entry));
        assertTrue(entries.size() > 33);
        assertTrue(ModMachineVariants.ALL.size() > 33);
        assertEquals(15, ModMachineVariants.T16_SELECTED.size());

        JsonArray allRows = new JsonArray();
        expected.getAsJsonArray("preserved_pre_t16")
                .forEach(allRows::add);
        expected.getAsJsonArray("t16a_selected")
                .forEach(allRows::add);
        expected.getAsJsonArray("t16b_selected")
                .forEach(allRows::add);
        assertEquals(24, allRows.size());
        for (var element : allRows) {
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
                    MachineKindSpec.OverclockPolicy.STANDARD,
                    entry.overclockPolicy());
            assertEquals(
                    row.get("parallel_duration").getAsBoolean(),
                    entry.parallelDuration());
            assertEquals(row.get("tier_band").getAsString(),
                    entry.tierBand().tierBandId().toString());
            assertEquals(row.get("material").getAsString(),
                    entry.tierBand().materialId());
            assertEquals(
                    EnergyType.valueOf(row.get("energy").getAsString()),
                    entry.tierBand().energyType());
            assertEquals(
                    nominal / 2L, entry.tierBand().inputMinimum());
            assertEquals(nominal, entry.tierBand().inputNominal());
            assertEquals(
                    nominal * 2L, entry.tierBand().inputMaximum());
            assertEquals(
                    nominal * 2L, entry.tierBand().energyCapacity());
            assertEquals(
                    row.get("parallel").getAsInt(),
                    entry.tierBand().parallelLimit());
            assertEquals(10_000, entry.tierBand().efficiency());

            assertSame(entry.tierBand(), variant.tierBand());
            assertEquals(entry.kindId(), variant.kind().id());
            assertEquals(
                    entry.overclockPolicy(),
                    variant.kind().overclockPolicy());
            assertEquals(
                    entry.parallelDuration(),
                    variant.kind().parallelDuration());
            assertSame(
                    variant.kind().behavior().items(),
                    variant.runtimeSpec().items());
            assertSame(
                    variant.kind().behavior().fluids(),
                    variant.runtimeSpec().fluids());
            assertSame(
                    variant.kind().behavior().validator(),
                    variant.runtimeSpec().validator());
            assertSame(
                    variant,
                    ModBlocks.configuredProcessingVariant(id));
        }
    }

    @Test
    void t16aAndT16bBlocksAndItemsCoverExactlyFifteenSelectedVariants() {
        Set<String> selected = new HashSet<>();
        expected.getAsJsonArray("t16a_selected").forEach(element ->
                selected.add(element.getAsJsonObject()
                        .get("id").getAsString()));
        expected.getAsJsonArray("t16b_selected").forEach(element ->
                selected.add(element.getAsJsonObject()
                        .get("id").getAsString()));
        assertEquals(15, selected.size());
        assertEquals(
                selected,
                ModMachineVariants.T16_SELECTED.stream()
                        .map(variant -> variant.id().toString())
                        .collect(java.util.stream.Collectors.toSet()));

        Set<String> registeredBlocks = new HashSet<>();
        ModBlocks.configuredProcessingBlockEntries().forEach(holder ->
                registeredBlocks.add(holder.getId().toString()));
        org.junit.jupiter.api.Assertions.assertTrue(
                registeredBlocks.containsAll(selected));

        Set<String> newBlocks = Set.of(
                ModBlocks.STEEL_LATHE.getId().toString(),
                ModBlocks.TITANIUM_LATHE.getId().toString(),
                ModBlocks.STEEL_ROLLINGMILL.getId().toString(),
                ModBlocks.TITANIUM_ROLLINGMILL.getId().toString(),
                ModBlocks.STEEL_WIREMILL.getId().toString(),
                ModBlocks.TITANIUM_WIREMILL.getId().toString(),
                ModBlocks.STEEL_SHREDDER.getId().toString(),
                ModBlocks.TITANIUM_SHREDDER.getId().toString(),
                ModBlocks.STEEL_PRESS.getId().toString(),
                ModBlocks.TITANIUM_PRESS.getId().toString());
        Set<String> newItems = Set.of(
                ModItems.STEEL_LATHE.getId().toString(),
                ModItems.TITANIUM_LATHE.getId().toString(),
                ModItems.STEEL_ROLLINGMILL.getId().toString(),
                ModItems.TITANIUM_ROLLINGMILL.getId().toString(),
                ModItems.STEEL_WIREMILL.getId().toString(),
                ModItems.TITANIUM_WIREMILL.getId().toString(),
                ModItems.STEEL_SHREDDER.getId().toString(),
                ModItems.TITANIUM_SHREDDER.getId().toString(),
                ModItems.STEEL_PRESS.getId().toString(),
                ModItems.TITANIUM_PRESS.getId().toString());
        assertEquals(10, newBlocks.size());
        assertEquals(newBlocks, newItems);
        assertTrue(selected.containsAll(newBlocks));
        assertSame(
                ModMachineVariants.require(ResourceLocation.parse(
                        "cruciblecraft:press")),
                ModBlocks.configuredProcessingVariant(
                        ResourceLocation.parse("cruciblecraft:press")));
    }

    @Test
    void selectedKindsUseRuWithoutChangingControllerProfile() {
        Set<ResourceLocation> selectedKinds = Set.of(
                ModMachineVariants.LATHE.id(),
                ModMachineVariants.ROLLINGMILL.id(),
                ModMachineVariants.WIREMILL.id(),
                ModMachineVariants.SHREDDER.id());
        for (ResourceLocation kindId : selectedKinds) {
            List<MachineVariant> variants =
                    ModMachineVariants.T16_SELECTED.stream()
                            .filter(variant -> variant.kind().id().equals(kindId))
                            .toList();
            assertEquals(3, variants.size());
            variants.forEach(variant -> {
                assertEquals(
                        EnergyType.KINETIC_ROTATION,
                        variant.kind().behavior().energy().type());
                assertEquals(
                        EnergyType.KINETIC_ROTATION,
                        variant.runtimeSpec().energy().type());
                assertEquals(
                        MachineKindSpec.OverclockPolicy.STANDARD,
                        variant.kind().overclockPolicy());
                assertEquals(false, variant.kind().parallelDuration());
            });
        }

        JsonObject controller =
                expected.getAsJsonObject("controller_profile");
        TierProfile profile =
                MachineTierCatalog.requireControllerTierBand(
                        ResourceLocation.parse(
                                controller.get("tier_band").getAsString()));
        assertEquals(controller.get("material").getAsString(),
                profile.materialId());
        assertEquals(
                EnergyType.valueOf(controller.get("energy").getAsString()),
                profile.energyType());
        assertEquals(controller.get("minimum").getAsLong(),
                profile.inputMinimum());
        assertEquals(controller.get("nominal").getAsLong(),
                profile.inputNominal());
        assertEquals(controller.get("maximum").getAsLong(),
                profile.inputMaximum());
        assertEquals(controller.get("capacity").getAsLong(),
                profile.energyCapacity());
        assertEquals(controller.get("parallel").getAsInt(),
                profile.parallelLimit());
        assertEquals(controller.get("efficiency").getAsInt(),
                profile.efficiency());
    }

    @Test
    void fiveSelectedKindsCoverEveryTierWindowParallelAndEnergyIdentity() {
        Map<ResourceLocation, EnergyType> selectedKinds = Map.of(
                ModMachineVariants.LATHE.id(),
                EnergyType.KINETIC_ROTATION,
                ModMachineVariants.ROLLINGMILL.id(),
                EnergyType.KINETIC_ROTATION,
                ModMachineVariants.WIREMILL.id(),
                EnergyType.KINETIC_ROTATION,
                ModMachineVariants.SHREDDER.id(),
                EnergyType.KINETIC_ROTATION,
                ModMachineVariants.PRESS.id(),
                EnergyType.KINETIC_PUSH);
        assertEquals(5, selectedKinds.size());

        for (var selected : selectedKinds.entrySet()) {
            List<MachineVariant> variants =
                    ModMachineVariants.T16_SELECTED.stream()
                            .filter(variant -> variant.kind().id().equals(
                                    selected.getKey()))
                            .toList();
            assertEquals(3, variants.size(), selected.getKey().toString());
            for (int tierIndex = 0; tierIndex < variants.size(); tierIndex++) {
                MachineVariant variant = variants.get(tierIndex);
                long nominal = 32L << (tierIndex * 2);
                int parallel = selected.getKey().equals(
                        ModMachineVariants.PRESS.id())
                                ? 4 << tierIndex
                                : 1;

                assertEquals(selected.getValue(),
                        variant.tierBand().energyType());
                assertEquals(nominal / 2L,
                        variant.tierBand().inputMinimum());
                assertEquals(nominal,
                        variant.tierBand().inputNominal());
                assertEquals(nominal * 2L,
                        variant.tierBand().inputMaximum());
                assertEquals(parallel,
                        variant.tierBand().parallelLimit());
                assertEquals(
                        MachineKindSpec.OverclockPolicy.STANDARD,
                        variant.kind().overclockPolicy());
                assertEquals(
                        selected.getKey().equals(
                                ModMachineVariants.PRESS.id()),
                        variant.kind().parallelDuration());
                assertEquals(selected.getValue(),
                        variant.runtimeSpec().energy().type());
            }
        }
    }

    @Test
    void representativeRuAndKuPlansAndFailureStatesStayDistinct() {
        MachineVariant ru = ModMachineVariants.require(
                ResourceLocation.parse("cruciblecraft:lathe"));
        MachineVariant ku = ModMachineVariants.require(
                ResourceLocation.parse("cruciblecraft:press"));
        GTRecipe recipe = recipe(4L, 100);

        MachineExecutionPlan ruPlan = MachineExecutionPlan.create(
                recipe, ru.kind(), ru.tierBand(), 1).orElseThrow();
        assertEquals(16L, ruPlan.minimumPower());
        assertEquals(32L, ruPlan.nominalPower());
        assertEquals(800L, ruPlan.totalWork());
        assertEquals(25, ruPlan.effectiveDuration());
        assertEquals(1, ruPlan.overclockSteps());

        MachineExecutionPlan kuPlan = MachineExecutionPlan.create(
                recipe,
                ku.kind(),
                ku.tierBand(),
                ku.tierBand().parallelLimit()).orElseThrow();
        assertEquals(16L, kuPlan.minimumPower());
        assertEquals(32L, kuPlan.nominalPower());
        assertEquals(3_200L, kuPlan.totalWork());
        assertEquals(100, kuPlan.effectiveDuration());
        assertEquals(4, kuPlan.operations());
        assertEquals(1, kuPlan.overclockSteps());

        for (MachineVariant variant : List.of(ru, ku)) {
            ProcessingRuntime runtime = new ProcessingRuntime();
            assertEquals(
                    ProcessingRuntime.Result.UNDERPOWERED,
                    runtime.tick(
                            "test:matrix",
                            20,
                            true,
                            true,
                            false,
                            variant.runtimeSpec().buffering()));
            assertEquals("underpowered", runtime.status());
            assertEquals(
                    ProcessingRuntime.Result.OUTPUT_BLOCKED,
                    runtime.tick(
                            "test:matrix",
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
            runtime.recipePowerExceeded("test:matrix", 20);
            assertEquals("recipe_power_exceeded", runtime.status());
        }
    }

    @Test
    void variantProjectionDoesNotPublishOrCreateTierSpecificRecipeMaps() {
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

        ModMachineVariants.ALL.forEach(variant ->
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

        Set<ResourceLocation> mapIds = entriesAfter.keySet();
        JsonArray selected = new JsonArray();
        expected.getAsJsonArray("t16a_selected").forEach(selected::add);
        expected.getAsJsonArray("t16b_selected").forEach(selected::add);
        selected.forEach(element -> {
            ResourceLocation variantId = ResourceLocation.parse(
                    element.getAsJsonObject().get("id").getAsString());
            if (variantId.getPath().startsWith("steel_")
                    || variantId.getPath().startsWith("titanium_")) {
                org.junit.jupiter.api.Assertions.assertFalse(
                        mapIds.contains(variantId),
                        variantId + " must reuse its kind RecipeMap");
            }
        });
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
