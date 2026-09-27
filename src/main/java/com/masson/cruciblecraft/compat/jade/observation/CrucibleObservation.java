package com.masson.cruciblecraft.compat.jade.observation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

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
        ObservationField<List<MetalAmount>> metals,
        ObservationField<Integer> totalUnits,
        ObservationField<Integer> maxUnits,
        ObservationField<Boolean> cacheSlotPresent) {
    public static final String TEMPERATURE_C = "cc_crucible_temp_c";
    public static final String MELTDOWN_C = "cc_crucible_max_c";
    public static final String BUFFERED_HU = "cc_crucible_hu";
    public static final String FILL = "cc_crucible_fill";
    public static final String RENDER = "cc_crucible_render";
    public static final String ACTIVE = "cc_crucible_active";
    public static final String METALS = "cc_crucible_metals";
    public static final String UNITS = "cc_crucible_units";
    public static final String MAX_UNITS = "cc_crucible_max_units";
    public static final String CACHE = "cc_crucible_cache";

    public record MetalAmount(String materialId, int units) {}

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
        ObservationField<List<MetalAmount>> metals = data.contains(METALS, Tag.TAG_LIST)
                ? ObservationField.of(readMetals(data.getList(METALS, Tag.TAG_COMPOUND)))
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
                metals,
                units,
                capacity,
                cache);
    }

    public static void writeServerData(
            CompoundTag data, CrucibleBlockEntity crucible) {
        data.putFloat(TEMPERATURE_C, crucible.temperatureCelsius());
        data.putFloat(MELTDOWN_C, crucible.casingMaxTemperature());
        data.putLong(BUFFERED_HU, crucible.bufferedHeatHu());
        Map<String, Integer> shown = crucible.displayComposition();
        int shownUnits = shown.values().stream().mapToInt(Integer::intValue).sum();
        data.putFloat(FILL, shownUnits / (float) Math.max(1, CrucibleBlockEntity.maxUnits()));
        data.putString(RENDER, crucible.renderState());
        data.putBoolean(ACTIVE, crucible.processActive());
        data.put(METALS, writeMetals(shown));
        data.putInt(UNITS, shownUnits);
        data.putInt(MAX_UNITS, CrucibleBlockEntity.maxUnits());
        data.putBoolean(CACHE, crucible.hasCacheSlot());
    }

    public static List<MetalAmount> metalAmounts(Map<String, Integer> composition) {
        List<MetalAmount> metals = new ArrayList<>();
        composition.forEach((id, units) -> metals.add(new MetalAmount(id, units)));
        return List.copyOf(metals);
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
        return new CrucibleObservation(
                ObservationField.of(JadeDisplayUnits.celsiusToKelvin(
                        temperatureCelsius)),
                ObservationField.of(bufferedHeatHu),
                ObservationField.of(JadeDisplayUnits.celsiusToKelvin(
                        casingMaxCelsius)),
                ObservationField.of(JadeDisplayUnits.fillPercent(fillFraction)),
                ObservationField.of(renderState),
                ObservationField.of(processActive),
                ObservationField.of(metalAmounts(composition)),
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

    private static List<MetalAmount> readMetals(ListTag list) {
        List<MetalAmount> metals = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            CompoundTag row = list.getCompound(index);
            metals.add(new MetalAmount(row.getString("id"), row.getInt("u")));
        }
        return List.copyOf(metals);
    }

    private static ListTag writeMetals(Map<String, Integer> composition) {
        ListTag list = new ListTag();
        composition.forEach((id, units) -> {
            CompoundTag row = new CompoundTag();
            row.putString("id", id);
            row.putInt("u", units);
            list.add(row);
        });
        return list;
    }
}
