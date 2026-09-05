package com.masson.cruciblecraft.logistics.displaycpu;

import java.util.Map;

import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Writes previous-window used/capacity onto scanned Display covers. */
public final class DisplayCpuWriteback {
    public record Load(
            int logicUsed,
            int logicCapacity,
            int controlUsed,
            int controlCapacity,
            int storageUsed,
            int storageCapacity,
            int conversionUsed,
            int conversionCapacity) {}

    private DisplayCpuWriteback() {}

    public static void write(
            Level level, BlockPos pipe, Direction side, Load load) {
        if (level == null || pipe == null || side == null || load == null) {
            return;
        }
        BlockEntity be = level.getBlockEntity(pipe);
        PipeCover cover = coverAt(be, side);
        if (cover == null) {
            return;
        }
        DisplayCpuKinds.Kind kind =
                DisplayCpuKinds.kind(cover.definitionId()).orElse(null);
        if (kind == null) {
            return;
        }
        int used;
        int capacity;
        switch (kind) {
            case LOGIC -> {
                used = load.logicUsed();
                capacity = load.logicCapacity();
            }
            case CONTROL -> {
                used = load.controlUsed();
                capacity = load.controlCapacity();
            }
            case STORAGE -> {
                used = load.storageUsed();
                capacity = load.storageCapacity();
            }
            case CONVERSION -> {
                used = load.conversionUsed();
                capacity = load.conversionCapacity();
            }
            default -> {
                return;
            }
        }
        PipeCover changed = cover.withDisplay(
                DisplayCpuLevels.visual(used, capacity),
                DisplayCpuLevels.redstone(used, capacity));
        if (be instanceof ItemPipeBlockEntity item) {
            item.replaceCoverQuiet(side, changed);
        } else if (be instanceof FluidPipeBlockEntity fluid) {
            fluid.replaceCoverQuiet(side, changed);
        }
    }

    public static int redstoneOut(BlockEntity be, Direction queryDirection) {
        if (be == null || queryDirection == null) {
            return 0;
        }
        PipeCover cover = coverAt(be, queryDirection.getOpposite());
        if (cover == null || !DisplayCpuKinds.isDisplay(cover.definitionId())) {
            return 0;
        }
        return cover.config().redstone();
    }

    public static boolean hasDisplay(BlockEntity be, Direction face) {
        PipeCover cover = coverAt(be, face);
        return cover != null && DisplayCpuKinds.isDisplay(cover.definitionId());
    }

    public static boolean hasAnyDisplay(BlockEntity be) {
        Map<Direction, PipeCover> covers = snapshot(be);
        if (covers == null) {
            return false;
        }
        for (PipeCover cover : covers.values()) {
            if (DisplayCpuKinds.isDisplay(cover.definitionId())) {
                return true;
            }
        }
        return false;
    }

    private static PipeCover coverAt(BlockEntity be, Direction side) {
        Map<Direction, PipeCover> covers = snapshot(be);
        return covers == null ? null : covers.get(side);
    }

    private static Map<Direction, PipeCover> snapshot(BlockEntity be) {
        if (be instanceof ItemPipeBlockEntity item) {
            return item.coverSnapshot();
        }
        if (be instanceof FluidPipeBlockEntity fluid) {
            return fluid.coverSnapshot();
        }
        return null;
    }
}
