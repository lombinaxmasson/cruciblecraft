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
 * Unregistered copy of the live Sanding Machine contract.
 *
 * <p>The registered host is {@code ModProcessingMachines.SANDING}. This
 * file stays out of every registry so prep-era source shape can still be
 * compared without a second RecipeMap. Grindstone {@code 32703} stays out
 * of this spec.
 */
public final class SandingPrepSpec {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "sanding");
    private static final RecipeMap UNREGISTERED_MAP = new RecipeMap(ID);

    public static final ProcessingMachineSpec SPEC = new ProcessingMachineSpec(
            ID,
            ID,
            () -> UNREGISTERED_MAP,
            new ProcessingMachineSpec.SlotLayout(3, List.of(0), List.of(1, 2)),
            new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.KINETIC_ROTATION,
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    PrepMachineCommon.RU_CAPACITY,
                    PrepMachineCommon.RU_MAX_PACKET),
            new ProcessingMachineSpec.SidedIoPolicy(
                    PrepSidedIo.leftInRightOut(),
                    PrepSidedIo.none(),
                    PrepSidedIo.upEnergy()),
            SandingPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    1, 2, 0, 0,
                    1, 2, 0, 0,
                    PrepMachineCommon.STATUSES));

    private SandingPrepSpec() {}

    private static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().size() != 1
                || recipe.itemOutputs().isEmpty()
                || recipe.itemOutputs().size() > 2
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() != 16L) {
            return Optional.of("sanding_recipe_shape");
        }
        if (recipe.itemInputCounts().size() != 1
                || recipe.itemInputCounts().getFirst() <= 0
                || recipe.itemOutputs().getFirst().isEmpty()
                || recipe.itemOutputs().getFirst().getCount() <= 0) {
            return Optional.of("sanding_recipe_amount");
        }
        return Optional.empty();
    }
}
