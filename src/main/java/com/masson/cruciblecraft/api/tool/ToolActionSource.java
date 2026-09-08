package com.masson.cruciblecraft.api.tool;

import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;

/** An item that can perform one or more {@link ToolAction}s. */
public interface ToolActionSource {
    boolean provides(ToolAction action);

    static boolean provides(ItemStack stack, ToolAction action) {
        return stack.getItem() instanceof ToolActionSource source
                && source.provides(action);
    }

    static boolean providesAny(
            ItemStack stack, Predicate<ToolAction> test) {
        if (!(stack.getItem() instanceof ToolActionSource source)) {
            return false;
        }
        for (ToolAction action : ToolAction.values()) {
            if (source.provides(action) && test.test(action)) {
                return true;
            }
        }
        return false;
    }
}
