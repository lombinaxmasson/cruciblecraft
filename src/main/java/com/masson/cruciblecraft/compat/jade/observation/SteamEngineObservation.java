package com.masson.cruciblecraft.compat.jade.observation;

import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;

import net.minecraft.nbt.CompoundTag;

/** Source projection for GT6 {@code MultiTileEntityEngineSteam}. */
public record SteamEngineObservation(
        ObservationField<String> state,
        ObservationField<Long> inputMinimum,
        ObservationField<Long> inputMaximum,
        ObservationField<Long> outputMinimum,
        ObservationField<Long> outputMaximum,
        ObservationField<Long> outputRate) {
    public static final String STATE = "cc_waila_steam_engine_state";
    public static final String INPUT_MIN = "cc_waila_steam_engine_input_min";
    public static final String INPUT_MAX = "cc_waila_steam_engine_input_max";
    public static final String OUTPUT_MIN = "cc_waila_steam_engine_output_min";
    public static final String OUTPUT_MAX = "cc_waila_steam_engine_output_max";
    public static final String OUTPUT_RATE = "cc_waila_steam_engine_output_rate";

    public static SteamEngineObservation fromServerData(CompoundTag data) {
        return new SteamEngineObservation(
                stringField(data, STATE),
                longField(data, INPUT_MIN),
                longField(data, INPUT_MAX),
                longField(data, OUTPUT_MIN),
                longField(data, OUTPUT_MAX),
                longField(data, OUTPUT_RATE));
    }

    public static void writeServerData(
            CompoundTag data, SteamEngineBlockEntity engine) {
        EnergyConverterProfile profile = engine.profile();
        data.putString(STATE, engine.status());
        EnergyConverterProfile.Window window = profile.inputWindow();
        if (window.minimum() != null) {
            data.putLong(INPUT_MIN, window.minimum());
            data.putLong(INPUT_MAX, window.maximum());
        }
        long nominal = Math.max(1L, profile.outputPacket().size());
        data.putLong(OUTPUT_MIN, Math.max(1L, nominal / 2L));
        data.putLong(OUTPUT_MAX, Math.max(1L, nominal * 2L));
        data.putLong(OUTPUT_RATE, engine.currentOutputRate());
    }

    private static ObservationField<Long> longField(
            CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getLong(key))
                : ObservationField.unavailable();
    }

    private static ObservationField<String> stringField(
            CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getString(key))
                : ObservationField.unavailable();
    }
}
