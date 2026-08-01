package com.masson.cruciblecraft.machine.processing;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Directional fluid view: input views only fill, output views only drain. */
public final class SidedFluidHandler implements IFluidHandler {
    private final List<FluidTank> tanks;
    private final List<Integer> exposed;
    private final ProcessingMachineSpec.CapabilityAccess access;
    private final Runnable mutation;

    public SidedFluidHandler(
            List<FluidTank> tanks,
            List<Integer> exposed,
            ProcessingMachineSpec.CapabilityAccess access) {
        this(tanks, exposed, access, () -> {});
    }

    public SidedFluidHandler(
            List<FluidTank> tanks,
            List<Integer> exposed,
            ProcessingMachineSpec.CapabilityAccess access,
            Runnable mutation) {
        this.tanks = List.copyOf(tanks);
        this.exposed = List.copyOf(exposed);
        this.access = access;
        this.mutation = mutation;
        if (access == ProcessingMachineSpec.CapabilityAccess.NONE) {
            throw new IllegalArgumentException("Do not expose a NONE capability adapter");
        }
    }

    @Override public int getTanks() { return exposed.size(); }
    @Override public FluidStack getFluidInTank(int tank) {
        return actual(tank).getFluid().copy();
    }
    @Override public int getTankCapacity(int tank) {
        return actual(tank).getCapacity();
    }
    @Override public boolean isFluidValid(int tank, FluidStack stack) {
        return access == ProcessingMachineSpec.CapabilityAccess.INPUT
                && actual(tank).isFluidValid(stack);
    }
    @Override public int fill(FluidStack resource, FluidAction action) {
        if (access != ProcessingMachineSpec.CapabilityAccess.INPUT) {
            return 0;
        }
        int remaining = resource.getAmount();
        for (int index : exposed) {
            remaining -= tanks.get(index).fill(
                    resource.copyWithAmount(remaining), action);
            if (remaining <= 0) {
                break;
            }
        }
        int filled = resource.getAmount() - remaining;
        if (action.execute() && filled > 0) {
            mutation.run();
        }
        return filled;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        if (access != ProcessingMachineSpec.CapabilityAccess.OUTPUT || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }

        List<DrainStep> plan = new ArrayList<>();
        FluidStack result = FluidStack.EMPTY;
        for (int index : exposed) {
            int remaining = resource.getAmount() - result.getAmount();
            FluidStack drained = tanks.get(index).drain(
                    resource.copyWithAmount(remaining),
                    FluidAction.SIMULATE);
            if (!drained.isEmpty()) {
                if (result.isEmpty()) {
                    result = drained.copy();
                } else if (FluidStack.isSameFluidSameComponents(result, drained)) {
                    result.grow(drained.getAmount());
                } else {
                    throw incompatibleDrain(result, drained, index);
                }
                plan.add(new DrainStep(index, drained.copy()));
            }
            if (result.getAmount() >= resource.getAmount()) {
                break;
            }
        }
        if (!action.execute() || result.isEmpty()) {
            return result;
        }

        for (DrainStep step : plan) {
            FluidStack drained = tanks.get(step.tankIndex()).drain(
                    step.expected().copy(),
                    FluidAction.EXECUTE);
            if (!FluidStack.isSameFluidSameComponents(step.expected(), drained)
                    || step.expected().getAmount() != drained.getAmount()) {
                throw new IllegalStateException(
                        "Tank " + step.tankIndex() + " executed drain "
                                + fluidDescription(drained) + " x" + drained.getAmount()
                                + " after simulating "
                                + fluidDescription(step.expected()) + " x"
                                + step.expected().getAmount());
            }
        }
        mutation.run();
        return result;
    }
    @Override public FluidStack drain(int maxDrain, FluidAction action) {
        if (access != ProcessingMachineSpec.CapabilityAccess.OUTPUT || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        for (int index : exposed) {
            FluidStack drained = tanks.get(index).drain(maxDrain, action);
            if (!drained.isEmpty()) {
                if (action.execute()) {
                    mutation.run();
                }
                return drained;
            }
        }
        return FluidStack.EMPTY;
    }

    private FluidTank actual(int tank) {
        if (tank < 0 || tank >= exposed.size()) {
            throw new IndexOutOfBoundsException("Sided tank " + tank);
        }
        return tanks.get(exposed.get(tank));
    }

    private static IllegalStateException incompatibleDrain(
            FluidStack aggregate,
            FluidStack drained,
            int tankIndex) {
        return new IllegalStateException(
                "Cannot aggregate " + fluidDescription(drained) + " from tank "
                        + tankIndex + " with " + fluidDescription(aggregate));
    }

    private static String fluidDescription(FluidStack stack) {
        return stack.isEmpty()
                ? "empty"
                : BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString();
    }

    private record DrainStep(int tankIndex, FluidStack expected) {}
}
