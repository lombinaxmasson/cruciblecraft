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
 * Prep-only GT6 Laminator (HU / adjacent heat). Not the smelter or melter.
 * Not registered.
 */
public final class LaminatorPrepSpec {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "laminator");
    private static final RecipeMap UNREGISTERED_MAP = new RecipeMap(ID);

    public static final ProcessingMachineSpec SPEC = new ProcessingMachineSpec(
            ID,
            ID,
            () -> UNREGISTERED_MAP,
            new ProcessingMachineSpec.SlotLayout(3, List.of(0, 1), List.of(2)),
            new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.ADJACENT,
                    0L,
                    PrepMachineCommon.HU_MAX_PACKET),
            Gt6SidedIo.policy("laminator"),
            LaminatorPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    2, 1, 0, 0,
                    2, 1, 0, 0,
                    PrepMachineCommon.STATUSES));

    private LaminatorPrepSpec() {}

    private static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 2
                || recipe.itemOutputs().size() != 1
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > PrepMachineCommon.HU_MAX_PACKET) {
            return Optional.of("laminator_recipe_shape");
        }
        return Optional.empty();
    }
}
