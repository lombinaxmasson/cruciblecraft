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
 * Prep-only kinetic GT6 Loom. Shares {@code gt.recipe.loom} with the electric
 * host; this spec is not registered.
 */
public final class LoomPrepSpec {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "loom");
    private static final RecipeMap UNREGISTERED_MAP = new RecipeMap(ID);

    public static final ProcessingMachineSpec SPEC = new ProcessingMachineSpec(
            ID,
            ID,
            () -> UNREGISTERED_MAP,
            new ProcessingMachineSpec.SlotLayout(
                    7, List.of(0, 1, 2, 3, 4, 5), List.of(6)),
            new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.KINETIC_ROTATION,
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    PrepMachineCommon.RU_CAPACITY,
                    PrepMachineCommon.RU_MAX_PACKET),
            new ProcessingMachineSpec.SidedIoPolicy(
                    PrepSidedIo.topInBottomOut(),
                    PrepSidedIo.none(),
                    PrepSidedIo.leftRightEnergy()),
            LoomPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    6, 1, 0, 0,
                    6, 1, 0, 0,
                    PrepMachineCommon.STATUSES));

    private LoomPrepSpec() {}

    static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().isEmpty()
                || recipe.itemInputs().size() > 6
                || recipe.itemOutputs().size() != 1
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > PrepMachineCommon.RU_MAX_PACKET) {
            return Optional.of("loom_recipe_shape");
        }
        return Optional.empty();
    }
}
