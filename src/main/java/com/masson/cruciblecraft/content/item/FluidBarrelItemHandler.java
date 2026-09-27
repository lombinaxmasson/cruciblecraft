package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelContents;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelFluids;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelProfile;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelTank;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Item-form barrel. Unlike the placed tank, this rejects fluids the barrel
 * is not allowed to keep. A sealed stack rejects both fill and drain.
 */
public final class FluidBarrelItemHandler implements IFluidHandlerItem {
    private final ItemStack stack;
    private final FluidBarrelProfile profile;

    public FluidBarrelItemHandler(ItemStack stack, FluidBarrelProfile profile) {
        this.stack = stack;
        this.profile = profile;
    }

    @Override
    public ItemStack getContainer() {
        return stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 ? copy().sample() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0
                ? FluidBarrelTank.saturatedInt(profile.capacity())
                : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack resource) {
        FluidBarrelContents contents = contents();
        return tank == 0
                && !contents.sealed()
                && FluidBarrelFluids.acceptedByItem(profile, resource)
                && copy().accepts(resource);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        FluidBarrelContents contents = contents();
        if (contents.sealed()
                || !FluidBarrelFluids.acceptedByItem(profile, resource)) {
            return 0;
        }
        if (action.execute() && stack.getCount() != 1) {
            return 0;
        }
        FluidBarrelTank tank = copy();
        int filled = tank.fill(resource, action);
        if (filled > 0 && action.execute()) {
            write(contents, tank);
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource == null || resource.isEmpty() || contents().sealed()) {
            return FluidStack.EMPTY;
        }
        FluidBarrelTank tank = copy();
        if (!tank.hasType() || resource.getFluid() != tank.fluid()) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (contents().sealed()) {
            return FluidStack.EMPTY;
        }
        if (action.execute() && stack.getCount() != 1) {
            return FluidStack.EMPTY;
        }
        FluidBarrelTank tank = copy();
        FluidStack drained = tank.drain(maxDrain, action);
        if (!drained.isEmpty() && action.execute()) {
            write(contents(), tank);
        }
        return drained;
    }

    private FluidBarrelContents contents() {
        return stack.getOrDefault(
                ModComponents.FLUID_BARREL.get(), FluidBarrelContents.EMPTY);
    }

    private FluidBarrelTank copy() {
        FluidBarrelContents contents = contents();
        FluidBarrelTank tank = new FluidBarrelTank(
                profile.capacity(),
                profile.keepsFilter(),
                () -> false,
                () -> {});
        tank.readFluid(contents.fluidId(), contents.amount());
        return tank;
    }

    private void write(FluidBarrelContents contents, FluidBarrelTank tank) {
        FluidBarrelContents next = contents.withFluid(
                tank.storedFluidId(), tank.amount());
        if (next.isDefault()) {
            stack.remove(ModComponents.FLUID_BARREL.get());
        } else {
            stack.set(ModComponents.FLUID_BARREL.get(), next);
        }
    }
}
