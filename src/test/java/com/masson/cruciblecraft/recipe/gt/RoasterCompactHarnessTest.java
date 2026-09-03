package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.util.ArrayList;
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

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoasterCompactHarnessTest {
    private static final ResourceLocation T36_COAL_DUST_BOOTSTRAP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "machine/bootstrap/roaster/coal_dust_bootstrap");

    private static RegistryAccess registries;
    private static List<JsonObject> generatedFamilies;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        generatedFamilies = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies(
                CompactGTRecipeFamilyGeneratedSupport.roasterGeneratedRoot());
    }

    @Test
    void allSeventyThreeRelationsMatchConsumeAndRejectLikeTheLiveMatcher() {
        assertEquals(29, generatedFamilies.size());
        RecipeMap map = new RecipeMap(ModRecipeMaps.ROASTER.id());
        List<CompactRecipeFamilySource> sources = generatedFamilies.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(
                                CompactGTRecipeFamilyGeneratedSupport.roasterGeneratedRoot(),
                                document,
                                registries))
                .toList();
        var snapshot = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        map.prepareRecipes(List.of(), List.of(snapshot), 1L).publish();

        assertEquals(73, snapshot.logicalRecipeCount());
        assertEquals(73, snapshot.eagerRecipeCount());
        Set<ResourceLocation> stableIds = new HashSet<>(snapshot.recipeIds());
        assertEquals(73, stableIds.size());
        for (ResourceLocation id : snapshot.recipeIds()) {
            assertTrue(id.getPath().startsWith("roaster/compact/"), id::toString);
        }
        assertTrue(
                !stableIds.contains(T36_COAL_DUST_BOOTSTRAP),
                "machine-bootstrap coal_dust_bootstrap must not appear in the compact roaster-compact snapshot");

        int relationCount = 0;
        for (JsonObject document : generatedFamilies) {
            java.util.List<JsonObject> relations =
                    CompactGTRecipeFamilyGeneratedSupport.authoredRelationJsons(document);
            for (int index = 0; index < relations.size(); index++) {
                relationCount++;
                JsonObject relationJson = relations.get(index);
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
                GTRecipeQuery missingQuery = query(recipe, missingConsume);
                Optional<RecipeMap.Match> missing = map.findMatch(missingQuery);
                assertTrue(
                        missing.isEmpty() || !missing.orElseThrow().id().equals(stableId),
                        () -> stableId + " still matched after a consume stack was omitted");

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
        assertEquals(73, relationCount);
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
