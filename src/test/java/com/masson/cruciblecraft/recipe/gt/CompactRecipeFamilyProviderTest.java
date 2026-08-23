package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactRecipeFamilyProviderTest {
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void codecRoundTripsExactRelationContract() {
        CompactGTRecipeFamilyDefinition original = assemblerFamily(
                "gt.recipe.assembler#0002",
                List.of(itemRelation(
                        "t37/assembler_row",
                        Ingredient.of(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_NUGGET),
                        0)));
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var encoded = CompactGTRecipeFamilyDefinition.CODEC
                .encodeStart(ops, original)
                .getOrThrow();
        CompactGTRecipeFamilyDefinition decoded =
                CompactGTRecipeFamilyDefinition.CODEC.parse(ops, encoded).getOrThrow();

        assertEquals(original.familyId(), decoded.familyId());
        assertEquals(original.targetMap(), decoded.targetMap());
        assertEquals(original.sourceRevision(), decoded.sourceRevision());
        assertEquals(1, decoded.relations().size());
        assertEquals(
                original.relations().getFirst().stableId(),
                decoded.relations().getFirst().stableId());
        assertSameRecipe(
                original.relations().getFirst().materialize(),
                decoded.relations().getFirst().materialize());
        assertTrue(decoded.parameterized().isEmpty());
    }

    @Test
    void unknownTargetMapFailsClosedBeforeSnapshot() {
        RecipeMap assembler = ModRecipeMaps.ASSEMBLER;
        CompactRecipeFamilySource source = new CompactRecipeFamilySource(
                id("authored/unknown_map"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.missing#0001",
                        id("missing_map"),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(itemRelation(
                                "t37/missing",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0))));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepareByTarget(
                        List.of(source),
                        Map.of(assembler.id(), assembler),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertTrue(thrown.getMessage().contains("Unknown compact family target map"));
        assertTrue(thrown.getMessage().contains("missing_map"));
    }

    @Test
    void assemblerAndSyntheticSecondMapAreHostNeutral() {
        RecipeMap assembler = ModRecipeMaps.ASSEMBLER;
        RecipeMap synthetic = new RecipeMap(id("synthetic_second"));
        CompactRecipeFamilySource assemblerSource = new CompactRecipeFamilySource(
                id("authored/assembler"),
                assemblerFamily(
                        "gt.recipe.assembler#0002",
                        List.of(itemRelation(
                                "t37/assembler_iron",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0))));
        CompactRecipeFamilySource syntheticSource = new CompactRecipeFamilySource(
                id("authored/synthetic"),
                new CompactGTRecipeFamilyDefinition(
                        "synthetic.family#0001",
                        synthetic.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(fluidRelation(
                                "t37/synthetic_water",
                                new FluidStack(Fluids.WATER, 250),
                                new ItemStack(Items.GLASS_BOTTLE),
                                0))));
        Map<ResourceLocation, RecipeMap> known = new HashMap<>();
        known.put(assembler.id(), assembler);
        known.put(synthetic.id(), synthetic);

        var snapshots = CompactRecipeFamilyProvider.prepareByTarget(
                List.of(assemblerSource, syntheticSource),
                known,
                9L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());

        assertEquals(2, snapshots.size());
        assertTrue(snapshots.containsKey(assembler.id()));
        assertTrue(snapshots.containsKey(synthetic.id()));
        assertEquals(
                CompactRecipeFamilyProvider.familyId(assembler.id()),
                snapshots.get(assembler.id()).familyId());
        assertNotEquals(
                snapshots.get(assembler.id()).familyId(),
                snapshots.get(synthetic.id()).familyId());
        assertEquals(
                id("t37/assembler_iron"),
                snapshots.get(assembler.id()).recipeIds().getFirst());
        assertEquals(
                id("t37/synthetic_water"),
                snapshots.get(synthetic.id()).recipeIds().getFirst());
    }

    @Test
    void concreteRowsAndCompactFamiliesCoexist() {
        RecipeMap map = new RecipeMap(id("coexist"));
        RecipeMap.Entry concrete = new RecipeMap.Entry(
                id("concrete/cobble"),
                new GTRecipe(
                        List.of(Ingredient.of(Items.COBBLESTONE)),
                        List.of(1),
                        List.of(new ItemStack(Items.STONE)),
                        List.of(),
                        List.of(),
                        List.of(GTRecipe.GUARANTEED_CHANCE),
                        20,
                        8L,
                        0L));
        var family = prepare(
                map,
                List.of(source(
                        map,
                        "coexist.family#0001",
                        itemRelation(
                                "t37/coexist_iron",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0))),
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                3L);
        map.prepareRecipes(List.of(concrete), List.of(family), 3L).publish();

        assertEquals(
                id("concrete/cobble"),
                map.findMatch(GTRecipeQuery.items(new ItemStack(Items.COBBLESTONE)))
                        .orElseThrow()
                        .id());
        assertEquals(
                id("t37/coexist_iron"),
                map.findMatch(GTRecipeQuery.items(new ItemStack(Items.IRON_INGOT)))
                        .orElseThrow()
                        .id());
        assertEquals(2, map.entries().size());
    }

    @Test
    void epochPublishIsAtomicAndStaleViewsFail() {
        RecipeMap map = new RecipeMap(id("epoch"));
        var first = prepare(
                map,
                List.of(source(
                        map,
                        "epoch.family#0001",
                        itemRelation(
                                "t37/epoch_a",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0))),
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                1L);
        map.prepareRecipes(List.of(), List.of(first), 1L).publish();
        List<RecipeMap.Entry> stale = map.entries();
        assertEquals(1, stale.size());

        var second = prepare(
                map,
                List.of(source(
                        map,
                        "epoch.family#0001",
                        itemRelation(
                                "t37/epoch_b",
                                Ingredient.of(Items.GOLD_INGOT),
                                new ItemStack(Items.GOLD_NUGGET),
                                0))),
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                2L);
        map.prepareRecipes(List.of(), List.of(second), 2L).publish();
        assertThrows(IllegalStateException.class, stale::size);
        assertEquals(
                id("t37/epoch_b"),
                map.entry(id("t37/epoch_b")).orElseThrow().id());
        assertFalse(map.entry(id("t37/epoch_a")).isPresent());
    }

    @Test
    void enumerationDoesNotGrowOnDemandCache() {
        RecipeMap map = new RecipeMap(id("enum_cache"));
        var snapshot = prepare(
                map,
                List.of(source(
                        map,
                        "enum.family#0001",
                        itemRelation(
                                "t37/enum_a",
                                Ingredient.of(Items.IRON_INGOT),
                                new ItemStack(Items.IRON_NUGGET),
                                0),
                        itemRelation(
                                "t37/enum_b",
                                Ingredient.of(Items.GOLD_INGOT),
                                new ItemStack(Items.GOLD_NUGGET),
                                1),
                        itemRelation(
                                "t37/enum_c",
                                Ingredient.of(Items.COPPER_INGOT),
                                new ItemStack(Items.COPPER_BLOCK),
                                2))),
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(8));
        assertEquals(0, snapshot.eagerRecipeCount());
        assertEquals(3, snapshot.lazyRecipeCount());
        int cacheBefore = snapshot.cacheSize();
        for (int index = 0; index < snapshot.logicalRecipeCount(); index++) {
            assertEquals(
                    snapshot.recipeIds().get(index),
                    snapshot.enumerationEntry(index).id());
        }
        assertEquals(cacheBefore, snapshot.cacheSize());
        assertEquals(0, snapshot.cacheSize());
    }

    @Test
    void immediateAndOnDemandMaterializeTheSameRecipe() {
        RecipeMap map = new RecipeMap(id("strategy"));
        List<CompactRecipeFamilySource> sources = List.of(source(
                map,
                "strategy.family#0001",
                itemRelation(
                        "t37/strategy_row",
                        Ingredient.of(Items.IRON_INGOT),
                        new ItemStack(Items.IRON_NUGGET),
                        0)));
        var immediate = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                4L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        var onDemand = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                4L,
                CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(4));
        var hybrid = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                4L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                        4, (index, relation) -> index == 0));

        ResourceLocation stableId = id("t37/strategy_row");
        GTRecipe expected = immediate.entry(stableId).orElseThrow().recipe();
        assertSameRecipe(expected, onDemand.entry(stableId).orElseThrow().recipe());
        assertSameRecipe(expected, hybrid.entry(stableId).orElseThrow().recipe());
        assertEquals(immediate.recipeIds(), onDemand.recipeIds());
        assertEquals(immediate.stableFingerprint(), onDemand.stableFingerprint());
        assertEquals(immediate.stableFingerprint(), hybrid.stableFingerprint());
        assertEquals(1, immediate.eagerRecipeCount());
        assertEquals(0, onDemand.eagerRecipeCount());
        assertEquals(1, hybrid.eagerRecipeCount());
    }

    @Test
    void integratedClientRefusesToBuild() {
        RecipeMap map = new RecipeMap(id("integrated"));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepare(
                        map,
                        List.of(),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertTrue(thrown.getMessage().contains("Integrated clients"));
    }

    @Test
    void supportsActionsFluidsChanceAndUnindexedFallback() {
        RecipeMap map = new RecipeMap(id("coverage"));
        ItemStack named = new ItemStack(Items.STICK);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("catalyst"));
        CompactGTRecipeFamilyDefinition.Relation wearAndChance =
                new CompactGTRecipeFamilyDefinition.Relation(
                        id("t37/coverage_multi"),
                        List.of(
                                Ingredient.of(Items.IRON_INGOT),
                                Ingredient.of(Items.IRON_PICKAXE),
                                Ingredient.of(ItemTags.PLANKS),
                                DataComponentIngredient.of(false, named)),
                        List.of(2, 0, 1, 0),
                        List.of(
                                ItemInputAction.CONSUME,
                                ItemInputAction.wear(1),
                                ItemInputAction.CONSUME,
                                ItemInputAction.PRESERVE),
                        List.of(
                                new ItemStack(Items.IRON_NUGGET, 3),
                                new ItemStack(Items.REDSTONE)),
                        List.of(new FluidStack(Fluids.WATER, 500)),
                        List.of(new FluidStack(Fluids.LAVA, 50)),
                        List.of(GTRecipe.GUARANTEED_CHANCE, 2500),
                        64,
                        32L,
                        7L,
                        true,
                        3,
                        provenance("gt.recipe.coverage#0001"));
        var snapshot = prepare(
                map,
                List.of(source(map, "coverage.family#0001", wearAndChance)),
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(2));

        RecipeMap.Entry byId = snapshot.entry(id("t37/coverage_multi")).orElseThrow();
        assertEquals(2, byId.recipe().itemOutputs().size());
        assertEquals(2500, byId.recipe().outputChances().get(1));
        assertEquals(ItemInputAction.Kind.WEAR, byId.recipe().itemInputActions().get(1).kind());
        assertEquals(1, snapshot.unindexedRelationCount());
        RecipeMap.Entry byIdAgain = snapshot.entry(id("t37/coverage_multi")).orElseThrow();
        assertEquals(id("t37/coverage_multi"), byIdAgain.id());
        assertEquals(0, snapshot.cacheSize());
        snapshot.findLazy(new GTRecipeQuery(
                List.of(
                        new ItemStack(Items.IRON_INGOT, 2),
                        new ItemStack(Items.IRON_PICKAXE),
                        new ItemStack(Items.OAK_PLANKS),
                        named.copy()),
                List.of(new FluidStack(Fluids.WATER, 500))));
        assertTrue(snapshot.cacheSize() <= snapshot.cacheCeiling());
    }

    @Test
    void parameterizedDefinitionIsRejected() {
        RecipeMap map = new RecipeMap(id("parameterized"));
        CompactGTRecipeFamilyDefinition definition = new CompactGTRecipeFamilyDefinition(
                "future.bath#0001",
                map.id(),
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                List.of(),
                Optional.of(new CompactGTRecipeFamilyDefinition.ParameterizedSpec(
                        "bath_template",
                        Map.of("axis", "material"))));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepare(
                        map,
                        List.of(new CompactRecipeFamilySource(
                                id("authored/parameterized"), definition)),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertTrue(thrown.getMessage().contains("not implemented"));
        assertTrue(thrown.getMessage().contains("bath_template"));
    }

    @Test
    void jsonContractParsesPinnedAssemblerShape() {
        String json = """
                {
                  "family_id": "gt.recipe.assembler#0002",
                  "target_map": "cruciblecraft:assembler",
                  "source_revision": "3703e40308c8c030763fd6297dea8b210d2a77b1",
                  "relations": [
                    {
                      "stable_id": "cruciblecraft:t37/contract_row",
                      "item_inputs": [{"item": "minecraft:iron_ingot"}],
                      "item_input_counts": [1],
                      "item_input_actions": [{"kind": "consume"}],
                      "item_outputs": [{"id": "minecraft:iron_nugget", "count": 1}],
                      "fluid_inputs": [],
                      "fluid_outputs": [],
                      "output_chances": [10000],
                      "duration": 32,
                      "eut": 16,
                      "special_value": 0,
                      "can_be_buffered": true,
                      "shadow_order": 0,
                      "provenance": {
                        "source_kind": "SOURCE_BACKED",
                        "selected_source_recipe": "gt.recipe.assembler#0002",
                        "evidence_hashes": ["abc"]
                      }
                    }
                  ]
                }
                """;
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        CompactGTRecipeFamilyDefinition decoded =
                CompactGTRecipeFamilyDefinition.CODEC
                        .parse(ops, JsonParser.parseString(json))
                        .getOrThrow();
        assertEquals("gt.recipe.assembler#0002", decoded.familyId());
        assertEquals(ModRecipeMaps.ASSEMBLER.id(), decoded.targetMap());
        assertEquals(32, decoded.relations().getFirst().duration());
        assertEquals(16L, decoded.relations().getFirst().eut());
        assertEquals(
                ItemInputAction.CONSUME,
                decoded.relations().getFirst().itemInputActions().getFirst());
    }

    @Test
    void fiftySingletonRowsMaterializeUnderFrozenT37Policies() {
        int[] durations = {16, 32, 96, 48, 64, 64, 128, 64, 32};
        List<String> familyIds = new ArrayList<>();
        List<CompactRecipeFamilySource> sources = new ArrayList<>();
        RecipeMap map = new RecipeMap(id("t37_workload"));
        for (int number = 2; number <= 51; number++) {
            String familyId = "gt.recipe.assembler#%04d".formatted(number);
            familyIds.add(familyId);
            int duration = durations[(number - 2) % durations.length];
            sources.add(source(
                    map,
                    familyId,
                    new CompactGTRecipeFamilyDefinition.Relation(
                            id("t37/row_" + number),
                            List.of(Ingredient.of(Items.IRON_INGOT)),
                            List.of(1),
                            List.of(ItemInputAction.CONSUME),
                            List.of(new ItemStack(Items.IRON_NUGGET)),
                            List.of(),
                            List.of(),
                            List.of(GTRecipe.GUARANTEED_CHANCE),
                            duration,
                            16L,
                            0L,
                            true,
                            number - 2,
                            provenance(familyId))));
        }
        Set<String> firstTen = new HashSet<>(familyIds.subList(0, 10));
        CompactRecipeFamilyProvider.EagerSelector hybridSelector =
                (index, relation) -> relation.duration() <= 16
                        || firstTen.contains(
                                relation.provenance().selectedSourceRecipe().orElse(""));

        var immediate = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                7L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        var onDemand = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                7L,
                CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(8));
        var hybrid = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                7L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(8, hybridSelector));

        assertEquals(50, immediate.logicalRecipeCount());
        assertEquals(50, immediate.eagerRecipeCount());
        assertEquals(0, immediate.lazyRecipeCount());
        assertEquals(0, immediate.cacheCeiling());
        assertEquals(0, onDemand.eagerRecipeCount());
        assertEquals(50, onDemand.lazyRecipeCount());
        assertEquals(8, onDemand.cacheCeiling());
        assertEquals(14, hybrid.eagerRecipeCount());
        assertEquals(36, hybrid.lazyRecipeCount());
        assertEquals(8, hybrid.cacheCeiling());
        assertEquals(immediate.recipeIds(), onDemand.recipeIds());
        assertEquals(immediate.stableFingerprint(), onDemand.stableFingerprint());
        assertEquals(immediate.stableFingerprint(), hybrid.stableFingerprint());

        int cacheBefore = onDemand.cacheSize();
        for (int index = 0; index < onDemand.logicalRecipeCount(); index++) {
            assertEquals(
                    onDemand.recipeIds().get(index),
                    onDemand.enumerationEntry(index).id());
        }
        assertEquals(cacheBefore, onDemand.cacheSize());
        assertTrue(onDemand.cacheSize() <= onDemand.cacheCeiling());
        Optional<RecipeMap.Entry> lookedUp = onDemand.findLazy(
                GTRecipeQuery.items(new ItemStack(Items.IRON_INGOT)));
        assertTrue(lookedUp.isPresent());
        assertTrue(onDemand.cacheSize() <= onDemand.cacheCeiling());
        assertTrue(hybrid.syncPayloadBytes() > 0L);
        assertEquals(
                CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                onDemand.side());
        IllegalArgumentException integrated = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepare(
                        map,
                        sources,
                        7L,
                        CompactRecipeFamilyProvider.RuntimeSide.INTEGRATED_CLIENT,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertTrue(integrated.getMessage().contains("Integrated clients"));
    }

    private static CompactRecipeFamilyProvider.Snapshot prepare(
            RecipeMap map,
            List<CompactRecipeFamilySource> sources,
            CompactRecipeFamilyProvider.MaterializationPolicy policy) {
        return prepare(map, sources, policy, 1L);
    }

    private static CompactRecipeFamilyProvider.Snapshot prepare(
            RecipeMap map,
            List<CompactRecipeFamilySource> sources,
            CompactRecipeFamilyProvider.MaterializationPolicy policy,
            long epoch) {
        return CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                epoch,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                policy);
    }

    private static CompactRecipeFamilySource source(
            RecipeMap map,
            String familyId,
            CompactGTRecipeFamilyDefinition.Relation... relations) {
        return new CompactRecipeFamilySource(
                id("authored/" + familyId.replace('#', '_')),
                new CompactGTRecipeFamilyDefinition(
                        familyId,
                        map.id(),
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(relations)));
    }

    private static CompactGTRecipeFamilyDefinition assemblerFamily(
            String familyId,
            List<CompactGTRecipeFamilyDefinition.Relation> relations) {
        return new CompactGTRecipeFamilyDefinition(
                familyId,
                ModRecipeMaps.ASSEMBLER.id(),
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                relations);
    }

    private static CompactGTRecipeFamilyDefinition.Relation itemRelation(
            String path,
            Ingredient input,
            ItemStack output,
            int shadowOrder) {
        return new CompactGTRecipeFamilyDefinition.Relation(
                id(path),
                List.of(input),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(output),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                32,
                16L,
                0L,
                true,
                shadowOrder,
                provenance("gt.recipe.assembler#0002"));
    }

    private static CompactGTRecipeFamilyDefinition.Relation fluidRelation(
            String path,
            FluidStack input,
            ItemStack output,
            int shadowOrder) {
        return new CompactGTRecipeFamilyDefinition.Relation(
                id(path),
                List.of(),
                List.of(),
                List.of(),
                List.of(output),
                List.of(input),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                8L,
                0L,
                true,
                shadowOrder,
                provenance("synthetic.family#0001"));
    }

    private static GTRecipeProvenance provenance(String selected) {
        return new GTRecipeProvenance(
                "SOURCE_BACKED",
                Optional.of(selected),
                List.of("abc"));
    }

    private static void assertSameRecipe(GTRecipe expected, GTRecipe actual) {
        assertEquals(expected.itemInputCounts(), actual.itemInputCounts());
        assertEquals(expected.itemInputActions(), actual.itemInputActions());
        assertEquals(expected.outputChances(), actual.outputChances());
        assertEquals(expected.duration(), actual.duration());
        assertEquals(expected.eut(), actual.eut());
        assertEquals(expected.specialValue(), actual.specialValue());
        assertEquals(expected.canBeBuffered(), actual.canBeBuffered());
        assertEquals(expected.provenance(), actual.provenance());
        assertEquals(expected.itemInputs().size(), actual.itemInputs().size());
        assertEquals(expected.itemOutputs().size(), actual.itemOutputs().size());
        for (int index = 0; index < expected.itemOutputs().size(); index++) {
            ItemStack left = expected.itemOutputs().get(index);
            ItemStack right = actual.itemOutputs().get(index);
            assertEquals(left.getItem(), right.getItem());
            assertEquals(left.getCount(), right.getCount());
        }
        assertEquals(expected.fluidInputs().size(), actual.fluidInputs().size());
        assertEquals(expected.fluidOutputs().size(), actual.fluidOutputs().size());
        for (int index = 0; index < expected.fluidInputs().size(); index++) {
            assertEquals(
                    expected.fluidInputs().get(index).getFluid(),
                    actual.fluidInputs().get(index).getFluid());
            assertEquals(
                    expected.fluidInputs().get(index).getAmount(),
                    actual.fluidInputs().get(index).getAmount());
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
