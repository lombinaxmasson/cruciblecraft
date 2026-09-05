package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.logistics.pipe.PipeTransferPhase;

import net.minecraft.world.level.Level;

/** Maps GT6 cover intervals onto the shared five-tick pipe phase. */
public final class CoverTransferTiming {
    private CoverTransferTiming() {}

    public static boolean due(Level world, int intervalTicks) {
        if (world == null || intervalTicks <= 1) {
            return true;
        }
        int phase = PipeTransferPhase.INTERVAL;
        int period = Math.max(1, (intervalTicks + phase - 1) / phase);
        return Math.floorMod(world.getGameTime() / phase, period) == 0L;
    }
}
