package com.masson.cruciblecraft.machine.processing.prep;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.resources.ResourceLocation;

/**
 * Prep-only GT6 Slicer shape. Not referenced by any registry.
 */
public final class SlicerPrepSpec {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "slicer");
    private static final RecipeMap UNREGISTERED_MAP = new RecipeMap(ID);

    public static final ProcessingMachineSpec SPEC = new ProcessingMachineSpec(
            ID,
            ID,
            () -> UNREGISTERED_MAP,
            new ProcessingMachineSpec.SlotLayout(4, List.of(0, 1), List.of(2, 3)),
            new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.ELECTRIC,
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    PrepMachineCommon.EU_CAPACITY,
                    PrepMachineCommon.EU_MAX_PACKET),
            new ProcessingMachineSpec.SidedIoPolicy(
                    PrepSidedIo.leftUpInRightDownOut(),
                    PrepSidedIo.none(),
                    PrepSidedIo.backEnergy()),
            SlicerPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    2, 2, 0, 0,
                    2, 2, 0, 0,
                    PrepMachineCommon.STATUSES));

    private SlicerPrepSpec() {}

    private static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 2
                || recipe.itemOutputs().size() > 2
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > PrepMachineCommon.EU_MAX_PACKET) {
            return Optional.of("slicer_recipe_shape");
        }
        return Optional.empty();
    }
}
