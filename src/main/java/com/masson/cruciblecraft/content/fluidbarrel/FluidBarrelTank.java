package com.masson.cruciblecraft.content.fluidbarrel;

import java.util.function.BooleanSupplier;

import com.masson.cruciblecraft.api.fluid.LongFluidHandler;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Long-capacity tank. Logistics barrels keep the fluid type at 0 mB.
 * Sealed barrels reject both fill and drain through this capability surface;
 * plunger and hazard removal use {@link #removeAmount}.
 */
public final class FluidBarrelTank implements IFluidHandler, LongFluidHandler {
    private final long capacity;
    private final boolean keepFilter;
    private final BooleanSupplier transferLocked;
    private final Runnable changed;
    private Fluid fluid = Fluids.EMPTY;
    private boolean hasType;
    private long amount;
    private boolean notify = true;

    public FluidBarrelTank(
            long capacity,
            boolean keepFilter,
            BooleanSupplier transferLocked,
            Runnable changed) {
        if (capacity <= 0L) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.keepFilter = keepFilter;
        this.transferLocked = transferLocked;
        this.changed = changed;
    }

    public long capacity() {
        return capacity;
    }

    public long amount() {
        return amount;
    }

    /** Int-capped view of {@link #amount()} for the NeoForge fluid API. */
    public int getFluidAmount() {
        return saturatedInt(amount);
    }

    public boolean hasType() {
        return hasType;
    }

    public Fluid fluid() {
        return hasType ? fluid : Fluids.EMPTY;
    }

    public FluidStack sample() {
        if (!hasType || amount <= 0L || fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, saturatedInt(amount));
    }

    public void setEmpty() {
        if (!hasType && amount == 0L) {
            return;
        }
        hasType = false;
        fluid = Fluids.EMPTY;
        amount = 0L;
        notifyChanged();
    }

    public void readFluid(String fluidId, long nextAmount) {
        notify = false;
        try {
            Fluid resolved = FluidBarrelFluids.fluid(fluidId);
            if (resolved == Fluids.EMPTY) {
                hasType = false;
                fluid = Fluids.EMPTY;
                amount = 0L;
            } else {
                if (nextAmount < 0L) {
                    nextAmount = 0L;
                }
                if (nextAmount > capacity) {
                    nextAmount = capacity;
                }
                fluid = resolved;
                hasType = true;
                amount = nextAmount;
            }
        } finally {
            notify = true;
        }
    }

    public String storedFluidId() {
        return FluidBarrelFluids.fluidId(fluid());
    }

    public void setContents(Fluid next, long nextAmount) {
        if (next == null || next == Fluids.EMPTY) {
            setEmpty();
            return;
        }
        if (nextAmount < 0L) {
            nextAmount = 0L;
        }
        if (nextAmount > capacity) {
            nextAmount = capacity;
        }
        if (hasType && fluid == next && amount == nextAmount) {
            return;
        }
        fluid = next;
        hasType = true;
        amount = nextAmount;
        notifyChanged();
    }

    public boolean accepts(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return !hasType || stack.getFluid() == fluid;
    }

    public long fillAmount(
            FluidStack resource, long maxFill, FluidAction action) {
        if (transferLocked.getAsBoolean()
                || !accepts(resource)
                || maxFill <= 0L) {
            return 0L;
        }
        long accepted = Math.min(Math.max(0L, capacity - amount), maxFill);
        if (accepted <= 0L) {
            return 0L;
        }
        if (action.execute()) {
            if (!hasType) {
                fluid = resource.getFluid();
                hasType = true;
            }
            amount += accepted;
            notifyChanged();
        }
        return accepted;
    }

    public LongFluidStack extractAmount(long maxDrain, FluidAction action) {
        if (transferLocked.getAsBoolean()) {
            return LongFluidStack.empty();
        }
        return removeAmount(maxDrain, action);
    }

    public LongFluidStack removeAmount(long maxDrain, FluidAction action) {
        if (maxDrain <= 0L || !hasType || amount <= 0L) {
            return LongFluidStack.empty();
        }
        long drained = Math.min(amount, maxDrain);
        FluidStack result = new FluidStack(fluid, saturatedInt(drained));
        if (action.execute()) {
            amount -= drained;
            if (amount <= 0L) {
                amount = 0L;
                if (!keepFilter) {
                    hasType = false;
                    fluid = Fluids.EMPTY;
                }
            }
            notifyChanged();
        }
        return new LongFluidStack(result, drained);
    }

    private void notifyChanged() {
        if (notify) {
            changed.run();
        }
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public int tanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? sample() : FluidStack.EMPTY;
    }

    @Override
    public FluidStack fluid(int tank) {
        return getFluidInTank(tank);
    }

    @Override
    public long amount(int tank) {
        return tank == 0 ? amount : 0L;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? saturatedInt(capacity) : 0;
    }

    @Override
    public long capacity(int tank) {
        return tank == 0 ? capacity : 0L;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0
                && !transferLocked.getAsBoolean()
                && accepts(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty()) {
            return 0;
        }
        return saturatedInt(fillAmount(resource, resource.getAmount(), action));
    }

    @Override
    public long fill(
            int tank, FluidStack resource, long maxFill, FluidAction action) {
        if (tank != 0) {
            return 0L;
        }
        return fillAmount(resource, maxFill, action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource == null
                || resource.isEmpty()
                || !hasType
                || resource.getFluid() != fluid) {
            return FluidStack.EMPTY;
        }
        LongFluidStack drained = extractAmount(resource.getAmount(), action);
        return drained.isEmpty() ? FluidStack.EMPTY : drained.fluid();
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        LongFluidStack drained = extractAmount(maxDrain, action);
        return drained.isEmpty() ? FluidStack.EMPTY : drained.fluid();
    }

    @Override
    public LongFluidStack drain(int tank, long maxDrain, FluidAction action) {
        if (tank != 0) {
            return LongFluidStack.empty();
        }
        return extractAmount(maxDrain, action);
    }

    public static int saturatedInt(long value) {
        if (value <= 0L) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, value);
    }
}
