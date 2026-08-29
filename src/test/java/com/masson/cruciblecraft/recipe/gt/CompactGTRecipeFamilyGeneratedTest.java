package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactGTRecipeFamilyGeneratedTest {
    private static RegistryAccess registries;
    private static List<JsonObject> generatedFamilies;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        generatedFamilies = loadGeneratedFamilies();
    }

    @Test
    void generatedAssemblerFamiliesHaveFiftyUniqueStableIds() {
        assertEquals(50, generatedFamilies.size());
        Set<String> familyIds = new HashSet<>();
        Set<String> stableIds = new HashSet<>();
        for (JsonObject document : generatedFamilies) {
            assertFalse(document.has("parameterized"));
            assertEquals(
                    "cruciblecraft:compact_gt_recipe_family",
                    document.get("type").getAsString());
            assertTrue(familyIds.add(document.get("family_id").getAsString()));
            document.getAsJsonArray("relations").forEach(element -> {
                JsonObject relation = element.getAsJsonObject();
                assertTrue(stableIds.add(relation.get("stable_id").getAsString()));
            });
        }
        assertEquals(50, familyIds.size());
        assertEquals(50, stableIds.size());
        for (int number = 2; number <= 51; number++) {
            assertTrue(familyIds.contains("gt.recipe.assembler#%04d".formatted(number)));
        }
    }

    @Test
    void inMemoryRoasterFamilyDecodesEveryRelationAndFluids() {
        JsonObject document = JsonParser.parseString("""
                {
                  "family_id": "gt.recipe.roaster#0001",
                  "target_map": "cruciblecraft:roaster",
                  "source_revision": "t38-test",
                  "relations": [
                    {
                      "stable_id": "cruciblecraft:t38/roaster_water",
                      "item_inputs": [{"item": "minecraft:iron_ingot"}],
                      "item_input_counts": [1],
                      "item_input_actions": [{"kind": "consume"}],
                      "item_outputs": [{"id": "minecraft:iron_nugget", "count": 1}],
                      "fluid_inputs": [{"amount": 250, "id": "minecraft:water"}],
                      "fluid_outputs": [{"amount": 25, "id": "minecraft:lava"}],
                      "output_chances": [10000],
                      "duration": 40,
                      "eut": 16,
                      "special_value": 0,
                      "can_be_buffered": true,
                      "shadow_order": 0,
                      "provenance": {
                        "source_kind": "SOURCE_BACKED",
                        "selected_source_recipe": "gt.recipe.roaster#0001",
                        "evidence_hashes": ["first"]
                      }
                    },
                    {
                      "stable_id": "cruciblecraft:t38/roaster_second",
                      "item_inputs": [{"item": "minecraft:gold_ingot"}],
                      "item_input_counts": [2],
                      "item_input_actions": [{"kind": "consume"}],
                      "item_outputs": [{"id": "minecraft:gold_nugget", "count": 2}],
                      "fluid_inputs": [],
                      "fluid_outputs": [],
                      "output_chances": [10000],
                      "duration": 80,
                      "eut": 32,
                      "special_value": 1,
                      "can_be_buffered": false,
                      "shadow_order": 1,
                      "provenance": {
                        "source_kind": "SOURCE_BACKED",
                        "selected_source_recipe": "gt.recipe.roaster#0002",
                        "evidence_hashes": ["second"]
                      }
                    }
                  ]
                }
                """).getAsJsonObject();

        CompactRecipeFamilySource source =
                CompactGTRecipeFamilyGeneratedSupport.sourceFromGenerated(
                        CompactGTRecipeFamilyGeneratedSupport.t38GeneratedRoot(),
                        document,
                        registries);

        assertEquals(id("t38/roaster/gt_recipe_roaster_0001"), source.id());
        assertEquals(2, source.definition().relations().size());
        var first = source.definition().relations().getFirst();
        assertEquals(id("t38/roaster_water"), first.stableId());
        assertEquals(Fluids.WATER, first.fluidInputs().getFirst().getFluid());
        assertEquals(250, first.fluidInputs().getFirst().getAmount());
        assertEquals(Fluids.LAVA, first.fluidOutputs().getFirst().getFluid());
        assertEquals(25, first.fluidOutputs().getFirst().getAmount());
        assertEquals(
                id("t38/roaster_second"),
                source.definition().relations().get(1).stableId());
    }

    @Test
    void generatedRoasterFamiliesDecodeAllRelationsWhenPresent() throws IOException {
        Path root = CompactGTRecipeFamilyGeneratedSupport.t38GeneratedRoot();
        Assumptions.assumeTrue(
                Files.isDirectory(root),
                () -> "T38 generated files are not available at " + root);
        List<JsonObject> documents =
                CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies(root);
        assertEquals(29, documents.size());
        List<CompactRecipeFamilySource> sources = documents.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(root, document, registries))
                .toList();
        Set<ResourceLocation> stableIds = new HashSet<>();
        for (CompactRecipeFamilySource source : sources) {
            assertTrue(source.id().getPath().startsWith("t38/roaster/"));
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.definition().relations()) {
                assertTrue(stableIds.add(relation.stableId()), relation.stableId()::toString);
            }
        }
        assertEquals(73, stableIds.size());
    }

    @Test
    void productionCentrifugeFamiliesDecodeLockedRelationsWhenPresent() throws IOException {
        Path root = CompactGTRecipeFamilyGeneratedSupport.t39GeneratedRoot();
        Assumptions.assumeTrue(
                Files.isDirectory(root),
                () -> "T39 generated files are not available at " + root);
        List<JsonObject> documents =
                CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies(root);
        assertEquals(22, documents.size());
        List<CompactRecipeFamilySource> sources = documents.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(root, document, registries))
                .toList();
        Set<ResourceLocation> stableIds = new HashSet<>();
        int singletonCount = 0;
        int multiCount = 0;
        for (CompactRecipeFamilySource source : sources) {
            assertTrue(source.id().getPath().startsWith("t39/centrifuge/"));
            ResourceLocation group = source.definition().resolvedPublicationGroup();
            if (group.equals(CompactGTRecipeFamilyDefinition
                    .T39_CENTRIFUGE_SINGLETON_PUBLICATION_GROUP)) {
                singletonCount++;
                assertEquals(1, source.definition().relations().size());
            } else if (group.equals(CompactGTRecipeFamilyDefinition
                    .T39_CENTRIFUGE_MULTI_PUBLICATION_GROUP)) {
                multiCount++;
                assertTrue(source.definition().relations().size() > 1);
            } else {
                throw new AssertionError("unexpected T39 publication group " + group);
            }
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.definition().relations()) {
                assertTrue(stableIds.add(relation.stableId()), relation.stableId()::toString);
            }
        }
        assertEquals(19, singletonCount);
        assertEquals(3, multiCount);
        assertEquals(32, stableIds.size());

        PublicationGroupKey singletonKey = new PublicationGroupKey(
                ModRecipeMaps.CENTRIFUGE.id(),
                CompactGTRecipeFamilyDefinition
                        .T39_CENTRIFUGE_SINGLETON_PUBLICATION_GROUP);
        PublicationGroupKey multiKey = new PublicationGroupKey(
                ModRecipeMaps.CENTRIFUGE.id(),
                CompactGTRecipeFamilyDefinition
                        .T39_CENTRIFUGE_MULTI_PUBLICATION_GROUP);
        var snapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources,
                Map.of(ModRecipeMaps.CENTRIFUGE.id(), ModRecipeMaps.CENTRIFUGE),
                39L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                Map.of(
                        singletonKey,
                        CompactRecipeFamilyProvider.t39SingletonPolicy(sources),
                        multiKey,
                        CompactRecipeFamilyProvider.t39MultiPolicy(sources)));
        assertEquals(19, snapshots.get(singletonKey).logicalRecipeCount());
        assertEquals(13, snapshots.get(multiKey).logicalRecipeCount());
        assertEquals(0, snapshots.get(singletonKey).eagerRecipeCount());
        assertEquals(0, snapshots.get(multiKey).eagerRecipeCount());
    }

    @Test
    void withdrawnCatalogFixtureStillProvesRouterCapacity() throws IOException {
        Path root = CompactGTRecipeFamilyGeneratedSupport.t39CatalogFixtureRoot();
        List<JsonObject> documents =
                CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies(root);
        assertEquals(157, documents.size());
        List<CompactRecipeFamilySource> sources = documents.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(root, document, registries))
                .toList();
        List<CompactGTRecipeFamilyDefinition.Relation> singleton = new ArrayList<>();
        List<CompactGTRecipeFamilyDefinition.Relation> multi = new ArrayList<>();
        for (CompactRecipeFamilySource source : sources) {
            if (source.definition().resolvedPublicationGroup().equals(
                    CompactGTRecipeFamilyDefinition
                            .T39_CENTRIFUGE_SINGLETON_PUBLICATION_GROUP)) {
                singleton.addAll(source.definition().relations());
            } else {
                multi.addAll(source.definition().relations());
            }
        }
        assertEquals(123, singleton.size());
        assertEquals(127, multi.size());
        CompactRecipeShardRouter singletonRouter = new CompactRecipeShardRouter(
                ModRecipeMaps.CENTRIFUGE.id(),
                CompactGTRecipeFamilyDefinition
                        .T39_CENTRIFUGE_SINGLETON_PUBLICATION_GROUP,
                singleton);
        CompactRecipeShardRouter multiRouter = new CompactRecipeShardRouter(
                ModRecipeMaps.CENTRIFUGE.id(),
                CompactGTRecipeFamilyDefinition.T39_CENTRIFUGE_MULTI_PUBLICATION_GROUP,
                multi);
        assertTrue(singletonRouter.shardCount() > 0);
        assertTrue(multiRouter.shardCount() > 0);
        assertTrue(singletonRouter.overflowCount()
                <= CompactRecipeShardRouter.HARD_SHARD_CEILING);
        assertTrue(multiRouter.overflowCount()
                <= CompactRecipeShardRouter.HARD_SHARD_CEILING);
    }

    @Test
    void generatedAssemblerFamiliesMaterializeEquallyImmediateAndOnDemand() {
        RecipeMap map = ModRecipeMaps.ASSEMBLER;
        List<CompactRecipeFamilySource> sources = generatedFamilies.stream()
                .map(CompactGTRecipeFamilyGeneratedTest::sourceFromGenerated)
                .toList();
        var immediate = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                11L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        var onDemand = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                11L,
                CompactRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(8));
        var hybrid = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                11L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.t37ProductionPolicy(sources));

        assertEquals(50, immediate.logicalRecipeCount());
        assertEquals(50, immediate.eagerRecipeCount());
        assertEquals(0, immediate.lazyRecipeCount());
        assertEquals(0, onDemand.eagerRecipeCount());
        assertEquals(50, onDemand.lazyRecipeCount());
        assertEquals(14, hybrid.eagerRecipeCount());
        assertEquals(36, hybrid.lazyRecipeCount());
        assertEquals(8, hybrid.cacheCeiling());
        assertEquals(50, new HashSet<>(immediate.recipeIds()).size());
        assertEquals(immediate.recipeIds(), onDemand.recipeIds());
        assertEquals(immediate.stableFingerprint(), onDemand.stableFingerprint());
        for (ResourceLocation id : immediate.recipeIds()) {
            assertSameRecipe(
                    immediate.entry(id).orElseThrow().recipe(),
                    onDemand.entry(id).orElseThrow().recipe());
        }
    }

    static List<JsonObject> loadGeneratedFamilies() throws IOException {
        Path root = CompactGTRecipeFamilyGeneratedSupport.generatedRoot();
        assertTrue(Files.isDirectory(root), () -> "missing generated T37 root " + root);
        return CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies();
    }

    static CompactRecipeFamilySource sourceFromGenerated(JsonObject document) {
        return CompactGTRecipeFamilyGeneratedSupport.sourceFromGenerated(
                document, registries);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
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
    }
}
