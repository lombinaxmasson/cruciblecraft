package com.masson.cruciblecraft.fusion;

import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Source-backed GT6 fusion octagon: 19×19 layers plus a 5×5×5 core.
 *
 * <p>Copied from {@code MultiTileEntityFusionReactor.checkStructure2} /
 * {@code OCTAGONS}. GameTests assert {@link #counts()} rather than assembling
 * the 19×19 hull in a 7×7 template.
 */
public final class FusionStructure {
    public static final int IRIDIUM_COILS = 144;
    public static final int TUNGSTENSTEEL_WALLS = 576;
    public static final int VENTILATION_UNITS = 50;
    public static final int STAINLESS_STEEL_WALLS = 36;
    public static final int GALVANIZED_STEEL_WALLS = 53;
    public static final int VERSATILE_PROCESSORS = 3;
    public static final int LOGIC_PROCESSORS = 12;
    public static final int CONTROL_PROCESSORS = 12;

    private static final String[][] OCTAGONS = {
        {
            "0000000111110000000",
            "0000001000001000000",
            "0000010000000100000",
            "0000100000000010000",
            "0001000111110001000",
            "0010001000001000100",
            "0100010000000100010",
            "1000100000000010001",
            "1000100000000010001",
            "1000100000000010001",
            "1000100000000010001",
            "1000100000000010001",
            "0100010000000100010",
            "0010001000001000100",
            "0001000111110001000",
            "0000100000000010000",
            "0000010000000100000",
            "0000001000001000000",
            "0000000111110000000"
        },
        {
            "0000000000000000000",
            "0000000111110000000",
            "0000001000001000000",
            "0000010111110100000",
            "0000101000001010000",
            "0001010000000101000",
            "0010100000000010100",
            "0101000000000001010",
            "0101000000000001010",
            "0101000000000001010",
            "0101000000000001010",
            "0101000000000001010",
            "0010100000000010100",
            "0001010000000101000",
            "0000101000001010000",
            "0000010111110100000",
            "0000001000001000000",
            "0000000111110000000",
            "0000000000000000000"
        },
        {
            "0000000000000000000",
            "0000000000000000000",
            "0000000111110000000",
            "0000001000001000000",
            "0000010000000100000",
            "0000100000000010000",
            "0001000000000001000",
            "0010000000000000100",
            "0010000000000000100",
            "0010000000000000100",
            "0010000000000000100",
            "0010000000000000100",
            "0001000000000001000",
            "0000100000000010000",
            "0000010000000100000",
            "0000001000001000000",
            "0000000111110000000",
            "0000000000000000000",
            "0000000000000000000"
        }
    };

    private FusionStructure() {}

    public record PartCounts(
            int iridiumCoils,
            int tungstensteelWalls,
            int ventilationUnits,
            int stainlessSteelWalls,
            int galvanizedSteelWalls,
            int versatileProcessors,
            int logicProcessors,
            int controlProcessors) {
        public boolean matchesTooltip() {
            return iridiumCoils == IRIDIUM_COILS
                    && tungstensteelWalls == TUNGSTENSTEEL_WALLS
                    && ventilationUnits == VENTILATION_UNITS
                    && stainlessSteelWalls == STAINLESS_STEEL_WALLS
                    && galvanizedSteelWalls == GALVANIZED_STEEL_WALLS
                    && versatileProcessors == VERSATILE_PROCESSORS
                    && logicProcessors == LOGIC_PROCESSORS
                    && controlProcessors == CONTROL_PROCESSORS;
        }
    }

    public static BlockPos center(BlockPos controller, Direction facing) {
        return controller.relative(facing, 2);
    }

    public static PartCounts counts() {
        int processors = 0;
        int vents = 0;
        int galvanized = 0;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                for (int k = -2; k <= 2; k++) {
                    int radius = i * i + j * j + k * k;
                    boolean plus = j == 0
                            && ((Math.abs(i) == 2 && k == 0)
                                    || (Math.abs(k) == 2 && i == 0));
                    if (radius < 4) {
                        processors++;
                    } else if (radius > 6 || plus) {
                        galvanized++;
                    } else {
                        vents++;
                    }
                }
            }
        }
        galvanized += 6;
        galvanized -= 1;
        if (processors
                != VERSATILE_PROCESSORS + LOGIC_PROCESSORS + CONTROL_PROCESSORS) {
            throw new IllegalStateException(
                    "Fusion processor cells drifted: " + processors);
        }
        int layer0 = occupied(0);
        int layer1 = occupied(1);
        int layer2 = occupied(2);
        return new PartCounts(
                layer1 + layer2 * 2,
                layer0 * 3 + layer1 * 4 + layer2 * 2,
                vents,
                layer2,
                galvanized,
                VERSATILE_PROCESSORS,
                LOGIC_PROCESSORS,
                CONTROL_PROCESSORS);
    }

    public static boolean check(Level level, BlockPos controller, Direction facing) {
        BlockPos center = center(controller, facing);
        int versatile = VERSATILE_PROCESSORS;
        int logic = LOGIC_PROCESSORS;
        int control = CONTROL_PROCESSORS;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                for (int k = -2; k <= 2; k++) {
                    BlockPos pos = center.offset(i, j, k);
                    int radius = i * i + j * j + k * k;
                    boolean plus = j == 0
                            && ((Math.abs(i) == 2 && k == 0)
                                    || (Math.abs(k) == 2 && i == 0));
                    if (radius < 4) {
                        Block block = level.getBlockState(pos).getBlock();
                        if (block == ModBlocks.VERSATILE_PROCESSOR_UNIT.get()) {
                            versatile--;
                        } else if (block == ModBlocks.LOGIC_PROCESSOR_UNIT.get()) {
                            logic--;
                        } else if (block == ModBlocks.CONTROL_PROCESSOR_UNIT.get()) {
                            control--;
                        } else {
                            return false;
                        }
                    } else if (pos.equals(controller)) {
                        continue;
                    } else if (radius > 6 || plus) {
                        if (!is(level, pos, ModBlocks.GALVANIZED_STEEL_WALL.get())) {
                            return false;
                        }
                    } else if (!is(level, pos, ModBlocks.VENTILATION_UNIT.get())) {
                        return false;
                    }
                }
            }
        }
        if (versatile != 0 || logic != 0 || control != 0) {
            return false;
        }
        if (facing != Direction.WEST
                && (!is(level, center.offset(-3, 0, 0), ModBlocks.GALVANIZED_STEEL_WALL.get())
                        || !is(level, center.offset(-4, 0, 0), ModBlocks.GALVANIZED_STEEL_WALL.get()))) {
            return false;
        }
        if (facing != Direction.EAST
                && (!is(level, center.offset(3, 0, 0), ModBlocks.GALVANIZED_STEEL_WALL.get())
                        || !is(level, center.offset(4, 0, 0), ModBlocks.GALVANIZED_STEEL_WALL.get()))) {
            return false;
        }
        if (facing != Direction.NORTH
                && (!is(level, center.offset(0, 0, -3), ModBlocks.GALVANIZED_STEEL_WALL.get())
                        || !is(level, center.offset(0, 0, -4), ModBlocks.GALVANIZED_STEEL_WALL.get()))) {
            return false;
        }
        if (facing != Direction.SOUTH
                && (!is(level, center.offset(0, 0, 3), ModBlocks.GALVANIZED_STEEL_WALL.get())
                        || !is(level, center.offset(0, 0, 4), ModBlocks.GALVANIZED_STEEL_WALL.get()))) {
            return false;
        }
        BlockPos origin = center.offset(-9, 0, -9);
        for (int i = 0; i < 19; i++) {
            for (int j = 0; j < 19; j++) {
                if (occupied(0, i, j)) {
                    if (!is(level, origin.offset(i, -1, j), ModBlocks.TUNGSTENSTEEL_WALL.get())
                            || !is(level, origin.offset(i, 0, j), ModBlocks.TUNGSTENSTEEL_WALL.get())
                            || !is(level, origin.offset(i, 1, j), ModBlocks.TUNGSTENSTEEL_WALL.get())) {
                        return false;
                    }
                }
                if (occupied(1, i, j)) {
                    if (!is(level, origin.offset(i, -2, j), ModBlocks.TUNGSTENSTEEL_WALL.get())
                            || !is(level, origin.offset(i, -1, j), ModBlocks.TUNGSTENSTEEL_WALL.get())
                            || !is(level, origin.offset(i, 0, j), ModBlocks.LARGE_IRIDIUM_COIL.get())
                            || !is(level, origin.offset(i, 1, j), ModBlocks.TUNGSTENSTEEL_WALL.get())
                            || !is(level, origin.offset(i, 2, j), ModBlocks.TUNGSTENSTEEL_WALL.get())) {
                        return false;
                    }
                }
                if (occupied(2, i, j)) {
                    if (!is(level, origin.offset(i, -2, j), ModBlocks.TUNGSTENSTEEL_WALL.get())
                            || !is(level, origin.offset(i, -1, j), ModBlocks.LARGE_IRIDIUM_COIL.get())
                            || !is(level, origin.offset(i, 0, j), ModBlocks.STAINLESS_STEEL_WALL.get())
                            || !is(level, origin.offset(i, 1, j), ModBlocks.LARGE_IRIDIUM_COIL.get())
                            || !is(level, origin.offset(i, 2, j), ModBlocks.TUNGSTENSTEEL_WALL.get())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean occupied(int layer, int i, int j) {
        return OCTAGONS[layer][i].charAt(j) == '1';
    }

    private static int occupied(int layer) {
        int cells = 0;
        for (int i = 0; i < 19; i++) {
            for (int j = 0; j < 19; j++) {
                if (occupied(layer, i, j)) {
                    cells++;
                }
            }
        }
        return cells;
    }

    private static boolean is(Level level, BlockPos pos, Block expected) {
        return level.getBlockState(pos).getBlock() == expected;
    }
}
