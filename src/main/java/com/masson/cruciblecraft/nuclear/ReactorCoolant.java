package com.masson.cruciblecraft.nuclear;

import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** GT6 reactor coolant identities that currently exist as CC fluids. */
public enum ReactorCoolant {
    DISTILLED_WATER("water_distilled", 80, 160, "steam", false),
    SEMI_HEAVY_WATER("semiheavy_water", 80, 1, "", true),
    HEAVY_WATER("heavy_water", 80, 1, "", true),
    MOLTEN_TIN("tin", 40, 1, "", true),
    MOLTEN_SODIUM("sodium", 80, 1, "", true),
    MOLTEN_LICL("lithium_chloride", 80, 1, "", true),
    CARBON_DIOXIDE("carbon_dioxide", 80, 1, "", true),
    HELIUM("helium", 80, 1, "", true);

    public static final int EU_PER_WATER = 80;
    public static final int STEAM_PER_WATER = 160;

    private final String material;
    private final int euPerUnit;
    private final int outputMultiplier;
    private final String hotOutputMaterial;
    private final boolean hotOutputSameMaterial;

    ReactorCoolant(
            String material,
            int euPerUnit,
            int outputMultiplier,
            String hotOutputMaterial,
            boolean hotOutputSameMaterial) {
        this.material = material;
        this.euPerUnit = euPerUnit;
        this.outputMultiplier = outputMultiplier;
        this.hotOutputMaterial = hotOutputMaterial;
        this.hotOutputSameMaterial = hotOutputSameMaterial;
    }

    public static ReactorCoolant of(FluidStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        for (ReactorCoolant coolant : values()) {
            if (coolant.matches(stack.getFluid())) {
                return coolant;
            }
        }
        return null;
    }

    public boolean matches(Fluid fluid) {
        return ModFluids.materialFluid(material)
                .filter(candidate -> candidate == fluid)
                .isPresent();
    }

    public int euPerUnit() {
        return euPerUnit;
    }

    public FluidStack hotOutput(int units) {
        if (units <= 0) {
            return FluidStack.EMPTY;
        }
        if (!hotOutputSameMaterial && "steam".equals(hotOutputMaterial)) {
            return new FluidStack(
                    ModFluids.STEAM_SOURCE.get(),
                    units * outputMultiplier);
        }
        String output = hotOutputSameMaterial ? material : hotOutputMaterial;
        if (output == null || output.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return ModFluids.materialFluid(output)
                .map(fluid -> new FluidStack(fluid, units * outputMultiplier))
                .orElse(FluidStack.EMPTY);
    }

    public boolean moderatesFuel() {
        return this == DISTILLED_WATER
                || this == SEMI_HEAVY_WATER
                || this == HEAVY_WATER;
    }

    public int heatDivider() {
        return switch (this) {
            case MOLTEN_SODIUM -> 6;
            case MOLTEN_TIN -> 3;
            default -> 1;
        };
    }
}
