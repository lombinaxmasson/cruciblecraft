package com.masson.cruciblecraft.machine.processing.prep;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.machine.processing.Gt6SidedIo;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.resources.ResourceLocation;

/**
 * Prep-only GT6 Pressure Washer / Debarker. Textures use the GT6
 * {@code debarker} key. Not registered.
 */
public final class PressureWasherPrepSpec {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "pressurewasher");
    private static final RecipeMap UNREGISTERED_MAP = new RecipeMap(ID);

    public static final ProcessingMachineSpec SPEC = new ProcessingMachineSpec(
            ID,
            ID,
            () -> UNREGISTERED_MAP,
            new ProcessingMachineSpec.SlotLayout(3, List.of(0), List.of(1, 2)),
            new ProcessingMachineSpec.TankLayout(
                    List.of(new ProcessingMachineSpec.TankSpec(
                            0, PrepMachineCommon.PANEL_TANK)),
                    List.of()),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.KINETIC_ROTATION,
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    PrepMachineCommon.RU_CAPACITY,
                    PrepMachineCommon.RU_MAX_PACKET),
            Gt6SidedIo.policy("pressurewasher"),
            PressureWasherPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    1, 2, 1, 0,
                    1, 2, 1, 0,
                    PrepMachineCommon.STATUSES));

    private PressureWasherPrepSpec() {}

    private static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 1
                || recipe.itemOutputs().size() > 2
                || recipe.fluidInputs().size() > 1
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > PrepMachineCommon.RU_MAX_PACKET) {
            return Optional.of("pressurewasher_recipe_shape");
        }
        if (recipe.fluidInputs().stream().anyMatch(
                stack -> stack.getAmount() > PrepMachineCommon.PANEL_TANK)) {
            return Optional.of("pressurewasher_recipe_amount");
        }
        return Optional.empty();
    }
}
