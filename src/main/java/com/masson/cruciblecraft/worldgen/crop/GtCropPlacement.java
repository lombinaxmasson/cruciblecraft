package com.masson.cruciblecraft.worldgen.crop;

import java.util.Set;

/**
 * GT6 {@code WorldgenOnSurface} biome gates. Overworld only.
 */
public final class GtCropPlacement {
    public static final Set<String> GLOWTUS_BIOMES = Set.of(
            "minecraft:jungle",
            "minecraft:sparse_jungle",
            "minecraft:bamboo_jungle");
    public static final Set<String> BUSH_BIOMES = Set.of(
            "minecraft:plains",
            "minecraft:sunflower_plains",
            "minecraft:meadow",
            "minecraft:forest",
            "minecraft:flower_forest",
            "minecraft:birch_forest",
            "minecraft:old_growth_birch_forest",
            "minecraft:dark_forest",
            "minecraft:taiga",
            "minecraft:old_growth_pine_taiga",
            "minecraft:old_growth_spruce_taiga",
            "minecraft:jungle",
            "minecraft:sparse_jungle",
            "minecraft:bamboo_jungle");
    public static final Set<String> FROZEN_BIOMES = Set.of(
            "minecraft:snowy_plains",
            "minecraft:ice_spikes",
            "minecraft:snowy_taiga",
            "minecraft:frozen_river",
            "minecraft:snowy_beach",
            "minecraft:grove",
            "minecraft:snowy_slopes",
            "minecraft:jagged_peaks",
            "minecraft:frozen_peaks",
            "minecraft:frozen_ocean",
            "minecraft:deep_frozen_ocean");

    private GtCropPlacement() {}

    public static int canGenerate(GtCropKind kind, Set<String> biomeNames) {
        for (String name : biomeNames) {
            if (FROZEN_BIOMES.contains(name)) {
                continue;
            }
            if (kind == GtCropKind.GLOWTUS && GLOWTUS_BIOMES.contains(name)) {
                return kind.amount();
            }
            if (kind == GtCropKind.BUSH && BUSH_BIOMES.contains(name)) {
                return kind.amount();
            }
        }
        return 0;
    }

    public static boolean shouldPlaceRay(GtCropKind kind, java.util.Random random) {
        return random.nextInt(kind.probability()) == 0;
    }
}
