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
 * Prep-only electric GT6 Loom. Same dump / RecipeMap as {@link LoomPrepSpec};
 * not registered. {@link #EFFICIENCY_PERMILLE} is GT6 {@code NBT_EFFICIENCY}.
 */
public final class ElectricLoomPrepSpec {
    public static final int EFFICIENCY_PERMILLE = 5_000;
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "electricloom");
    private static final ResourceLocation MAP_ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "loom");
    private static final RecipeMap UNREGISTERED_MAP = new RecipeMap(MAP_ID);

    public static final ProcessingMachineSpec SPEC = new ProcessingMachineSpec(
            ID,
            MAP_ID,
            () -> UNREGISTERED_MAP,
            new ProcessingMachineSpec.SlotLayout(
                    7, List.of(0, 1, 2, 3, 4, 5), List.of(6)),
            new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.ELECTRIC,
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    PrepMachineCommon.EU_CAPACITY,
                    PrepMachineCommon.EU_MAX_PACKET),
            new ProcessingMachineSpec.SidedIoPolicy(
                    PrepSidedIo.topInBottomOut(),
                    PrepSidedIo.none(),
                    PrepSidedIo.leftRightEnergy()),
            ElectricLoomPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    6, 1, 0, 0,
                    6, 1, 0, 0,
                    PrepMachineCommon.STATUSES));

    private ElectricLoomPrepSpec() {}

    private static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().isEmpty()
                || recipe.itemInputs().size() > 6
                || recipe.itemOutputs().size() != 1
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > PrepMachineCommon.EU_MAX_PACKET) {
            return Optional.of("electricloom_recipe_shape");
        }
        return Optional.empty();
    }
}
