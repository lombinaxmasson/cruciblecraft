package com.masson.cruciblecraft.energy.battery;

import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Item-component charge shared with the placed battery block entity. */
public final class BatteryCharge {
    private BatteryCharge() {}

    public static long get(ItemStack stack) {
        return stack.getOrDefault(ModComponents.BATTERY_CHARGE.get(), 0L);
    }

    public static void set(ItemStack stack, long charge, long capacity) {
        long clamped = Math.max(0L, Math.min(capacity, charge));
        if (clamped <= 0L) {
            stack.remove(ModComponents.BATTERY_CHARGE.get());
            stack.remove(DataComponents.MAX_STACK_SIZE);
            return;
        }
        stack.set(ModComponents.BATTERY_CHARGE.get(), clamped);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
    }
}
