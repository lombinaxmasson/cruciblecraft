package com.masson.cruciblecraft.compat.jade.observation;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.energy.battery.BatteryBlockEntity;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryProfile;

import net.minecraft.nbt.CompoundTag;

/** Shared Jade contract for all 37 battery identities. */
public record BatteryObservation(
        ObservationField<String> energyType,
        ObservationField<Long> stored,
        ObservationField<Long> capacity,
        ObservationField<Long> sizeMin,
        ObservationField<Long> sizeMax,
        ObservationField<Long> inputSize) {
    public static final String ENERGY_TYPE = "cc_battery_energy_type";
    public static final String STORED = "cc_battery_stored";
    public static final String CAPACITY = "cc_battery_capacity";
    public static final String SIZE_MIN = "cc_battery_size_min";
    public static final String SIZE_MAX = "cc_battery_size_max";
    public static final String INPUT_SIZE = "cc_battery_input_size";

    public static BatteryObservation fromServerData(CompoundTag data) {
        return new BatteryObservation(
                stringField(data, ENERGY_TYPE),
                longField(data, STORED),
                longField(data, CAPACITY),
                longField(data, SIZE_MIN),
                longField(data, SIZE_MAX),
                longField(data, INPUT_SIZE));
    }

    public static void writeServerData(
            CompoundTag data, BatteryBlockEntity battery) {
        EnergyBatteryProfile profile = battery.profile();
        data.putString(
                ENERGY_TYPE,
                profile.energyType() == EnergyType.LU ? "LU" : "EU");
        data.putLong(STORED, battery.stored(profile.energyType()));
        data.putLong(CAPACITY, battery.capacity(profile.energyType()));
        data.putLong(SIZE_MIN, profile.sizeMin());
        data.putLong(SIZE_MAX, profile.sizeMax());
        data.putLong(INPUT_SIZE, profile.inputSize());
    }

    public static BatteryObservation fromSnapshot(
            String energyType,
            long stored,
            long capacity,
            long sizeMin,
            long sizeMax,
            long inputSize) {
        return new BatteryObservation(
                ObservationField.of(energyType),
                ObservationField.of(stored),
                ObservationField.of(capacity),
                ObservationField.of(sizeMin),
                ObservationField.of(sizeMax),
                ObservationField.of(inputSize));
    }

    private static ObservationField<Long> longField(CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getLong(key))
                : ObservationField.unavailable();
    }

    private static ObservationField<String> stringField(CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getString(key))
                : ObservationField.unavailable();
    }
}
