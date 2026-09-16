package com.masson.cruciblecraft.worldgen;

import java.util.Set;

/**
 * GT6 {@code WorldgenRocks#canGenerate} overworld biome gate.
 * DESIGN_POLICY maps 1.7 vanilla cores onto 1.21 biome ids. Jungle, ocean and
 * beach stay out; Twilight / Tropics / Atum / planet rocks are other cards.
 */
public final class SurfaceRockPlacement {
    public static final int AMOUNT = 2;
    public static final int PROBABILITY = 3;
    public static final Set<String> OVERWORLD_BIOMES = Set.of(
            "minecraft:desert",
            "minecraft:badlands",
            "minecraft:wooded_badlands",
            "minecraft:eroded_badlands",
            "minecraft:savanna",
            "minecraft:savanna_plateau",
            "minecraft:windswept_savanna",
            "minecraft:swamp",
            "minecraft:mangrove_swamp",
            "minecraft:taiga",
            "minecraft:snowy_taiga",
            "minecraft:old_growth_pine_taiga",
            "minecraft:old_growth_spruce_taiga",
            "minecraft:plains",
            "minecraft:sunflower_plains",
            "minecraft:meadow",
            "minecraft:forest",
            "minecraft:flower_forest",
            "minecraft:birch_forest",
            "minecraft:old_growth_birch_forest",
            "minecraft:dark_forest",
            "minecraft:windswept_hills",
            "minecraft:windswept_forest",
            "minecraft:windswept_gravelly_hills",
            "minecraft:stony_peaks",
            "minecraft:jagged_peaks",
            "minecraft:frozen_peaks",
            "minecraft:stony_shore");

    private SurfaceRockPlacement() {}

    public static int canGenerate(Set<String> biomeNames) {
        for (String name : biomeNames) {
            if (OVERWORLD_BIOMES.contains(name)) {
                return AMOUNT;
            }
        }
        return 0;
    }

    public static boolean shouldPlaceRay(java.util.Random random) {
        return random.nextInt(PROBABILITY) == 0;
    }
}
