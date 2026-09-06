package com.masson.cruciblecraft.compat.jade.observation;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;

import net.minecraft.nbt.CompoundTag;

/**
 * Source-backed crucible Jade projection. Runtime Celsius is converted to
 * Kelvin only at this display boundary.
 */
public record CrucibleObservation(
        ObservationField<Double> temperatureKelvin,
        ObservationField<Long> bufferedHeatHu,
        ObservationField<Double> meltdownKelvin,
        ObservationField<Integer> fillPercent,
        ObservationField<String> renderState,
        ObservationField<Boolean> processActive,
        ObservationField<String> contents,
        ObservationField<Integer> totalUnits,
        ObservationField<Integer> maxUnits,
        ObservationField<Boolean> cacheSlotPresent) {
    public static final String TEMPERATURE_C = "cc_crucible_temp_c";
    public static final String MELTDOWN_C = "cc_crucible_max_c";
    public static final String BUFFERED_HU = "cc_crucible_hu";
    public static final String FILL = "cc_crucible_fill";
    public static final String RENDER = "cc_crucible_render";
    public static final String ACTIVE = "cc_crucible_active";
    public static final String CONTENTS = "cc_crucible_contents";
    public static final String UNITS = "cc_crucible_units";
    public static final String MAX_UNITS = "cc_crucible_max_units";
    public static final String CACHE = "cc_crucible_cache";

    public static CrucibleObservation fromServerData(CompoundTag data) {
        ObservationField<Double> temperature = data.contains(TEMPERATURE_C)
                ? ObservationField.of(JadeDisplayUnits.celsiusToKelvin(
                        data.getFloat(TEMPERATURE_C)))
                : ObservationField.unavailable();
        ObservationField<Long> heat = data.contains(BUFFERED_HU)
                ? ObservationField.of(data.getLong(BUFFERED_HU))
                : ObservationField.unavailable();
        ObservationField<Double> meltdown = data.contains(MELTDOWN_C)
                ? ObservationField.of(JadeDisplayUnits.celsiusToKelvin(
                        data.getFloat(MELTDOWN_C)))
                : ObservationField.unavailable();
        ObservationField<Integer> fill = data.contains(FILL)
                ? ObservationField.of(JadeDisplayUnits.fillPercent(
                        data.getFloat(FILL)))
                : ObservationField.unavailable();
        ObservationField<String> render = data.contains(RENDER)
                ? ObservationField.of(data.getString(RENDER))
                : ObservationField.unavailable();
        ObservationField<Boolean> active = data.contains(ACTIVE)
                ? ObservationField.of(data.getBoolean(ACTIVE))
                : ObservationField.unavailable();
        ObservationField<String> contents = data.contains(CONTENTS)
                ? ObservationField.of(data.getString(CONTENTS))
                : ObservationField.unavailable();
        ObservationField<Integer> units = data.contains(UNITS)
                ? ObservationField.of(data.getInt(UNITS))
                : ObservationField.unavailable();
        ObservationField<Integer> capacity = data.contains(MAX_UNITS)
                ? ObservationField.of(data.getInt(MAX_UNITS))
                : ObservationField.unavailable();
        ObservationField<Boolean> cache = data.contains(CACHE)
                ? ObservationField.of(data.getBoolean(CACHE))
                : ObservationField.unavailable();
        return new CrucibleObservation(
                temperature,
                heat,
                meltdown,
                fill,
                render,
                active,
                contents,
                units,
                capacity,
                cache);
    }

    public static void writeServerData(
            CompoundTag data, CrucibleBlockEntity crucible) {
        data.putFloat(TEMPERATURE_C, crucible.temperatureCelsius());
        data.putFloat(MELTDOWN_C, crucible.casingMaxTemperature());
        data.putLong(BUFFERED_HU, crucible.bufferedHeatHu());
        data.putFloat(FILL, crucible.fillFraction());
        data.putString(RENDER, crucible.renderState());
        data.putBoolean(ACTIVE, crucible.processActive());
        data.putString(CONTENTS, formatContents(crucible.composition()));
        data.putInt(UNITS, crucible.totalUnits());
        data.putInt(MAX_UNITS, CrucibleBlockEntity.maxUnits());
        data.putBoolean(CACHE, crucible.hasCacheSlot());
    }

    public static String formatContents(Map<String, Integer> composition) {
        if (composition.isEmpty()) {
            return "";
        }
        return composition.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue() + " u")
                .collect(Collectors.joining(", "));
    }

    public static CrucibleObservation fromSnapshot(
            float temperatureCelsius,
            long bufferedHeatHu,
            float casingMaxCelsius,
            float fillFraction,
            String renderState,
            boolean processActive,
            Map<String, Integer> composition,
            int totalUnits,
            int maxUnits,
            boolean cacheSlotPresent) {
        Map<String, Integer> copy = new LinkedHashMap<>(composition);
        return new CrucibleObservation(
                ObservationField.of(JadeDisplayUnits.celsiusToKelvin(
                        temperatureCelsius)),
                ObservationField.of(bufferedHeatHu),
                ObservationField.of(JadeDisplayUnits.celsiusToKelvin(
                        casingMaxCelsius)),
                ObservationField.of(JadeDisplayUnits.fillPercent(fillFraction)),
                ObservationField.of(renderState),
                ObservationField.of(processActive),
                ObservationField.of(formatContents(copy)),
                ObservationField.of(totalUnits),
                ObservationField.of(maxUnits),
                ObservationField.of(cacheSlotPresent));
    }

    public String cacheDisplay() {
        if (!cacheSlotPresent.available()) {
            return "unavailable";
        }
        return Boolean.TRUE.equals(cacheSlotPresent.value())
                ? "present"
                : "unavailable";
    }

    public String renderKey() {
        if (!renderState.available() || renderState.value() == null
                || renderState.value().isBlank()) {
            return "unavailable";
        }
        return renderState.value().toLowerCase(Locale.ROOT);
    }
}
