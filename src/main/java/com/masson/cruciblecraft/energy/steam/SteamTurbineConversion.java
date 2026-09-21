package com.masson.cruciblecraft.energy.steam;

import com.masson.cruciblecraft.energy.drive.RotationEngineConversion;

/**
 * GT6 {@code MultiTileEntityTurbineSteam}/{@code LargeTurbineSteam}
 * {@code doConversion} plus {@code TE_Behavior_Energy_Converter} with
 * {@code NBT_WASTE_ENERGY}. RU is size-relevant: one packet of
 * {@code units(stored, inputRec, outputRec)}.
 */
public final class SteamTurbineConversion {
    private SteamTurbineConversion() {}

    public record Tick(
            boolean canEmit,
            boolean overloaded,
            boolean fast,
            long packetSize,
            long storedAfterWaste) {}

    /**
     * Steam dump threshold: {@code getEnergySizeInputMin * 2}. For these
     * rows {@code NBT_INPUT > 16}, so min is {@code input/2} and the
     * threshold equals {@code steamInputMax}.
     */
    public static int conversionThresholdMb(SteamTurbineCatalog.Profile profile) {
        require(profile);
        return profile.steamInputMax();
    }

    public static long ruFromSteam(
            int steam, SteamTurbineCatalog.Profile profile) {
        require(profile);
        if (steam <= 0) {
            return 0L;
        }
        return steam / (long) profile.steamPerEu();
    }

    public static long outputMin(SteamTurbineCatalog.Profile profile) {
        require(profile);
        return profile.ruOutput() / 2L;
    }

    public static long outputMax(SteamTurbineCatalog.Profile profile) {
        require(profile);
        return Math.multiplyExact(profile.ruOutput(), 2L);
    }

    /**
     * Converter half of {@code doConversion} after steam has already been
     * credited into {@code stored}. Overload zeros the capacitor and skips
     * waste. Otherwise emit when {@code tOutput >= outputMin}, then waste
     * {@code inputMax}.
     */
    public static Tick emit(long stored, SteamTurbineCatalog.Profile profile) {
        require(profile);
        long tOutput = RotationEngineConversion.units(
                stored, profile.steamInputMax(), profile.ruOutput(), false);
        boolean canEmit = tOutput >= outputMin(profile);
        boolean fast = canEmit && tOutput > profile.ruOutput();
        if (canEmit && tOutput > outputMax(profile)) {
            return new Tick(false, true, false, tOutput, 0L);
        }
        long remaining = Math.max(0L, stored - profile.steamInputMax());
        return new Tick(canEmit, false, fast, canEmit ? tOutput : 0L, remaining);
    }

    private static void require(SteamTurbineCatalog.Profile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("steam turbine profile");
        }
    }
}
