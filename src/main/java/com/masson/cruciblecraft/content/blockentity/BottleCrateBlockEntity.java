package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.BottleCrateBlock;
import com.masson.cruciblecraft.content.menu.StorageMenu;
import com.masson.cruciblecraft.content.storage.StorageClientSync;
import com.masson.cruciblecraft.content.storage.StorageFilters;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

public final class BottleCrateBlockEntity extends MachineCoverHostBlockEntity
        implements MenuProvider {
    private final StorageVariant variant;
    private final ItemStackHandler inventory;

    public BottleCrateBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.BOTTLE_CRATE.get(), pos, state, variantOf(state));
    }

    public BottleCrateBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            StorageVariant variant) {
        super(type, pos, state);
        this.variant = variant;
        this.inventory = new ItemStackHandler(variant.slots()) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return StorageFilters.bottle(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                StorageClientSync.send(BottleCrateBlockEntity.this);
            }
        };
    }

    public StorageVariant variant() {
        return variant;
    }

    public ItemStackHandler inventory() {
        return inventory;
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.cruciblecraft.bottle_crate");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int id, Inventory playerInventory, Player player) {
        return new StorageMenu(
                ModMenus.BOTTLE_CRATE.get(), id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }

    @Override
    public boolean allowCover(Direction side) {
        return false;
    }

    @Override
    public boolean hasEnergyBuffer() {
        return false;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            BottleCrateBlockEntity crate) {
        crate.tickCovers();
    }

    private static StorageVariant variantOf(BlockState state) {
        if (state.getBlock() instanceof BottleCrateBlock block) {
            return block.variant();
        }
        throw new IllegalStateException("Bottle crate BE missing host variant");
    }
}
