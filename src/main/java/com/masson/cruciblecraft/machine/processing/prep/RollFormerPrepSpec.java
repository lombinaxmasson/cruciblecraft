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
 * Prep-only source shape for the GT6 Roll Former.
 *
 * <p>This spec deliberately is not referenced by any registry. Landing will
 * bind the same shape to {@code ModRecipeMaps.ROLLFORMER} after the current
 * unique-active card is closed.
 */
public final class RollFormerPrepSpec {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "rollformer");
    private static final RecipeMap UNREGISTERED_MAP = new RecipeMap(ID);
    private static final List<String> STATUSES = List.of(
            "idle",
            "running",
            "invalid_recipe",
            "recipe_power_exceeded",
            "overcharged",
            "output_blocked",
            "underpowered",
            "unsupported_version",
            "inventory_layout_quarantined",
            "material_quarantined",
            "unknown");

    /**
     * An isolated copy of the component-machine contract. It must not be
     * added to a registry until the roll-former prep card is promoted.
     */
    public static final ProcessingMachineSpec SPEC = new ProcessingMachineSpec(
            ID,
            ID,
            () -> UNREGISTERED_MAP,
            new ProcessingMachineSpec.SlotLayout(2, List.of(0), List.of(1)),
            new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.KINETIC_ROTATION,
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    4_096L,
                    256L),
            new ProcessingMachineSpec.SidedIoPolicy(
                    (front, side) -> side == null
                            ? ProcessingMachineSpec.CapabilityAccess.NONE
                            : side == front
                                    ? ProcessingMachineSpec.CapabilityAccess.OUTPUT
                                    : ProcessingMachineSpec.CapabilityAccess.INPUT,
                    (front, side) ->
                            ProcessingMachineSpec.CapabilityAccess.NONE,
                    (front, side) -> side != null
                            && side == front.getOpposite()
                                    ? ProcessingMachineSpec.CapabilityAccess.INPUT
                                    : ProcessingMachineSpec.CapabilityAccess.NONE),
            RollFormerPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    1, 1, 0, 0,
                    1, 1, 0, 0,
                    STATUSES));

    private RollFormerPrepSpec() {}

    private static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().size() != 1
                || recipe.itemOutputs().size() != 1
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 256L) {
            return Optional.of("rollformer_recipe_shape");
        }
        if (recipe.itemInputCounts().size() != 1
                || recipe.itemInputCounts().getFirst() <= 0
                || recipe.itemOutputs().getFirst().isEmpty()
                || recipe.itemOutputs().getFirst().getCount() <= 0
                || recipe.outputChances().size() != 1
                || recipe.outputChances().getFirst() != GTRecipe.GUARANTEED_CHANCE) {
            return Optional.of("rollformer_recipe_amount");
        }
        return Optional.empty();
    }
}
