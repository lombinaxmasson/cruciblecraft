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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactT39CentrifugeHarnessTest {
    private static RegistryAccess registries;
    private static List<JsonObject> generatedFamilies;
    private static List<CompactRecipeFamilySource> sources;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path root = CompactGTRecipeFamilyGeneratedSupport.t39GeneratedRoot();
        generatedFamilies = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies(root);
        sources = generatedFamilies.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(root, document, registries))
                .toList();
    }

    @Test
    void allLockedRelationsMatchConsumeAndRejectLikeTheLiveMatcher() {
        assertEquals(22, generatedFamilies.size());
        RecipeMap map = new RecipeMap(ModRecipeMaps.CENTRIFUGE.id());
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
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                Map.of(
                        singletonKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(19),
                        multiKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                13, CompactRecipeFamilyProvider.EagerSelector.NONE)));
        assertEquals(19, snapshots.get(singletonKey).logicalRecipeCount());
        assertEquals(13, snapshots.get(multiKey).logicalRecipeCount());
        map.prepareRecipes(List.of(), List.copyOf(snapshots.values()), 1L).publish();

        Set<ResourceLocation> stableIds = new HashSet<>();
        for (CompactRecipeFamilyProvider.Snapshot snapshot : snapshots.values()) {
            stableIds.addAll(snapshot.recipeIds());
        }
        assertEquals(32, stableIds.size());
        for (ResourceLocation id : stableIds) {
            assertTrue(id.getPath().startsWith("t39/"), id::toString);
        }

        int relationCount = 0;
        for (JsonObject document : generatedFamilies) {
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

                List<ItemStack> wrongCircuit = wrongCircuitStacks(recipe);
                if (wrongCircuit != null) {
                    GTRecipeQuery wrongQuery = query(recipe, wrongCircuit);
                    Optional<RecipeMap.Match> wrong = map.findMatch(wrongQuery);
                    assertTrue(
                            wrong.isEmpty() || !wrong.orElseThrow().id().equals(stableId),
                            () -> stableId + " still matched a wrong circuit_config");
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
        assertEquals(32, relationCount);
    }

    @Test
    void compactConsumeInputSignaturesAreUniqueAcrossLockedRelations() {
        Map<String, List<ResourceLocation>> grouped = new HashMap<>();
        for (CompactRecipeFamilySource source : sources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.definition().relations()) {
                String signature = GTRecipeMapLoader.inputSignature(
                        relation.materialize());
                grouped.computeIfAbsent(signature, ignored -> new ArrayList<>())
                        .add(relation.stableId());
            }
        }
        List<List<ResourceLocation>> collisions = grouped.values().stream()
                .filter(stableIds -> stableIds.size() > 1)
                .toList();
        assertEquals(0, collisions.size());
        assertEquals(32, grouped.size());
        for (List<ResourceLocation> stableIds : collisions) {
            assertEquals(2, stableIds.size());
            assertEquals(2, new HashSet<>(stableIds).size());
        }
    }

    @Test
    void generatedRelationsRouteSelectivelyWithinPublicationGroups() {
        assertEquals(22, generatedFamilies.size());
        RecipeMap map = new RecipeMap(ModRecipeMaps.CENTRIFUGE.id());
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
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                Map.of(
                        singletonKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.onDemand(19),
                        multiKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                                13, CompactRecipeFamilyProvider.EagerSelector.NONE)));
        map.prepareRecipes(List.of(), List.copyOf(snapshots.values()), 1L).publish();

        List<CompactGTRecipeFamilyDefinition.Relation> singletonRelations =
                relationsForGroup(
                        CompactGTRecipeFamilyDefinition
                                .T39_CENTRIFUGE_SINGLETON_PUBLICATION_GROUP);
        List<CompactGTRecipeFamilyDefinition.Relation> multiRelations =
                relationsForGroup(
                        CompactGTRecipeFamilyDefinition
                                .T39_CENTRIFUGE_MULTI_PUBLICATION_GROUP);
        assertEquals(19, singletonRelations.size());
        assertEquals(13, multiRelations.size());

        assertShardRouting(
                map,
                singletonRelations,
                CompactGTRecipeFamilyDefinition
                        .T39_CENTRIFUGE_SINGLETON_PUBLICATION_GROUP);
        assertShardRouting(
                map,
                multiRelations,
                CompactGTRecipeFamilyDefinition
                        .T39_CENTRIFUGE_MULTI_PUBLICATION_GROUP);
    }

    private static void assertShardRouting(
            RecipeMap map,
            List<CompactGTRecipeFamilyDefinition.Relation> relations,
            ResourceLocation publicationGroup) {
        CompactRecipeShardRouter router = new CompactRecipeShardRouter(
                ModRecipeMaps.CENTRIFUGE.id(), publicationGroup, relations);
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
        if (!dirtShards.isEmpty()) {
            assertTrue(
                    dirtShards.size() < router.shardCount(),
                    () -> publicationGroup + " dirt-only query scanned every shard");
        }
    }

    private static List<CompactGTRecipeFamilyDefinition.Relation> relationsForGroup(
            ResourceLocation publicationGroup) {
        List<CompactGTRecipeFamilyDefinition.Relation> relations = new ArrayList<>();
        for (CompactRecipeFamilySource source : sources) {
            if (source.definition().resolvedPublicationGroup().equals(publicationGroup)) {
                relations.addAll(source.definition().relations());
            }
        }
        return relations;
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

    private static List<ItemStack> wrongCircuitStacks(GTRecipe recipe) {
        boolean hasCircuit = false;
        List<ItemStack> stacks = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            ItemStack stack = sample(recipe, index);
            if (recipe.itemInputActions().get(index).kind()
                    == ItemInputAction.Kind.PRESERVE) {
                hasCircuit = true;
                stack.set(
                        DataComponents.CUSTOM_NAME,
                        Component.literal("circuit:wrong"));
            }
            stacks.add(stack);
        }
        return hasCircuit ? stacks : null;
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
