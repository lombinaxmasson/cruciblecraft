package com.masson.cruciblecraft.logistics.hopper;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT6 {@code ST.put} when the facing side is not an inventory or item pipe.
 *
 * <p>Rails and colliding blocks keep the items. Lava, fire, and air below the
 * world void them. Any other non-colliding block receives one ejected stack
 * per {@code ST.move} batch.
 */
public final class HopperEject {
    public enum Target {
        BLOCKED,
        TRASH,
        EJECT
    }

    private HopperEject() {}

    public static Target classify(
            BlockGetter level, BlockPos pos, int minBuildHeight) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BaseRailBlock) {
            return Target.BLOCKED;
        }
        Fluid fluid = state.getFluidState().getType();
        if (fluid == Fluids.LAVA
                || fluid == Fluids.FLOWING_LAVA
                || state.getBlock() instanceof BaseFireBlock
                || (state.isAir() && pos.getY() < minBuildHeight)) {
            return Target.TRASH;
        }
        if (state.canOcclude()
                || !state.getCollisionShape(level, pos).isEmpty()) {
            return Target.BLOCKED;
        }
        return Target.EJECT;
    }

    /** Normal hopper batches. The list receives one stack per successful move. */
    public static int drain(
            IItemHandler source,
            int mode,
            boolean exact,
            List<ItemStack> ejected) {
        return HopperTransferCore.push(
                source, new OpenAir(ejected), mode, exact, false);
    }

    /** Queue hopper: last slot only, up to {@code slotSize} items. */
    public static int drainQueue(
            IItemHandler source, int slotSize, List<ItemStack> ejected) {
        return HopperTransferCore.pushQueue(
                source, new OpenAir(ejected), slotSize, false);
    }

    private static final class OpenAir implements IItemHandler {
        private final List<ItemStack> ejected;

        private OpenAir(List<ItemStack> ejected) {
            this.ejected = ejected;
        }

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
            if (slot != 0 || stack.isEmpty()) {
                return stack;
            }
            if (!simulate) {
                ejected.add(stack.copy());
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return HopperTransferCore.TICK_ITEM_CAP;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && !stack.isEmpty();
        }
    }
}
