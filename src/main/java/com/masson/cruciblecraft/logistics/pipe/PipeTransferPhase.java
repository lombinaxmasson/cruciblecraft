package com.masson.cruciblecraft.logistics.pipe;

import net.minecraft.core.BlockPos;

/** Shared staggered five-tick phase for cover pumps. Fluid pipe-to-pipe
 * distribution is every server tick and must not read this interval. */
public final class PipeTransferPhase {
    public static final int INTERVAL = 5;

    public static boolean isDue(long gameTime, BlockPos position) {
        return Math.floorMod(
                        gameTime
                                + position.getX() * 31L
                                + position.getY() * 17L
                                + position.getZ(),
                        INTERVAL)
                == 0L;
    }

    private PipeTransferPhase() {}
}
