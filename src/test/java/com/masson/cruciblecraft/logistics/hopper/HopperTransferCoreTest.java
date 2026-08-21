package com.masson.cruciblecraft.logistics.hopper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

class HopperTransferCoreTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void modeZeroMovesUpToSixtyFour() {
        ItemStackHandler source = handler(iron(64));
        ItemStackHandler dest = new ItemStackHandler(1);
        assertEquals(
                64,
                HopperTransferCore.push(source, dest, 0, false, false));
        assertEquals(0, HopperTransferCore.count(source));
        assertEquals(64, dest.getStackInSlot(0).getCount());
    }

    @Test
    void modeZeroExactMovesOneStackThenStops() {
        ItemStackHandler source = new ItemStackHandler(2);
        source.setStackInSlot(0, iron(32));
        source.setStackInSlot(1, iron(32));
        ItemStackHandler dest = new ItemStackHandler(2);
        assertEquals(
                32,
                HopperTransferCore.push(source, dest, 0, true, false));
        assertEquals(32, HopperTransferCore.count(source));
        assertEquals(32, HopperTransferCore.count(dest));
    }

    @Test
    void modeOneMovesSingleItemsUntilCap() {
        ItemStackHandler source = handler(iron(8));
        ItemStackHandler dest = new ItemStackHandler(1);
        assertEquals(8, HopperTransferCore.push(source, dest, 1, false, false));
        assertEquals(8, dest.getStackInSlot(0).getCount());
    }

    @Test
    void modeNExactMovesOnlyACompleteBatch() {
        ItemStackHandler source = handler(iron(10));
        ItemStackHandler dest = new ItemStackHandler(1);
        assertEquals(8, HopperTransferCore.push(source, dest, 8, true, false));
        assertEquals(2, source.getStackInSlot(0).getCount());
        assertEquals(8, dest.getStackInSlot(0).getCount());
    }

    @Test
    void modeNDivisibleRejectsPartialDestination() {
        ItemStackHandler source = handler(iron(16));
        ItemStackHandler dest = handler(iron(62));
        assertEquals(0, HopperTransferCore.push(source, dest, 8, false, false));
        assertEquals(16, source.getStackInSlot(0).getCount());
        assertEquals(62, dest.getStackInSlot(0).getCount());
    }

    @Test
    void modeSixtyFourMovesOneFullStack() {
        ItemStackHandler source = handler(iron(64));
        ItemStackHandler dest = new ItemStackHandler(1);
        assertEquals(
                64,
                HopperTransferCore.push(source, dest, 64, false, false));
        assertEquals(0, HopperTransferCore.count(source));
    }

    @Test
    void partialDestinationTakesWhatFitsWhenModeZero() {
        ItemStackHandler source = handler(iron(20));
        ItemStackHandler dest = handler(iron(60));
        assertEquals(4, HopperTransferCore.push(source, dest, 0, false, false));
        assertEquals(16, source.getStackInSlot(0).getCount());
        assertEquals(64, dest.getStackInSlot(0).getCount());
    }

    @Test
    void differentItemsDoNotMerge() {
        ItemStackHandler source = handler(iron(8));
        ItemStackHandler dest = handler(new ItemStack(Items.GOLD_INGOT, 8));
        assertEquals(0, HopperTransferCore.push(source, dest, 0, false, false));
        assertEquals(8, source.getStackInSlot(0).getCount());
    }

    @Test
    void differentComponentsDoNotMerge() {
        ItemStack named = iron(8);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("tagged"));
        ItemStackHandler source = handler(named);
        ItemStackHandler dest = handler(iron(8));
        assertEquals(0, HopperTransferCore.push(source, dest, 0, false, false));
        assertEquals(8, source.getStackInSlot(0).getCount());
        assertEquals(8, dest.getStackInSlot(0).getCount());
    }

    @Test
    void simulateDoesNotMutateAndMatchesExecuteWhenHonest() {
        ItemStackHandler source = handler(iron(16));
        ItemStackHandler dest = new ItemStackHandler(1);
        int planned = HopperTransferCore.push(source, dest, 8, true, true);
        assertEquals(16, source.getStackInSlot(0).getCount());
        assertTrue(dest.getStackInSlot(0).isEmpty());
        assertEquals(
                planned,
                HopperTransferCore.push(source, dest, 8, true, false));
        assertEquals(8, dest.getStackInSlot(0).getCount());
    }

    @Test
    void simulateExecuteMismatchFailsClosed() {
        ItemStackHandler source = handler(iron(16));
        IItemHandler dest = new HostileHandler();
        int before = HopperTransferCore.count(source);
        assertEquals(0, HopperTransferCore.push(source, dest, 8, true, false));
        assertEquals(before, HopperTransferCore.count(source));
    }

    @Test
    void queueMovesLastSlotFirstInFirstOut() {
        ItemStackHandler queue = HopperTransferCore.limitedInventory(3, 8);
        queue.setStackInSlot(0, iron(4));
        queue.setStackInSlot(2, new ItemStack(Items.GOLD_INGOT, 3));
        ItemStackHandler dest = new ItemStackHandler(1);
        assertEquals(3, HopperTransferCore.pushQueue(queue, dest, 8, false));
        assertEquals(Items.GOLD_INGOT, dest.getStackInSlot(0).getItem());
        assertEquals(4, queue.getStackInSlot(0).getCount());
        assertTrue(queue.getStackInSlot(2).isEmpty());
    }

    @Test
    void queueSlotLimitRejectsOverfill() {
        ItemStackHandler queue = HopperTransferCore.limitedInventory(2, 4);
        ItemStackHandler above = handler(iron(16));
        assertEquals(4, HopperTransferCore.pullQueue(above, queue, false));
        assertEquals(4, queue.getStackInSlot(0).getCount());
        assertEquals(0, HopperTransferCore.pullQueue(above, queue, false));
    }

    @Test
    void queueAdvanceIsFifo() {
        ItemStackHandler queue = HopperTransferCore.limitedInventory(3, 16);
        queue.setStackInSlot(0, iron(5));
        assertEquals(10, HopperTransferCore.advanceQueue(queue, false));
        assertTrue(queue.getStackInSlot(0).isEmpty());
        assertEquals(5, queue.getStackInSlot(2).getCount());
    }

    @Test
    void compactMergesMatchingStacksForward() {
        ItemStackHandler hopper = new ItemStackHandler(3);
        hopper.setStackInSlot(0, iron(10));
        hopper.setStackInSlot(2, iron(7));
        assertNotEquals(0, HopperTransferCore.compact(hopper, false));
        assertEquals(17, hopper.getStackInSlot(0).getCount());
        assertTrue(hopper.getStackInSlot(2).isEmpty());
    }

    @Test
    void simulateCompactDoesNotMutate() {
        ItemStackHandler hopper = new ItemStackHandler(2);
        hopper.setStackInSlot(1, iron(3));
        assertEquals(3, HopperTransferCore.compact(hopper, true));
        assertTrue(hopper.getStackInSlot(0).isEmpty());
        assertEquals(3, hopper.getStackInSlot(1).getCount());
    }

    private static ItemStackHandler handler(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, stack);
        return handler;
    }

    private static ItemStack iron(int count) {
        return new ItemStack(Items.IRON_INGOT, count);
    }

    private static final class HostileHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return simulate ? ItemStack.EMPTY : stack.copy();
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }
}
