package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.MassStorageBlock;
import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.storage.ILogisticsStorage;
import com.masson.cruciblecraft.content.storage.MassStorageHandler;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class MassStorageBlockEntity extends BlockEntity
        implements ILogisticsStorage {
    private final StorageVariant variant;
    private final MassStorageHandler inventory;

    public MassStorageBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MASS_STORAGE.get(), pos, state, variantOf(state));
    }

    public MassStorageBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            StorageVariant variant) {
        super(type, pos, state);
        this.variant = variant;
        this.inventory = new MassStorageHandler(variant.capacity(), this::setChanged);
    }

    public StorageVariant variant() {
        return variant;
    }

    public MassStorageHandler inventory() {
        return inventory;
    }

    public IItemHandler itemHandler(Direction side) {
        return inventory;
    }

    public ILogisticsStorage logisticsStorage() {
        return variant.logistics() ? this : null;
    }

    @Override
    public int getLogisticsPriorityItem() {
        return variant.logistics() ? 2 : 0;
    }

    @Override
    public ItemStack getLogisticsFilterItem() {
        return variant.logistics() ? inventory.filter() : ItemStack.EMPTY;
    }

    public void playerInsertOrExtract(Player player, ItemStack held) {
        if (!held.isEmpty()) {
            ItemStack leftover = inventory.insertAll(held.copy(), false);
            held.setCount(leftover.getCount());
        } else {
            ItemStack extracted = inventory.extractItem(0, 64, false);
            if (!extracted.isEmpty() && !player.addItem(extracted)) {
                player.drop(extracted, false);
            }
        }
        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        while (inventory.stored() > 0) {
            ItemStack extracted = inventory.extractItem(0, 64, false);
            if (extracted.isEmpty()) {
                break;
            }
            Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(), extracted);
        }
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            MassStorageBlockEntity storage) {
        if (level.isClientSide || level.getGameTime() % 8L != 0L) {
            return;
        }
        Direction facing = state.getValue(StorageHostBlock.FACING);
        for (Direction side : Direction.values()) {
            if (side == facing) {
                continue;
            }
            IItemHandler neighbor = level.getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    pos.relative(side),
                    side.getOpposite());
            if (neighbor == null) {
                continue;
            }
            for (int slot = 0; slot < neighbor.getSlots(); slot++) {
                ItemStack extracted = neighbor.extractItem(slot, 1, true);
                if (extracted.isEmpty() || !storage.inventory.sameType(extracted)) {
                    continue;
                }
                ItemStack leftover = storage.inventory.insertAll(extracted, true);
                if (!leftover.isEmpty()) {
                    continue;
                }
                storage.inventory.insertAll(
                        neighbor.extractItem(slot, 1, false), false);
                storage.setChanged();
                return;
            }
        }
        IItemHandler output = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                pos.relative(facing),
                facing.getOpposite());
        if (output == null) {
            return;
        }
        ItemStack extracted = storage.inventory.extractItem(0, 1, true);
        if (extracted.isEmpty()) {
            return;
        }
        ItemStack leftover = extracted.copy();
        for (int slot = 0; slot < output.getSlots() && !leftover.isEmpty(); slot++) {
            leftover = output.insertItem(slot, leftover, false);
        }
        if (leftover.getCount() < extracted.getCount()) {
            storage.inventory.extractItem(
                    0, extracted.getCount() - leftover.getCount(), false);
            storage.setChanged();
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!inventory.filter().isEmpty()) {
            tag.put("filter", inventory.filter().save(registries));
        }
        tag.putInt("stored", inventory.stored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ItemStack filter = tag.contains("filter")
                ? ItemStack.parseOptional(registries, tag.getCompound("filter"))
                : ItemStack.EMPTY;
        inventory.load(filter, tag.getInt("stored"));
    }

    private static StorageVariant variantOf(BlockState state) {
        if (state.getBlock() instanceof MassStorageBlock block) {
            return block.variant();
        }
        throw new IllegalStateException("Mass storage BE missing host variant");
    }
}
