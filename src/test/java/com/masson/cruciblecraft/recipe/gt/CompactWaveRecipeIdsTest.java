package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.resources.ResourceLocation;

class CompactWaveRecipeIdsTest {
    static {
        MinecraftTestBootstrap.bootstrap();
    }
    @Test
    void acceptsSemanticSmelterAndMixerPaths() {
        assertTrue(CompactWaveRecipeIds.isSemanticWaveRecipe(
                id("smelter/ordinary_closure/singleton/gt_recipe_smelter_0001")));
        assertTrue(CompactWaveRecipeIds.isSemanticWaveRecipe(
                id("mixer/ordinary_closure/opaque/gt_recipe_mixer_0001")));
        assertFalse(CompactWaveRecipeIds.isSemanticWaveRecipe(
                id("bath/tiny_purified/gt_recipe_bath_0072")));
        assertFalse(CompactWaveRecipeIds.isSemanticWaveRecipe(
                id("t50/mixer/gt_recipe_mixer_0001")));
    }

    @Test
    void gradleNamespaceUsesSlugNotT50() {
        assertEquals(
                "cruciblecraft_wave_smelter_ordinary_closure",
                CompactWaveRecipeIds.gradleNamespace("smelter/ordinary-closure"));
        assertThrows(
                IllegalArgumentException.class,
                () -> CompactWaveRecipeIds.gradleNamespace("T50"));
        assertThrows(
                IllegalArgumentException.class,
                () -> CompactWaveRecipeIds.gradleNamespace("t50Recipes"));
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_wave_smelter_ordinary_closure/structure/empty.nbt")));
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_wave_smelter_ordinary_closure/gametest/structure/empty.nbt")));
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_wave_mixer_ordinary_closure/structure/empty.nbt")));
        assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                "src/main/resources/data/cruciblecraft_wave_mixer_ordinary_closure/gametest/structure/empty.nbt")));
        for (String ns : java.util.List.of(
                "cruciblecraft_wave_recycling_smelter_mte_identity",
                "cruciblecraft_wave_smelter_deferred_recycling")) {
            assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                    "src/main/resources/data/" + ns + "/structure/empty.nbt")));
            assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                    "src/main/resources/data/" + ns + "/gametest/structure/empty.nbt")));
        }
        for (String host : java.util.List.of(
                "drying", "electrolyzer", "centrifuge", "autoclave", "compressor")) {
            String ns = "src/main/resources/data/cruciblecraft_wave_"
                    + host + "_ordinary_closure";
            assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                    ns + "/structure/empty.nbt")));
            assertTrue(java.nio.file.Files.isRegularFile(java.nio.file.Path.of(
                    ns + "/gametest/structure/empty.nbt")));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
