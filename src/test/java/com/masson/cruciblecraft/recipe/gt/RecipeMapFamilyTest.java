package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

class RecipeMapFamilyTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void familyEnumerationLookupCacheAndEpochAreFailClosed() {
        RecipeMap map = new RecipeMap(id("family"));
        FakeFamily server = new FakeFamily(1L);
        RecipeMap.Prepared first = map.prepareRecipes(
                List.of(), List.of(server), 1L);
        first.publish();
        List<RecipeMap.Entry> stale = map.entries();

        assertEquals(
                server.recipeIds(),
                stale.stream().map(RecipeMap.Entry::id).toList());
        assertEquals(
                id("family/iron"),
                map.findMatch(GTRecipeQuery.items(
                                new ItemStack(Items.IRON_INGOT)))
                        .orElseThrow().id());
        assertEquals(1, server.cacheSize());
        assertEquals(1, server.cacheCeiling());
        assertFalse(map.entry(id("family/unknown")).isPresent());

        FakeFamily dedicatedClient = new FakeFamily(17L);
        assertEquals(server.recipeIds(), dedicatedClient.recipeIds());
        assertEquals(
                server.stableFingerprint(),
                dedicatedClient.stableFingerprint());

        RecipeMap.Prepared second = map.prepareRecipes(
                List.of(), List.of(new FakeFamily(2L)), 2L);
        second.publish();
        assertThrows(IllegalStateException.class, stale::size);
    }

    @Test
    void familyStableIdsCannotDuplicateConcreteRows() {
        RecipeMap map = new RecipeMap(id("duplicates"));
        FakeFamily family = new FakeFamily(1L);
        assertThrows(IllegalArgumentException.class, () ->
                map.prepareRecipes(
                        List.of(family.enumerationEntry(1)),
                        List.of(family),
                        1L));
        assertEquals(0L, map.revision());
    }

    @Test
    void lazyBadFamilyRowFailsReloadValidationBeforePublication() {
        RecipeMap map = new RecipeMap(id("lazy_reload_validation"));
        ItemStack named = new ItemStack(Items.IRON_INGOT);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("not-indexed"));
        RecipeMap.Entry bad = new RecipeMap.Entry(
                id("family/bad_lazy"),
                new GTRecipe(
                        List.of(DataComponentIngredient.of(false, named)),
                        List.of(1),
                        List.of(new ItemStack(Items.IRON_NUGGET)),
                        List.of(),
                        List.of(),
                        List.of(GTRecipe.GUARANTEED_CHANCE),
                        20,
                        8L,
                        0L));
        RecipeMap.RecipeFamily family = new SingleLazyFamily(7L, bad);

        assertThrows(
                IllegalArgumentException.class,
                () -> GTRecipeMapLoader.validateCompleteReloadRows(
                        map, List.of(), List.of(family), 7L));
        assertEquals(0L, map.revision());
        assertEquals(0, family.cacheSize());
    }

    private static final class FakeFamily
            implements RecipeMap.RecipeFamily {
        private final long epoch;
        private final List<RecipeMap.Entry> entries = List.of(
                RecipeMapFamilyTest.entry(
                        "family/cobble", Items.COBBLESTONE, Items.STONE),
                RecipeMapFamilyTest.entry(
                        "family/iron", Items.IRON_INGOT, Items.IRON_NUGGET),
                RecipeMapFamilyTest.entry(
                        "family/gold", Items.GOLD_INGOT, Items.GOLD_NUGGET));
        private final LinkedHashMap<ResourceLocation, RecipeMap.Entry> cache =
                new LinkedHashMap<>(2, 0.75F, true);

        private FakeFamily(long epoch) {
            this.epoch = epoch;
        }

        @Override public String familyId() { return "test_family"; }
        @Override public long epoch() { return epoch; }
        @Override public int logicalRecipeCount() { return entries.size(); }
        @Override public int eagerRecipeCount() { return 1; }
        @Override public int lazyRecipeCount() { return 2; }
        @Override public int cacheSize() { return cache.size(); }
        @Override public int cacheCeiling() { return 1; }
        @Override public String stableFingerprint() {
            return "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        }
        @Override public List<ResourceLocation> recipeIds() {
            return entries.stream().map(RecipeMap.Entry::id).toList();
        }
        @Override public List<RecipeMap.Entry> eagerEntries() {
            return entries.subList(0, 1);
        }
        @Override public RecipeMap.Entry enumerationEntry(int index) {
            return entries.get(index);
        }
        @Override public Optional<RecipeMap.Entry> entry(ResourceLocation id) {
            return entries.stream().filter(row -> row.id().equals(id)).findFirst();
        }
        @Override public Optional<RecipeMap.Entry> findLazy(GTRecipeQuery query) {
            for (RecipeMap.Entry entry : entries.subList(1, entries.size())) {
                if (entry.recipe().matches(query)) {
                    cache.put(entry.id(), entry);
                    while (cache.size() > cacheCeiling()) {
                        var iterator = cache.entrySet().iterator();
                        iterator.next();
                        iterator.remove();
                    }
                    return Optional.of(entry);
                }
            }
            return Optional.empty();
        }
        @Override public int indexedLazyCandidateCount(GTRecipeQuery query) {
            return (int) entries.subList(1, entries.size()).stream()
                    .filter(entry -> entry.recipe().matches(query))
                    .count();
        }
        @Override public boolean hasLazyCandidate(ItemStack stack) {
            return entries.subList(1, entries.size()).stream()
                    .anyMatch(entry -> entry.recipe().itemInputs().getFirst()
                            .test(stack));
        }
    }

    private record SingleLazyFamily(
            long epoch,
            RecipeMap.Entry row) implements RecipeMap.RecipeFamily {
        @Override public String familyId() { return "bad_lazy_family"; }
        @Override public int logicalRecipeCount() { return 1; }
        @Override public int eagerRecipeCount() { return 0; }
        @Override public int lazyRecipeCount() { return 1; }
        @Override public int cacheSize() { return 0; }
        @Override public int cacheCeiling() { return 0; }
        @Override public String stableFingerprint() {
            return "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                    + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        }
        @Override public List<ResourceLocation> recipeIds() {
            return List.of(row.id());
        }
        @Override public List<RecipeMap.Entry> eagerEntries() {
            return List.of();
        }
        @Override public RecipeMap.Entry enumerationEntry(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException(index);
            }
            return row;
        }
        @Override public Optional<RecipeMap.Entry> entry(ResourceLocation id) {
            return row.id().equals(id) ? Optional.of(row) : Optional.empty();
        }
        @Override public Optional<RecipeMap.Entry> findLazy(GTRecipeQuery query) {
            return Optional.empty();
        }
        @Override public int indexedLazyCandidateCount(GTRecipeQuery query) {
            return 0;
        }
        @Override public boolean hasLazyCandidate(ItemStack stack) {
            return false;
        }
    }

    private static RecipeMap.Entry entry(
            String path, Item input, Item output) {
        return new RecipeMap.Entry(
                id(path),
                new GTRecipe(
                        List.of(Ingredient.of(input)),
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
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }
}
