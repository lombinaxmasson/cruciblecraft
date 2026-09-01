package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockObjectHarnessTest {
    static {
        MinecraftTestBootstrap.bootstrap();
    }

    private static final int LOCKED_RELATIONS = 379;
    private static final int SMELTER_RELATIONS = 271;
    private static final int DRYING_RELATIONS = 108;
    private static final ResourceLocation SMELTER_GROUP =
            CompactPublicationGroups.SMELTER_BLOCK;
    private static final ResourceLocation DRYING_GROUP =
            CompactPublicationGroups.DRYING_BLOCK;

    private static RegistryAccess registries;
    private static List<JsonObject> generatedFamilies;
    private static List<CompactRecipeFamilySource> sources;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        List<Path> roots = CompactGTRecipeFamilyGeneratedSupport.blockObjectGeneratedRoots();
        Assumptions.assumeTrue(
                CompactGTRecipeFamilyGeneratedSupport.hasGeneratedFamiliesRecursive(roots),
                () -> "block-object generated compact families are not available at " + roots);
        generatedFamilies =
                CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamiliesRecursive(roots);
        sources = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedSourcesRecursive(
                roots, registries);
    }

    @Test
    void isolatedGameTestEmptyStructureIsBundled() {
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_wave_block_object/structure/empty.nbt")));
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_wave_block_object/gametest/structure/empty.nbt")));
    }

    @Test
    void allLockedRelationsMatchConsumeAndRejectLikeTheLiveMatcher() {
        assertEquals(LOCKED_RELATIONS, generatedFamilies.size());
        RecipeMap smelter = new RecipeMap(ModRecipeMaps.SMELTER.id());
        RecipeMap drying = new RecipeMap(ModRecipeMaps.DRYING.id());
        PublicationGroupKey smelterKey = new PublicationGroupKey(
                ModRecipeMaps.SMELTER.id(), SMELTER_GROUP);
        PublicationGroupKey dryingKey = new PublicationGroupKey(
                ModRecipeMaps.DRYING.id(), DRYING_GROUP);
        var snapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources,
                Map.of(
                        ModRecipeMaps.SMELTER.id(), smelter,
                        ModRecipeMaps.DRYING.id(), drying),
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                Map.of(
                        smelterKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                        dryingKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertEquals(SMELTER_RELATIONS, snapshots.get(smelterKey).logicalRecipeCount());
        assertEquals(DRYING_RELATIONS, snapshots.get(dryingKey).logicalRecipeCount());
        smelter.prepareRecipes(List.of(), List.of(snapshots.get(smelterKey)), 1L).publish();
        drying.prepareRecipes(List.of(), List.of(snapshots.get(dryingKey)), 1L).publish();

        Set<ResourceLocation> stableIds = new HashSet<>();
        stableIds.addAll(snapshots.get(smelterKey).recipeIds());
        stableIds.addAll(snapshots.get(dryingKey).recipeIds());
        assertEquals(LOCKED_RELATIONS, stableIds.size());
        for (ResourceLocation id : stableIds) {
            assertTrue(
                    CompactWaveRecipeIds.matchesAny(
                            id, CompactWaveRecipeIds.BLOCK_OBJECT_PREFIXES),
                    id::toString);
        }

        int relationCount = 0;
        for (JsonObject document : generatedFamilies) {
            String group = document.get("publication_group").getAsString();
            RecipeMap map = SMELTER_GROUP.toString().equals(group) ? smelter : drying;
            JsonArray relations = document.getAsJsonArray("relations");
            for (int index = 0; index < relations.size(); index++) {
                relationCount++;
                JsonObject relationJson = relations.get(index).getAsJsonObject();
                ResourceLocation stableId = ResourceLocation.parse(
                        relationJson.get("stable_id").getAsString());
                GTRecipe recipe = map.entry(stableId).orElseThrow().recipe();

                assertEquals(
                        relationJson.get("duration").getAsInt(),
                        recipe.duration(),
                        stableId::toString);
                assertEquals(
                        relationJson.get("eut").getAsLong(),
                        recipe.eut(),
                        stableId::toString);
                assertEquals(
                        relationJson.get("special_value").getAsLong(),
                        recipe.specialValue(),
                        stableId::toString);
                assertEquals(
                        relationJson.get("can_be_buffered").getAsBoolean(),
                        recipe.canBeBuffered(),
                        stableId::toString);
                assertEquals(
                        ints(relationJson.getAsJsonArray("output_chances")),
                        recipe.outputChances(),
                        stableId::toString);
                assertEquals(
                        relationJson.getAsJsonArray("fluid_inputs").size(),
                        recipe.fluidInputs().size(),
                        stableId::toString);
                assertEquals(
                        relationJson.getAsJsonArray("fluid_outputs").size(),
                        recipe.fluidOutputs().size(),
                        stableId::toString);
                assertEquals(
                        recipe.itemInputs().size(),
                        recipe.itemInputActions().size(),
                        stableId::toString);
                for (int inputIndex = 0; inputIndex < recipe.itemInputActions().size();
                        inputIndex++) {
                    ItemInputAction.Kind kind = recipe.itemInputActions()
                            .get(inputIndex).kind();
                    int count = recipe.itemInputCounts().get(inputIndex);
                    if (kind == ItemInputAction.Kind.PRESERVE) {
                        assertEquals(0, count, () -> stableId + " PRESERVE must use count 0");
                    } else if (kind == ItemInputAction.Kind.CONSUME) {
                        assertTrue(
                                count > 0,
                                () -> stableId + " CONSUME must use a positive count");
                    }
                }

                List<ItemStack> exact = offeredStacks(recipe);
                GTRecipeQuery exactQuery = query(recipe, exact);
                assertTrue(
                        recipe.matches(exactQuery),
                        () -> stableId + " does not match its own offered query");
                Optional<RecipeMap.Match> exactMatch = map.findMatch(exactQuery);
                assertTrue(
                        exactMatch.isPresent(),
                        () -> stableId + " exact consume query missed: empty");
                assertTrue(
                        exactMatch.orElseThrow().recipe().matches(exactQuery),
                        () -> stableId + " findMatch returned a non-matching recipe");
                List<ResourceLocation> matchingIds = map.entries().stream()
                        .filter(entry -> entry.recipe().matches(exactQuery))
                        .map(RecipeMap.Entry::id)
                        .toList();
                assertTrue(
                        matchingIds.contains(stableId),
                        () -> stableId + " is not among query matchers: " + matchingIds);
                if (matchingIds.size() == 1) {
                    assertEquals(stableId, exactMatch.orElseThrow().id(), stableId::toString);
                } else {
                    assertTrue(
                            matchingIds.contains(exactMatch.orElseThrow().id()),
                            () -> stableId + " findMatch tie-break left the matcher set: "
                                    + exactMatch.orElseThrow().id());
                }

                List<ItemStack> missingConsume = omitFirstConsume(recipe);
                GTRecipeQuery missingQuery;
                if (missingConsume.size() < recipe.itemInputs().size()) {
                    missingQuery = query(recipe, missingConsume);
                } else if (!recipe.fluidInputs().isEmpty()) {
                    missingQuery = new GTRecipeQuery(
                            offeredStacks(recipe),
                            recipe.fluidInputs().stream().skip(1)
                                    .map(FluidStack::copy)
                                    .toList());
                } else {
                    missingQuery = null;
                }
                if (missingQuery != null) {
                    Optional<RecipeMap.Match> missing = map.findMatch(missingQuery);
                    assertTrue(
                            missing.isEmpty()
                                    || !missing.orElseThrow().id().equals(stableId),
                            () -> stableId
                                    + " still matched after a consume stack was omitted");
                }

                List<ItemStack> extraJunk = new ArrayList<>(exact);
                extraJunk.add(new ItemStack(Items.DIRT));
                GTRecipeQuery extraQuery = query(recipe, extraJunk);
                Optional<RecipeMap.Match> extra = map.findMatch(extraQuery);
                if (recipe.matches(extraQuery)) {
                    assertTrue(
                            extra.isPresent(),
                            () -> stableId
                                    + " matcher accepted extra unused items, so findMatch must succeed");
                    assertTrue(
                            extra.orElseThrow().recipe().matches(extraQuery),
                            () -> stableId + " extra-junk match does not satisfy the query");
                } else {
                    assertTrue(
                            extra.isEmpty() || !extra.orElseThrow().id().equals(stableId),
                            () -> stableId + " matcher rejected extras but findMatch still returned it");
                }
            }
        }
        assertEquals(LOCKED_RELATIONS, relationCount);
    }

    @Test
    void compactConsumeInputSignaturesAreUniqueWithinEachHost() {
        Map<ResourceLocation, Map<String, List<ResourceLocation>>> byHost = new HashMap<>();
        for (CompactRecipeFamilySource source : sources) {
            ResourceLocation host = source.definition().targetMap();
            Map<String, List<ResourceLocation>> grouped =
                    byHost.computeIfAbsent(host, ignored -> new HashMap<>());
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.definition().relations()) {
                String signature = GTRecipeMapLoader.inputSignature(
                        relation.materialize());
                grouped.computeIfAbsent(signature, ignored -> new ArrayList<>())
                        .add(relation.stableId());
            }
        }
        int unique = 0;
        for (Map<String, List<ResourceLocation>> grouped : byHost.values()) {
            List<List<ResourceLocation>> collisions = grouped.values().stream()
                    .filter(stableIds -> stableIds.size() > 1)
                    .toList();
            assertEquals(0, collisions.size(), collisions::toString);
            unique += grouped.size();
        }
        assertEquals(LOCKED_RELATIONS, unique);
    }

    @Test
    void generatedRelationsRouteSelectivelyWithinPublicationGroups() {
        assertEquals(LOCKED_RELATIONS, generatedFamilies.size());
        RecipeMap smelter = new RecipeMap(ModRecipeMaps.SMELTER.id());
        RecipeMap drying = new RecipeMap(ModRecipeMaps.DRYING.id());
        PublicationGroupKey smelterKey = new PublicationGroupKey(
                ModRecipeMaps.SMELTER.id(), SMELTER_GROUP);
        PublicationGroupKey dryingKey = new PublicationGroupKey(
                ModRecipeMaps.DRYING.id(), DRYING_GROUP);
        var snapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources,
                Map.of(
                        ModRecipeMaps.SMELTER.id(), smelter,
                        ModRecipeMaps.DRYING.id(), drying),
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                Map.of(
                        smelterKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                        dryingKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        smelter.prepareRecipes(List.of(), List.of(snapshots.get(smelterKey)), 1L).publish();
        drying.prepareRecipes(List.of(), List.of(snapshots.get(dryingKey)), 1L).publish();
        List<CompactGTRecipeFamilyDefinition.Relation> smelterRelations = new ArrayList<>();
        List<CompactGTRecipeFamilyDefinition.Relation> dryingRelations = new ArrayList<>();
        for (CompactRecipeFamilySource source : sources) {
            if (SMELTER_GROUP.equals(source.definition().resolvedPublicationGroup())) {
                smelterRelations.addAll(source.definition().relations());
            } else {
                dryingRelations.addAll(source.definition().relations());
            }
        }
        assertEquals(SMELTER_RELATIONS, smelterRelations.size());
        assertEquals(DRYING_RELATIONS, dryingRelations.size());
        assertShardRouting(smelter, smelterRelations, SMELTER_GROUP, ModRecipeMaps.SMELTER.id());
        assertShardRouting(drying, dryingRelations, DRYING_GROUP, ModRecipeMaps.DRYING.id());
    }

    private static void assertShardRouting(
            RecipeMap map,
            List<CompactGTRecipeFamilyDefinition.Relation> relations,
            ResourceLocation publicationGroup,
            ResourceLocation mapId) {
        CompactRecipeShardRouter router = new CompactRecipeShardRouter(
                mapId, publicationGroup, relations);
        assertTrue(
                router.overflowCount() <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                () -> publicationGroup + " overflow exceeded the hard ceiling: "
                        + router.overflowCount());

        for (CompactGTRecipeFamilyDefinition.Relation relation : relations) {
            ResourceLocation stableId = relation.stableId();
            String shard = router.shardId(stableId).orElseThrow();

            GTRecipe recipe = map.entry(stableId).orElseThrow().recipe();
            List<ItemStack> stacks = offeredStacks(recipe);
            GTRecipeQuery forwardQuery = query(recipe, stacks);
            assertTrue(
                    router.routedShardIds(forwardQuery).contains(shard),
                    () -> stableId + " forward query missed its shard");
            assertTrue(
                    router.routedCandidates(forwardQuery).stream()
                            .anyMatch(candidate -> candidate.stableId().equals(stableId)),
                    () -> stableId + " forward query missed routed candidate");
            assertTrue(
                    router.indexedCandidateCount(forwardQuery)
                            <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                    () -> stableId + " indexed too many candidates");
        }

        GTRecipeQuery dirtOnly =
                GTRecipeQuery.items(new ItemStack(Items.DIRT));
        Set<String> dirtShards = router.routedShardIds(dirtOnly);
        boolean overflowOnly = router.shardCount() == 1 && router.overflowCount() > 0;
        if (!dirtShards.isEmpty() && !overflowOnly) {
            assertTrue(
                    dirtShards.size() < router.shardCount(),
                    () -> publicationGroup + " dirt-only query scanned every shard");
        }
    }

    private static GTRecipeQuery query(GTRecipe recipe, List<ItemStack> stacks) {
        List<FluidStack> fluids = recipe.fluidInputs().stream()
                .map(FluidStack::copy)
                .toList();
        return new GTRecipeQuery(stacks, fluids);
    }

    private static List<ItemStack> offeredStacks(GTRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            stacks.add(sample(recipe, index));
        }
        return stacks;
    }

    private static List<ItemStack> omitFirstConsume(GTRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        boolean omitted = false;
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            if (recipe.itemInputActions().get(index).kind()
                    == ItemInputAction.Kind.CONSUME) {
                if (!omitted) {
                    omitted = true;
                    continue;
                }
            }
            stacks.add(sample(recipe, index));
        }
        return stacks;
    }

    private static ItemStack sample(GTRecipe recipe, int index) {
        ItemStack[] items = recipe.itemInputs().get(index).getItems();
        assertTrue(items.length > 0, "ingredient produced no sample stacks");
        ItemStack stack = items[0].copy();
        stack.setCount(Math.max(1, recipe.itemInputCounts().get(index)));
        return stack;
    }

    private static List<Integer> ints(JsonArray array) {
        List<Integer> values = new ArrayList<>();
        array.forEach(element -> values.add(element.getAsInt()));
        return values;
    }
}
