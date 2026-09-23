package com.masson.cruciblecraft.content.multiblock;

import java.util.Objects;
import java.util.function.Predicate;

import com.masson.cruciblecraft.api.fluid.LongFluidHandler;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Long-capacity fluid storage for a physical port such as a large boiler. */
public final class PortStoreLongTank implements LongFluidHandler {
    private final long capacity;
    private final Predicate<FluidStack> validator;
    private final Runnable mutation;
    private FluidStack fluid = FluidStack.EMPTY;
    private long amount;

    public PortStoreLongTank(
            long capacity,
            Predicate<FluidStack> validator,
            Runnable mutation) {
        if (capacity <= 0L) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.validator = Objects.requireNonNull(validator, "validator");
        this.mutation = Objects.requireNonNull(mutation, "mutation");
    }

    @Override
    public int tanks() {
        return 1;
    }

    @Override
    public FluidStack fluid(int tank) {
        return tank == 0 ? fluid.copyWithAmount(1) : FluidStack.EMPTY;
    }

    @Override
    public long amount(int tank) {
        return tank == 0 ? amount : 0L;
    }

    @Override
    public long capacity(int tank) {
        return tank == 0 ? capacity : 0L;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && validator.test(stack);
    }

    @Override
    public long fill(
            int tank,
            FluidStack resource,
            long maxFill,
            IFluidHandler.FluidAction action) {
        if (tank != 0
                || resource == null
                || resource.isEmpty()
                || !validator.test(resource)
                || maxFill <= 0L) {
            return 0L;
        }
        if (!fluid.isEmpty()
                && !FluidStack.isSameFluidSameComponents(fluid, resource)) {
            return 0L;
        }
        long accepted = Math.min(maxFill, capacity - amount);
        if (accepted <= 0L) {
            return 0L;
        }
        if (action.execute()) {
            if (fluid.isEmpty()) {
                fluid = resource.copyWithAmount(1);
            }
            amount += accepted;
            mutation.run();
        }
        return accepted;
    }

    @Override
    public LongFluidStack drain(
            int tank,
            long maxDrain,
            IFluidHandler.FluidAction action) {
        if (tank != 0 || maxDrain <= 0L || fluid.isEmpty()) {
            return LongFluidStack.empty();
        }
        long drained = Math.min(maxDrain, amount);
        FluidStack result = fluid.copyWithAmount(
                (int) Math.min(Integer.MAX_VALUE, drained));
        if (action.execute()) {
            amount -= drained;
            if (amount == 0L) {
                fluid = FluidStack.EMPTY;
            }
            mutation.run();
        }
        return new LongFluidStack(result, drained);
    }

    public CompoundTag writeNbt(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!fluid.isEmpty() && amount > 0L) {
            FluidStack encoded = fluid.copyWithAmount(
                    (int) Math.min(Integer.MAX_VALUE, amount));
            tag.put("fluid", encoded.saveOptional(registries));
            tag.putLong("amount", amount);
        }
        return tag;
    }

    public void readNbt(
            HolderLookup.Provider registries,
            CompoundTag tag) {
        if (!tag.contains("fluid")) {
            return;
        }
        FluidStack parsed = FluidStack.parseOptional(
                registries, tag.getCompound("fluid"));
        long requested = tag.contains("amount")
                ? tag.getLong("amount")
                : parsed.getAmount();
        if (parsed.isEmpty() || !validator.test(parsed)) {
            return;
        }
        fluid = parsed.copyWithAmount(1);
        amount = Math.min(capacity, Math.max(0L, requested));
    }

}
