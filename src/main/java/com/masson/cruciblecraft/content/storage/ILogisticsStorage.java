package com.masson.cruciblecraft.content.storage;

import net.minecraft.world.item.ItemStack;

/** GT6 logistics storage adapter: filtered bulk endpoint, not a texture swap. */
public interface ILogisticsStorage {
    /** 0 disabled, 1 generic, 2 semi-filtered, 3 filtered. */
    int getLogisticsPriorityItem();

    ItemStack getLogisticsFilterItem();
}
