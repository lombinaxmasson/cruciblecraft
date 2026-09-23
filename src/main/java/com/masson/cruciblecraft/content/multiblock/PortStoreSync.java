package com.masson.cruciblecraft.content.multiblock;

import com.masson.cruciblecraft.api.fluid.LongFluidHandler;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerFluidTank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.world.item.ItemStack;

/**
 * Bridges independent physical port stores into the existing processing
 * transaction buffer. Inputs are gathered before recipe matching; completed
 * outputs are distributed back to their assigned physical ports.
 */
public final class PortStoreSync {
    private PortStoreSync() {}

    public static void pullInputs(MultiblockPortHost host) {
        for (PortStoreCarrier carrier : PortStoreRegistry.stores(host)) {
            PortStore store = carrier.portStore();
            PortStore.Assignment assignment = store.assignment();
            for (int local : assignment.itemInputLocals()) {
                int global = assignment.itemGlobalSlot(local);
                ItemStack offered = store.items().getStackInSlot(local);
                if (offered.isEmpty()) {
                    continue;
                }
                ItemStack remainder = host.inventory().insertItem(
                        global, offered, false);
                if (remainder.getCount() != offered.getCount()) {
                    store.items().setStackInSlot(local, remainder);
                }
            }
            for (int local : assignment.fluidInputLocals()) {
                int global = assignment.fluidGlobalTank(local);
                if (local >= store.tanks().size()
                        || global < 0
                        || global >= host.tanks().size()) {
                    continue;
                }
                if (local < store.longTanks().size()
                        && host.tanks().get(global)
                                instanceof LargeBoilerFluidTank boiler) {
                    PortStoreLongTank source = store.longTanks().get(local);
                    FluidStack longOffered = source.fluid(0);
                    if (!longOffered.isEmpty()) {
                        long accepted = boiler.fillLong(
                                longOffered,
                                source.amount(0),
                                IFluidHandler.FluidAction.EXECUTE);
                        if (accepted > 0L) {
                            source.drain(
                                    0,
                                    accepted,
                                    IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                    continue;
                }
                FluidStack offered = store.tanks().get(local).getFluid();
                if (offered.isEmpty()) {
                    continue;
                }
                int accepted = host.tanks().get(global).fill(
                        offered.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (accepted > 0) {
                    store.tanks().get(local).drain(
                            accepted, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    public static void pushOutputs(MultiblockPortHost host) {
        for (PortStoreCarrier carrier : PortStoreRegistry.stores(host)) {
            PortStore store = carrier.portStore();
            PortStore.Assignment assignment = store.assignment();
            for (int local : assignment.itemOutputLocals()) {
                int global = assignment.itemGlobalSlot(local);
                if (global < 0 || global >= host.inventory().getSlots()) {
                    continue;
                }
                ItemStack offered = host.inventory()
                        .getStackInSlot(global).copy();
                if (offered.isEmpty()) {
                    continue;
                }
                int moved = store.acceptInternalItem(local, offered);
                if (moved > 0) {
                    host.inventory().extractItem(global, moved, false);
                }
            }
            for (int local : assignment.fluidOutputLocals()) {
                int global = assignment.fluidGlobalTank(local);
                if (local >= store.tanks().size()
                        || global < 0
                        || global >= host.tanks().size()) {
                    continue;
                }
                if (local < store.longTanks().size()
                        && host.tanks().get(global)
                                instanceof LargeBoilerFluidTank boiler) {
                    PortStoreLongTank target = store.longTanks().get(local);
                    if (boiler.longAmount() > 0L) {
                        FluidStack offered = boiler.getFluid().copy();
                        LongFluidHandler.LongFluidStack simulated =
                                boiler.drainLong(
                                        offered,
                                        boiler.longAmount(),
                                        IFluidHandler.FluidAction.SIMULATE);
                        long accepted = target.fill(
                                0,
                                simulated.fluid(),
                                simulated.amount(),
                                IFluidHandler.FluidAction.EXECUTE);
                        if (accepted > 0L) {
                            boiler.drainLong(
                                    offered,
                                    accepted,
                                    IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                    continue;
                }
                FluidStack offered = host.tanks().get(global).getFluid();
                if (offered.isEmpty()) {
                    continue;
                }
                int accepted = store.tanks().get(local).fill(
                        offered.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (accepted > 0) {
                    host.tanks().get(global).drain(
                            accepted, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }
}
