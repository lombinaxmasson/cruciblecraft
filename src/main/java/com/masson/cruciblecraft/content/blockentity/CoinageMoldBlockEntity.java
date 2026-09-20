package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.item.CoinItem;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GT6 {@code MultiTileEntityMoldCoinage}: one tiny plate in, hammer stamps a
 * coin, hopper extracts only the coin.
 */
public final class CoinageMoldBlockEntity extends BlockEntity {
    private final ItemStackHandler slot = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int index, ItemStack stack) {
            return isBlankTinyPlate(stack);
        }

        @Override
        public int getSlotLimit(int index) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int index) {
            setChanged();
        }
    };
    private final IItemHandler hopper = new HopperHandler();

    public CoinageMoldBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COINAGE_MOLD.get(), pos, state);
    }

    public ItemStack contents() {
        return slot.getStackInSlot(0);
    }

    public boolean insertBlank(ItemStack stack) {
        if (!isBlankTinyPlate(stack) || !slot.getStackInSlot(0).isEmpty()) {
            return false;
        }
        slot.setStackInSlot(0, stack.copyWithCount(1));
        return true;
    }

    public ItemStack takeContents() {
        ItemStack held = slot.getStackInSlot(0);
        if (held.isEmpty()) {
            return ItemStack.EMPTY;
        }
        slot.setStackInSlot(0, ItemStack.EMPTY);
        return held;
    }

    public boolean stamp() {
        ItemStack held = slot.getStackInSlot(0);
        if (!isBlankTinyPlate(held)) {
            return false;
        }
        String materialId = MaterialUnits.resolve(held)
                .map(MaterialUnits.Entry::materialId)
                .orElse("");
        if (!CoinItem.allowed(materialId)) {
            return false;
        }
        slot.setStackInSlot(0, CoinItem.stack(materialId, held.getCount()));
        return true;
    }

    public IItemHandler itemHandler() {
        return hopper;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack held = slot.getStackInSlot(0);
        if (!held.isEmpty()) {
            Containers.dropItemStack(
                    level,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    held);
            slot.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    public static boolean isBlankTinyPlate(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() instanceof CoinItem) {
            return false;
        }
        return MaterialUnits.resolve(stack)
                .filter(entry -> entry.form().equals(MaterialPrefixes.TINY_PLATE)
                        && CoinItem.allowed(entry.materialId()))
                .isPresent();
    }

    public static boolean isCoin(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof CoinItem coin)) {
            return false;
        }
        String materialId = stack.get(ModComponents.PREFIX_MATERIAL);
        return materialId != null && coin.isPersistedMaterialAllowed(materialId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("slot")) {
            slot.deserializeNBT(registries, tag.getCompound("slot"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("slot", slot.serializeNBT(registries));
    }

    private final class HopperHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return slot.getStackInSlot(index);
        }

        @Override
        public ItemStack insertItem(int index, ItemStack stack, boolean simulate) {
            return slot.insertItem(index, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int index, int amount, boolean simulate) {
            if (isBlankTinyPlate(slot.getStackInSlot(index))) {
                return ItemStack.EMPTY;
            }
            return slot.extractItem(index, amount, simulate);
        }

        @Override
        public int getSlotLimit(int index) {
            return 1;
        }

        @Override
        public boolean isItemValid(int index, ItemStack stack) {
            return slot.isItemValid(index, stack);
        }
    }
}
