package com.masson.cruciblecraft.logistics.core;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.content.block.LogisticsCoreBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Formed 5x5x5 CPU mix and wall bindings. */
public final class LogisticsCoreStructure {
    public record Result(
            boolean formed,
            int logic,
            int control,
            int storage,
            int conversion,
            List<BlockPos> walls) {
        public static Result broken() {
            return new Result(false, 0, 0, 0, 0, List.of());
        }
    }

    private LogisticsCoreStructure() {}

    public static BlockPos center(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite(), LogisticsCoreGeometry.HALF);
    }

    public static Direction facingOf(BlockState state) {
        if (state.hasProperty(LogisticsCoreBlock.FACING)) {
            return state.getValue(LogisticsCoreBlock.FACING);
        }
        return Direction.NORTH;
    }

    public static Result check(Level level, BlockPos controller, Direction facing) {
        if (level == null || controller == null || facing == null) {
            return Result.broken();
        }
        BlockPos center = center(controller, facing);
        int logic = 0;
        int control = 0;
        int storage = 0;
        int conversion = 0;
        ArrayList<BlockPos> walls = new ArrayList<>();
        for (int i = -LogisticsCoreGeometry.HALF;
                i <= LogisticsCoreGeometry.HALF;
                i++) {
            for (int j = -LogisticsCoreGeometry.HALF;
                    j <= LogisticsCoreGeometry.HALF;
                    j++) {
                for (int k = -LogisticsCoreGeometry.HALF;
                        k <= LogisticsCoreGeometry.HALF;
                        k++) {
                    BlockPos pos = center.offset(i, j, k);
                    if (!level.hasChunkAt(pos)) {
                        return Result.broken();
                    }
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();
                    LogisticsCorePart part = LogisticsCorePart.of(block);
                    LogisticsCoreGeometry.CellKind kind =
                            LogisticsCoreGeometry.cell(i, j, k);
                    if (pos.equals(controller)) {
                        if (kind != LogisticsCoreGeometry.CellKind.VENT
                                || part != LogisticsCorePart.CONTROLLER) {
                            return Result.broken();
                        }
                        continue;
                    }
                    if (part == null) {
                        return Result.broken();
                    }
                    switch (kind) {
                        case INNER -> {
                            if (!part.innerAllowed()) {
                                return Result.broken();
                            }
                            switch (part) {
                                case VERSATILE -> {
                                    logic++;
                                    control++;
                                    storage++;
                                    conversion++;
                                }
                                case LOGIC -> logic += 4;
                                case CONTROL -> control += 4;
                                case STORAGE -> storage += 4;
                                case CONVERSION -> conversion += 4;
                                default -> {
                                }
                            }
                        }
                        case WALL -> {
                            if (part != LogisticsCorePart.WALL) {
                                return Result.broken();
                            }
                            walls.add(pos.immutable());
                        }
                        case VENT -> {
                            if (part != LogisticsCorePart.VENT) {
                                return Result.broken();
                            }
                        }
                    }
                }
            }
        }
        if (storage > LogisticsCoreGeometry.MAX_STORAGE_CPU) {
            storage = LogisticsCoreGeometry.MAX_STORAGE_CPU;
        }
        if (logic <= 0 || control <= 0 || storage <= 0 || conversion <= 0) {
            return Result.broken();
        }
        return new Result(true, logic, control, storage, conversion, List.copyOf(walls));
    }

    public static void bindWalls(
            Level level, BlockPos controller, List<BlockPos> walls) {
        for (BlockPos wall : walls) {
            if (level.getBlockEntity(wall)
                    instanceof MteInPlaceBlockEntity be) {
                be.bindLogisticsCore(controller);
            }
        }
    }

    public static void unbindWalls(Level level, List<BlockPos> walls) {
        for (BlockPos wall : walls) {
            if (level.getBlockEntity(wall)
                    instanceof MteInPlaceBlockEntity be) {
                be.unbindLogisticsCore();
            }
        }
    }
}
