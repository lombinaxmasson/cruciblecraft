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
 * Prep-only GT6 Melter. Not the smelter. {@link #PARALLEL} is GT6
 * {@code NBT_PARALLEL} and must not be dropped on landing.
 */
public final class MelterPrepSpec {
    public static final int PARALLEL = 1_000;
    public static final boolean PARALLEL_DURATION = true;
    public static final boolean CHEAP_OVERCLOCKING = true;
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "melter");
    private static final RecipeMap UNREGISTERED_MAP = new RecipeMap(ID);

    public static final ProcessingMachineSpec SPEC = new ProcessingMachineSpec(
            ID,
            ID,
            () -> UNREGISTERED_MAP,
            new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
            new ProcessingMachineSpec.TankLayout(
                    List.of(new ProcessingMachineSpec.TankSpec(
                            0, PrepMachineCommon.MELTER_TANK_IN)),
                    List.of(new ProcessingMachineSpec.TankSpec(
                            1, PrepMachineCommon.MELTER_TANK_OUT))),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.HEAT,
                    ProcessingMachineSpec.EnergyMode.ADJACENT,
                    0L,
                    PrepMachineCommon.HU_MAX_PACKET),
            Gt6SidedIo.policy("melter"),
            MelterPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    1, 1, 1, 1,
                    1, 1, 1, 1,
                    PrepMachineCommon.STATUSES));

    private MelterPrepSpec() {}

    private static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 1
                || recipe.itemOutputs().size() > 1
                || recipe.fluidInputs().size() > 1
                || recipe.fluidOutputs().size() > 1
                || recipe.eut() <= 0L
                || recipe.eut() > PrepMachineCommon.HU_MAX_PACKET) {
            return Optional.of("melter_recipe_shape");
        }
        if (recipe.itemInputs().isEmpty() && recipe.fluidInputs().isEmpty()
                || recipe.itemOutputs().isEmpty() && recipe.fluidOutputs().isEmpty()) {
            return Optional.of("melter_recipe_shape");
        }
        if (recipe.fluidInputs().stream().anyMatch(
                stack -> stack.getAmount() > PrepMachineCommon.MELTER_TANK_IN)
                || recipe.fluidOutputs().stream().anyMatch(
                        stack -> stack.getAmount() > PrepMachineCommon.MELTER_TANK_OUT)) {
            return Optional.of("melter_recipe_amount");
        }
        return Optional.empty();
    }
}
