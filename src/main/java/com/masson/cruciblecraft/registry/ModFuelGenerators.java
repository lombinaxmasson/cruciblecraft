package com.masson.cruciblecraft.registry;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorSpec;

import net.minecraft.resources.ResourceLocation;

/** Registry-owned T11 fuel-generator configurations. */
public final class ModFuelGenerators {
    public static final FuelGeneratorSpec FUEL_ENGINE =
            new FuelGeneratorSpec(
                    id("fuel_engine"),
                    () -> ModRecipeMaps.FUELS_ENGINE,
                    8_000,
                    8_000,
                    1);
    public static final FuelGeneratorSpec BURNING_GAS_GENERATOR =
            new FuelGeneratorSpec(
                    id("burning_gas_generator"),
                    () -> ModRecipeMaps.FUELS_GAS,
                    8_000,
                    8_000,
                    2);
    public static final List<FuelGeneratorSpec> ALL =
            List.of(FUEL_ENGINE, BURNING_GAS_GENERATOR);
    private static final Map<ResourceLocation, FuelGeneratorSpec> BY_MAP =
            ALL.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    spec -> spec.requireRecipeMap().id(),
                    spec -> spec));

    private ModFuelGenerators() {}

    public static Optional<FuelGeneratorSpec> forRecipeMap(
            ResourceLocation id) {
        return Optional.ofNullable(BY_MAP.get(id));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID, path);
    }
}
