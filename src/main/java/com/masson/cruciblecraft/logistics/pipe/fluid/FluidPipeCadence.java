package com.masson.cruciblecraft.logistics.pipe.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * GT6 {@code WD.even} plus {@code SERVER_TICK_PRE} / {@code SERVER_TICK_PR2}.
 * Both GT6 phases still run every server tick; even/odd only chooses scan
 * order. This is not the five-tick cover pump phase.
 */
public final class FluidPipeCadence {
    private static final Direction[] EVEN_ORDER = Direction.values();
    private static final Direction[] ODD_ORDER;

    static {
        Direction[] reversed = EVEN_ORDER.clone();
        for (int i = 0, j = reversed.length - 1; i < j; i++, j--) {
            Direction swap = reversed[i];
            reversed[i] = reversed[j];
            reversed[j] = swap;
        }
        ODD_ORDER = reversed;
    }

    private FluidPipeCadence() {}

    /** Matches {@code gregapi.util.WD.even(int...)}. */
    public static boolean even(BlockPos pos) {
        int evenCount = 0;
        if (pos.getX() % 2 == 0) {
            evenCount++;
        }
        if (pos.getY() % 2 == 0) {
            evenCount++;
        }
        if (pos.getZ() % 2 == 0) {
            evenCount++;
        }
        return evenCount % 2 == 0;
    }

    public static Direction[] scanOrder(BlockPos pos) {
        return even(pos) ? EVEN_ORDER : ODD_ORDER;
    }
}
