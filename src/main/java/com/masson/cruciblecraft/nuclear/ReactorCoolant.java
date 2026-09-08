package com.masson.cruciblecraft.nuclear;

import java.util.Optional;

import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * GT6 reactor coolant identities owned by CC. Conversion numbers come from
 * {@code CS.java} and {@code MultiTileEntityReactorCore1x1/2x2}; hot outputs
 * are independent fluids, never {@code materialFluid(source)}.
 */
public enum ReactorCoolant {
    DISTILLED_WATER("water_distilled", InputForm.CHEMICAL, "steam", 80, 160, 1, true),
    SEMI_HEAVY_WATER(
            "semiheavy_water", InputForm.CHEMICAL, "hot_semiheavy_water", 40, 1, 1, true),
    HEAVY_WATER("heavy_water", InputForm.CHEMICAL, "hot_heavy_water", 50, 1, 1, true),
    TRITIATED_WATER(
            "tritiated_water", InputForm.CHEMICAL, "hot_tritiated_water", 60, 1, 1, true),
    MOLTEN_TIN("tin", InputForm.MOLTEN, "hot_molten_tin", 40, 1, 3, false),
    MOLTEN_SODIUM("sodium", InputForm.MOLTEN, "hot_molten_sodium", 30, 1, 6, false),
    MOLTEN_LICL(
            "lithium_chloride", InputForm.MOLTEN, "hot_molten_licl", 15, 1, 1, false),
    CARBON_DIOXIDE(
            "carbon_dioxide", InputForm.CHEMICAL, "hot_carbon_dioxide", 20, 1, 1, false),
    HELIUM("helium", InputForm.CHEMICAL, "hot_helium", 30, 1, 1, false);

    public static final int EU_PER_WATER = 80;
    public static final int STEAM_PER_WATER = 160;

    private final String inputMaterial;
    private final InputForm inputForm;
    private final String hotOutputId;
    private final int euPerUnit;
    private final int outputMultiplier;
    private final int heatDivider;
    private final boolean moderatesFuel;

    ReactorCoolant(
            String inputMaterial,
            InputForm inputForm,
            String hotOutputId,
            int euPerUnit,
            int outputMultiplier,
            int heatDivider,
            boolean moderatesFuel) {
        this.inputMaterial = inputMaterial;
        this.inputForm = inputForm;
        this.hotOutputId = hotOutputId;
        this.euPerUnit = euPerUnit;
        this.outputMultiplier = outputMultiplier;
        this.heatDivider = heatDivider;
        this.moderatesFuel = moderatesFuel;
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
        return inputFluid()
                .filter(candidate -> candidate == fluid)
                .isPresent()
                || flowingInput()
                        .filter(candidate -> candidate == fluid)
                        .isPresent();
    }

    public Optional<Fluid> inputFluid() {
        if (inputForm == InputForm.MOLTEN) {
            return ModFluids.molten(inputMaterial).map(entry -> entry.source().get());
        }
        return ModFluids.materialFluid(inputMaterial);
    }

    public Optional<Fluid> flowingInput() {
        if (inputForm == InputForm.MOLTEN) {
            return ModFluids.molten(inputMaterial).map(entry -> entry.flowing().get());
        }
        return ModFluids.chemical(inputMaterial).map(entry -> entry.flowing().get());
    }

    public int euPerUnit() {
        return euPerUnit;
    }

    public int outputMultiplier() {
        return outputMultiplier;
    }

    public String hotOutputId() {
        return hotOutputId;
    }

    public boolean steamOutput() {
        return "steam".equals(hotOutputId);
    }

    public FluidStack hotOutput(int units) {
        if (units <= 0) {
            return FluidStack.EMPTY;
        }
        int amount = outputAmount(units);
        if (steamOutput()) {
            return new FluidStack(ModFluids.STEAM_SOURCE.get(), amount);
        }
        return ModFluids.hotSource(hotOutputId)
                .map(fluid -> new FluidStack(fluid, amount))
                .orElse(FluidStack.EMPTY);
    }

    public int outputAmount(int units) {
        if (units <= 0) {
            return 0;
        }
        return Math.multiplyExact(units, outputMultiplier);
    }

    public boolean moderatesFuel() {
        return moderatesFuel;
    }

    public int heatDivider() {
        return heatDivider;
    }

    private enum InputForm {
        CHEMICAL,
        MOLTEN
    }
}
