package com.masson.cruciblecraft.compat.jade.observation;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.content.item.ReactorRodItem;
import com.masson.cruciblecraft.nuclear.ReactorSafety;
import com.masson.cruciblecraft.nuclear.ReactorRodPhysics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Source-backed reactor Jade projection. Heat is HU. No Kelvin fields.
 */
public record ReactorCoreObservation(
        ObservationField<Long> heatHu,
        ObservationField<Long> lastHeatHu,
        ObservationField<Integer> neutrons,
        ObservationField<String> coolantId,
        ObservationField<Integer> coolantAmount,
        ObservationField<String> outputId,
        ObservationField<Integer> outputAmount,
        ObservationField<Boolean> running,
        ObservationField<Boolean> stopped,
        ObservationField<String> safety,
        ObservationField<List<Rod>> rods) {
    public static final String HEAT = "cc_reactor_heat";
    public static final String LAST_HEAT = "cc_reactor_last_heat";
    public static final String NEUTRONS = "cc_reactor_neutrons";
    public static final String COOLANT_ID = "cc_reactor_coolant_id";
    public static final String COOLANT_MB = "cc_reactor_coolant_mb";
    public static final String OUTPUT_ID = "cc_reactor_output_id";
    public static final String OUTPUT_MB = "cc_reactor_output_mb";
    public static final String RUNNING = "cc_reactor_running";
    public static final String STOPPED = "cc_reactor_stopped";
    public static final String SAFETY = "cc_reactor_safety";
    public static final String ROD_COUNT = "cc_reactor_rod_count";
    public static final String ROD_NAME = "cc_reactor_rod_name_";
    public static final String ROD_REMAINING = "cc_reactor_rod_remaining_";
    public static final String ROD_MODERATED = "cc_reactor_rod_moderated_";
    public static final String ROD_NEUTRONS = "cc_reactor_rod_neutrons_";

    public record Rod(
            String name,
            long remaining,
            boolean moderated,
            int neutrons) {}

    public static ReactorCoreObservation fromServerData(CompoundTag data) {
        return new ReactorCoreObservation(
                longField(data, HEAT),
                longField(data, LAST_HEAT),
                intField(data, NEUTRONS),
                stringField(data, COOLANT_ID),
                intField(data, COOLANT_MB),
                stringField(data, OUTPUT_ID),
                intField(data, OUTPUT_MB),
                boolField(data, RUNNING),
                boolField(data, STOPPED),
                stringField(data, SAFETY),
                data.contains(ROD_COUNT)
                        ? ObservationField.of(readRods(data))
                        : ObservationField.unavailable());
    }

    public static void writeServerData(
            CompoundTag data, ReactorCoreBlockEntity core) {
        data.putLong(HEAT, core.heat());
        data.putLong(LAST_HEAT, core.lastHeat());
        data.putInt(NEUTRONS, core.neutronSum());
        writeFluid(data, core.coolantTank().getFluid(), COOLANT_ID, COOLANT_MB);
        writeFluid(data, core.outputTank().getFluid(), OUTPUT_ID, OUTPUT_MB);
        data.putBoolean(RUNNING, core.running());
        data.putBoolean(STOPPED, core.stopped());
        data.putString(SAFETY, core.safety().key());
        data.putInt(ROD_COUNT, core.slots());
        for (int slot = 0; slot < core.slots(); slot++) {
            var rod = core.rod(slot);
            data.putString(
                    ROD_NAME + slot,
                    rod.isEmpty() ? "" : rod.getHoverName().getString());
            if (rod.getItem() instanceof ReactorRodItem) {
                data.putLong(
                        ROD_REMAINING + slot,
                        ReactorRodItem.state(rod).durability());
                data.putBoolean(
                        ROD_MODERATED + slot,
                        ReactorRodPhysics.moderated(rod));
            } else {
                data.putLong(ROD_REMAINING + slot, 0L);
                data.putBoolean(ROD_MODERATED + slot, false);
            }
            data.putInt(ROD_NEUTRONS + slot, core.neutrons(slot));
        }
    }

    public static ReactorCoreObservation fromSnapshot(
            long heatHu,
            long lastHeatHu,
            int neutrons,
            String coolantId,
            int coolantAmount,
            String outputId,
            int outputAmount,
            boolean running,
            boolean stopped,
            ReactorSafety safety) {
        return new ReactorCoreObservation(
                ObservationField.of(heatHu),
                ObservationField.of(lastHeatHu),
                ObservationField.of(neutrons),
                ObservationField.of(coolantId),
                ObservationField.of(coolantAmount),
                ObservationField.of(outputId),
                ObservationField.of(outputAmount),
                ObservationField.of(running),
                ObservationField.of(stopped),
                ObservationField.of(safety.key()),
                ObservationField.of(List.of()));
    }

    public boolean hasKelvinField() {
        return false;
    }

    private static void writeFluid(
            CompoundTag data, FluidStack fluid, String idKey, String amountKey) {
        data.putString(
                idKey,
                fluid.isEmpty()
                        ? ""
                        : BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString());
        data.putInt(amountKey, fluid.getAmount());
    }

    private static ObservationField<Long> longField(CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getLong(key))
                : ObservationField.unavailable();
    }

    private static ObservationField<Integer> intField(CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getInt(key))
                : ObservationField.unavailable();
    }

    private static ObservationField<Boolean> boolField(CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getBoolean(key))
                : ObservationField.unavailable();
    }

    private static ObservationField<String> stringField(CompoundTag data, String key) {
        return data.contains(key)
                ? ObservationField.of(data.getString(key))
                : ObservationField.unavailable();
    }

    private static List<Rod> readRods(CompoundTag data) {
        int count = Math.max(0, data.getInt(ROD_COUNT));
        List<Rod> rods = new ArrayList<>(count);
        for (int slot = 0; slot < count; slot++) {
            rods.add(new Rod(
                    data.getString(ROD_NAME + slot),
                    data.getLong(ROD_REMAINING + slot),
                    data.getBoolean(ROD_MODERATED + slot),
                    data.getInt(ROD_NEUTRONS + slot)));
        }
        return List.copyOf(rods);
    }
}
