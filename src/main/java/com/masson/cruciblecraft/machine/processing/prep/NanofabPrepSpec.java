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
 * Prep-only GT6 Nanoscale Fabricator. Not registered. Obtain grids stay
 * blocked until the GT6 IL parts exist.
 */
public final class NanofabPrepSpec {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "nanofab");
    private static final RecipeMap UNREGISTERED_MAP = new RecipeMap(ID);

    public static final ProcessingMachineSpec SPEC = new ProcessingMachineSpec(
            ID,
            ID,
            () -> UNREGISTERED_MAP,
            new ProcessingMachineSpec.SlotLayout(3, List.of(0, 1), List.of(2)),
            new ProcessingMachineSpec.TankLayout(
                    List.of(new ProcessingMachineSpec.TankSpec(
                            0, PrepMachineCommon.PANEL_TANK)),
                    List.of(new ProcessingMachineSpec.TankSpec(
                            1, PrepMachineCommon.PANEL_TANK))),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.ELECTRIC,
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    PrepMachineCommon.EU_CAPACITY,
                    PrepMachineCommon.EU_MAX_PACKET),
            Gt6SidedIo.policy("nanofab"),
            NanofabPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    2, 1, 1, 1,
                    2, 1, 1, 1,
                    PrepMachineCommon.STATUSES));

    private NanofabPrepSpec() {}

    private static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 2
                || recipe.itemOutputs().size() > 1
                || recipe.fluidInputs().size() > 1
                || recipe.fluidOutputs().size() > 1
                || recipe.eut() <= 0L
                || recipe.eut() > PrepMachineCommon.EU_MAX_PACKET) {
            return Optional.of("nanofab_recipe_shape");
        }
        if (recipe.fluidInputs().stream().anyMatch(
                stack -> stack.getAmount() > PrepMachineCommon.PANEL_TANK)
                || recipe.fluidOutputs().stream().anyMatch(
                        stack -> stack.getAmount() > PrepMachineCommon.PANEL_TANK)) {
            return Optional.of("nanofab_recipe_amount");
        }
        return Optional.empty();
    }
}
