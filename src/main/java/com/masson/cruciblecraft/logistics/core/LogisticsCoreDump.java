package com.masson.cruciblecraft.logistics.core;

import java.util.Set;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Invert-filter leftover move: items whose ids are in {@code filteredFor}
 * stay; everything else is offered to the dump inventory.
 */
public final class LogisticsCoreDump {
    private LogisticsCoreDump() {}

    public static boolean isLeftover(
            ItemStack stack, Set<ResourceLocation> filteredFor) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (filteredFor == null || filteredFor.isEmpty()) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return !filteredFor.contains(id);
    }

    public static int moveLeftover(
            IItemHandler from,
            IItemHandler to,
            Set<ResourceLocation> filteredFor,
            int maxCount) {
        if (from == null || to == null || maxCount <= 0) {
            return 0;
        }
        ItemStack taken = extractLeftover(from, filteredFor, maxCount, false);
        if (taken.isEmpty()) {
            return 0;
        }
        ItemStack leftover = insertAll(to, taken, false);
        if (!leftover.isEmpty()) {
            insertAll(from, leftover, false);
        }
        return taken.getCount() - leftover.getCount();
    }

    private static ItemStack extractLeftover(
            IItemHandler handler,
            Set<ResourceLocation> filteredFor,
            int amount,
            boolean simulate) {
        ItemStack taken = ItemStack.EMPTY;
        for (int slot = 0;
                slot < handler.getSlots() && taken.getCount() < amount;
                slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            if (!isLeftover(slotStack, filteredFor)) {
                continue;
            }
            if (!taken.isEmpty()
                    && !ItemStack.isSameItemSameComponents(taken, slotStack)) {
                continue;
            }
            ItemStack extracted = handler.extractItem(
                    slot, amount - taken.getCount(), simulate);
            if (extracted.isEmpty()) {
                continue;
            }
            if (taken.isEmpty()) {
                taken = extracted.copy();
            } else {
                taken.grow(extracted.getCount());
            }
        }
        return taken;
    }

    private static ItemStack insertAll(
            IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0;
                slot < handler.getSlots() && !remaining.isEmpty();
                slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }
}
