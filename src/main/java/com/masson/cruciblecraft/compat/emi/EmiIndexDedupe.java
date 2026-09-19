package com.masson.cruciblecraft.compat.emi;

import java.util.Set;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * EMI-free identity for item-list rows: item plus component patch.
 * EMI's creative/registry merge already unique-ifies this way; plugin
 * {@code addEmiStack} does not, so bake-time invalidators reuse the key.
 */
final class EmiIndexDedupe {
    private EmiIndexDedupe() {}

    record ItemIndexKey(Item item, DataComponentPatch patch) {
        static ItemIndexKey of(ItemStack stack) {
            return new ItemIndexKey(stack.getItem(), stack.getComponentsPatch());
        }
    }

    /** True when this stack is a later copy of an earlier item+patch row. */
    static boolean isLaterCopy(Set<ItemIndexKey> seen, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return !seen.add(ItemIndexKey.of(stack));
    }
}
