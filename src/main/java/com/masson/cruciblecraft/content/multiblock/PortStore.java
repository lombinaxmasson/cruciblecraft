package com.masson.cruciblecraft.content.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.masson.cruciblecraft.api.fluid.LongFluidHandler;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerFluidTank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.world.item.ItemStack;

/**
 * Persistent storage owned by one physical multiblock port.
 *
 * <p>The controller remains the transaction owner, but no physical port
 * aliases the controller inventory anymore. Assignments map local port slots
 * back to the controller's source layout for aggregate recipe transactions.</p>
 */
public final class PortStore {
    private static final String TAG_ASSIGNMENT = "assignment";
    private static final String TAG_ITEMS = "items";
    private static final String TAG_TANKS = "tanks";

    private final Runnable mutation;
    private Assignment assignment = Assignment.EMPTY;
    private MultiblockPortHost host;
    private ItemStackHandler items = new ItemStackHandler(0);
    private List<FluidTank> tanks = List.of();
    private List<PortStoreLongTank> longTanks = List.of();
    private CompoundTag pendingLoad;
    private HolderLookup.Provider pendingRegistries;
    private boolean aliasHandoffDone;

    public PortStore(Runnable mutation) {
        this.mutation = Objects.requireNonNull(mutation, "mutation");
    }

    public Assignment assignment() {
        return assignment;
    }

    public ItemStackHandler items() {
        return items;
    }

    public List<FluidTank> tanks() {
        return tanks;
    }

    public List<PortStoreLongTank> longTanks() {
        return longTanks;
    }

    public LongFluidHandler longFluids(
            MultiblockStructureDefinition.PortType type) {
        return new LongView(this, type);
    }

    /** Inserts into a store-owned output slot during controller commit. */
    public int acceptInternalItem(int local, ItemStack offered) {
        if (local < 0 || local >= items.getSlots() || offered.isEmpty()) {
            return 0;
        }
        ItemStack current = items.getStackInSlot(local);
        if (!current.isEmpty()
                && !ItemStack.isSameItemSameComponents(current, offered)) {
            return 0;
        }
        int limit = current.isEmpty()
                ? offered.getMaxStackSize()
                : current.getMaxStackSize() - current.getCount();
        int moved = Math.min(limit, offered.getCount());
        if (moved <= 0) {
            return 0;
        }
        items.setStackInSlot(
                local,
                current.isEmpty()
                        ? offered.copyWithCount(moved)
                        : current.copyWithCount(current.getCount() + moved));
        return moved;
    }

    public boolean configured() {
        return host != null;
    }

    public boolean aliasHandoffDone() {
        return aliasHandoffDone;
    }

    public void finishAliasHandoff() {
        aliasHandoffDone = true;
    }

