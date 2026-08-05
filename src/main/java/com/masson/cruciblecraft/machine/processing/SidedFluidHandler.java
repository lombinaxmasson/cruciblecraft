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
        boolean[] seen = new boolean[this.tanks.size()];
        for (int index : this.exposed) {
            if (index < 0 || index >= this.tanks.size()) {
                throw new IndexOutOfBoundsException("Exposed fluid tank " + index);
            }
            if (seen[index]) {
                throw new IllegalArgumentException(
                        "Fluid tank " + index + " is exposed more than once");
            }
            seen[index] = true;
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
        if (access != ProcessingMachineSpec.CapabilityAccess.INPUT
                || resource.isEmpty()) {
            return 0;
        }

        List<TankSnapshot> before = snapshotExposed();
        int filled;
        try {
            FillPlan plan = planFill(resource);
            verifySnapshots("fill simulation", before);
            filled = plan.filled();
            if (!action.execute() || filled == 0) {
                return filled;
            }
            for (FillStep step : plan.steps()) {
                int executed = tanks.get(step.tankIndex()).fill(
                        resource.copyWithAmount(step.amount()),
                        FluidAction.EXECUTE);
                if (executed != step.amount()) {
                    throw new IllegalStateException(
                            "Tank " + step.tankIndex() + " executed fill x"
                                    + executed + " after simulating x" + step.amount()
                                    + " of " + fluidDescription(resource));
                }
            }
            verifyFillPostImages(before, plan, resource);
        } catch (RuntimeException failure) {
            throw rollback("multi-tank fill", before, failure);
        }
        mutation.run();
        return filled;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) {
        if (access != ProcessingMachineSpec.CapabilityAccess.OUTPUT || resource.isEmpty()) {
            return FluidStack.EMPTY;
        }

        List<TankSnapshot> before = snapshotExposed();
        FluidStack result;
        try {
            DrainPlan plan = planDrain(resource);
            verifySnapshots("drain simulation", before);
            result = resource.copyWithAmount(plan.drained());
            if (!action.execute() || result.isEmpty()) {
                return result;
            }
            for (DrainStep step : plan.steps()) {
                FluidStack drained = tanks.get(step.tankIndex()).drain(
                        step.expected().copy(),
                        FluidAction.EXECUTE);
                if (!sameFluid(step.expected(), drained)) {
                    throw new IllegalStateException(
                            "Tank " + step.tankIndex() + " executed drain "
                                    + stackDescription(drained)
                                    + " after simulating "
                                    + stackDescription(step.expected()));
                }
            }
            verifyDrainPostImages(before, plan, resource);
        } catch (RuntimeException failure) {
            throw rollback("multi-tank drain", before, failure);
        }
        mutation.run();
        return result;
    }
    @Override public FluidStack drain(int maxDrain, FluidAction action) {
        if (access != ProcessingMachineSpec.CapabilityAccess.OUTPUT || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        for (int index : exposed) {
            FluidStack stored = tanks.get(index).getFluid();
            if (!stored.isEmpty()) {
                return drain(stored.copyWithAmount(maxDrain), action);
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

    private FillPlan planFill(FluidStack resource) {
        List<FillStep> steps = new ArrayList<>();
        int remaining = resource.getAmount();
        for (int index : exposed) {
            if (remaining == 0) {
                break;
            }
            int accepted = tanks.get(index).fill(
                    resource.copyWithAmount(remaining),
                    FluidAction.SIMULATE);
            if (accepted < 0 || accepted > remaining) {
                throw new IllegalStateException(
                        "Tank " + index + " simulated invalid fill x" + accepted
                                + " for remaining x" + remaining + " of "
                                + fluidDescription(resource));
            }
            if (accepted > 0) {
                steps.add(new FillStep(index, accepted));
                remaining -= accepted;
            }
        }
        return new FillPlan(List.copyOf(steps), resource.getAmount() - remaining);
    }

    private DrainPlan planDrain(FluidStack resource) {
        List<DrainStep> steps = new ArrayList<>();
        int drainedAmount = 0;
        for (int index : exposed) {
            int remaining = resource.getAmount() - drainedAmount;
            if (remaining == 0) {
                break;
            }
            FluidStack drained = tanks.get(index).drain(
                    resource.copyWithAmount(remaining),
                    FluidAction.SIMULATE);
            if (drained.isEmpty()) {
                continue;
            }
            if (drained.getAmount() > remaining
                    || !FluidStack.isSameFluidSameComponents(resource, drained)) {
                throw new IllegalStateException(
                        "Simulated incompatible drain from tank " + index + ": "
                                + stackDescription(drained) + " for request "
                                + stackDescription(resource));
            }
            steps.add(new DrainStep(index, drained.copy()));
            drainedAmount += drained.getAmount();
        }
        return new DrainPlan(List.copyOf(steps), drainedAmount);
    }

    private List<TankSnapshot> snapshotExposed() {
        boolean[] seen = new boolean[tanks.size()];
        List<TankSnapshot> snapshots = new ArrayList<>(exposed.size());
        for (int index : exposed) {
            if (index < 0 || index >= tanks.size()) {
                throw new IndexOutOfBoundsException("Exposed fluid tank " + index);
            }
            if (!seen[index]) {
                seen[index] = true;
                snapshots.add(new TankSnapshot(
                        index, tanks.get(index).getFluid().copy()));
            }
        }
        return List.copyOf(snapshots);
    }

    private void verifySnapshots(
            String phase,
            List<TankSnapshot> expected) {
        for (TankSnapshot snapshot : expected) {
            FluidStack actual = tanks.get(snapshot.tankIndex()).getFluid();
            if (!sameFluid(snapshot.fluid(), actual)) {
                throw new IllegalStateException(
                        "Tank " + snapshot.tankIndex() + " mutated during " + phase
                                + ": expected " + stackDescription(snapshot.fluid())
                                + ", observed " + stackDescription(actual));
            }
        }
    }

    private void verifyFillPostImages(
            List<TankSnapshot> before,
            FillPlan plan,
            FluidStack resource) {
        for (TankSnapshot snapshot : before) {
            int added = plan.steps().stream()
                    .filter(step -> step.tankIndex() == snapshot.tankIndex())
                    .mapToInt(FillStep::amount)
                    .sum();
            FluidStack expected = snapshot.fluid().copy();
            if (added > 0) {
                if (expected.isEmpty()) {
                    expected = resource.copyWithAmount(added);
                } else if (FluidStack.isSameFluidSameComponents(expected, resource)) {
                    expected.grow(added);
                } else {
                    throw new IllegalStateException(
                            "Tank " + snapshot.tankIndex()
                                    + " simulated filling incompatible pre-image "
                                    + stackDescription(expected) + " with "
                                    + stackDescription(resource));
                }
            }
            verifyPostImage("fill", snapshot.tankIndex(), expected);
        }
    }

    private void verifyDrainPostImages(
            List<TankSnapshot> before,
            DrainPlan plan,
            FluidStack resource) {
        for (TankSnapshot snapshot : before) {
            int removed = plan.steps().stream()
                    .filter(step -> step.tankIndex() == snapshot.tankIndex())
                    .mapToInt(step -> step.expected().getAmount())
                    .sum();
            FluidStack expected = snapshot.fluid().copy();
            if (removed > 0) {
                if (!FluidStack.isSameFluidSameComponents(expected, resource)
                        || expected.getAmount() < removed) {
                    throw new IllegalStateException(
                            "Tank " + snapshot.tankIndex()
                                    + " simulated draining " + removed + " mB of "
                                    + fluidDescription(resource) + " from pre-image "
                                    + stackDescription(expected));
                }
                expected.shrink(removed);
            }
            verifyPostImage("drain", snapshot.tankIndex(), expected);
        }
    }

    private void verifyPostImage(
            String operation,
            int tankIndex,
            FluidStack expected) {
        FluidStack actual = tanks.get(tankIndex).getFluid();
        if (!sameFluid(expected, actual)) {
            throw new IllegalStateException(
                    "Tank " + tankIndex + " violated executed " + operation
                            + " post-image: expected " + stackDescription(expected)
                            + ", observed " + stackDescription(actual));
        }
    }

    private IllegalStateException rollback(
            String operation,
            List<TankSnapshot> before,
            RuntimeException failure) {
        List<RuntimeException> rollbackFailures = new ArrayList<>();
        for (TankSnapshot snapshot : before) {
            try {
                tanks.get(snapshot.tankIndex()).setFluid(snapshot.fluid().copy());
                FluidStack restored = tanks.get(snapshot.tankIndex()).getFluid();
                if (!sameFluid(snapshot.fluid(), restored)) {
                    rollbackFailures.add(new IllegalStateException(
                            "Tank " + snapshot.tankIndex()
                                    + " rollback post-image mismatch: expected "
                                    + stackDescription(snapshot.fluid()) + ", observed "
                                    + stackDescription(restored)));
                }
            } catch (RuntimeException rollbackFailure) {
                rollbackFailures.add(new IllegalStateException(
                        "Tank " + snapshot.tankIndex() + " threw during rollback",
                        rollbackFailure));
            }
        }

        String detail = failure.getMessage() == null
                ? failure.getClass().getName()
                : failure.getMessage();
        IllegalStateException result = new IllegalStateException(
                operation + " failed "
                        + (rollbackFailures.isEmpty()
                                ? "and all exposed tanks were restored: "
                                : "and ROLLBACK FAILED; fluid state may be inconsistent: ")
                        + detail,
                failure);
        rollbackFailures.forEach(result::addSuppressed);
        return result;
    }

    private static String fluidDescription(FluidStack stack) {
        return stack.isEmpty()
                ? "empty"
                : BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString();
    }

    private static String stackDescription(FluidStack stack) {
        return fluidDescription(stack) + " x" + stack.getAmount();
    }

    private static boolean sameFluid(FluidStack first, FluidStack second) {
        return first.getAmount() == second.getAmount()
                && (first.isEmpty() && second.isEmpty()
                || FluidStack.isSameFluidSameComponents(first, second));
    }

    private record TankSnapshot(int tankIndex, FluidStack fluid) {}
    private record FillStep(int tankIndex, int amount) {}
    private record FillPlan(List<FillStep> steps, int filled) {}
    private record DrainStep(int tankIndex, FluidStack expected) {}
    private record DrainPlan(List<DrainStep> steps, int drained) {}
}
