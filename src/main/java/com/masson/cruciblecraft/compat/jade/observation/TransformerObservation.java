package com.masson.cruciblecraft.compat.jade.observation;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.masson.cruciblecraft.energy.transformer.EnergyTransformerProfile;
import com.masson.cruciblecraft.energy.transformer.TransformerBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * Source-backed transformer Jade projection. Mode and per-side voltage come
 * from {@link EnergyTransformerProfile} plus {@code reversed()}, never from
 * guessing HV/LV by block facing alone.
 */
public record TransformerObservation(
        String profileId,
        String lowVoltage,
        String highVoltage,
        boolean reversed,
        boolean modeKnown,
        ObservationField<Long> storedEu,
        ObservationField<Long> capacityEu,
        ObservationField<Boolean> active,
        Map<String, Side> sides) {
    public static final String REVERSED = "cc_transformer_reversed";
    public static final String STORED = "cc_transformer_stored";
    public static final String CAPACITY = "cc_transformer_capacity";
    public static final String ACTIVE = "cc_transformer_active";
    public static final String PROFILE = "cc_transformer_profile";
    public static final String LOW = "cc_transformer_low";
    public static final String HIGH = "cc_transformer_high";
    public static final String SIDES = "cc_transformer_sides";

    public record Side(
            boolean available,
            boolean input,
            long voltage,
            long packetMultiplier) {
        public static Side unavailable() {
            return new Side(false, false, 0L, 0L);
        }

        public static Side input(long voltage) {
            return new Side(true, true, voltage, 1L);
        }

        public static Side output(long voltage, long packetMultiplier) {
            return new Side(true, false, voltage, packetMultiplier);
        }
    }

    public String modeKey() {
        return reversed ? "step_up" : "step_down";
    }

    public static TransformerObservation fromBlockAndServerData(
            EnergyTransformerProfile profile,
            CompoundTag data) {
        Map<String, Side> sides = new LinkedHashMap<>();
        boolean hasSides = data.contains(SIDES);
        CompoundTag sideTag = hasSides ? data.getCompound(SIDES) : new CompoundTag();
        for (Direction direction : Direction.values()) {
            String name = direction.getSerializedName();
            if (!hasSides || !sideTag.contains(name)) {
                sides.put(name, Side.unavailable());
                continue;
            }
            CompoundTag row = sideTag.getCompound(name);
            if (!row.contains("input") || !row.contains("voltage")) {
                sides.put(name, Side.unavailable());
                continue;
            }
            boolean input = row.getBoolean("input");
            long voltage = row.getLong("voltage");
            long packets = row.contains("packets") ? row.getLong("packets") : 1L;
            sides.put(
                    name,
                    input ? Side.input(voltage) : Side.output(voltage, packets));
        }
        return new TransformerObservation(
                data.contains(PROFILE)
                        ? data.getString(PROFILE)
                        : profile.id().toString(),
                data.contains(LOW) ? data.getString(LOW) : profile.lowVoltage(),
                data.contains(HIGH) ? data.getString(HIGH) : profile.highVoltage(),
                data.contains(REVERSED) && data.getBoolean(REVERSED),
                data.contains(REVERSED),
                data.contains(STORED)
                        ? ObservationField.of(data.getLong(STORED))
                        : ObservationField.unavailable(),
                data.contains(CAPACITY)
                        ? ObservationField.of(data.getLong(CAPACITY))
                        : ObservationField.unavailable(),
                data.contains(ACTIVE)
                        ? ObservationField.of(data.getBoolean(ACTIVE))
                        : ObservationField.unavailable(),
                Map.copyOf(sides));
    }

    public static void writeServerData(
            CompoundTag data, TransformerBlockEntity transformer) {
        EnergyTransformerProfile profile = transformer.profile();
        boolean reversed = transformer.reversed();
        data.putString(PROFILE, profile.id().toString());
        data.putString(LOW, profile.lowVoltage());
        data.putString(HIGH, profile.highVoltage());
        data.putBoolean(REVERSED, reversed);
        data.putLong(STORED, transformer.storedEu());
        data.putLong(CAPACITY, transformer.capacityEu());
        data.putBoolean(ACTIVE, transformer.activityActive());
        CompoundTag sides = new CompoundTag();
        for (Direction direction : Direction.values()) {
            CompoundTag row = new CompoundTag();
            boolean input = transformer.isInput(direction);
            row.putBoolean("input", input);
            row.putLong("voltage", transformer.sideVoltage(direction));
            row.putLong("packets", transformer.sidePacketMultiplier(direction));
            sides.put(direction.getSerializedName(), row);
        }
        data.put(SIDES, sides);
    }

    public static TransformerObservation fromSnapshot(
            String profileId,
            String lowVoltage,
            String highVoltage,
            boolean reversed,
            long stored,
            long capacity,
            boolean active,
            Map<String, Side> sides) {
        Map<String, Side> copy = new LinkedHashMap<>();
        for (String name : new String[] {
                "down", "up", "north", "south", "west", "east"
        }) {
            copy.put(
                    name,
                    sides.getOrDefault(name, Side.unavailable()));
        }
        return new TransformerObservation(
                profileId,
                lowVoltage.toLowerCase(Locale.ROOT),
                highVoltage.toLowerCase(Locale.ROOT),
                reversed,
                true,
                ObservationField.of(stored),
                ObservationField.of(capacity),
                ObservationField.of(active),
                Map.copyOf(copy));
    }
}
