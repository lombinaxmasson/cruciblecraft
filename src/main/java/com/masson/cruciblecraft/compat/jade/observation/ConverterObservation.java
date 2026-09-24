package com.masson.cruciblecraft.compat.jade.observation;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.blockentity.BoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricEngineBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ElectricHeaterBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidBedBurningBoxBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.energy.converter.EnergyConverterHost;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;

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
        ObservationField<Long> bufferCapacity,
        List<Tank> tanks) {
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
    public static final String TANK_COUNT = "cc_converter_tank_count";
    public static final String TANK_ID = "cc_converter_tank_id_";
    public static final String TANK_AMOUNT = "cc_converter_tank_amount_";
    public static final String TANK_CAP = "cc_converter_tank_cap_";

    public record Tank(String fluidId, int amount, int capacity) {
        public Tank {
            fluidId = fluidId == null ? "" : fluidId;
        }
    }

    public ConverterObservation {
        tanks = List.copyOf(tanks == null ? List.of() : tanks);
    }

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
                longField(data, BUFFER_CAP),
                readTanks(data));
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
                ObservationField.unavailable(),
                List.of());
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
            data.putLong(BUFFER_CAP, engine.kineticCapacity());
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
            writeTanks(data, fuelGeneratorTanks(generator));
            return;
        }
        if (be instanceof FluidBedBurningBoxBlockEntity box) {
            writeHandlerBuffer(data, box);
            writeTanks(data, List.of(tankOf(
                    box.inputFluid(), box.inputCapacity())));
            return;
        }
        if (be instanceof IEnergyHandler handler) {
            writeHandlerBuffer(data, handler);
        }
    }

    private static List<Tank> fuelGeneratorTanks(
            FuelGeneratorBlockEntity generator) {
        List<Tank> tanks = new ArrayList<>();
        tanks.add(tankOf(
                generator.inputFluid(), generator.inputCapacity()));
        for (int index = 0; index < generator.outputTankCount(); index++) {
            FluidStack output = generator.outputFluid(index);
            if (!output.isEmpty()) {
                tanks.add(tankOf(
                        output, generator.outputCapacity(index)));
            }
        }
        return tanks;
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

    private static List<Tank> readTanks(CompoundTag data) {
        int count = Math.max(0, data.getInt(TANK_COUNT));
        List<Tank> tanks = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            tanks.add(new Tank(
                    data.getString(TANK_ID + index),
                    data.getInt(TANK_AMOUNT + index),
                    data.getInt(TANK_CAP + index)));
        }
        return tanks;
    }

    private static void writeTanks(CompoundTag data, List<Tank> tanks) {
        data.putInt(TANK_COUNT, tanks.size());
        for (int index = 0; index < tanks.size(); index++) {
            Tank tank = tanks.get(index);
            data.putString(TANK_ID + index, tank.fluidId());
            data.putInt(TANK_AMOUNT + index, tank.amount());
            data.putInt(TANK_CAP + index, tank.capacity());
        }
    }

    public static Tank tankOf(FluidStack stack, int capacity) {
        String id = "";
        if (stack != null && !stack.isEmpty()) {
            ResourceLocation key =
                    BuiltInRegistries.FLUID.getKey(stack.getFluid());
            if (key != null) {
                id = key.toString();
            }
        }
        return new Tank(
                id,
                stack == null ? 0 : stack.getAmount(),
                capacity);
    }
}
