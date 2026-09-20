package com.masson.cruciblecraft.recipe.crafting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.masson.cruciblecraft.worldgen.StoneLayerStones;

import net.minecraft.resources.ResourceLocation;

/**
 * GT6 2×2 {@code OP.rockGt} → cobble: vanilla stone / netherrack / endstone
 * from {@code Loader_Recipes_Vanilla}, and each {@code BlockStones} cobble
 * cube from {@code BlockStones#run}.
 */
public final class RockCobbleCrafting {
    private RockCobbleCrafting() {}

    public static Map<String, ResourceLocation> cobbleResults() {
        LinkedHashMap<String, ResourceLocation> results = new LinkedHashMap<>();
        results.put("stone", ResourceLocation.parse("minecraft:cobblestone"));
        results.put("netherrack", ResourceLocation.parse("minecraft:netherrack"));
        results.put("endstone", ResourceLocation.parse("minecraft:end_stone"));
        for (StoneLayerStones.Cube cube : StoneLayerStones.cubes()) {
            if (cube.role() != StoneLayerStones.Role.COBBLE) {
                continue;
            }
            results.putIfAbsent(cube.material(), cube.id());
        }
        return Collections.unmodifiableMap(results);
    }
}
