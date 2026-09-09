package com.masson.cruciblecraft.machine.processing.prep;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.resources.ResourceLocation;

/**
 * Prep-only GT6 Printer (six ink tanks). Not registered.
 */
public final class PrinterPrepSpec {
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "printer");
    private static final RecipeMap UNREGISTERED_MAP = new RecipeMap(ID);

    public static final ProcessingMachineSpec SPEC = new ProcessingMachineSpec(
            ID,
            ID,
            () -> UNREGISTERED_MAP,
            new ProcessingMachineSpec.SlotLayout(3, List.of(0, 1), List.of(2)),
            new ProcessingMachineSpec.TankLayout(
                    IntStream.range(0, 6)
                            .mapToObj(index -> new ProcessingMachineSpec.TankSpec(
                                    index, PrepMachineCommon.PANEL_TANK))
                            .toList(),
                    List.of()),
            new ProcessingMachineSpec.EnergySpec(
                    EnergyType.ELECTRIC,
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    PrepMachineCommon.EU_CAPACITY,
                    PrepMachineCommon.EU_MAX_PACKET),
            new ProcessingMachineSpec.SidedIoPolicy(
                    PrepSidedIo.leftUpInRightDownOut(),
                    PrepSidedIo.leftUpIn(),
                    PrepSidedIo.backEnergy()),
            PrinterPrepSpec::validate,
            ProcessingMachineSpec.BufferPolicy.PAUSE,
            Gt6BasicMachineGui.ui(
                    2, 1, 6, 0,
                    2, 1, 6, 0,
                    PrepMachineCommon.STATUSES));

    private PrinterPrepSpec() {}

    private static Optional<String> validate(GTRecipe recipe) {
        if (recipe.itemInputs().size() > 2
                || recipe.itemOutputs().size() > 1
                || recipe.fluidInputs().size() > 6
                || !recipe.fluidOutputs().isEmpty()
                || recipe.eut() <= 0L
                || recipe.eut() > PrepMachineCommon.EU_MAX_PACKET) {
            return Optional.of("printer_recipe_shape");
        }
        if (recipe.fluidInputs().stream().anyMatch(
                stack -> stack.getAmount() > PrepMachineCommon.PANEL_TANK)) {
            return Optional.of("printer_recipe_amount");
        }
        return Optional.empty();
    }
}
