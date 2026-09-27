package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PortStoreAliasHandoffTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        net.neoforged.fml.loading.LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void spillsStoredOutputOntoTheControllerAndDetaches() {
        FakeHost host = new FakeHost();
        PortStore store = new PortStore(() -> {});
        PortStore.Assignment assignment = new PortStore.Assignment(
                List.of(0), List.of(1), List.of(), List.of());
        store.configure(host, assignment);
        store.items().setStackInSlot(1, new ItemStack(Items.DIRT, 8));
        Carrier carrier = new Carrier(store);

        PortStoreAliasHandoff.once(
                carrier, host, assignment, null, BlockPos.ZERO);

        assertEquals(8, host.inventory().getStackInSlot(1).getCount());
        assertTrue(host.inventory().getStackInSlot(1).is(Items.DIRT));
        assertFalse(store.configured());
        assertTrue(store.aliasHandoffDone());

        host.inventory().setStackInSlot(1, ItemStack.EMPTY);
        store.configure(host, assignment);
        store.items().setStackInSlot(1, new ItemStack(Items.DIRT, 3));
        PortStoreAliasHandoff.once(
                carrier, host, assignment, null, BlockPos.ZERO);
        assertTrue(host.inventory().getStackInSlot(1).isEmpty());
    }

    private static final class Carrier implements PortStoreCarrier {
        private final PortStore store;

        private Carrier(PortStore store) {
            this.store = store;
        }

        @Override
        public boolean ownsIndependentPortStore() {
            return false;
        }

        @Override
        public PortStore portStore() {
            return store;
        }

        @Override
        public void configurePortStore(
                MultiblockPortHost host, PortStore.Assignment assignment) {
            store.configure(host, assignment);
        }
    }

    private static final class FakeHost implements MultiblockPortHost {
        private final ItemStackHandler items = new ItemStackHandler(2);
        private final List<FluidTank> tanks = List.of(new FluidTank(4_000));

        @Override public ItemStackHandler inventory() { return items; }
        @Override public List<FluidTank> tanks() { return tanks; }
        @Override public List<Integer> itemInputSlots() { return List.of(0); }
        @Override public List<Integer> itemOutputSlots() { return List.of(1); }
        @Override public List<Integer> fluidInputTanks() { return List.of(); }
        @Override public List<Integer> fluidOutputTanks() { return List.of(0); }
        @Override public BlockState blockState() {
            return Blocks.AIR.defaultBlockState();
        }
    }
}
