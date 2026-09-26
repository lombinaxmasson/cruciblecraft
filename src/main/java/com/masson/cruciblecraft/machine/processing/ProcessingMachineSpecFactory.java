package com.masson.cruciblecraft.machine.processing;

import java.util.List;
import java.util.function.Supplier;

import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

/**
 * Builds a {@link ProcessingMachineSpec} from the kind-level delivery sidecar.
 *
 * <p>Slots, tanks, energy mode, sided I/O and GUI coordinates come from
 * {@link MachineDeliveryCatalog} plus {@link Gt6SidedIo} and
 * {@link Gt6BasicMachineGui}. Energy capacity, the input-tank size and the
 * recipe validator stay caller-supplied: capacity is the kind envelope, and
 * ignition / parallel / scan behavior stays a Java hook on that validator
 * or on {@link ProcessingMachineIgnition}.
 */
public final class ProcessingMachineSpecFactory {
    private ProcessingMachineSpecFactory() {}

    public static ProcessingMachineSpec create(
            MachineDeliveryCatalog.Host host,
            Supplier<RecipeMap> recipeMap,
            long energyCapacity,
            long maxPacket,
            int fluidInputCapacity,
            List<String> statuses,
            ProcessingMachineSpec.ExecutionValidator validator) {
        int itemInputs = host.itemInputs();
        int itemOutputs = host.itemOutputs();
        int fluidInputs = host.fluidInputs();
        int fluidOutputs = host.fluidOutputs();
        List<Integer> inputs = java.util.stream.IntStream.range(0, itemInputs)
                .boxed()
                .toList();
        List<Integer> outputs = java.util.stream.IntStream
                .range(itemInputs, itemInputs + itemOutputs)
                .boxed()
                .toList();
        List<ProcessingMachineSpec.TankSpec> inputTanks = java.util.stream.IntStream
                .range(0, fluidInputs)
                .mapToObj(index -> new ProcessingMachineSpec.TankSpec(
                        index, fluidInputCapacity))
                .toList();
        List<ProcessingMachineSpec.TankSpec> outputTanks = java.util.stream.IntStream
                .range(fluidInputs, fluidInputs + fluidOutputs)
                .mapToObj(index -> new ProcessingMachineSpec.TankSpec(
                        index, ModProcessingMachines.UNBOUNDED_FLUID_OUTPUT))
                .toList();
        return new ProcessingMachineSpec(
                host.id(),
                host.recipeMap(),
                recipeMap,
                new ProcessingMachineSpec.SlotLayout(
                        itemInputs + itemOutputs, inputs, outputs),
                new ProcessingMachineSpec.TankLayout(inputTanks, outputTanks),
                new ProcessingMachineSpec.EnergySpec(
                        host.energyType(),
                        host.energyMode(),
                        energyCapacity,
                        maxPacket),
                Gt6SidedIo.policy(host.id().getPath()),
                validator,
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                Gt6BasicMachineGui.ui(
                        host.gt6InItems(),
                        host.gt6OutItems(),
                        host.gt6InFluids(),
                        host.gt6OutFluids(),
                        itemInputs,
                        itemOutputs,
                        fluidInputs,
                        fluidOutputs,
                        statuses));
    }
}
