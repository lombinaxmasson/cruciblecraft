package com.masson.cruciblecraft.machine.processing;

import java.util.Objects;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/**
 * Source-backed numeric capability of one machine tier.
 *
 * <p>This value deliberately contains no validators, side rules, slot
 * acceptance or UI callbacks. Those are machine-kind behavior and must not
 * drift when another tier is added.
 */
public record TierProfile(
        ResourceLocation id,
        String materialId,
        EnergyType energyType,
        long inputMinimum,
        long inputNominal,
        long inputMaximum,
        long energyCapacity,
        int parallelLimit,
        int efficiency) {

    public TierProfile {
        Objects.requireNonNull(id, "id");
        if (materialId == null || materialId.isBlank()) {
            throw new IllegalArgumentException("Tier material id must not be blank");
        }
        Objects.requireNonNull(energyType, "energyType");
        if (inputMinimum <= 0L
                || inputNominal < inputMinimum
                || inputMaximum < inputNominal
                || energyCapacity < inputMaximum
                || parallelLimit <= 0
                || efficiency <= 0
                || efficiency > 10_000) {
            throw new IllegalArgumentException(
                    "Invalid tier input window/capacity/parallel/efficiency for "
                            + id);
        }
    }

    public boolean acceptsRecipePower(long eut) {
        return eut >= 0L && eut <= inputMaximum;
    }

    public boolean overcharges(long packetSize) {
        return packetSize == Long.MIN_VALUE
                || Math.abs(packetSize) > inputMaximum;
    }
}
