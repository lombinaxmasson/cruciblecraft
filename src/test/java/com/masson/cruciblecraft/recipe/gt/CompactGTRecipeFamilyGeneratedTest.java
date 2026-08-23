package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.LoadingModList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactGTRecipeFamilyGeneratedTest {
    private static RegistryAccess registries;
    private static List<JsonObject> generatedFamilies;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
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
            JsonObject relation = document.getAsJsonArray("relations")
                    .get(0)
                    .getAsJsonObject();
            assertTrue(stableIds.add(relation.get("stable_id").getAsString()));
        }
        assertEquals(50, familyIds.size());
        assertEquals(50, stableIds.size());
        for (int number = 2; number <= 51; number++) {
            assertTrue(familyIds.contains("gt.recipe.assembler#%04d".formatted(number)));
        }
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

        assertEquals(50, immediate.logicalRecipeCount());
        assertEquals(50, immediate.eagerRecipeCount());
        assertEquals(0, immediate.lazyRecipeCount());
        assertEquals(0, onDemand.eagerRecipeCount());
        assertEquals(50, onDemand.lazyRecipeCount());
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
