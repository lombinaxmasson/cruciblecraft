package com.masson.cruciblecraft.content.blockentity;

import java.util.Objects;
import java.util.function.Predicate;

import com.masson.cruciblecraft.api.fluid.LongFluidHandler;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.core.HolderLookup;

/**
 * A {@link FluidTank} facade with GT6's long fluid amount/capacity semantics.
 *
 * <p>NeoForge's capability surface still transfers an {@code int} per call,
 * so each individual fill/drain is bounded by an int while the stored amount
 * and capacity remain long. This matters for the Adamantium large boiler,
 * whose GT6 steam capacity is 2,621,440,000 mB.</p>
 */
public final class LargeBoilerFluidTank extends FluidTank {
    private final long longCapacity;
    private final Predicate<FluidStack> validator;
    private final Runnable changed;
    private FluidStack fluid = FluidStack.EMPTY;
    private long amount;

    public LargeBoilerFluidTank(
            long capacity,
            Predicate<FluidStack> validator) {
        this(capacity, validator, () -> {});
    }

    public LargeBoilerFluidTank(
            long capacity,
            Predicate<FluidStack> validator,
            Runnable changed) {
        super(Integer.MAX_VALUE);
        if (capacity <= 0L) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.longCapacity = capacity;
        this.validator = Objects.requireNonNull(validator, "validator");
        this.changed = Objects.requireNonNull(changed, "changed");
    }

    public long longCapacity() {
        return longCapacity;
    }

    public long longAmount() {
        return amount;
    }

    public long longSpace() {
        return Math.max(0L, longCapacity - amount);
    }

    @Override
    public int getCapacity() {
        return (int) Math.min(Integer.MAX_VALUE, longCapacity);
    }

