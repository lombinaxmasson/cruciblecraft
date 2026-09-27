package com.masson.cruciblecraft.content.multiblock;

import com.masson.cruciblecraft.content.blockentity.LargeBoilerFluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Moves a GT6 alias wall's leftover port-store contents back onto the
 * controller, then detaches the store so hull I/O and auto-output stay live.
 */
public final class PortStoreAliasHandoff {
    private PortStoreAliasHandoff() {}

    public static void once(
            PortStoreCarrier carrier,
            MultiblockPortHost host,
            PortStore.Assignment assignment,
            Level level,
            BlockPos position) {
        PortStore store = carrier.portStore();
        if (store.aliasHandoffDone()) {
            return;
        }
        store.finishAliasHandoff();
        if (!store.hasPendingLoad() && !store.configured()) {
            return;
        }
        if (store.hasPendingLoad() || store.hasLiveContents()) {
            store.configure(host, assignment);
            spill(store, host, assignment, level, position);
        }
        store.detach();
    }

    private static void spill(
            PortStore store,
            MultiblockPortHost host,
            PortStore.Assignment assignment,
            Level level,
            BlockPos position) {
        for (int local = 0; local < store.items().getSlots(); local++) {
            ItemStack stack = store.items().getStackInSlot(local).copy();
            store.items().setStackInSlot(local, ItemStack.EMPTY);
            if (stack.isEmpty()) {
                continue;
            }
            int global = assignment.itemGlobalSlot(local);
            ItemStack remainder = placeItem(host, global, stack);
            if (!remainder.isEmpty() && level != null) {
                Containers.dropItemStack(
                        level,
                        position.getX(),
                        position.getY(),
                        position.getZ(),
                        remainder);
            }
        }
        for (int local = 0; local < store.tanks().size(); local++) {
            FluidStack stack = store.tanks().get(local).getFluid().copy();
            store.tanks().get(local).setFluid(FluidStack.EMPTY);
            if (!stack.isEmpty()) {
                placeFluid(host, assignment.fluidGlobalTank(local), stack);
            }
        }
        for (int local = 0; local < store.longTanks().size(); local++) {
            PortStoreLongTank source = store.longTanks().get(local);
            if (source.amount(0) <= 0L) {
                continue;
            }
            FluidStack fluid = source.fluid(0).copy();
            long amount = source.amount(0);
            source.drain(0, amount, IFluidHandler.FluidAction.EXECUTE);
            placeLongFluid(host, assignment.fluidGlobalTank(local), fluid, amount);
        }
    }

    private static ItemStack placeItem(
            MultiblockPortHost host, int global, ItemStack stack) {
        if (global < 0 || global >= host.inventory().getSlots()) {
            return stack;
        }
        ItemStack current = host.inventory().getStackInSlot(global);
        if (current.isEmpty()) {
            host.inventory().setStackInSlot(global, stack);
            return ItemStack.EMPTY;
        }
        if (!ItemStack.isSameItemSameComponents(current, stack)) {
            return stack;
        }
        int room = Math.min(
                host.inventory().getSlotLimit(global),
                current.getMaxStackSize()) - current.getCount();
        if (room <= 0) {
            return stack;
        }
        int moved = Math.min(room, stack.getCount());
        ItemStack merged = current.copyWithCount(current.getCount() + moved);
        host.inventory().setStackInSlot(global, merged);
        return moved == stack.getCount()
                ? ItemStack.EMPTY
                : stack.copyWithCount(stack.getCount() - moved);
    }

    private static void placeFluid(
            MultiblockPortHost host, int global, FluidStack stack) {
        if (global < 0 || global >= host.tanks().size() || stack.isEmpty()) {
            return;
        }
        FluidTank tank = host.tanks().get(global);
        int accepted = tank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        if (accepted >= stack.getAmount()) {
            return;
        }
        FluidStack leftover = stack.copyWithAmount(stack.getAmount() - accepted);
        if (tank.getFluid().isEmpty()) {
            tank.setFluid(leftover);
            return;
        }
        if (FluidStack.isSameFluidSameComponents(tank.getFluid(), leftover)) {
            int room = tank.getCapacity() - tank.getFluidAmount();
            if (room <= 0) {
                return;
            }
            int moved = Math.min(room, leftover.getAmount());
            FluidStack stored = tank.getFluid().copy();
            stored.grow(moved);
            tank.setFluid(stored);
        }
    }

    private static void placeLongFluid(
            MultiblockPortHost host,
            int global,
            FluidStack fluid,
            long amount) {
        if (global < 0 || global >= host.tanks().size() || amount <= 0L) {
            return;
        }
        if (host.tanks().get(global) instanceof LargeBoilerFluidTank boiler) {
            boiler.fillLong(fluid, amount, IFluidHandler.FluidAction.EXECUTE);
            return;
        }
        int moved = (int) Math.min(Integer.MAX_VALUE, amount);
        placeFluid(host, global, fluid.copyWithAmount(moved));
    }
}
