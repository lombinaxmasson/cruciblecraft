package com.masson.cruciblecraft.content.redstonewire;

import java.util.HashSet;
import java.util.Set;

import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Ports {@code ITileEntityRedstoneWire.Util.doRedstoneUpdate} from
 * {@code gt6_code/gregtech6}. {@code MAX_RANGE} is {@code Integer.MAX_VALUE}.
 */
public final class RedstoneWireNetwork {
    public static final long MAX_RANGE = Integer.MAX_VALUE;

    private RedstoneWireNetwork() {}

    public static int bind4(long value) {
        if (value <= 0L) {
            return 0;
        }
        if (value >= 15L) {
            return 15;
        }
        return (int) value;
    }

    public static long divup(long numerator, long denominator) {
        if (numerator <= 0L) {
            return 0L;
        }
        return (numerator + denominator - 1L) / denominator;
    }

    public static int visual(long redstone) {
        return bind4(divup(redstone, MAX_RANGE));
    }

    public static int comparator(long redstone) {
        return bind4(redstone / MAX_RANGE);
    }

    public static void propagate(RedstoneWireBlockEntity start) {
        Level level = start.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        Set<RedstoneWireBlockEntity> updating = new HashSet<>();
        Set<RedstoneWireBlockEntity> next = new HashSet<>();
        updating.add(start);
        while (!updating.isEmpty()) {
            for (RedstoneWireBlockEntity wire : updating) {
                for (Direction side : Direction.values()) {
                    if (!wire.canEmitToWire(side)) {
                        continue;
                    }
                    BlockPos neighborPos = wire.getBlockPos().relative(side);
                    BlockEntity neighbor = level.getBlockEntity(neighborPos);
                    if (neighbor instanceof RedstoneWireBlockEntity other
                            && other.canAcceptFromWire(side.getOpposite())
                            && other.updateRedstone()) {
                        next.add(other);
                    }
                }
            }
            updating.clear();
            updating.addAll(next);
            next.clear();
        }
    }
}
