package com.masson.cruciblecraft.logistics.hopper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.items.ItemStackHandler;

class HopperEjectTest {
    private static final int MIN_Y = -64;

    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void openAirEjectsAndSplitsAtTheTickCap() {
        ItemStackHandler source = new ItemStackHandler(2);
        source.setStackInSlot(0, iron(40));
        source.setStackInSlot(1, iron(40));
        List<ItemStack> ejected = new ArrayList<>();
        assertEquals(64, HopperEject.drain(source, 0, false, ejected));
        assertEquals(List.of(40, 24), counts(ejected));
        assertEquals(16, source.getStackInSlot(1).getCount());
        assertTrue(source.getStackInSlot(0).isEmpty());
    }

    @Test
    void exactModeEjectsOneBatch() {
        ItemStackHandler source = handler(iron(10));
        List<ItemStack> ejected = new ArrayList<>();
        assertEquals(8, HopperEject.drain(source, 8, true, ejected));
        assertEquals(List.of(8), counts(ejected));
        assertEquals(2, source.getStackInSlot(0).getCount());
    }

    @Test
    void partialBatchStaysInTheHopper() {
        ItemStackHandler source = handler(iron(3));
        List<ItemStack> ejected = new ArrayList<>();
        assertEquals(0, HopperEject.drain(source, 8, false, ejected));
        assertTrue(ejected.isEmpty());
        assertEquals(3, source.getStackInSlot(0).getCount());
    }

    @Test
    void queueEjectsOnlyTheLastSlot() {
        ItemStackHandler queue = HopperTransferCore.limitedInventory(3, 64);
        queue.setStackInSlot(0, iron(9));
        queue.setStackInSlot(2, iron(5));
        List<ItemStack> ejected = new ArrayList<>();
        assertEquals(5, HopperEject.drainQueue(queue, 8, ejected));
        assertEquals(List.of(5), counts(ejected));
        assertEquals(9, queue.getStackInSlot(0).getCount());
        assertTrue(queue.getStackInSlot(2).isEmpty());
    }

    @Test
    void classifyMatchesGt6PutTargets() {
        assertEquals(HopperEject.Target.EJECT, classify(Blocks.AIR.defaultBlockState(), 64));
        assertEquals(HopperEject.Target.EJECT, classify(Blocks.WATER.defaultBlockState(), 64));
        assertEquals(
                HopperEject.Target.EJECT,
                classify(Blocks.AIR.defaultBlockState(), 0));
        assertEquals(
                HopperEject.Target.TRASH,
                classify(Blocks.AIR.defaultBlockState(), MIN_Y - 1));
        assertEquals(HopperEject.Target.TRASH, classify(Blocks.LAVA.defaultBlockState(), 64));
        assertEquals(HopperEject.Target.TRASH, classify(Blocks.FIRE.defaultBlockState(), 64));
        assertEquals(HopperEject.Target.BLOCKED, classify(Blocks.STONE.defaultBlockState(), 64));
        assertEquals(HopperEject.Target.BLOCKED, classify(Blocks.GLASS.defaultBlockState(), 64));
        assertEquals(HopperEject.Target.BLOCKED, classify(Blocks.RAIL.defaultBlockState(), 64));
        assertEquals(
                HopperEject.Target.TRASH,
                classify(Blocks.SOUL_FIRE.defaultBlockState(), 64));
    }

    private static HopperEject.Target classify(BlockState state, int y) {
        return HopperEject.classify(new Solo(state), new BlockPos(0, y, 0), MIN_Y);
    }

    private static List<Integer> counts(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::getCount).toList();
    }

    private static ItemStackHandler handler(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(1);
        handler.setStackInSlot(0, stack);
        return handler;
    }

    private static ItemStack iron(int count) {
        return new ItemStack(Items.IRON_INGOT, count);
    }

    private static final class Solo implements BlockGetter {
        private final BlockState state;

        private Solo(BlockState state) {
            this.state = state;
        }

        @Override
        public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return state;
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return state.getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return MIN_Y;
        }
    }
}
