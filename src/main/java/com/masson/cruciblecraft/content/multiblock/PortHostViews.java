package com.masson.cruciblecraft.content.multiblock;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Per-physical-port host view over an independent {@link PortStore}. */
public final class PortHostViews {
    private PortHostViews() {}

    public static MultiblockPortHost forPort(
            MultiblockPortHost host,
            PortStoreCarrier carrier) {
        if (host == null
                || carrier == null
                || !carrier.portStore().configured()) {
            return host;
        }
        return new View(host, carrier.portStore());
    }

    private static final class View implements MultiblockPortHost {
        private final MultiblockPortHost host;
        private final PortStore store;

        private View(MultiblockPortHost host, PortStore store) {
            this.host = host;
            this.store = store;
        }

        @Override
        public ItemStackHandler inventory() {
            return store.items();
        }

        @Override
        public List<FluidTank> tanks() {
            return store.tanks();
        }

        @Override
        public List<Integer> itemInputSlots() {
            return store.assignment().itemInputLocals();
        }

        @Override
        public List<Integer> itemOutputSlots() {
            return store.assignment().itemOutputLocals();
        }

        @Override
        public List<Integer> fluidInputTanks() {
            return store.assignment().fluidInputLocals();
        }

        @Override
        public List<Integer> fluidOutputTanks() {
            return store.assignment().fluidOutputLocals();
        }

        @Override
        public BlockState blockState() {
            return host.blockState();
        }

        @Override
        public boolean handles(EnergyType type, Direction side) {
            return host.handles(type, side);
        }

        @Override
        public long stored(EnergyType type) {
            return host.stored(type);
        }

        @Override
        public long capacity(EnergyType type) {
            return host.capacity(type);
        }

        @Override
        public long insert(
                EnergyType type,
                long size,
                long amount,
                Direction side,
                boolean simulate) {
            return host.insert(type, size, amount, side, simulate);
        }

        @Override
        public long extract(
                EnergyType type,
                long size,
                long amount,
                Direction side,
                boolean simulate) {
            return host.extract(type, size, amount, side, simulate);
        }

        @Override
        public long insertFromMultiblockPort(
                EnergyType type,
                long size,
                long amount,
                boolean simulate) {
            return host.insertFromMultiblockPort(
                    type, size, amount, simulate);
        }
    }
}
