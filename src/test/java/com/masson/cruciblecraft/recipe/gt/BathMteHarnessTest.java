package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.content.item.BathMteIdentityCatalog;
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

class BathMteHarnessTest {
    static {
        MinecraftTestBootstrap.bootstrap();
    }

    private static final int LOCKED_FAMILIES = 803;
    private static final int LOCKED_RELATIONS = 1517;
    private static final ResourceLocation BATH_GROUP =
            ResourceLocation.parse("cruciblecraft:bath/mte");

    private static RegistryAccess registries;
    private static List<JsonObject> generatedFamilies;
    private static List<CompactRecipeFamilySource> sources;
    private static Map<ResourceLocation, CompactGTRecipeFamilyDefinition.Relation>
            liveRelations;
    private static Map<String, JsonObject> pinnedSourceRelations;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path root = CompactGTRecipeFamilyGeneratedSupport.bathMteGeneratedRoot();
        Assumptions.assumeTrue(
                CompactGTRecipeFamilyGeneratedSupport.hasGeneratedFamiliesRecursive(root),
                () -> "Bath/mte generated compact families are not available at " + root);
        generatedFamilies =
                CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamiliesRecursive(root);
        sources = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedSourcesRecursive(
                root, registries);
        liveRelations = new HashMap<>();
        for (CompactRecipeFamilySource source : sources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.definition().relations()) {
                assertEquals(
                        null,
                        liveRelations.put(relation.stableId(), relation),
                        () -> "duplicate live relation " + relation.stableId());
            }
        }
        assertEquals(LOCKED_RELATIONS, liveRelations.size());
        pinnedSourceRelations = loadPinnedSourceRelations();
    }

    @Test
    void isolatedGameTestEmptyStructureIsBundled() {
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_wave_bath_mte/structure/empty.nbt")));
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_wave_bath_mte/gametest/structure/empty.nbt")));
    }

    @Test
    void allLockedRelationsMatchConsumeAndRejectLikeTheLiveMatcher() {
        assertEquals(LOCKED_FAMILIES, generatedFamilies.size());
        for (JsonObject document : generatedFamilies) {
            assertFalse(document.has("parameterized"), () -> document.get("family_id").getAsString());
        }
        RecipeMap bath = new RecipeMap(ModRecipeMaps.BATH.id());
        PublicationGroupKey bathKey = new PublicationGroupKey(
                ModRecipeMaps.BATH.id(), BATH_GROUP);
        var snapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources,
                Map.of(ModRecipeMaps.BATH.id(), bath),
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                Map.of(
                        bathKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        assertEquals(LOCKED_RELATIONS, snapshots.get(bathKey).logicalRecipeCount());
        bath.prepareRecipes(List.of(), List.of(snapshots.get(bathKey)), 1L).publish();

        Set<ResourceLocation> stableIds = new HashSet<>();
        stableIds.addAll(snapshots.get(bathKey).recipeIds());
        assertEquals(LOCKED_RELATIONS, stableIds.size());
        for (ResourceLocation id : stableIds) {
            assertTrue(id.getPath().startsWith("bath/mte/"), id::toString);
        }

        int relationCount = 0;
        for (JsonObject document : generatedFamilies) {
            RecipeMap map = bath;
            JsonArray relations = document.getAsJsonArray("relations");
            for (int index = 0; index < relations.size(); index++) {
                relationCount++;
                JsonObject relationJson = relations.get(index).getAsJsonObject();
                ResourceLocation stableId = ResourceLocation.parse(
                        relationJson.get("stable_id").getAsString());
                GTRecipe recipe = map.entry(stableId).orElseThrow().recipe();
                CompactGTRecipeFamilyDefinition.Relation liveRelation =
                        liveRelations.get(stableId);
                JsonObject sourceRelation = pinnedSourceRelations.get(stableId.toString());
                assertTrue(
                        liveRelation != null,
                        () -> stableId + " is missing from live compact relations");
                assertTrue(
                        sourceRelation != null,
                        () -> stableId + " is missing from the pinned source pack");
                assertEquals(
                        sourceRelation.get("shadow_order").getAsInt(),
                        relationJson.get("shadow_order").getAsInt(),
                        () -> stableId + " shadow_order drifted from pinned source pack");
                assertEquals(
                        relationJson.get("shadow_order").getAsInt(),
                        liveRelation.shadowOrder(),
                        () -> stableId + " shadow_order drifted from generated JSON");
                assertSourceMteMeta(stableId, sourceRelation, relationJson, recipe);

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
                assertFluidStacks(
                        stableId,
                        relationJson.getAsJsonArray("fluid_inputs"),
                        recipe.fluidInputs());
                assertEquals(
                        relationJson.getAsJsonArray("fluid_outputs").size(),
                        recipe.fluidOutputs().size(),
                        stableId::toString);
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
    void generatedRelationsRouteSelectivelyWithinPublicationGroups() throws IOException {
        assertEquals(LOCKED_FAMILIES, generatedFamilies.size());
        RecipeMap bath = new RecipeMap(ModRecipeMaps.BATH.id());
        PublicationGroupKey bathKey = new PublicationGroupKey(
                ModRecipeMaps.BATH.id(), BATH_GROUP);
        var snapshots = CompactRecipeFamilyProvider.prepareByPublicationGroup(
                sources,
                Map.of(ModRecipeMaps.BATH.id(), bath),
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                Map.of(
                        bathKey,
                        CompactRecipeFamilyProvider.MaterializationPolicy.immediate()));
        bath.prepareRecipes(List.of(), List.of(snapshots.get(bathKey)), 1L).publish();
        List<CompactGTRecipeFamilyDefinition.Relation> relations = new ArrayList<>();
        for (CompactRecipeFamilySource source : sources) {
            relations.addAll(source.definition().relations());
        }
        assertEquals(LOCKED_RELATIONS, relations.size());
        assertShardRouting(bath, relations, BATH_GROUP, ModRecipeMaps.BATH.id());
    }

    private static void assertShardRouting(
            RecipeMap map,
            List<CompactGTRecipeFamilyDefinition.Relation> relations,
            ResourceLocation publicationGroup,
            ResourceLocation mapId) throws IOException {
        CompactRecipeShardRouter router = new CompactRecipeShardRouter(
                mapId, publicationGroup, relations);
        assertEquals(0, router.overflowCount(), () -> publicationGroup + " overflow");
        JsonObject manifest = JsonParser.parseString(
                Files.readString(
                        Path.of("tools/t46_shard_manifest.json"),
                        StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonObject group = manifest.getAsJsonArray("groups")
                .get(0).getAsJsonObject();
        assertEquals(
                LOCKED_RELATIONS,
                group.get("shard_count").getAsInt(),
                "frozen compact-shard-v1 manifest is not 1517");
        assertEquals(0, group.get("overflow_count").getAsInt());
        assertEquals(
                group.get("shard_count").getAsInt(),
                router.shardCount(),
                () -> publicationGroup + " live shard count drifted from the 1517 pair manifest");
        JsonObject manifestRelations = group.getAsJsonObject("relations");
        assertEquals(LOCKED_RELATIONS, manifestRelations.size());
        for (CompactGTRecipeFamilyDefinition.Relation relation : relations) {
            String stable = relation.stableId().toString();
            String expectedShard = manifestRelations.getAsJsonObject(stable)
                    .get("shard_id").getAsString();
            assertEquals(
                    expectedShard,
                    router.shardId(relation.stableId()).orElseThrow(),
                    () -> stable + " live shard id drifted from the frozen manifest");
        }
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

    private static Map<String, JsonObject> loadPinnedSourceRelations() throws IOException {
        Path path = Path.of("tools/t46_bath_source.json");
        JsonObject document = JsonParser.parseString(
                Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        Map<String, JsonObject> byId = new HashMap<>();
        for (JsonElement element : document.getAsJsonArray("relations")) {
            JsonObject relation = element.getAsJsonObject();
            String stableId = relation.get("stable_id").getAsString();
            assertEquals(
                    null,
                    byId.put(stableId, relation),
                    () -> "duplicate pinned source relation " + stableId);
        }
        assertEquals(LOCKED_RELATIONS, byId.size());
        return byId;
    }

    private static void assertSourceMteMeta(
            ResourceLocation stableId,
            JsonObject sourceRelation,
            JsonObject generatedRelation,
            GTRecipe recipe) {
        List<Integer> sourceMteMeta = new ArrayList<>();
        JsonArray sourceInputs = sourceRelation.getAsJsonArray("item_inputs");
        JsonArray generatedInputs = generatedRelation.getAsJsonArray("item_inputs");
        assertEquals(sourceInputs.size(), generatedInputs.size(), stableId::toString);
        for (int index = 0; index < sourceInputs.size(); index++) {
            JsonObject sourceStack = sourceInputs.get(index).getAsJsonObject();
            if (!sourceStack.has("source") || sourceStack.get("source").isJsonNull()) {
                continue;
            }
            JsonObject source = sourceStack.getAsJsonObject("source");
            if (!source.has("meta") || source.get("meta").isJsonNull()) {
                continue;
            }
            int meta = source.get("meta").getAsInt();
            String sourceItem = source.get("item").getAsString();
            sourceMteMeta.add(meta);
            ResourceLocation expected = runtimeIdForMeta(sourceItem, meta);
            String generatedId = generatedInputs.get(index).getAsJsonObject()
                    .get("item").getAsString();
            assertEquals(
                    expected.toString(),
                    generatedId,
                    () -> stableId + " source_mte_meta " + meta
                            + " drifted from catalog to generated JSON");
            ItemStack[] options = recipe.itemInputs().get(index).getItems();
            assertTrue(options.length > 0, () -> stableId + " item input has no sample");
            assertEquals(
                    expected,
                    BuiltInRegistries.ITEM.getKey(options[0].getItem()),
                    () -> stableId + " source_mte_meta " + meta
                            + " drifted from catalog to live GTRecipe");
        }
        JsonArray sourceOutputs = sourceRelation.getAsJsonArray("item_outputs");
        JsonArray generatedOutputs = generatedRelation.getAsJsonArray("item_outputs");
        assertEquals(sourceOutputs.size(), generatedOutputs.size(), stableId::toString);
        for (int index = 0; index < sourceOutputs.size(); index++) {
            JsonObject sourceStack = sourceOutputs.get(index).getAsJsonObject();
            if (!sourceStack.has("source") || sourceStack.get("source").isJsonNull()) {
                continue;
            }
            JsonObject source = sourceStack.getAsJsonObject("source");
            if (!source.has("meta") || source.get("meta").isJsonNull()) {
                continue;
            }
            int meta = source.get("meta").getAsInt();
            String sourceItem = source.get("item").getAsString();
            sourceMteMeta.add(meta);
            ResourceLocation expected = runtimeIdForMeta(sourceItem, meta);
            String generatedId = generatedOutputs.get(index).getAsJsonObject()
                    .get("id").getAsString();
            assertEquals(
                    expected.toString(),
                    generatedId,
                    () -> stableId + " source_mte_meta " + meta
                            + " output drifted from catalog to generated JSON");
            ItemStack actual = recipe.itemOutputs().get(index);
            assertEquals(
                    expected,
                    BuiltInRegistries.ITEM.getKey(actual.getItem()),
                    () -> stableId + " source_mte_meta " + meta
                            + " output drifted from catalog to live GTRecipe");
        }
        assertFalse(
                sourceMteMeta.isEmpty(),
                () -> stableId + " source_mte_meta is missing from the pinned source pack");
    }

    private static ResourceLocation runtimeIdForMeta(String sourceItem, int meta) {
        for (BathMteIdentityCatalog.Identity identity : BathMteIdentityCatalog.identities()) {
            if (identity.sourceItem().equals(sourceItem) && identity.meta() == meta) {
                return identity.id();
            }
        }
        throw new AssertionError("unmapped source_mte_meta " + sourceItem + "#" + meta);
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
}
