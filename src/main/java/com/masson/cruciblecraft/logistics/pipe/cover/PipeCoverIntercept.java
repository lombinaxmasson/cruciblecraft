package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireCovers;
import com.masson.cruciblecraft.logistics.core.LogisticsDumpKinds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * GT6 {@code interceptConnect} / {@code interceptCoverPlacement} for pipe
 * logistics, item filters, and pressure valves.
 */
public final class PipeCoverIntercept {
    public enum NeighborKind {
        ITEM_PIPE,
        FLUID_PIPE,
        OTHER
    }

    private PipeCoverIntercept() {}

    public static NeighborKind kind(BlockEntity blockEntity) {
        if (blockEntity instanceof ItemPipeBlockEntity) {
            return NeighborKind.ITEM_PIPE;
        }
        if (blockEntity instanceof FluidPipeBlockEntity) {
            return NeighborKind.FLUID_PIPE;
        }
        return NeighborKind.OTHER;
    }

    public static boolean isLogisticsPath(String path) {
        return path != null
                && LogisticsDumpKinds.KNOWN_LOGISTICS_PATHS.contains(path)
                && !path.startsWith("logistics_display_cpu");
    }

    public static boolean allowsPlacement(
            PipeCover cover,
            NeighborKind self,
            NeighborKind neighbor,
            int tankCount) {
        if (cover == null) {
            return false;
        }
        String path = cover.definitionId().getPath();
        if ("filter".equals(path)
                && self == NeighborKind.ITEM_PIPE
                && neighbor == NeighborKind.ITEM_PIPE) {
            return false;
        }
        if ("pressure_valve".equals(path)) {
            return self == NeighborKind.FLUID_PIPE
                    && tankCount == 1
                    && neighbor != NeighborKind.FLUID_PIPE;
        }
        return true;
    }

    public static boolean interceptConnect(
            PipeCover cover, NeighborKind neighbor) {
        return interceptConnect(cover, NeighborKind.OTHER, neighbor);
    }

    public static boolean interceptConnect(
            PipeCover cover, NeighborKind self, NeighborKind neighbor) {
        if (cover == null) {
            return false;
        }
        String path = cover.definitionId().getPath();
        if ("filter".equals(path)) {
            return self == NeighborKind.ITEM_PIPE
                    && neighbor == NeighborKind.ITEM_PIPE;
        }
        if (isLogisticsPath(path)) {
            return true;
        }
        return "pressure_valve".equals(path)
                && neighbor == NeighborKind.FLUID_PIPE;
    }

    public static boolean blocksConnect(
            Level level, BlockPos pos, Direction side) {
        if (RedstoneWireCovers.blocksConnect(level, pos, side)) {
            return true;
        }
        if (level == null || pos == null || side == null) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        PipeCover cover = coverOn(blockEntity, side);
        NeighborKind self = kind(blockEntity);
        NeighborKind neighbor = kind(
                level.getBlockEntity(pos.relative(side)));
        return interceptConnect(cover, self, neighbor);
    }

    private static PipeCover coverOn(
            BlockEntity blockEntity, Direction side) {
        if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            return pipe.coverSnapshot().get(side);
        }
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            return pipe.coverSnapshot().get(side);
        }
        return null;
    }
}