    @Override
    public int getFluidAmount() {
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    @Override
    public FluidStack getFluid() {
        if (fluid.isEmpty() || amount <= 0L) {
            return FluidStack.EMPTY;
        }
        return fluid.copyWithAmount(
                (int) Math.min(Integer.MAX_VALUE, amount));
    }

    @Override
    public boolean isFluidValid(FluidStack stack) {
        return stack != null && !stack.isEmpty() && validator.test(stack);
    }

    public long fillLong(
            FluidStack resource,
            long maxFill,
            IFluidHandler.FluidAction action) {
        if (!isFluidValid(resource)
                || maxFill <= 0L
                || (!fluid.isEmpty()
                        && !FluidStack.isSameFluidSameComponents(
                                fluid, resource))) {
            return 0L;
        }
        long accepted = Math.min(longSpace(), maxFill);
        if (accepted <= 0L) {
            return 0L;
        }
        if (action.execute()) {
            if (fluid.isEmpty()) {
                fluid = resource.copyWithAmount(1);
            }
            amount += accepted;
            onContentsChanged();
            changed.run();
        }
        return accepted;
    }

    public LongFluidHandler.LongFluidStack drainLong(
            FluidStack resource,
            long maxDrain,
            IFluidHandler.FluidAction action) {
        if (resource == null
                || resource.isEmpty()
                || fluid.isEmpty()
                || !FluidStack.isSameFluidSameComponents(fluid, resource)) {
            return LongFluidHandler.LongFluidStack.empty();
        }
        return drainLong(maxDrain, action);
    }

    public LongFluidHandler.LongFluidStack drainLong(
            long maxDrain,
            IFluidHandler.FluidAction action) {
        if (maxDrain <= 0L || fluid.isEmpty()) {
            return LongFluidHandler.LongFluidStack.empty();
        }
        long drained = Math.min(amount, maxDrain);
        if (drained <= 0L) {
            return LongFluidHandler.LongFluidStack.empty();
        }
        FluidStack result = fluid.copyWithAmount(
                (int) Math.min(Integer.MAX_VALUE, drained));
        if (action.execute()) {
            amount -= drained;
            if (amount <= 0L) {
                amount = 0L;
                fluid = FluidStack.EMPTY;
            }
            onContentsChanged();
            changed.run();
        }
        return new LongFluidHandler.LongFluidStack(result, drained);
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        return (int) Math.min(
                Integer.MAX_VALUE,
                fillLong(resource, resource.getAmount(), action));
    }

    @Override
    public FluidStack drain(
            FluidStack resource,
            IFluidHandler.FluidAction action) {
        LongFluidHandler.LongFluidStack drained = drainLong(
                resource,
                resource.getAmount(),
                action);
        return drained.isEmpty() ? FluidStack.EMPTY : drained.fluid();
    }

    @Override
    public FluidStack drain(
            int maxDrain,
            IFluidHandler.FluidAction action) {
        LongFluidHandler.LongFluidStack drained = drainLong(maxDrain, action);
        return drained.isEmpty() ? FluidStack.EMPTY : drained.fluid();
    }

    @Override
    public void setFluid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            fluid = FluidStack.EMPTY;
            amount = 0L;
        } else {
            if (!isFluidValid(stack)) {
                throw new IllegalArgumentException(
                        "Invalid large boiler fluid " + stack);
            }
            fluid = stack.copyWithAmount(1);
            amount = Math.min(longCapacity, stack.getAmount());
        }
        onContentsChanged();
        changed.run();
    }

    public void setLongFluid(FluidStack stack, long requestedAmount) {
        if (stack == null || stack.isEmpty() || requestedAmount <= 0L) {
            setFluid(FluidStack.EMPTY);
            return;
        }
        if (!isFluidValid(stack)) {
            throw new IllegalArgumentException(
                    "Invalid large boiler fluid " + stack);
        }
        fluid = stack.copyWithAmount(1);
        amount = Math.min(longCapacity, requestedAmount);
        onContentsChanged();
        changed.run();
    }

    /**
     * GT6 conversion writes directly to the tank and lets the safety check
     * observe an overfull tank on the same tick. This intentionally does not
     * clamp to capacity.
     */
    public void addUnsafe(FluidStack stack, long addedAmount) {
        if (stack == null
                || stack.isEmpty()
                || addedAmount <= 0L
                || !isFluidValid(stack)
                || (!fluid.isEmpty()
                        && !FluidStack.isSameFluidSameComponents(fluid, stack))) {
            return;
        }
        if (fluid.isEmpty()) {
            fluid = stack.copyWithAmount(1);
        }
        amount = amount > Long.MAX_VALUE - addedAmount
                ? Long.MAX_VALUE
                : amount + addedAmount;
        onContentsChanged();
        changed.run();
    }

    public CompoundTag writeLongNbt(
            HolderLookup.Provider registries,
            String fluidKey,
            String amountKey) {
        CompoundTag tag = new CompoundTag();
        if (!fluid.isEmpty() && amount > 0L) {
            FluidTank encoded = new FluidTank(Integer.MAX_VALUE);
            encoded.setFluid(fluid.copyWithAmount(
                    (int) Math.min(Integer.MAX_VALUE, amount)));
            CompoundTag encodedTag = encoded.writeToNBT(
                    registries, new CompoundTag());
            tag.put(
                    fluidKey,
                    encodedTag.getCompound("Fluid"));
            tag.putLong(amountKey, amount);
        }
        return tag;
    }

    public void readLongNbt(
            HolderLookup.Provider registries,
            CompoundTag tag,
            String fluidKey,
            String amountKey) {
        String storedFluidKey = tag.contains(fluidKey)
                ? fluidKey
                : tag.contains("Fluid") ? "Fluid" : null;
        if (storedFluidKey == null) {
            setFluid(FluidStack.EMPTY);
            return;
        }
        FluidStack parsed = FluidStack.parseOptional(
                registries, tag.getCompound(storedFluidKey));
        long stored = tag.contains(amountKey)
                ? tag.getLong(amountKey)
                : parsed.getAmount();
        setLongFluid(parsed, stored);
    }

}
