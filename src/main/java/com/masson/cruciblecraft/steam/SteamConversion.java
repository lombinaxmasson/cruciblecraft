package com.masson.cruciblecraft.steam;

/**
 * GT6-inspired steam accounting: 1 mB water plus 80 HU makes 160 mB steam;
 * an engine consumes 2 mB steam for 1 KU.
 */
public final class SteamConversion {
    public static final int HU_PER_BATCH = 80;
    public static final int WATER_PER_BATCH = 1;
    public static final int STEAM_PER_BATCH = 160;
    public static final int STEAM_PER_KU = 2;

    private SteamConversion() {}

    public static int boilerBatches(int water, int steamRoom, int accumulatedHu) {
        if (water <= 0 || steamRoom < STEAM_PER_BATCH || accumulatedHu < HU_PER_BATCH) {
            return 0;
        }
        return Math.min(water, Math.min(steamRoom / STEAM_PER_BATCH, accumulatedHu / HU_PER_BATCH));
    }

    public static int kineticFromSteam(int steam, int kineticRoom) {
        return Math.max(0, Math.min(steam / STEAM_PER_KU, kineticRoom));
    }
}
