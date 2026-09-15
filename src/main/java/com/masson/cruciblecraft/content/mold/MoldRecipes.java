package com.masson.cruciblecraft.content.mold;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;

/**
 * GT6 {@code MultiTileEntityMold.MOLD_RECIPES}: 5×5 bit masks to prefixes.
 * Unmapped non-zero masks are nuggets. Zero is unshaped and cannot fill.
 */
public final class MoldRecipes {
    public static final int CELL_COUNT = 25;
    public static final float INNER_MIN = 2.0F / 16.0F;
    public static final float INNER_SPAN = 12.0F / 16.0F;

    private static final Map<Integer, MaterialPrefix> RECIPES = new HashMap<>();
    private static final int[] ROTATE_CW = {
        4, 9, 14, 19, 24,
        3, 8, 13, 18, 23,
        2, 7, 12, 17, 22,
        1, 6, 11, 16, 21,
        0, 5, 10, 15, 20
    };

    static {
        putAllOrientations(MoldShape.INGOT.mask(), MaterialPrefixes.INGOT);
        putAllOrientations(MoldShape.PLATE.mask(), MaterialPrefixes.PLATE);
        putAllOrientations(MoldShape.ROD.mask(), MaterialPrefixes.ROD);
        putAllOrientations(MoldShape.BOLT.mask(), MaterialPrefixes.BOLT);
        putAllOrientations(0b0_10101_01110_11011_01110_10101, MaterialPrefixes.GEAR);
        putAllOrientations(0b0_01010_11111_01010_11111_01010, MaterialPrefixes.SMALL_GEAR);
        putAllOrientations(0b0_00000_01110_01010_01110_00000, MaterialPrefixes.RING);
        putAllOrientations(0b0_10000_01000_00100_00010_00001, MaterialPrefixes.LONG_ROD);
    }

    private MoldRecipes() {}

    public static Optional<MaterialPrefix> recipe(int shape) {
        if (shape == 0) {
            return Optional.empty();
        }
        MaterialPrefix mapped = RECIPES.get(shape & ((1 << CELL_COUNT) - 1));
        return Optional.of(mapped == null ? MaterialPrefixes.NUGGET : mapped);
    }

    public static int requiredUnits(int shape) {
        Optional<MaterialPrefix> prefix = recipe(shape);
        if (prefix.isEmpty()) {
            return 0;
        }
        if (prefix.get().equals(MaterialPrefixes.NUGGET)) {
            int cells = Integer.bitCount(shape & ((1 << CELL_COUNT) - 1));
            return cells * MaterialPrefixes.NUGGET.units();
        }
        return prefix.get().units();
    }

    public static int rotateClockwise(int shape) {
        int result = 0;
        for (int index = 0; index < CELL_COUNT; index++) {
            if ((shape & (1 << index)) != 0) {
                result |= 1 << ROTATE_CW[index];
            }
        }
        return result;
    }

    public static Optional<Integer> chiselBit(double hitX, double hitZ) {
        if (hitX <= INNER_MIN || hitX >= 1.0F - INNER_MIN
                || hitZ <= INNER_MIN || hitZ >= 1.0F - INNER_MIN) {
            return Optional.empty();
        }
        int x = (int) (5 * (hitX - INNER_MIN) / INNER_SPAN);
        int z = (int) (5 * (hitZ - INNER_MIN) / INNER_SPAN);
        if (x < 0 || x > 4 || z < 0 || z > 4) {
            return Optional.empty();
        }
        return Optional.of(1 << (x * 5 + z));
    }

    private static void putAllOrientations(int key, MaterialPrefix prefix) {
        int rotated = key;
        for (int step = 0; step < 4; step++) {
            RECIPES.put(rotated, prefix);
            RECIPES.put(mirrorZ(rotated), prefix);
            RECIPES.put(mirrorX(rotated), prefix);
            rotated = rotateClockwise(rotated);
        }
    }

    private static int mirrorZ(int shape) {
        int result = 0;
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                if ((shape & (1 << (x * 5 + z))) != 0) {
                    result |= 1 << (x * 5 + (4 - z));
                }
            }
        }
        return result;
    }

    private static int mirrorX(int shape) {
        int result = 0;
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                if ((shape & (1 << (x * 5 + z))) != 0) {
                    result |= 1 << ((4 - x) * 5 + z);
                }
            }
        }
        return result;
    }
}
