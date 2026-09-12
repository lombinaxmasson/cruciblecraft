package com.masson.cruciblecraft.worldgen.tree.prep;

import java.util.Random;

/**
 * GT6 {@code BlockTreeSaplingAB}/{@code BlockTreeSaplingCD} grow, unregistered.
 * Maple and rainbowood holes are drill-tapped on the log, not placed here.
 */
public final class GtTreeGrower {
    private GtTreeGrower() {}

    public static boolean grow(
            GtTreeSpecies species,
            GtTreeWorld world,
            int x,
            int y,
            int z,
            Random random) {
        return switch (species) {
            case RUBBER -> growRubber(world, x, y, z, random);
            case MAPLE -> growMaple(world, x, y, z, random);
            case WILLOW -> growWillow(world, x, y, z, random);
            case BLUE_MAHOE -> growBlueMahoe(world, x, y, z, random);
            case HAZEL -> growHazel(world, x, y, z);
            case CINNAMON -> growCinnamon(world, x, y, z, random);
            case COCONUT -> growCoconut(world, x, y, z, random);
            case RAINBOWOOD -> growRainbowood(world, x, y, z, random);
            case BLUE_SPRUCE -> growBlueSpruce(world, x, y, z, random);
        };
    }

    public static int getMaxHeight(
            GtTreeWorld world, int x, int y, int z, int maxTreeHeight) {
        maxTreeHeight--;
        int maxHeight = 0;
        while (maxHeight++ < maxTreeHeight) {
            if (y + maxHeight >= world.height()
                    || !world.canPlaceTree(x, y + maxHeight, z)) {
                return maxHeight - 1;
            }
        }
        return maxHeight;
    }

    private static boolean placeLog(GtTreeWorld world, int x, int y, int z) {
        if (!world.canPlaceTree(x, y, z)) {
            return false;
        }
        world.setLog(x, y, z);
        return true;
    }

    private static boolean placeLeaves(GtTreeWorld world, int x, int y, int z) {
        if (!world.canPlaceTree(x, y, z)) {
            return false;
        }
        world.setLeaves(x, y, z);
        return true;
    }

