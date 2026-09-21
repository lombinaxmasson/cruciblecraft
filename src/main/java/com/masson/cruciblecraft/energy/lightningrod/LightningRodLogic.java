package com.masson.cruciblecraft.energy.lightningrod;

import net.minecraft.util.RandomSource;

/**
 * GT6 {@code MultiTileEntityLightningRod.onTick2} strike and emit numbers.
 * {@code VREC[6] = 32768}, capacity {@code 18000 * VREC[6]}, emit 16 amps
 * downward.
 */
public final class LightningRodLogic {
    public static final long PACKET = 32_768L;
    public static final int AMPS = 16;
    public static final long CAPACITY = 18_000L * PACKET;
    public static final int TIP_MIN_Y = 100;
    public static final int COMPETITION_RANGE = 256;
    /** GT6 only evaluates the rod on this 1200-tick phase. */
    public static final long TICK_PERIOD = 1_200L;
    public static final long TICK_PHASE = 300L;
    /** GT6's {@code mSize < 100} gate excludes 100-block rods. */
    public static final int MAX_ACTIVE_SIZE = 100;

    private LightningRodLogic() {}

    public static boolean tickDue(long gameTime) {
        return Math.floorMod(gameTime, TICK_PERIOD) == TICK_PHASE;
    }

    public static boolean tipHighEnough(int tipY) {
        return tipY >= TIP_MIN_Y;
    }

    /**
     * GT6's literal strike gate is {@code yCoord + mSize >= 100}; the bolt is
     * then created four blocks above that coordinate.
     */
    public static boolean sourceHeightHighEnough(int controllerY, int size) {
        return size > 0 && controllerY + size >= TIP_MIN_Y;
    }

    public static boolean weatherRoll(
            RandomSource rng,
            int size,
            boolean thunder,
            boolean rain) {
        if (size <= 0 || rng == null) {
            return false;
        }
        if (rng.nextInt(1_000_000) >= Math.min(100, size)) {
            return false;
        }
        return thunder || (rain && rng.nextInt(10) == 0);
    }

    public static boolean winsCompetition(RandomSource rng, int includingSelf) {
        if (rng == null || includingSelf <= 0) {
            return false;
        }
        return rng.nextInt(includingSelf) == 0;
    }

    public static long emitDrain(long stored, long packetsAccepted) {
        if (stored < PACKET) {
            return 0L;
        }
        // GT6 consumes at least one packet whenever an output attempt occurs,
        // even when no adjacent consumer accepts it.
        long packets = Math.max(1L, packetsAccepted);
        return Math.max(0L, stored - packets * PACKET);
    }
}
