package com.masson.cruciblecraft.steam;

import java.util.Optional;

import com.masson.cruciblecraft.energy.converter.EnergyConverterCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

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
    public static final String DISTILLED_WATER_ID = "water_distilled";

    private SteamConversion() {}

    public static Optional<Fluid> distilledWater() {
        return ModFluids.materialFluid(DISTILLED_WATER_ID);
    }

    public static boolean isDistilledWater(FluidStack stack) {
        return !stack.isEmpty() && distilledWater().map(stack::is).orElse(false);
    }

    public static FluidStack distilledExhaust(int amount) {
        if (amount <= 0) {
            return FluidStack.EMPTY;
        }
        Fluid fluid = distilledWater().orElseThrow(() -> new IllegalStateException(
                "Steam engine DistW exhaust requires registered water_distilled"));
        return new FluidStack(fluid, amount);
    }

    public static FluidStack migrateLegacyExhaust(FluidStack stored) {
        if (stored.isEmpty() || isDistilledWater(stored)) {
            return stored;
        }
        if (stored.is(Fluids.WATER)) {
            return distilledExhaust(stored.getAmount());
        }
        return stored;
    }

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

    /**
     * GT6 {@code mTank.amount() / STEAM_PER_WATER}. Conversion is not gated
     * on KU room or DistW space; leftover DistW is pushed then trashed.
     */
    public static int engineBatches(int steam) {
        if (steam < ENGINE_STEAM_PER_BATCH) {
            return 0;
        }
        return steam / ENGINE_STEAM_PER_BATCH;
    }
}
