package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.content.item.BathRemainderFluidCatalog;
import com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * bath-remainder Bath remainder compact-family harness. Mirrors
 * {@link BathMteHarnessTest} for namespace {@code t47}.
 * Locked equivalence fields: {@code shadow_order}, selected source recipe.
 */
class BathRemainderHarnessTest {
    static {
        MinecraftTestBootstrap.bootstrap();
    }

    private static final int LOCKED_FAMILIES = 395;
    private static final int LOCKED_RELATIONS = 13708;
    private static final int EXACT_RELATIONS = 189;
    private static final int EXACT_MULTI_RELATIONS = 13519;
    private static final ResourceLocation BATH_EXACT_GROUP =
            CompactPublicationGroups.BATH_REMAINDER_EXACT;
    private static final ResourceLocation BATH_EXACT_MULTI_GROUP =
            CompactPublicationGroups.BATH_REMAINDER_EXACT_MULTI;

    private static RegistryAccess registries;
    private static boolean generatedAvailable;
    private static List<JsonObject> generatedFamilies;
    private static List<CompactRecipeFamilySource> sources;
    private static Map<ResourceLocation, CompactGTRecipeFamilyDefinition.Relation>
            liveRelations;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        PathRoot root = PathRoot.current();
        generatedAvailable =
                CompactGTRecipeFamilyGeneratedSupport.hasGeneratedFamiliesRecursive(root.path);
        if (!generatedAvailable) {
            return;
        }
        generatedFamilies =
                CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamiliesRecursive(root.path);
        sources = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedSourcesRecursive(
                root.path, registries);
        liveRelations = new HashMap<>();
        for (CompactRecipeFamilySource source : sources) {
            ResourceLocation group = source.definition().resolvedPublicationGroup();
            assertTrue(
                    BATH_EXACT_GROUP.equals(group) || BATH_EXACT_MULTI_GROUP.equals(group),
                    () -> "unexpected bath-remainder publication group " + group);
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.authoredRelations()) {
                assertEquals(
                        null,
                        liveRelations.put(relation.stableId(), relation),
                        () -> "duplicate live relation " + relation.stableId());
            }
        }
        assertEquals(LOCKED_FAMILIES, sources.size());
        assertEquals(LOCKED_RELATIONS, liveRelations.size());
    }

    @Test
    void lockedCatalogCounts() {
        assertEquals(283, BathRemainderBlockObjectCatalog.VARIANT_COUNT);
        assertEquals(283, BathRemainderBlockObjectCatalog.variants().size());
        assertEquals(3, BathRemainderFluidCatalog.FLUID_COUNT);
        assertEquals(3, BathRemainderFluidCatalog.fluids().size());
    }

    @Test
    void isolatedGameTestEmptyStructureIsBundled() {
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_wave_bath_remainder/structure/empty.nbt")));
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_wave_bath_remainder/gametest/structure/empty.nbt")));
    }

    @Test
    void allLockedRelationsMatchConsumeAndRejectLikeTheLiveMatcher() {
        assumeGenerated();
        for (JsonObject document : generatedFamilies) {
            assertFalse(
                    document.has("parameterized"),
                    () -> document.get("family_id").getAsString());
        }
        RecipeMap bath = new RecipeMap(ModRecipeMaps.BATH.id());
        PublicationGroupKey exactKey = exactKey();
        PublicationGroupKey multiKey = multiKey();
        var snapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources,
                Map.of(ModRecipeMaps.BATH.id(), bath),
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                immediatePolicies());
        assertEquals(EXACT_RELATIONS, snapshots.get(exactKey).logicalRecipeCount());
        assertEquals(EXACT_MULTI_RELATIONS, snapshots.get(multiKey).logicalRecipeCount());
        bath.prepareRecipes(
                List.of(),
                List.of(snapshots.get(exactKey), snapshots.get(multiKey)),
                1L).publish();

        Set<ResourceLocation> stableIds = new HashSet<>();
        stableIds.addAll(snapshots.get(exactKey).recipeIds());
        stableIds.addAll(snapshots.get(multiKey).recipeIds());
        assertEquals(LOCKED_RELATIONS, stableIds.size());
        for (ResourceLocation id : stableIds) {
            assertTrue(id.getPath().startsWith("bath/remainder/"), id::toString);
        }

        int relationCount = 0;
        for (JsonObject document : generatedFamilies) {
            java.util.List<JsonObject> relations =
                    CompactGTRecipeFamilyGeneratedSupport.authoredRelationJsons(document);
            for (int index = 0; index < relations.size(); index++) {
                relationCount++;
                JsonObject relationJson = relations.get(index);
                ResourceLocation stableId = ResourceLocation.parse(
                        relationJson.get("stable_id").getAsString());
                GTRecipe recipe = bath.entry(stableId).orElseThrow().recipe();
                CompactGTRecipeFamilyDefinition.Relation liveRelation =
                        liveRelations.get(stableId);
                assertTrue(
                        liveRelation != null,
                        () -> stableId + " is missing from live compact relations");
                assertLockedEquivalenceFields(stableId, relationJson, liveRelation);

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
                assertFluidStacks(
                        stableId,
                        relationJson.getAsJsonArray("fluid_inputs"),
                        recipe.fluidInputs());
                assertFluidStacks(
                        stableId,
                        relationJson.getAsJsonArray("fluid_outputs"),
                        recipe.fluidOutputs());
                JsonArray itemInputs = relationJson.getAsJsonArray("item_inputs");
                JsonArray itemCounts = relationJson.getAsJsonArray("item_input_counts");
                JsonArray itemActions = relationJson.getAsJsonArray("item_input_actions");
                assertEquals(itemInputs.size(), recipe.itemInputs().size(), stableId::toString);
                assertEquals(itemCounts.size(), recipe.itemInputCounts().size(), stableId::toString);
                assertEquals(
                        itemActions.size(),
                        recipe.itemInputActions().size(),
                        stableId::toString);
                for (int inputIndex = 0; inputIndex < itemInputs.size(); inputIndex++) {
                    String expectedId = itemInputs.get(inputIndex).getAsJsonObject()
                            .get("item").getAsString();
                    ItemStack[] options = recipe.itemInputs().get(inputIndex).getItems();
                    assertTrue(options.length > 0, () -> stableId + " item input has no sample");
                    assertEquals(
                            ResourceLocation.parse(expectedId),
                            BuiltInRegistries.ITEM.getKey(options[0].getItem()),
                            () -> stableId + " item input identity drifted");
                    assertEquals(
                            itemCounts.get(inputIndex).getAsInt(),
                            recipe.itemInputCounts().get(inputIndex).intValue(),
                            () -> stableId + " item input count drifted");
                    assertEquals(
                            itemActions.get(inputIndex).getAsJsonObject()
                                    .get("kind").getAsString().toUpperCase(Locale.ROOT),
                            recipe.itemInputActions().get(inputIndex).kind().name(),
                            () -> stableId + " item input action drifted");
                }
                JsonArray itemOutputs = relationJson.getAsJsonArray("item_outputs");
                assertEquals(
                        itemOutputs.size(),
                        recipe.itemOutputs().size(),
                        stableId::toString);
                for (int outputIndex = 0; outputIndex < itemOutputs.size(); outputIndex++) {
                    JsonObject expected = itemOutputs.get(outputIndex).getAsJsonObject();
                    ItemStack actual = recipe.itemOutputs().get(outputIndex);
                    assertEquals(
                            ResourceLocation.parse(expected.get("id").getAsString()),
                            BuiltInRegistries.ITEM.getKey(actual.getItem()),
                            () -> stableId + " item output identity drifted");
                    assertEquals(
                            expected.get("count").getAsInt(),
                            actual.getCount(),
                            () -> stableId + " item output count drifted");
                }
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
                Optional<RecipeMap.Match> exactMatch = bath.findMatch(exactQuery);
                assertTrue(
                        exactMatch.isPresent(),
                        () -> stableId + " exact consume query missed: empty");
                assertTrue(
                        exactMatch.orElseThrow().recipe().matches(exactQuery),
                        () -> stableId + " findMatch returned a non-matching recipe");

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
                    Optional<RecipeMap.Match> missing = bath.findMatch(missingQuery);
                    assertTrue(
                            missing.isEmpty()
                                    || !missing.orElseThrow().id().equals(stableId),
                            () -> stableId
                                    + " still matched after a consume stack was omitted");
                }

                List<ItemStack> extraJunk = new ArrayList<>(exact);
                extraJunk.add(new ItemStack(Items.DIRT));
                GTRecipeQuery extraQuery = query(recipe, extraJunk);
                Optional<RecipeMap.Match> extra = bath.findMatch(extraQuery);
                if (recipe.matches(extraQuery)) {
                    assertTrue(
                            extra.isPresent(),
                            () -> stableId
                                    + " matcher accepted extra unused items, so findMatch must succeed");
                } else {
                    assertTrue(
                            extra.isEmpty() || !extra.orElseThrow().id().equals(stableId),
                            () -> stableId + " matcher rejected extras but findMatch still returned it");
                }
            }
        }
        assertEquals(LOCKED_RELATIONS, liveRelations.size());
        assertEquals(LOCKED_RELATIONS, relationCount);
        assertEquals(
                LOCKED_RELATIONS,
                snapshots.get(exactKey).logicalRecipeCount()
                        + snapshots.get(multiKey).logicalRecipeCount());
    }

    @Test
    void compactConsumeInputSignaturesAreUniqueWithinEachHost() {
        assumeGenerated();
        Map<ResourceLocation, Map<String, List<ResourceLocation>>> byHost = new HashMap<>();
        for (CompactRecipeFamilySource source : sources) {
            ResourceLocation host = source.definition().targetMap();
            Map<String, List<ResourceLocation>> grouped =
                    byHost.computeIfAbsent(host, ignored -> new HashMap<>());
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.authoredRelations()) {
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
        assumeGenerated();
        RecipeMap bath = new RecipeMap(ModRecipeMaps.BATH.id());
        PublicationGroupKey exactKey = exactKey();
        PublicationGroupKey multiKey = multiKey();
        var snapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources,
                Map.of(ModRecipeMaps.BATH.id(), bath),
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                immediatePolicies());
        bath.prepareRecipes(
                List.of(),
                List.of(snapshots.get(exactKey), snapshots.get(multiKey)),
                1L).publish();
        List<CompactGTRecipeFamilyDefinition.Relation> exactRelations = new ArrayList<>();
        List<CompactGTRecipeFamilyDefinition.Relation> multiRelations = new ArrayList<>();
        for (CompactRecipeFamilySource source : sources) {
            if (BATH_EXACT_GROUP.equals(source.definition().resolvedPublicationGroup())) {
                exactRelations.addAll(source.authoredRelations());
            } else {
                multiRelations.addAll(source.authoredRelations());
            }
        }
        assertEquals(EXACT_RELATIONS, exactRelations.size());
        assertEquals(EXACT_MULTI_RELATIONS, multiRelations.size());
        assertShardRouting(
                bath, exactRelations, BATH_EXACT_GROUP, ModRecipeMaps.BATH.id());
        assertShardRouting(
                bath, multiRelations, BATH_EXACT_MULTI_GROUP, ModRecipeMaps.BATH.id());
    }

    private static void assumeGenerated() {
        Assumptions.assumeTrue(
                generatedAvailable,
                () -> "bath-remainder generated compact families are not available at "
                        + PathRoot.current().path);
    }

    private static PublicationGroupKey exactKey() {
        return new PublicationGroupKey(ModRecipeMaps.BATH.id(), BATH_EXACT_GROUP);
    }

    private static PublicationGroupKey multiKey() {
        return new PublicationGroupKey(ModRecipeMaps.BATH.id(), BATH_EXACT_MULTI_GROUP);
    }

    private static Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy>
            immediatePolicies() {
        return Map.of(
                exactKey(),
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate(),
                multiKey(),
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
    }

    private static void assertShardRouting(
            RecipeMap map,
            List<CompactGTRecipeFamilyDefinition.Relation> relations,
            ResourceLocation publicationGroup,
            ResourceLocation mapId) {
        CompactRecipeShardRouter router = new CompactRecipeShardRouter(
                mapId, publicationGroup, relations);
        assertEquals(0, router.overflowCount(), publicationGroup::toString);
        assertEquals(
                relations.size(),
                router.shardCount(),
                () -> publicationGroup + " shard_count");
        assertTrue(
                router.overflowCount() <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                () -> publicationGroup + " overflow exceeded the hard ceiling: "
                        + router.overflowCount());
        for (CompactGTRecipeFamilyDefinition.Relation relation : relations) {
            ResourceLocation stableId = relation.stableId();
            String shard = router.shardId(stableId).orElseThrow();
            GTRecipe recipe = map.entry(stableId).orElseThrow().recipe();
            GTRecipeQuery forwardQuery = query(recipe, offeredStacks(recipe));
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
        GTRecipeQuery dirtOnly = GTRecipeQuery.items(new ItemStack(Items.DIRT));
        Set<String> dirtShards = router.routedShardIds(dirtOnly);
        boolean overflowOnly = router.shardCount() == 1 && router.overflowCount() > 0;
        if (!dirtShards.isEmpty() && !overflowOnly) {
            assertTrue(
                    dirtShards.size() < router.shardCount(),
                    () -> publicationGroup + " dirt-only query scanned every shard");
        }
    }

    private static void assertLockedEquivalenceFields(
            ResourceLocation stableId,
            JsonObject relationJson,
            CompactGTRecipeFamilyDefinition.Relation liveRelation) {
        assertTrue(
                relationJson.has("shadow_order"),
                () -> stableId + " missing locked field shadow_order");
        assertEquals(
                relationJson.get("shadow_order").getAsInt(),
                liveRelation.shadowOrder(),
                () -> stableId + " shadow_order drifted from generated JSON");
        JsonObject provenance = relationJson.has("provenance")
                && relationJson.get("provenance").isJsonObject()
                ? relationJson.getAsJsonObject("provenance")
                : new JsonObject();
        boolean hasSource = provenance.has("selected_source_recipe")
                && !provenance.get("selected_source_recipe").getAsString().isBlank();
        assertTrue(
                hasSource,
                () -> stableId + " missing selected_source_recipe");
    }

    private static void assertFluidStacks(
            ResourceLocation stableId,
            JsonArray expected,
            List<FluidStack> actual) {
        assertEquals(expected.size(), actual.size(), stableId::toString);
        for (int index = 0; index < actual.size(); index++) {
            JsonObject stack = expected.get(index).getAsJsonObject();
            FluidStack live = actual.get(index);
            ResourceLocation expectedId = ResourceLocation.parse(stack.get("id").getAsString());
            ResourceLocation liveId = BuiltInRegistries.FLUID.getKey(live.getFluid());
            assertEquals(
                    expectedId,
                    liveId,
                    () -> stableId + " fluid identity drifted");
            assertEquals(
                    stack.get("amount").getAsInt(),
                    live.getAmount(),
                    () -> stableId + " fluid amount drifted");
            assertFalse(
                    liveId.equals(ResourceLocation.parse("minecraft:water"))
                            && expectedId.getNamespace().equals("cruciblecraft"),
                    () -> stableId + " collapsed a CC fluid into water");
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

    private record PathRoot(java.nio.file.Path path) {
        private static PathRoot current() {
            return new PathRoot(CompactGTRecipeFamilyGeneratedSupport.bathRemainderGeneratedRoot());
        }
    }
}
