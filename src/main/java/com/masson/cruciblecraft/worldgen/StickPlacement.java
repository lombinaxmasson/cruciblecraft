package com.masson.cruciblecraft.worldgen;

import java.util.Set;

/**
 * GT6 {@code WorldgenSticks#canGenerate} biome gate.
 *
 * <p>The base amount and ray probability remain the source values from
 * {@code new WorldgenSticks("sticks", true, 2, 2, ...)}; biome groups only
 * scale the number of candidate columns.
 */
public final class StickPlacement {
    public static final int AMOUNT = 2;
    public static final int PROBABILITY = 2;

    private static final Set<String> WOODS = Set.of(
            "minecraft:forest",
            "minecraft:flower_forest",
            "minecraft:birch_forest",
            "minecraft:old_growth_birch_forest",
            "minecraft:dark_forest");
    private static final Set<String> SWAMPS = Set.of(
            "minecraft:swamp",
            "minecraft:mangrove_swamp");
    private static final Set<String> RIVERS = Set.of(
            "minecraft:river",
            "minecraft:frozen_river");
    private static final Set<String> PLAINS = Set.of(
            "minecraft:plains",
            "minecraft:sunflower_plains",
            "minecraft:meadow");
    private static final Set<String> SAVANNAS = Set.of(
            "minecraft:savanna",
            "minecraft:savanna_plateau");
    private static final Set<String> TAIGAS = Set.of(
            "minecraft:taiga",
            "minecraft:snowy_taiga",
            "minecraft:old_growth_pine_taiga",
            "minecraft:old_growth_spruce_taiga");
    private static final Set<String> MESAS = Set.of(
            "minecraft:badlands",
            "minecraft:wooded_badlands");

    private StickPlacement() {}

    public static int canGenerate(Set<String> biomeNames) {
        return canGenerate(biomeNames, AMOUNT);
    }

    public static int canGenerate(Set<String> biomeNames, int amount) {
        for (String name : biomeNames) {
            if (WOODS.contains(name) || SWAMPS.contains(name)) {
                return amount * 3;
            }
        }
        for (String name : biomeNames) {
            if (RIVERS.contains(name)
                    || PLAINS.contains(name)
                    || SAVANNAS.contains(name)) {
                return amount * 2;
            }
        }
        for (String name : biomeNames) {
            if (TAIGAS.contains(name) || MESAS.contains(name)) {
                return amount;
            }
        }
        return 0;
    }

    public static boolean shouldPlaceRay(java.util.Random random) {
        return shouldPlaceRay(random, PROBABILITY);
    }

    public static boolean shouldPlaceRay(java.util.Random random, int probability) {
        return random.nextInt(probability) == 0;
    }

    public static boolean isPlantable(net.minecraft.world.level.block.state.BlockState contact) {
        return contact.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || contact.is(net.minecraft.tags.BlockTags.DIRT);
    }
}
