package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.CrusherBlock;
import com.masson.cruciblecraft.content.menu.CrusherMenu;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineDisplayData;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Thin crusher host over the shared processing runtime. */
public final class CrusherBlockEntity extends ProcessingMachineBlockEntity
        implements MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final long KU_CAPACITY = 1_024L;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress();
                case 1 -> duration();
                case 2 -> powerDemand();
                case 3 -> ProcessingMachineDisplayData.statusIndex(
                        ModProcessingMachines.CRUSHER, pausedReason());
                case 6 -> statusArgument();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) {
                runtime().processor().setProgress(value);
            } else if (index == 1) {
                runtime().processor().setDuration(value);
            }
        }
        @Override public int getCount() { return 7; }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state, ModProcessingMachines.CRUSHER);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CrusherBlockEntity crusher) {
        crusher.tickProcessingServer();
    }

    public ContainerData data() {
        return data;
    }

    public int powerDemand() {
        return (int) Math.min(Integer.MAX_VALUE, powerDemandLong());
    }

    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                        worldPosition.getX() + .5,
                        worldPosition.getY() + .5,
                        worldPosition.getZ() + .5) <= 64;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < inventory().getSlots(); slot++) {
            ItemStack stack = inventory().getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        stack);
                inventory().setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.cruciblecraft.bronze_crusher");
    }

    @Override public AbstractContainerMenu createMenu(
            int id,
            Inventory inventory,
            Player player) {
        return new CrusherMenu(id, inventory, this);
    }

    @Override protected Direction machineFront() {
        BlockState state = getBlockState();
        return state.hasProperty(CrusherBlock.FACING)
                ? state.getValue(CrusherBlock.FACING)
                : null;
    }

}
