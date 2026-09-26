package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RecipeMapLogicalOrderTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void randomAccessMatchesIterationAcrossFamilies() {
        RecipeMap map = new RecipeMap(id("logical_order"));
        RecipeMap.Entry base = entry("base", Items.COBBLESTONE);
        RecipeMap.Entry first = entry("a0", Items.IRON_INGOT);
        RecipeMap.Entry second = entry("a1", Items.GOLD_INGOT);
        RecipeMap.Entry third = entry("b0", Items.COPPER_INGOT);
        map.prepareRecipes(
                List.of(base),
                List.of(
                        new FakeFamily("empty", List.of()),
                        new FakeFamily("a", List.of(first, second)),
                        new FakeFamily("b", List.of(third))),
                1L).publish();

        List<RecipeMap.Entry> expected = List.of(base, first, second, third);
        List<RecipeMap.Entry> iterated = new ArrayList<>();
        for (RecipeMap.Entry entry : map.entries()) {
            iterated.add(entry);
        }

        assertEquals(expected, iterated);
        List<RecipeMap.Entry> indexed = map.entries();
        assertEquals(expected.size(), indexed.size());
        for (int index = 0; index < expected.size(); index++) {
            assertSame(expected.get(index), indexed.get(index));
        }
    }

    @Test
    void materializedFamilyRecipeSharesValidatedStacks() {
        ItemStack output = new ItemStack(Items.IRON_INGOT);
        CompactGTRecipeFamilyDefinition.Relation relation =
                new CompactGTRecipeFamilyDefinition.Relation(
                        id("shared_output"),
                        List.of(Ingredient.of(Items.COBBLESTONE)),
                        List.of(1),
                        List.of(ItemInputAction.CONSUME),
                        List.of(output),
                        List.of(),
                        List.of(),
                        List.of(GTRecipe.GUARANTEED_CHANCE),
                        20,
                        8L,
                        0L,
                        true,
                        0,
                        new GTRecipeProvenance("test", Optional.empty()));
        GTRecipe adopted = relation.materialize();
        ItemStack shared = relation.itemOutputs().getFirst();

        assertSame(shared, adopted.itemOutputsView().getFirst());
        int count = shared.getCount();
        adopted.itemOutputs().getFirst().setCount(count + 1);
        assertEquals(count, relation.materialize().itemOutputsView().getFirst().getCount());
    }

    private static RecipeMap.Entry entry(String path, net.minecraft.world.item.Item output) {
        return new RecipeMap.Entry(id(path), new GTRecipe(
                List.of(Ingredient.of(Items.COBBLESTONE)),
                List.of(1),
                List.of(new ItemStack(output)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                8L,
                0L));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    private record FakeFamily(String familyId, List<RecipeMap.Entry> rows)
            implements RecipeMap.RecipeFamily {
        @Override
        public long epoch() {
            return 1L;
        }

        @Override
        public int logicalRecipeCount() {
            return rows.size();
        }

        @Override
        public int eagerRecipeCount() {
            return 0;
        }

        @Override
        public int lazyRecipeCount() {
            return rows.size();
        }

        @Override
        public int cacheSize() {
            return 0;
        }

        @Override
        public int cacheCeiling() {
            return 0;
        }

        @Override
        public String stableFingerprint() {
            return familyId;
        }

        @Override
        public List<ResourceLocation> recipeIds() {
            return rows.stream().map(RecipeMap.Entry::id).toList();
        }

        @Override
        public List<RecipeMap.Entry> eagerEntries() {
            return List.of();
        }

        @Override
        public RecipeMap.Entry enumerationEntry(int index) {
            return rows.get(index);
        }

        @Override
        public Optional<RecipeMap.Entry> entry(ResourceLocation id) {
            return rows.stream().filter(row -> row.id().equals(id)).findFirst();
        }

        @Override
        public Optional<RecipeMap.Entry> findLazy(GTRecipeQuery query) {
            return Optional.empty();
        }

        @Override
        public int indexedLazyCandidateCount(GTRecipeQuery query) {
            return 0;
        }

        @Override
        public boolean hasLazyCandidate(ItemStack stack) {
            return false;
        }
    }
}
