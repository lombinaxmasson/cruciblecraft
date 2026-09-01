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
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssemblerCompactHarnessTest {
    private static RegistryAccess registries;
    private static List<JsonObject> generatedFamilies;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        generatedFamilies = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies();
    }

    @Test
    void allFiftyRelationsMatchConsumeAndRejectLikeTheLiveMatcher() {
        assertEquals(50, generatedFamilies.size());
        RecipeMap map = new RecipeMap(ModRecipeMaps.ASSEMBLER.id());
        List<CompactRecipeFamilySource> sources = generatedFamilies.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(document, registries))
                .toList();
        var snapshot = CompactRecipeFamilyProvider.prepare(
                map,
                sources,
                1L,
                CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                CompactRecipeFamilyProvider.MaterializationPolicy.immediate());
        map.prepareRecipes(List.of(), List.of(snapshot), 1L).publish();

        assertEquals(50, snapshot.logicalRecipeCount());
        assertEquals(50, snapshot.eagerRecipeCount());
        Set<ResourceLocation> stableIds = new HashSet<>(snapshot.recipeIds());
        assertEquals(50, stableIds.size());
        for (ResourceLocation id : snapshot.recipeIds()) {
            assertTrue(id.getPath().startsWith("assembler/compact/"), id::toString);
        }

        for (JsonObject document : generatedFamilies) {
            JsonObject relationJson = document.getAsJsonArray("relations")
                    .get(0)
                    .getAsJsonObject();
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
                    recipe.itemInputs().size(),
                    recipe.itemInputActions().size(),
                    stableId::toString);
            for (int index = 0; index < recipe.itemInputActions().size(); index++) {
                ItemInputAction.Kind kind = recipe.itemInputActions().get(index).kind();
                int count = recipe.itemInputCounts().get(index);
                if (kind == ItemInputAction.Kind.PRESERVE) {
                    assertEquals(0, count, () -> stableId + " PRESERVE must use count 0");
                } else if (kind == ItemInputAction.Kind.CONSUME) {
                    assertTrue(count > 0, () -> stableId + " CONSUME must use a positive count");
                }
            }

            List<ItemStack> exact = offeredStacks(recipe);
            RecipeMap.Match exactMatch = map.findMatch(query(exact)).orElse(null);
            assertTrue(
                    exactMatch != null && exactMatch.id().equals(stableId),
                    () -> stableId
                            + " exact consume+PRESERVE query missed: "
                            + (exactMatch == null ? "empty" : exactMatch.id()));

            List<ItemStack> missingPlank = omitPlank(recipe);
            Optional<RecipeMap.Match> missing = map.findMatch(query(missingPlank));
            assertTrue(
                    missing.isEmpty() || !missing.orElseThrow().id().equals(stableId),
                    () -> stableId + " still matched after the plank consume stack was omitted");

            List<ItemStack> wrongCircuit = wrongCircuitStacks(recipe);
            if (wrongCircuit != null) {
                Optional<RecipeMap.Match> wrong = map.findMatch(query(wrongCircuit));
                assertTrue(
                        wrong.isEmpty() || !wrong.orElseThrow().id().equals(stableId),
                        () -> stableId + " still matched a wrong circuit_config");
            }

            List<ItemStack> extraJunk = new ArrayList<>(exact);
            extraJunk.add(new ItemStack(Items.DIRT));
            GTRecipeQuery extraQuery = query(extraJunk);
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

    private static GTRecipeQuery query(List<ItemStack> stacks) {
        return GTRecipeQuery.items(stacks.toArray(ItemStack[]::new));
    }

    private static List<ItemStack> offeredStacks(GTRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            stacks.add(sample(recipe, index));
        }
        return stacks;
    }

    private static List<ItemStack> omitPlank(GTRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        Integer firstConsume = null;
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            if (recipe.itemInputActions().get(index).kind()
                    != ItemInputAction.Kind.CONSUME) {
                stacks.add(sample(recipe, index));
                continue;
            }
            if (firstConsume == null) {
                firstConsume = index;
            }
            boolean plank = java.util.Arrays.stream(recipe.itemInputs().get(index).getItems())
                    .anyMatch(stack -> stack.is(Items.OAK_PLANKS));
            if (!plank && index != firstConsume) {
                stacks.add(sample(recipe, index));
            }
        }
        return stacks;
    }

    private static List<ItemStack> wrongCircuitStacks(GTRecipe recipe) {
        boolean hasCircuit = false;
        List<ItemStack> stacks = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            ItemStack stack = sample(recipe, index);
            if (stack.has(ModComponents.CIRCUIT_CONFIG.get())) {
                hasCircuit = true;
                stack.set(ModComponents.CIRCUIT_CONFIG.get(), 99);
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
