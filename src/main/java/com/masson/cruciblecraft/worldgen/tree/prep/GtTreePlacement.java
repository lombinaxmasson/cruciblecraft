package com.masson.cruciblecraft.worldgen.tree.prep;

import java.util.Random;
import java.util.Set;

/**
 * GT6 {@code WorldgenOnSurface} + per-tree {@code canGenerate}, unregistered.
 */
public final class GtTreePlacement {
    private GtTreePlacement() {}

    public static int canGenerate(GtTreeSpecies species, Set<String> biomeNames, Random random) {
        if (species == GtTreeSpecies.RAINBOWOOD) {
            if (biomeNames.stream().anyMatch(species.overworldBiomes()::contains)) {
                return species.amount();
            }
            return random.nextInt(GtTreeSpecies.RAINBOWOOD_RARE_CHANCE) == 0
                    ? species.amount()
                    : 0;
        }
        for (String name : biomeNames) {
            if (species.overworldBiomes().contains(name)) {
                return species.amount();
            }
        }
        return 0;
    }

    public static boolean shouldPlaceRay(GtTreeSpecies species, Random random) {
        return random.nextInt(species.probability()) == 0;
    }
}
