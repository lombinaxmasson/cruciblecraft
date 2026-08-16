package com.masson.cruciblecraft.steam;

import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;

/**
 * Catalog-backed integer steam accounting for the selected bronze source rows.
 */
public final class SteamConversion {
    private static final EnergyConverterProfile BOILER =
            EnergyConverterCatalog.require(
                    "cruciblecraft:bronze_boiler");
    private static final EnergyConverterProfile ENGINE =
            EnergyConverterCatalog.require(
                    "cruciblecraft:bronze_steam_engine");
    public static final int HU_PER_BATCH =
            BOILER.conservation().primaryInputUnits();
    public static final int WATER_PER_BATCH =
            BOILER.conservation().secondaryInputUnits();
    public static final int STEAM_PER_BATCH =
            BOILER.conservation().outputUnits();
    /** SOURCE_BACKED source-1302 conversion input: 200 mB steam. */
    public static final int ENGINE_STEAM_PER_BATCH =
            ENGINE.conservation().primaryInputUnits();
    /** SOURCE_BACKED source-1302 conversion output: 50 KU (4 mB/KU). */
    public static final int KU_PER_ENGINE_BATCH =
            ENGINE.conservation().outputUnits();
    public static final int EXHAUST_WATER_PER_BATCH =
            ENGINE.conservation().exhaustUnits();

    private SteamConversion() {}

    public static int boilerBatches(int water, int steamRoom, int accumulatedHu) {
        if (water <= 0 || steamRoom < STEAM_PER_BATCH || accumulatedHu < HU_PER_BATCH) {
            return 0;
        }
        return Math.min(
                water / WATER_PER_BATCH,
                Math.min(
                        steamRoom / STEAM_PER_BATCH,
                        accumulatedHu / HU_PER_BATCH));
    }

    public static int engineBatches(
            int steam,
            long kineticRoom,
            int exhaustRoom) {
        if (steam < ENGINE_STEAM_PER_BATCH
                || kineticRoom < KU_PER_ENGINE_BATCH
                || exhaustRoom < EXHAUST_WATER_PER_BATCH) {
            return 0;
        }
        long batches = Math.min(
                steam / ENGINE_STEAM_PER_BATCH,
                Math.min(
                        kineticRoom / KU_PER_ENGINE_BATCH,
                        exhaustRoom / EXHAUST_WATER_PER_BATCH));
        return (int) Math.min(Integer.MAX_VALUE, batches);
    }
}
