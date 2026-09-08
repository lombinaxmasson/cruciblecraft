package com.masson.cruciblecraft.compat.jade.observation;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricHeaterBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.energy.converter.EnergyConverterHost;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shared observation adapter for 179 live converter rows. Missing runtime
 * fields stay {@link ObservationField#unavailable()}.
 */
public record ConverterObservation(
        ObservationField<String> accepts,
        ObservationField<String> emits,
        ObservationField<Long> inputPacket,
        ObservationField<Long> outputPacket,
        ObservationField<Long> windowMin,
        ObservationField<Long> windowNominal,
        ObservationField<Long> windowMax,
        ObservationField<String> activity,
        ObservationField<Long> bufferStored,
        ObservationField<Long> bufferCapacity) {
    public static final String ACCEPTS = "cc_converter_accepts";
    public static final String EMITS = "cc_converter_emits";
    public static final String INPUT_PACKET = "cc_converter_input_packet";
    public static final String OUTPUT_PACKET = "cc_converter_output_packet";
    public static final String WINDOW_MIN = "cc_converter_window_min";
    public static final String WINDOW_NOM = "cc_converter_window_nom";
    public static final String WINDOW_MAX = "cc_converter_window_max";
    public static final String ACTIVITY = "cc_converter_activity";
    public static final String BUFFER_STORED = "cc_converter_buffer_stored";
    public static final String BUFFER_CAP = "cc_converter_buffer_cap";

    public static ConverterObservation fromServerData(CompoundTag data) {
        return new ConverterObservation(
                stringField(data, ACCEPTS),
                stringField(data, EMITS),
                longField(data, INPUT_PACKET),
                longField(data, OUTPUT_PACKET),
                longField(data, WINDOW_MIN),
                longField(data, WINDOW_NOM),
                longField(data, WINDOW_MAX),
                stringField(data, ACTIVITY),
                longField(data, BUFFER_STORED),
                longField(data, BUFFER_CAP));
    }

    public static void writeServerData(CompoundTag data, BlockEntity be) {
        if (!(be.getBlockState().getBlock() instanceof EnergyConverterHost host)) {
            return;
        }
        EnergyConverterProfile profile = host.converterProfile();
        data.putString(ACCEPTS, String.join(",", profile.accepts()));
        data.putString(EMITS, String.join(",", profile.emits()));
        data.putLong(INPUT_PACKET, profile.inputPacket().size());
        data.putLong(OUTPUT_PACKET, profile.outputPacket().size());
        if (profile.inputWindow().minimum() != null) {
            data.putLong(WINDOW_MIN, profile.inputWindow().minimum());
            data.putLong(WINDOW_NOM, profile.inputWindow().nominal());
            data.putLong(WINDOW_MAX, profile.inputWindow().maximum());
        }
        writeRuntime(data, be);
    }

    public static ConverterObservation fromProfile(EnergyConverterProfile profile) {
        return new ConverterObservation(
                ObservationField.of(String.join(",", profile.accepts())),
                ObservationField.of(String.join(",", profile.emits())),
                ObservationField.of(profile.inputPacket().size()),
                ObservationField.of(profile.outputPacket().size()),
                profile.inputWindow().minimum() == null
                        ? ObservationField.unavailable()
                        : ObservationField.of(profile.inputWindow().minimum()),
                profile.inputWindow().nominal() == null
                        ? ObservationField.unavailable()
                        : ObservationField.of(profile.inputWindow().nominal()),
                profile.inputWindow().maximum() == null
                        ? ObservationField.unavailable()
                        : ObservationField.of(profile.inputWindow().maximum()),
                ObservationField.unavailable(),
                ObservationField.unavailable(),
                ObservationField.unavailable());
    }

    private static void writeRuntime(CompoundTag data, BlockEntity be) {
        if (be instanceof BoilerBlockEntity boiler) {
            data.putString(ACTIVITY, boiler.status());
            data.putLong(BUFFER_STORED, boiler.accumulatedHu());
            data.putLong(BUFFER_CAP, BoilerBlockEntity.WATER_CAPACITY);
            return;
        }
        if (be instanceof SteamEngineBlockEntity engine) {
            data.putString(ACTIVITY, engine.status());
            data.putLong(BUFFER_STORED, engine.stored());
            data.putLong(BUFFER_CAP, SteamEngineBlockEntity.KU_CAPACITY);
            return;
        }
        if (be instanceof ElectricHeaterBlockEntity heater) {
            data.putString(ACTIVITY, heater.status());
            data.putLong(BUFFER_STORED, heater.stored());
            data.putLong(BUFFER_CAP, heater.capacity(EnergyType.ELECTRIC));
            return;
        }
        if (be instanceof ElectricEngineBlockEntity engine) {
            data.putString(ACTIVITY, engine.status());
            data.putLong(BUFFER_STORED, engine.stored());
            data.putLong(BUFFER_CAP, engine.capacity(EnergyType.ELECTRIC));
            return;
        }
        if (be instanceof FuelGeneratorBlockEntity generator) {
            data.putString(ACTIVITY, generator.status());
            writeHandlerBuffer(data, generator);
            return;
        }
        if (be instanceof IEnergyHandler handler) {
            writeHandlerBuffer(data, handler);
        }
    }

    private static void writeHandlerBuffer(CompoundTag data, IEnergyHandler handler) {
        for (EnergyType type : EnergyType.values()) {
            for (Direction side : Direction.values()) {
                if (!handler.handles(type, side)) {
                    continue;
                }
                data.putLong(BUFFER_STORED, handler.stored(type));
                data.putLong(BUFFER_CAP, handler.capacity(type));
                return;
            }
        }
    }

    public String acceptsDisplay() {
        if (!accepts.available() || accepts.value() == null || accepts.value().isBlank()) {
            return "unavailable";
        }
        return accepts.value();
    }

    public String emitsDisplay() {
        if (!emits.available() || emits.value() == null || emits.value().isBlank()) {
            return "unavailable";
        }
        return emits.value();
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
