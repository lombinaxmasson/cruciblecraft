package com.masson.cruciblecraft.energy.battery;

import java.util.Objects;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;

/** Immutable projection of one census battery row. */
public record EnergyBatteryProfile(
        ResourceLocation id,
        ResourceLocation kindId,
        EnergyType energyType,
        String voltage,
        long inputSize,
        long sizeMin,
        long sizeMax,
        long capacity,
        int color,
        String textureProfile,
        boolean hasBar,
        VoxelShape shape,
        int displayScaleMax,
        int sourceId,
        int sourceLine,
        String gt6Class,
        String langZh,
        String langEn) {
    public EnergyBatteryProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kindId, "kindId");
        Objects.requireNonNull(energyType, "energyType");
        Objects.requireNonNull(voltage, "voltage");
        Objects.requireNonNull(textureProfile, "textureProfile");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(gt6Class, "gt6Class");
        Objects.requireNonNull(langZh, "langZh");
        Objects.requireNonNull(langEn, "langEn");
        if (inputSize <= 0L
                || sizeMin <= 0L
                || sizeMax < inputSize
                || sizeMin > inputSize
                || capacity <= 0L
                || displayScaleMax <= 0
                || sourceId <= 0
                || sourceLine <= 0) {
            throw new IllegalArgumentException(
                    "Battery profile window and source must be valid");
        }
    }

    static EnergyBatteryProfile synthesize(
            EnergyBatteryKindCatalog.Kind kind,
            EnergyBatteryTierCatalog.Entry tier) {
        if (!kind.id().equals(tier.kindId())) {
            throw new IllegalStateException(
                    "Battery kind/tier mismatch " + tier.id());
        }
        if (!kind.energyType().equals(energyType(tier.energy()))) {
            throw new IllegalStateException(
                    "Battery energy drifted on " + tier.id());
        }
        long expected = Math.multiplyExact(
                tier.inputSize(), kind.capacityMultiplier());
        if (tier.capacity() != expected) {
            throw new IllegalStateException(
                    "Battery capacity drifted on " + tier.id());
        }
        long min = tier.inputSize() / 2L;
        if (min <= 8L && tier.inputSize() > 0L) {
            min = 1L;
        }
        return new EnergyBatteryProfile(
                tier.id(),
                kind.id(),
                kind.energyType(),
                tier.voltage(),
                tier.inputSize(),
                min,
                Math.multiplyExact(tier.inputSize(), 2L),
                tier.capacity(),
                kind.color(),
                kind.textureProfile(),
                kind.hasBar(),
                voxel(tier.voxel()),
                tier.displayScaleMax(),
                tier.sourceId(),
                tier.sourceLine(),
                tier.gt6Class(),
                kind.langZh(),
                kind.langEn());
    }

    static EnergyType energyType(String token) {
        if ("EU".equals(token)) {
            return EnergyType.ELECTRIC;
        }
        if ("LU".equals(token)) {
            return EnergyType.LU;
        }
        throw new IllegalStateException("Unsupported battery energy " + token);
    }

    public byte displayedEnergy(long stored) {
        if (capacity <= 0L || stored <= 0L) {
            return 0;
        }
        long scaled = stored * displayScaleMax / capacity;
        if (scaled > displayScaleMax) {
            scaled = displayScaleMax;
        }
        return (byte) scaled;
    }

    public String textureFolder() {
        return "block/machine/battery/" + textureProfile + "/" + inputSize;
    }

    private static VoxelShape voxel(int[] box) {
        return Block.box(box[0], box[1], box[2], box[3], box[4], box[5]);
    }
}
