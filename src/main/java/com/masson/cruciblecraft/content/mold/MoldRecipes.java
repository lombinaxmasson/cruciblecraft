package com.masson.cruciblecraft.content.mold;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;

import net.minecraft.core.Direction;

/**
 * GT6 {@code MultiTileEntityMold.MOLD_RECIPES}: 5×5 bit masks to prefixes.
 * Unmapped non-zero masks are nuggets. Zero is unshaped and cannot fill.
 * Required units are the prefix amount; nuggets cost one nugget per cell.
 */
public final class MoldRecipes {
    public static final int CELL_COUNT = 25;
    public static final float INNER_MIN = 2.0F / 16.0F;
    public static final float INNER_SPAN = 12.0F / 16.0F;

    private static final Map<Integer, MaterialPrefix> RECIPES = new HashMap<>();
    private static final Map<MaterialPrefix, Integer> REPRESENTATIVE_MASKS =
            new LinkedHashMap<>();
    private static final int[] ROTATE_CW = {
        4, 9, 14, 19, 24,
        3, 8, 13, 18, 23,
        2, 7, 12, 17, 22,
        1, 6, 11, 16, 21,
        0, 5, 10, 15, 20
    };

    static {
        Map<Integer, MaterialPrefix> temp = new LinkedHashMap<>();
        put(temp, 0b0_00100_11111_01110_01010_00000, prefix("tool_head_builderwand"));
        put(temp, 0b0_00000_00100_11111_01110_01010, prefix("tool_head_builderwand"));

        put(temp, 0b0_00000_00110_01111_01111_00110, prefix("billet"));
        put(temp, 0b0_00000_01100_11110_11110_01100, prefix("billet"));
        put(temp, 0b0_00110_01111_01111_00110_00000, prefix("billet"));
        put(temp, 0b0_01100_11110_11110_01100_00000, prefix("billet"));

        put(temp, bits(
                0, 1, 2, 3,
                5, 6, 7, 8,
                10, 11, 12, 13, 14,
                15, 16, 17, 18,
                20, 21, 22, 23),
                prefix("tool_head_raw_plow"));

        put(temp, bits(4, 8, 12, 16, 20), MaterialPrefixes.LONG_ROD);
        put(temp, bits(
                0, 1, 2, 3, 4,
                5, 6, 7, 8, 9,
                10, 11, 12, 13, 14,
                15, 16, 17, 18, 19,
                20, 21, 22, 23, 24),
                MaterialPrefixes.PLATE);
        put(temp, bits(
                0, 1, 2, 4,
                5, 6, 7, 9,
                10, 11, 12, 14,
                19,
                20, 21, 22),
                prefix("small_casing"));
        put(temp, bits(
                0, 2, 4,
                6, 7, 8,
                10, 11, 13, 14,
                16, 17, 18,
                20, 22, 24),
                MaterialPrefixes.GEAR);
        put(temp, bits(
                1, 3,
                5, 6, 7, 8, 9,
                11, 13,
                15, 16, 17, 18, 19,
                21, 23),
                MaterialPrefixes.SMALL_GEAR);

        for (int i = 0; i < 3; i++) {
            put(temp, bits(
                    i, i + 1, i + 2,
                    i + 5, i + 6, i + 7,
                    i + 10, i + 11, i + 12,
                    i + 15, i + 16, i + 17,
                    i + 20, i + 21, i + 22),
                    MaterialPrefixes.INGOT);
            put(temp, bits(
                    i, i + 1, i + 2,
                    i + 5, i + 6,
                    i + 10, i + 11,
                    i + 15, i + 16,
                    i + 20, i + 21, i + 22),
                    prefix("tool_head_raw_axe_double"));
            put(temp, bits(
                    i, i + 1, i + 2,
                    i + 5, i + 6, i + 7,
                    i + 10, i + 12,
                    i + 15, i + 16, i + 17,
                    i + 20, i + 21, i + 22),
                    prefix("tool_head_hammer"));
            put(temp, bits(i, i + 5, i + 10, i + 15, i + 20),
                    MaterialPrefixes.ROD);
            put(temp, bits(
                    i, i + 1, i + 2,
                    i + 6,
                    i + 11,
                    i + 16,
                    i + 21),
                    prefix("tool_head_raw_chisel"));
            put(temp, bits(
                    i, i + 1, i + 2,
                    i + 5, i + 6, i + 7,
                    i + 10, i + 11, i + 12,
                    i + 16,
                    i + 21),
                    prefix("tool_head_file"));
            put(temp, bits(
                    i + 1,
                    i + 5, i + 6, i + 7,
                    i + 10, i + 11, i + 12,
                    i + 15, i + 16, i + 17,
                    i + 20, i + 21, i + 22),
                    prefix("tool_head_raw_sword"));
            for (int j = 0; j < 4; j++) {
                put(temp, bits(
                        i + j * 5 + 1, i + j * 5 + 2,
                        i + j * 5 + 5, i + j * 5 + 6, i + j * 5 + 7),
                        prefix("tool_head_raw_hoe"));
            }
            for (int j = 0; j < 3; j++) {
                put(temp, bits(
                        i + j * 5 + 1,
                        i + j * 5 + 6,
                        i + j * 5 + 10, i + j * 5 + 11, i + j * 5 + 12),
                        prefix("tool_head_raw_arrow"));
                put(temp, bits(
                        i + j * 5, i + j * 5 + 1, i + j * 5 + 2,
                        i + j * 5 + 5, i + j * 5 + 6, i + j * 5 + 7,
                        i + j * 5 + 10),
                        prefix("tool_head_raw_axe"));
                put(temp, bits(
                        i + j * 5, i + j * 5 + 1,
                        i + j * 5 + 5, i + j * 5 + 6),
                        prefix("chunk"));
                put(temp, bits(
                        i + j * 5, i + j * 5 + 1, i + j * 5 + 2,
                        i + j * 5 + 5, i + j * 5 + 7,
                        i + j * 5 + 10, i + j * 5 + 11, i + j * 5 + 12),
                        MaterialPrefixes.RING);
                put(temp, bits(
                        i + j * 5, i + j * 5 + 1, i + j * 5 + 2,
                        i + j * 5 + 5, i + j * 5 + 6, i + j * 5 + 7,
                        i + j * 5 + 10, i + j * 5 + 11, i + j * 5 + 12),
                        prefix("tiny_plate"));
                put(temp, bits(i + j * 5, i + j * 5 + 5),
                        MaterialPrefixes.BOLT);
            }
            for (int j = 0; j < 2; j++) {
                put(temp, bits(
                        i + j * 5 + 1,
                        i + j * 5 + 5, i + j * 5 + 6, i + j * 5 + 7,
                        i + j * 5 + 10, i + j * 5 + 11, i + j * 5 + 12,
                        i + j * 5 + 15, i + j * 5 + 16, i + j * 5 + 17),
                        prefix("tool_head_raw_shovel"));
                put(temp, bits(
                        i + j * 5, i + j * 5 + 1, i + j * 5 + 2,
                        i + j * 5 + 5, i + j * 5 + 6, i + j * 5 + 7,
                        i + j * 5 + 10, i + j * 5 + 11, i + j * 5 + 12,
                        i + j * 5 + 15, i + j * 5 + 17),
                        prefix("tool_head_raw_spade"));
                put(temp, bits(
                        i + j * 5 + 1,
                        i + j * 5 + 5, i + j * 5 + 6, i + j * 5 + 7,
                        i + j * 5 + 10, i + j * 5 + 11,
                        i + j * 5 + 15, i + j * 5 + 16, i + j * 5 + 17),
                        prefix("tool_head_raw_universal_spade"));
                put(temp, bits(
                        i + j * 5,
                        i + j * 5 + 5,
                        i + j * 5 + 10,
                        i + j * 5 + 15),
                        prefix("tool_head_screwdriver"));
            }
        }

        for (int i = 0; i < 4; i++) {
            put(temp, bits(
                    i + 1,
                    i + 5,
                    i + 10,
                    i + 15,
                    i + 21),
                    prefix("tool_head_raw_pickaxe"));
            put(temp, bits(
                    i, i + 1,
                    i + 5, i + 6,
                    i + 10, i + 11,
                    i + 15, i + 16,
                    i + 20, i + 21),
                    prefix("tool_head_raw_saw"));
            put(temp, bits(
                    i, i + 1,
                    i + 5, i + 6,
                    i + 10, i + 11,
                    i + 15, i + 16,
                    i + 21),
                    prefix("tool_head_raw_sense"));
        }

        for (var entry : temp.entrySet()) {
            putAllOrientations(entry.getKey(), entry.getValue());
        }
        preferRepresentative(MoldShape.INGOT.mask(), MaterialPrefixes.INGOT);
        preferRepresentative(MoldShape.PLATE.mask(), MaterialPrefixes.PLATE);
        preferRepresentative(MoldShape.ROD.mask(), MaterialPrefixes.ROD);
        preferRepresentative(MoldShape.BOLT.mask(), MaterialPrefixes.BOLT);
        preferRepresentative(0b0_01100_11110_11110_01100_00000, prefix("billet"));
        preferRepresentative(0b0_11000_11000_00000_00000_00000, prefix("chunk"));
        preferRepresentative(0b0_00000_01110_01110_01110_00000, prefix("tiny_plate"));
        preferRepresentative(0b0_10000_01000_00100_00010_00001, MaterialPrefixes.LONG_ROD);
        preferRepresentative(0b0_11101_11101_11101_00001_11100, prefix("small_casing"));
        preferRepresentative(0b0_00000_01110_01010_01110_00000, MaterialPrefixes.RING);
        preferRepresentative(0b0_10101_01110_11011_01110_10101, MaterialPrefixes.GEAR);
        preferRepresentative(0b0_01010_11111_01010_11111_01010, MaterialPrefixes.SMALL_GEAR);
        preferRepresentative(0b0_00100_01110_01110_01110_01110, prefix("tool_head_raw_sword"));
        preferRepresentative(0b0_00000_01110_10001_00000_00000, prefix("tool_head_raw_pickaxe"));
        preferRepresentative(0b0_01110_01110_01110_01010_00000, prefix("tool_head_raw_spade"));
        preferRepresentative(0b0_00100_01110_01110_01110_00000, prefix("tool_head_raw_shovel"));
        preferRepresentative(0b0_00100_01110_01100_01110_00000, prefix("tool_head_raw_universal_spade"));
        preferRepresentative(0b0_00000_01110_01110_01000_00000, prefix("tool_head_raw_axe"));
        preferRepresentative(0b0_00000_11111_11111_10001_00000, prefix("tool_head_raw_axe_double"));
        preferRepresentative(0b0_00000_11111_11111_00000_00000, prefix("tool_head_raw_saw"));
        preferRepresentative(0b0_01110_01110_01010_01110_01110, prefix("tool_head_hammer"));
        preferRepresentative(0b0_01110_01110_01110_00100_00100, prefix("tool_head_file"));
        preferRepresentative(0b0_00000_00100_00100_00100_00100, prefix("tool_head_screwdriver"));
        preferRepresentative(0b0_01110_00100_00100_00100_00100, prefix("tool_head_raw_chisel"));
        preferRepresentative(0b0_00000_00100_00100_01110_00000, prefix("tool_head_raw_arrow"));
        preferRepresentative(0b0_00000_00110_01110_00000_00000, prefix("tool_head_raw_hoe"));
        preferRepresentative(0b0_00000_01111_11111_00000_00000, prefix("tool_head_raw_sense"));
        preferRepresentative(0b0_11111_11111_11111_11111_00100, prefix("tool_head_raw_plow"));
        preferRepresentative(0b0_00000_00100_11111_01110_01010, prefix("tool_head_builderwand"));
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

    /**
     * First GT6 source mask for each mapped prefix, used by EMI and tests.
     */
    public static Map<MaterialPrefix, Integer> representativeMasks() {
        return Map.copyOf(REPRESENTATIVE_MASKS);
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

    public static boolean cellPresent(int shape, int index) {
        return index >= 0
                && index < CELL_COUNT
                && (shape & (1 << index)) == 0;
    }

    /**
     * GT6 {@code getTexture2} after pass 18: hide chiseled cells and the
     * bottom face; show a side only when the neighbour in that direction is
     * already chiseled out.
     */
    public static boolean cellFaceVisible(int shape, int index, Direction face) {
        if (!cellPresent(shape, index) || face == Direction.DOWN) {
            return false;
        }
        return switch (face) {
            case UP -> true;
            case EAST -> index < 20 && (shape & (1 << (index + 5))) != 0;
            case WEST -> index >= 5 && (shape & (1 << (index - 5))) != 0;
            case SOUTH -> index % 5 != 4 && (shape & (1 << (index + 1))) != 0;
            case NORTH -> index % 5 != 0 && (shape & (1 << (index - 1))) != 0;
            default -> false;
        };
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

    private static void put(Map<Integer, MaterialPrefix> temp, int shape, MaterialPrefix prefix) {
        temp.put(shape, prefix);
        REPRESENTATIVE_MASKS.putIfAbsent(prefix, shape);
    }

    private static void preferRepresentative(int mask, MaterialPrefix prefix) {
        if (prefix.equals(RECIPES.get(mask & ((1 << CELL_COUNT) - 1)))) {
            REPRESENTATIVE_MASKS.put(prefix, mask);
        }
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

    private static int bits(int... indices) {
        int shape = 0;
        for (int index : indices) {
            shape |= 1 << index;
        }
        return shape;
    }

    private static MaterialPrefix prefix(String path) {
        return new MaterialPrefix(CrucibleCraft.MODID + ":" + path);
    }
}
