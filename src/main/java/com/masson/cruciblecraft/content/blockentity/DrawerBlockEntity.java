package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.DrawerBlock;
import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.menu.StorageMenu;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class DrawerBlockEntity extends BlockEntity implements MenuProvider {
    public static final int COMPARTMENTS = 4;
    public static final int COMPARTMENT_SLOTS = 36;
    private final StorageVariant variant;
    private final ItemStackHandler inventory;
    private int openCompartment;

    public DrawerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.DRAWER.get(), pos, state, variantOf(state));
    }

    public DrawerBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            StorageVariant variant) {
        super(type, pos, state);
        this.variant = variant;
        this.inventory = new ItemStackHandler(variant.slots()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    public StorageVariant variant() {
        return variant;
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public int openCompartment() {
        return openCompartment;
    }

    public void setOpenCompartment(int compartment) {
        if (compartment < 0 || compartment >= COMPARTMENTS) {
            throw new IllegalArgumentException("drawer compartment " + compartment);
        }
        this.openCompartment = compartment;
    }

    public static int compartmentFor(BlockState state, Direction clicked) {
        Direction facing = state.getValue(StorageHostBlock.FACING);
        if (clicked == facing) {
            return 0;
        }
        if (clicked == facing.getOpposite()) {
            return 1;
        }
        if (clicked == facing.getClockWise()) {
            return 2;
        }
        if (clicked == facing.getCounterClockWise()) {
            return 3;
        }
        return clicked == Direction.UP ? 0 : 1;
    }

    public IItemHandler itemHandler(Direction side) {
        return inventory;
    }

    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) <= 64.0;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(),
                    inventory.getStackInSlot(slot));
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    public CompoundTag saveForTest(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    public void loadForTest(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.cruciblecraft.drawer");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int id, Inventory playerInventory, Player player) {
        return new StorageMenu(
                ModMenus.DRAWER.get(),
                id,
                playerInventory,
                inventory,
                COMPARTMENT_SLOTS,
                openCompartment * COMPARTMENT_SLOTS,
                worldPosition,
                this::stillValid);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("compartment", openCompartment);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        openCompartment = Math.floorMod(tag.getInt("compartment"), COMPARTMENTS);
    }

    private static StorageVariant variantOf(BlockState state) {
        if (state.getBlock() instanceof DrawerBlock block) {
            return block.variant();
        }
        throw new IllegalStateException("Drawer BE missing host variant");
    }
}