    private static boolean growRubber(
            GtTreeWorld world, int x, int y, int z, Random random) {
        int maxHeight = getMaxHeight(world, x, y, z, 9);
        if (maxHeight < 7) {
            return false;
        }
        maxHeight = y + 7 + random.nextInt(maxHeight - 6);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 || j != 0) {
                    if (!world.canPlaceTree(x + i, maxHeight - 5, z + j)) {
                        return false;
                    }
                }
            }
        }
        world.setLog(x, y, z);
        boolean canPlaceResinHole = true;
        for (int tY = y + 1; tY < maxHeight; tY++) {
            if (canPlaceResinHole
                    && maxHeight - tY > 5
                    && (random.nextInt(2) == 0
                            || !world.hasNearbyRubberResinHole(x, z))) {
                canPlaceResinHole = false;
                world.setRubberResinHole(
                        x, tY, z, HorizontalFacing.fromGtIndex(random.nextInt(4)));
                continue;
            }
            placeLog(world, x, tY, z);
        }
        placeLeaves(world, x, maxHeight, z);
        placeLeaves(world, x, maxHeight + 1, z);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i != 0 || j != 0) {
                    placeLeaves(world, x + i, maxHeight - 1, z + j);
                }
            }
        }
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i != 0 || j != 0) {
                    if (Math.abs(i * j) < 2) {
                        placeLeaves(world, x + i, maxHeight - 2, z + j);
                    }
                    if (Math.abs(i * j) < 4) {
                        placeLeaves(world, x + i, maxHeight - 3, z + j);
                        placeLeaves(world, x + i, maxHeight - 4, z + j);
                    }
                    placeLeaves(world, x + i, maxHeight - 5, z + j);
                }
            }
        }
        return true;
    }

    private static boolean growMaple(
            GtTreeWorld world, int x, int y, int z, Random random) {
        int maxHeight = getMaxHeight(world, x, y, z, 11);
        if (maxHeight < 9) {
            return false;
        }
        maxHeight = y + 9 + random.nextInt(maxHeight - 8);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i != 0 || j != 0) {
                    if (!world.canPlaceTree(x + i, maxHeight - 4, z + j)) {
                        return false;
                    }
                }
            }
        }
        world.setLog(x, y, z);
        for (int tY = y + 1; tY < maxHeight; tY++) {
            placeLog(world, x, tY, z);
        }
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                placeLeaves(world, x + i, maxHeight + 1, z + j);
            }
        }
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                placeLeaves(world, x + i, maxHeight, z + j);
                if (i != 0 || j != 0) {
                    if (Math.abs(i * j) < 4) {
                        placeLeaves(world, x + i, maxHeight - 7, z + j);
                    }
                    placeLeaves(world, x + i, maxHeight - 1, z + j);
                }
            }
        }
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (i != 0 || j != 0) {
                    if (Math.abs(i * j) < 9) {
                        placeLeaves(world, x + i, maxHeight - 2, z + j);
                        placeLeaves(world, x + i, maxHeight - 3, z + j);
                        placeLeaves(world, x + i, maxHeight - 6, z + j);
                    }
                    placeLeaves(world, x + i, maxHeight - 4, z + j);
                    placeLeaves(world, x + i, maxHeight - 5, z + j);
                }
            }
        }
        return true;
    }

    private static boolean growWillow(
            GtTreeWorld world, int x, int y, int z, Random random) {
        int maxHeight = getMaxHeight(world, x, y, z, 7);
        if (maxHeight < 5) {
            return false;
        }
        maxHeight = y + 5 + random.nextInt(maxHeight - 4);
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (i != 0 || j != 0) {
                    if (!world.canPlaceTree(x + i, maxHeight - 2, z + j)) {
                        return false;
                    }
                }
            }
        }
        world.setLog(x, y, z);
        for (int tY = y + 1; tY < maxHeight; tY++) {
            placeLog(world, x, tY, z);
        }
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (Math.abs(i * j) < 9) {
                    placeLeaves(world, x + i, maxHeight + 1, z + j);
                    if (i != 0 || j != 0) {
                        placeLeaves(world, x + i, maxHeight - 2, z + j);
                    }
                }
                placeLeaves(world, x + i, maxHeight, z + j);
            }
        }
        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                if (i != 0 || j != 0) {
                    if (Math.abs(i * j) < 10) {
                        placeLeaves(world, x + i, maxHeight - 1, z + j);
                        if (maxHeight - 2 <= y) {
                            continue;
                        }
                        if (Math.abs(i * j) > 6) {
                            placeLeaves(world, x + i, maxHeight - 2, z + j);
                            if (maxHeight - 3 <= y) {
                                continue;
                            }
                            placeLeaves(world, x + i, maxHeight - 3, z + j);
                            if (maxHeight - 4 <= y) {
                                continue;
                            }
                            placeLeaves(world, x + i, maxHeight - 4, z + j);
                            if (maxHeight - 5 <= y) {
                                continue;
                            }
                            placeLeaves(world, x + i, maxHeight - 5, z + j);
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean growBlueMahoe(
            GtTreeWorld world, int x, int y, int z, Random random) {
        int maxHeight = getMaxHeight(world, x, y, z, 5);
        if (maxHeight < 4) {
            return false;
        }
        maxHeight = y + 4 + random.nextInt(maxHeight - 3);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i != 0 || j != 0) {
                    if (!world.canPlaceTree(x + i, maxHeight - 2, z + j)) {
                        return false;
                    }
                }
            }
        }
        world.setLog(x, y, z);
        for (int tY = y + 1; tY < maxHeight; tY++) {
            placeLog(world, x, tY, z);
        }
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                placeLeaves(world, x + i, maxHeight + 3, z + j);
            }
        }
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i * j) < 4) {
                    placeLeaves(world, x + i, maxHeight + 2, z + j);
                    placeLeaves(world, x + i, maxHeight + 1, z + j);
                    if (i != 0 || j != 0) {
                        placeLeaves(world, x + i, maxHeight, z + j);
                        placeLeaves(world, x + i, maxHeight - 1, z + j);
                    }
                }
            }
        }
        return true;
    }

    private static boolean growHazel(GtTreeWorld world, int x, int y, int z) {
        if (getMaxHeight(world, x, y, z, 4) < 4) {
            return false;
        }
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i != 0 || j != 0) {
                    if (!world.canPlaceTree(x + i, y + 2, z + j)) {
                        return false;
                    }
                }
            }
        }
        world.setLog(x, y, z);
        placeLog(world, x, y + 1, z);
        placeLog(world, x, y + 2, z);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                placeLeaves(world, x + i, y + 4, z + j);
            }
        }
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i != 0 || j != 0) {
                    placeLeaves(world, x + i, y + 2, z + j);
                }
                if (Math.abs(i * j) < 4) {
                    placeLeaves(world, x + i, y + 3, z + j);
                }
            }
        }
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (Math.abs(i * j) < 9 && (i != 0 || j != 0)) {
                    placeLeaves(world, x + i, y + 1, z + j);
                }
            }
        }
        return true;
    }

    private static boolean growCinnamon(
            GtTreeWorld world, int x, int y, int z, Random random) {
        int maxHeight = getMaxHeight(world, x, y, z, 8);
        if (maxHeight < 6) {
            return false;
        }
        maxHeight = y + 6 + random.nextInt(maxHeight - 5);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i != 0 || j != 0) {
                    if (!world.canPlaceTree(x + i, maxHeight - 4, z + j)) {
                        return false;
                    }
                }
            }
        }
        world.setLog(x, y, z);
        for (int tY = y + 1; tY < maxHeight; tY++) {
            placeLog(world, x, tY, z);
        }
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                placeLeaves(world, x + i, maxHeight + 2, z + j);
                if (i != 0 || j != 0) {
                    placeLeaves(world, x + i, maxHeight - 4, z + j);
                }
            }
        }
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (Math.abs(i * j) < 9) {
                    if (i != 0 || j != 0) {
                        placeLeaves(world, x + i, maxHeight - 1, z + j);
                        placeLeaves(world, x + i, maxHeight - 2, z + j);
                        placeLeaves(world, x + i, maxHeight - 3, z + j);
                    }
                    placeLeaves(world, x + i, maxHeight, z + j);
                    placeLeaves(world, x + i, maxHeight + 1, z + j);
                }
            }
        }
        return true;
    }

    private static boolean growCoconut(
            GtTreeWorld world, int x, int y, int z, Random random) {
        int maxHeight = getMaxHeight(world, x, y, z, 12);
        if (maxHeight < 8) {
            return false;
        }
        maxHeight = y + 8 + random.nextInt(maxHeight - 7);
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (i != 0 || j != 0) {
                    if (!world.canPlaceTree(x + i, maxHeight, z + j)) {
                        return false;
                    }
                }
            }
        }
        world.setLog(x, y, z);
        for (int tY = y + 1; tY < maxHeight; tY++) {
            placeLog(world, x, tY, z);
        }
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (i == j || i == -j) {
                    if (Math.abs(i) == 3 || Math.abs(j) == 3) {
                        placeLeaves(world, x + i, maxHeight - 1, z + j);
                        placeLeaves(world, x + i, maxHeight - 2, z + j);
                    } else if (Math.abs(i) == 2 || Math.abs(j) == 2) {
                        placeLeaves(world, x + i, maxHeight, z + j);
                        placeLeaves(world, x + i, maxHeight - 1, z + j);
                    } else {
                        placeLeaves(world, x + i, maxHeight, z + j);
                    }
                }
            }
        }
        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                if (i == 0 || j == 0) {
                    if (Math.abs(i) == 4 || Math.abs(j) == 4) {
                        placeLeaves(world, x + i, maxHeight - 1, z + j);
                        placeLeaves(world, x + i, maxHeight - 2, z + j);
                    } else if (Math.abs(i) == 3 || Math.abs(j) == 3) {
                        placeLeaves(world, x + i, maxHeight, z + j);
                        placeLeaves(world, x + i, maxHeight - 1, z + j);
                    } else {
                        placeLeaves(world, x + i, maxHeight, z + j);
                    }
                }
            }
        }
        return true;
    }

    private static boolean growRainbowood(
            GtTreeWorld world, int x, int y, int z, Random random) {
        int maxHeight = getMaxHeight(world, x, y, z, 9);
        if (maxHeight < 7) {
            return false;
        }
        maxHeight = y + 7 + random.nextInt(maxHeight - 6);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (i != 0 || j != 0) {
                    if (!world.canPlaceTree(x + i, maxHeight - 4, z + j)) {
                        return false;
                    }
                }
            }
        }
        world.setLog(x, y, z);
        for (int tY = y + 1; tY < maxHeight; tY++) {
            placeLog(world, x, tY, z);
        }
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                placeLeaves(world, x + i, maxHeight + 2, z + j);
                if (i != 0 || j != 0) {
                    placeLeaves(world, x + i, maxHeight - 4, z + j);
                }
            }
        }
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (Math.abs(i * j) < 9) {
                    if (i != 0 || j != 0) {
                        placeLeaves(world, x + i, maxHeight - 1, z + j);
                        placeLeaves(world, x + i, maxHeight - 2, z + j);
                        placeLeaves(world, x + i, maxHeight - 3, z + j);
                    }
                    placeLeaves(world, x + i, maxHeight, z + j);
                    placeLeaves(world, x + i, maxHeight + 1, z + j);
                }
            }
        }
        return true;
    }

    private static boolean growBlueSpruce(
            GtTreeWorld world, int x, int y, int z, Random random) {
        int maxHeight = getMaxHeight(world, x, y, z, 16);
        if (maxHeight < 16) {
            return false;
        }
        maxHeight = y + maxHeight - random.nextInt(3);
        for (int i = -3; i <= 3; i++) {
            for (int j = -3; j <= 3; j++) {
                if (i != 0 || j != 0) {
                    if (!world.canPlaceTree(x + i, maxHeight - 5, z + j)) {
                        return false;
                    }
                }
            }
        }
        world.setLog(x, y, z);
        for (int tY = y + 1; tY < maxHeight; tY++) {
            placeLog(world, x, tY, z);
        }
        placeLeaves(world, x, maxHeight, z);
        placeLeaves(world, x, maxHeight + 1, z);
        placeLeaves(world, x + 1, maxHeight - 1, z);
        placeLeaves(world, x - 1, maxHeight - 1, z);
        placeLeaves(world, x, maxHeight - 1, z + 1);
        placeLeaves(world, x, maxHeight - 1, z - 1);
        for (int i = -6; i <= 6; i++) {
            for (int j = -6; j <= 6; j++) {
                if (i != 0 || j != 0) {
                    for (int k = 1; k <= 14; k++) {
                        if (i * i + j * j < k * k * 0.2) {
                            placeLeaves(world, x + i, maxHeight + 1 - k, z + j);
                        }
                    }
                    if (i * i + j * j <= 30) {
                        for (int k = 0; k <= 3; k++) {
                            int groundY = y - k;
                            if (world.isAir(x + i, groundY, z + j)) {
                                continue;
                            }
                            if (world.isDirtOrGrass(x + i, groundY, z + j)) {
                                world.setPodzol(x + i, groundY, z + j);
                            }
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }
}