    public boolean hasLiveContents() {
        for (int slot = 0; slot < items.getSlots(); slot++) {
            if (!items.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        for (FluidTank tank : tanks) {
            if (!tank.getFluid().isEmpty()) {
                return true;
            }
        }
        for (PortStoreLongTank tank : longTanks) {
            if (tank.amount(0) > 0L) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPendingLoad() {
        return pendingLoad != null;
    }

    /** Drops the independent-store role after an alias handoff. */
    public void detach() {
        host = null;
        assignment = Assignment.EMPTY;
        items = new ItemStackHandler(0);
        tanks = List.of();
        longTanks = List.of();
        pendingLoad = null;
        pendingRegistries = null;
    }

    /**
     * Rebinds this physical store to the current source-backed port layout.
     * Existing local contents are retained when their local shape survives.
     */
    public void configure(
            MultiblockPortHost host,
            Assignment next) {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(next, "next");
        if (this.host == host && assignment.equals(next)) {
            return;
        }
        ItemStackHandler oldItems = items;
        List<FluidTank> oldTanks = tanks;
        Assignment oldAssignment = assignment;
        this.host = host;
        this.assignment = next;
        this.items = new ItemStackHandler(next.itemSlotCount()) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (!next.itemInputLocals().contains(slot)) {
                    return false;
                }
                int global = next.itemGlobalSlot(slot);
                return global >= 0
                        && global < host.inventory().getSlots()
                        && host.inventory().isItemValid(global, stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                mutation.run();
            }
        };
        List<FluidTank> created = new ArrayList<>();
        List<PortStoreLongTank> createdLong = new ArrayList<>();
        for (int local = 0; local < next.fluidTankCount(); local++) {
            int global = next.fluidGlobalTank(local);
            boolean input = next.fluidInputLocals().contains(local);
            long capacity = global >= 0 && global < host.tanks().size()
                    ? host.tanks().get(global) instanceof LargeBoilerFluidTank boiler
                            ? boiler.longCapacity()
                            : host.tanks().get(global).getCapacity()
                    : 1;
            created.add(new FluidTank(
                    Math.toIntExact(Math.min(
                            Integer.MAX_VALUE,
                            Math.max(1L, capacity))),
                    stack -> !input
                            || (global >= 0
                            && global < host.tanks().size()
                            && host.tanks().get(global).isFluidValid(stack))) {
                @Override
                protected void onContentsChanged() {
                    mutation.run();
                }
            });
            createdLong.add(new PortStoreLongTank(
                    Math.max(1L, capacity),
                    stack -> !input
                            || (global >= 0
                            && global < host.tanks().size()
                            && host.tanks().get(global).isFluidValid(stack)),
                    mutation));
        }
        this.tanks = List.copyOf(created);
        this.longTanks = List.copyOf(createdLong);
        copyCompatibleContents(oldItems, oldAssignment, oldTanks);
        if (pendingRegistries != null) {
            loadPending(pendingRegistries);
        }
        mutation.run();
    }

    private void copyCompatibleContents(
            ItemStackHandler oldItems,
            Assignment oldAssignment,
            List<FluidTank> oldTanks) {
        for (int local = 0; local < oldAssignment.itemSlotCount()
                && local < assignment.itemSlotCount(); local++) {
            ItemStack stack = oldItems.getStackInSlot(local);
            if (!stack.isEmpty() && items.getStackInSlot(local).isEmpty()) {
                items.setStackInSlot(local, stack.copy());
            }
        }
        for (int local = 0; local < oldAssignment.fluidTankCount()
                && local < assignment.fluidTankCount(); local++) {
            FluidStack stack = oldTanks.get(local).getFluid();
            if (!stack.isEmpty() && tanks.get(local).getFluid().isEmpty()) {
                tanks.get(local).setFluid(stack.copy());
            }
        }
    }

    public void save(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        CompoundTag store = new CompoundTag();
        store.put(TAG_ASSIGNMENT, assignment.save());
        store.put(TAG_ITEMS, items.serializeNBT(registries));
        ListTag tankList = new ListTag();
        for (FluidTank tank : tanks) {
            tankList.add(tank.writeToNBT(registries, new CompoundTag()));
        }
        store.put(TAG_TANKS, tankList);
        ListTag longTankList = new ListTag();
        for (PortStoreLongTank tank : longTanks) {
            longTankList.add(tank.writeNbt(registries));
        }
        store.put("long_tanks", longTankList);
        tag.put("port_store", store);
    }

    public void load(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        if (!tag.contains("port_store")) {
            return;
        }
        aliasHandoffDone = false;
        pendingLoad = tag.getCompound("port_store").copy();
        pendingRegistries = registries;
        if (configured()) {
            loadPending(registries);
        }
    }

    private void loadPending(HolderLookup.Provider registries) {
        if (pendingLoad == null) {
            return;
        }
        CompoundTag stored = pendingLoad;
        if (stored.contains(TAG_ITEMS)) {
            items.deserializeNBT(
                    registries,
                    stored.getCompound(TAG_ITEMS));
        }
        if (stored.contains(TAG_TANKS)) {
            ListTag tankList = stored.getList(TAG_TANKS, Tag.TAG_COMPOUND);
            for (int index = 0; index < Math.min(tanks.size(), tankList.size()); index++) {
                tanks.get(index).readFromNBT(
                        registries,
                        tankList.getCompound(index));
            }
        }
        if (stored.contains("long_tanks")) {
            ListTag longTankList = stored.getList("long_tanks", Tag.TAG_COMPOUND);
            for (int index = 0;
                    index < Math.min(longTanks.size(), longTankList.size());
                    index++) {
                longTanks.get(index).readNbt(
                        registries, longTankList.getCompound(index));
            }
        }
        pendingLoad = null;
        pendingRegistries = null;
    }

    private static final class LongView implements LongFluidHandler {
        private final PortStore store;
        private final MultiblockStructureDefinition.PortType type;

        private LongView(
                PortStore store,
                MultiblockStructureDefinition.PortType type) {
            this.store = store;
            this.type = type;
        }

        private PortStoreLongTank tank(int index) {
            boolean input = index == 0
                    && PortCapabilityGate.fluidFill(type)
                    && !store.assignment().fluidInputLocals().isEmpty();
            boolean output = index == 1
                    && PortCapabilityGate.fluidDrain(type)
                    && !store.assignment().fluidOutputLocals().isEmpty();
            if (!input && !output) {
                return null;
            }
            int local = input
                    ? store.assignment().fluidInputLocals().getFirst()
                    : store.assignment().fluidOutputLocals().getFirst();
            return store.longTanks().get(local);
        }

        @Override public int tanks() { return 2; }
        @Override public FluidStack fluid(int tank) {
            PortStoreLongTank view = tank(tank);
            return view == null ? FluidStack.EMPTY : view.fluid(0);
        }
        @Override public long amount(int tank) {
            PortStoreLongTank view = tank(tank);
            return view == null ? 0L : view.amount(0);
        }
        @Override public long capacity(int tank) {
            PortStoreLongTank view = tank(tank);
            return view == null ? 0L : view.capacity(0);
        }
        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            PortStoreLongTank view = tank(tank);
            return view != null && view.isFluidValid(0, stack);
        }
        @Override public long fill(
                int tank, FluidStack resource, long maxFill,
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
            PortStoreLongTank view = tank(tank);
            return view == null ? 0L : view.fill(0, resource, maxFill, action);
        }
        @Override public LongFluidStack drain(
                int tank, long maxDrain,
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction action) {
            PortStoreLongTank view = tank(tank);
            return view == null
                    ? LongFluidStack.empty()
                    : view.drain(0, maxDrain, action);
        }
    }

    public record Assignment(
            List<Integer> itemInputGlobals,
            List<Integer> itemOutputGlobals,
            List<Integer> fluidInputGlobals,
            List<Integer> fluidOutputGlobals) {
        public static final Assignment EMPTY =
                new Assignment(List.of(), List.of(), List.of(), List.of());

        public Assignment {
            itemInputGlobals = List.copyOf(itemInputGlobals);
            itemOutputGlobals = List.copyOf(itemOutputGlobals);
            fluidInputGlobals = List.copyOf(fluidInputGlobals);
            fluidOutputGlobals = List.copyOf(fluidOutputGlobals);
        }

        public int itemSlotCount() {
            return itemInputGlobals.size() + itemOutputGlobals.size();
        }

        public int fluidTankCount() {
            return fluidInputGlobals.size() + fluidOutputGlobals.size();
        }

        public List<Integer> itemInputLocals() {
            return java.util.stream.IntStream.range(
                            0, itemInputGlobals.size())
                    .boxed()
                    .toList();
        }

        public List<Integer> itemOutputLocals() {
            int start = itemInputGlobals.size();
            return java.util.stream.IntStream.range(
                            start, start + itemOutputGlobals.size())
                    .boxed()
                    .toList();
        }

        public List<Integer> fluidInputLocals() {
            return java.util.stream.IntStream.range(
                            0, fluidInputGlobals.size())
                    .boxed()
                    .toList();
        }

        public List<Integer> fluidOutputLocals() {
            int start = fluidInputGlobals.size();
            return java.util.stream.IntStream.range(
                            start, start + fluidOutputGlobals.size())
                    .boxed()
                    .toList();
        }

        public int itemGlobalSlot(int local) {
            if (local < itemInputGlobals.size()) {
                return itemInputGlobals.get(local);
            }
            return itemOutputGlobals.get(local - itemInputGlobals.size());
        }

        public int fluidGlobalTank(int local) {
            if (local < fluidInputGlobals.size()) {
                return fluidInputGlobals.get(local);
            }
            return fluidOutputGlobals.get(local - fluidInputGlobals.size());
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putIntArray("item_in", itemInputGlobals);
            tag.putIntArray("item_out", itemOutputGlobals);
            tag.putIntArray("fluid_in", fluidInputGlobals);
            tag.putIntArray("fluid_out", fluidOutputGlobals);
            return tag;
        }
    }
}
