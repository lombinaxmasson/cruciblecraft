package com.masson.cruciblecraft.compat.jade.observation;

import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerTier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Long-capacity Jade snapshot for the GT6 large boiler controller. */
public record LargeBoilerObservation(
        ObservationField<Long> waterAmount,
        ObservationField<Long> waterCapacity,
        ObservationField<String> waterId,
        ObservationField<Long> steamAmount,
        ObservationField<Long> steamCapacity,
        ObservationField<String> steamId,
        ObservationField<Long> heat,
        ObservationField<Integer> efficiency,
        ObservationField<Integer> barometer,
        ObservationField<Boolean> structureValid,
        ObservationField<Boolean> pluginQuarantined) {
    public static final String WATER_AMOUNT = "cc_large_boiler_water_amount";
    public static final String WATER_CAPACITY = "cc_large_boiler_water_capacity";
    public static final String WATER_ID = "cc_large_boiler_water_id";
    public static final String STEAM_AMOUNT = "cc_large_boiler_steam_amount";
    public static final String STEAM_CAPACITY = "cc_large_boiler_steam_capacity";
    public static final String STEAM_ID = "cc_large_boiler_steam_id";
    public static final String HEAT = "cc_large_boiler_heat";
    public static final String EFFICIENCY = "cc_large_boiler_efficiency";
    public static final String BAROMETER = "cc_large_boiler_barometer";
    public static final String STRUCTURE = "cc_large_boiler_structure";
    public static final String QUARANTINED = "cc_large_boiler_quarantined";

    public static LargeBoilerObservation fromServerData(CompoundTag data) {
        return new LargeBoilerObservation(
                longField(data, WATER_AMOUNT),
                longField(data, WATER_CAPACITY),
                stringField(data, WATER_ID),
                longField(data, STEAM_AMOUNT),
                longField(data, STEAM_CAPACITY),
                stringField(data, STEAM_ID),
                longField(data, HEAT),
                intField(data, EFFICIENCY),
                intField(data, BAROMETER),
                boolField(data, STRUCTURE),
                boolField(data, QUARANTINED));
    }

    public static void writeServerData(
            CompoundTag data, LargeBoilerBlockEntity boiler) {
        data.putLong(WATER_AMOUNT, boiler.waterTank().getFluidAmount());
        data.putLong(WATER_CAPACITY, LargeBoilerTier.WATER_CAPACITY);
        data.putString(WATER_ID, fluidId(boiler.waterTank().getFluid()));
        data.putLong(STEAM_AMOUNT, boiler.steamAmountLong());
        data.putLong(STEAM_CAPACITY, boiler.steamCapacityLong());
        data.putString(STEAM_ID, fluidId(boiler.steamTank().getFluid()));
        data.putLong(HEAT, boiler.heatAmount());
        data.putInt(EFFICIENCY, boiler.efficiency());
        data.putInt(BAROMETER, boiler.barometer());
        data.putBoolean(STRUCTURE, boiler.structureValid());
        data.putBoolean(QUARANTINED, boiler.pluginQuarantined());
    }

    private static String fluidId(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            return "";
        }
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
        return id == null ? "" : id.toString();
    }

    private static ObservationField<Long> longField(
            CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getLong(key))
                : ObservationField.unavailable();
    }

    private static ObservationField<Integer> intField(
            CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getInt(key))
                : ObservationField.unavailable();
    }

    private static ObservationField<Boolean> boolField(
            CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getBoolean(key))
                : ObservationField.unavailable();
    }

    private static ObservationField<String> stringField(
            CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getString(key))
                : ObservationField.unavailable();
    }
}
