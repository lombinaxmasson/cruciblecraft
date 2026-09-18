package com.masson.cruciblecraft.energy.converter;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorSpec;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.Direction;

/** Per-block fuel-generator specs for catalog fluid boxes and fuel engines. */
public final class EnergyConverterFuelSpecs {
    private EnergyConverterFuelSpecs() {}

    public static FuelGeneratorSpec fromProfile(EnergyConverterProfile profile) {
        String runtime = profile.runtimeBinding();
        if ("fuel_engine".equals(runtime)) {
            return new FuelGeneratorSpec(
                    profile.id(),
                    () -> ModRecipeMaps.FUELS_ENGINE,
                    Math.max(1, profile.inputCapacity()),
                    Math.max(1, profile.exhaust().capacity()),
                    1,
                    EnergyType.KINETIC_ROTATION,
                    profile.outputPacket().size(),
                    Math.max(1L, profile.outputPacket().maxAmountPerTick()),
                    Math.max(
                            profile.outputPacket().size(),
                            profile.outputCapacity()),
                    requireEfficiency(profile),
                    FuelGeneratorSpec.EnergyOutputFace.FRONT,
                    List.of(Direction.UP),
                    1,
                    FuelGeneratorSpec.ExhaustRouting.BACK,
                    FuelGeneratorSpec.InputPhase.ANY);
        }
        if ("fluid_burning_box".equals(runtime)) {
            boolean gas = "MultiTileEntityGeneratorGas".equals(
                    profile.source().machineKind());
            return new FuelGeneratorSpec(
                    profile.id(),
                    () -> ModRecipeMaps.FUELS_GAS,
                    Math.max(1, profile.inputCapacity()),
                    Math.max(1, profile.exhaust().capacity()),
                    2,
                    EnergyType.HEAT,
                    profile.outputPacket().size(),
                    Math.max(1L, profile.outputPacket().maxAmountPerTick()),
                    Math.max(
                            profile.outputPacket().size(),
                            profile.outputCapacity()),
                    requireEfficiency(profile),
                    FuelGeneratorSpec.EnergyOutputFace.UP,
                    List.of(Direction.NORTH, Direction.SOUTH),
                    2,
                    gas
                            ? FuelGeneratorSpec.InputPhase.GAS
                            : FuelGeneratorSpec.InputPhase.LIQUID);
        }
        throw new IllegalArgumentException(
                "No fuel-generator spec for " + profile.id());
    }

    private static int requireEfficiency(EnergyConverterProfile profile) {
        Integer efficiency = profile.efficiencyBps();
        if (efficiency == null || efficiency <= 0 || efficiency > 10_000) {
            throw new IllegalStateException(
                    "Fuel converter efficiency missing for " + profile.id());
        }
        return efficiency;
    }
}
