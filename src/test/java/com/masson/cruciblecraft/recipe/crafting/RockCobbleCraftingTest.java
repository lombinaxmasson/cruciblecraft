package com.masson.cruciblecraft.recipe.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import com.masson.cruciblecraft.worldgen.StoneLayerStones;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

/** GT6 2×2 {@code OP.rockGt} → cobble map from vanilla + {@code BlockStones}. */
class RockCobbleCraftingTest {
    @Test
    void cobbleResultsCoverVanillaAndLayerCubes() {
        Map<String, ResourceLocation> results = RockCobbleCrafting.cobbleResults();
        assertEquals(
                ResourceLocation.parse("minecraft:cobblestone"),
                results.get("stone"));
        assertEquals(
                ResourceLocation.parse("minecraft:netherrack"),
                results.get("netherrack"));
        assertEquals(
                ResourceLocation.parse("minecraft:end_stone"),
                results.get("endstone"));
        long cobbles = StoneLayerStones.cubes().stream()
                .filter(cube -> cube.role() == StoneLayerStones.Role.COBBLE)
                .count();
        assertEquals(3 + cobbles, results.size());
        assertTrue(cobbles >= 15);
        assertEquals(
                ResourceLocation.parse("cruciblecraft:granite_black/cobble"),
                results.get("granite_black"));
        assertEquals(
                ResourceLocation.parse("cruciblecraft:shale/cobble"),
                results.get("shale"));
    }
}
