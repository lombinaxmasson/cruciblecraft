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
 * Prep-only source shape for the GT6 Cluster Mill.
 *
 * <p>This spec deliberately is not referenced by any registry. Landing will
 * bind the same shape to {@code ModRecipeMaps.CLUSTERMILL}. Item IO is left in
 * / right out; energy is back, matching GT6 {@code SBIT_L}/{@code SBIT_R}/
 * {@code SBIT_B}.
 */
public final class ClusterMillPrepSpec {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "clustermill");
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
     * An isolated copy of the one-input/one-output RU machine contract. It
     * must not be added to a registry until the Cluster Mill prep card is
     * promoted.
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
            Gt6SidedIo.policy("clustermill"),
            ClusterMillPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    1, 1, 0, 0,
                    1, 1, 0, 0,
                    STATUSES));

    private ClusterMillPrepSpec() {}

    private static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().size() != 1
                || recipe.itemOutputs().size() != 1
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > 256L) {
            return Optional.of("clustermill_recipe_shape");
        }
        if (recipe.itemInputCounts().size() != 1
                || recipe.itemInputCounts().getFirst() <= 0
                || recipe.itemOutputs().getFirst().isEmpty()
                || recipe.itemOutputs().getFirst().getCount() <= 0
                || recipe.outputChances().size() != 1
                || recipe.outputChances().getFirst() != GTRecipe.GUARANTEED_CHANCE) {
            return Optional.of("clustermill_recipe_amount");
        }
        return Optional.empty();
    }
}
