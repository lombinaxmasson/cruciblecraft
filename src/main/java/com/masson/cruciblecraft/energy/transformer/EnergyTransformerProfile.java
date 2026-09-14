package com.masson.cruciblecraft.energy.transformer;

import java.util.Objects;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.energy.cable.GT6VoltageTiers;

import net.minecraft.resources.ResourceLocation;

/** Immutable projection of one GT6 electric transformer row. */
public record EnergyTransformerProfile(
        ResourceLocation id,
        ResourceLocation kindId,
        EnergyType energyType,
        String lowVoltage,
        String highVoltage,
        int voltageIndex,
        long inputSize,
        long outputSize,
        long multiplier,
        long capacity,
        String material,
        String textureProfile,
        int sourceId,
        int sourceLine,
        String gt6Class,
        String langZh,
        String langEn) {
    public EnergyTransformerProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kindId, "kindId");
        Objects.requireNonNull(energyType, "energyType");
        Objects.requireNonNull(lowVoltage, "lowVoltage");
        Objects.requireNonNull(highVoltage, "highVoltage");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(textureProfile, "textureProfile");
        Objects.requireNonNull(gt6Class, "gt6Class");
        Objects.requireNonNull(langZh, "langZh");
        Objects.requireNonNull(langEn, "langEn");
        if (inputSize <= 0L
                || outputSize <= 0L
                || multiplier != 4L
                || capacity <= 0L
                || voltageIndex < 0
                || sourceId <= 0
                || sourceLine < 0
                || energyType != EnergyType.ELECTRIC) {
            throw new IllegalArgumentException(
                    "Transformer profile window and source must be valid");
        }
    }

    static EnergyTransformerProfile synthesize(
            EnergyTransformerKindCatalog.Kind kind,
            EnergyTransformerTierCatalog.Entry tier) {
        if (!kind.id().equals(tier.kindId())) {
            throw new IllegalStateException(
                    "Transformer kind/tier mismatch " + tier.id());
        }
        if (!kind.energyType().equals(energyType(tier.energy()))) {
            throw new IllegalStateException(
                    "Transformer energy drifted on " + tier.id());
        }
        if (tier.multiplier() != 4L
                || tier.capacity() != Math.multiplyExact(tier.inputSize(), 2L)
                || tier.voltageIndex() > GT6VoltageTiers.VOLTAGES.length - 2
                || GT6VoltageTiers.VOLTAGES[tier.voltageIndex()]
                        != tier.outputSize()
                || GT6VoltageTiers.VOLTAGES[tier.voltageIndex() + 1]
                        != tier.inputSize()
                || tier.inputSize() != Math.multiplyExact(
                        tier.outputSize(), 4L)) {
            throw new IllegalStateException(
                    "Transformer voltage pair drifted on " + tier.id());
        }
        return new EnergyTransformerProfile(
                tier.id(),
                kind.id(),
                kind.energyType(),
                tier.lowVoltage(),
                tier.highVoltage(),
                tier.voltageIndex(),
                tier.inputSize(),
                tier.outputSize(),
                tier.multiplier(),
                tier.capacity(),
                tier.material(),
                kind.textureProfile(),
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
        throw new IllegalStateException("Unsupported transformer energy " + token);
    }

    public String textureFolder() {
        return "block/machine/transformer/" + textureProfile;
    }

    public long acceptMin(boolean reversed) {
        if (reversed) {
            long outMin = outputSize / 2L;
            return outMin <= 8L ? 1L : outMin;
        }
        long min = inputSize / 2L;
        return inputSize <= 16L ? 1L : min;
    }

    public long acceptMax(boolean reversed) {
        if (reversed) {
            return Math.max(inputSize, Math.multiplyExact(outputSize * 2L, multiplier));
        }
        return Math.multiplyExact(inputSize, 2L);
    }

    public long acceptRec(boolean reversed) {
        return reversed ? outputSize : inputSize;
    }

    public long emitRec(boolean reversed) {
        return reversed ? inputSize : outputSize;
    }

    public long packetMultiplier(boolean reversed) {
        return reversed ? 1L : multiplier;
    }
}
