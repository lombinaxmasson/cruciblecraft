package com.masson.cruciblecraft.content.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** GT6 sided access: no insert from below while auto-output is on. */
public final class MassStorageSidedHandler implements IItemHandler {
    private final MassStorageHandler inventory;
    private final boolean blockInsert;

    public MassStorageSidedHandler(MassStorageHandler inventory, boolean blockInsert) {
        this.inventory = inventory;
        this.blockInsert = blockInsert;
    }

    @Override
    public int getSlots() {
        return inventory.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (blockInsert) {
            return stack;
        }
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return !blockInsert && inventory.isItemValid(slot, stack);
    }
}
