package com.masson.cruciblecraft.content.menu;

import com.masson.cruciblecraft.content.blockentity.CrusherBlockEntity;
import com.masson.cruciblecraft.registry.ModMenus;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Stable crusher menu id with shared spec-driven behavior. */
public final class CrusherMenu extends ProcessingMachineMenu {
    public CrusherMenu(int id, Inventory inventory) {
        super(
                ModMenus.CRUSHER.get(),
                id,
                inventory,
                new ItemStackHandler(ModProcessingMachines.CRUSHER.items().slotCount()),
                new SimpleContainerData(3),
                ModProcessingMachines.CRUSHER,
                player -> true);
    }

    public CrusherMenu(int id, Inventory inventory, CrusherBlockEntity blockEntity) {
        super(
                ModMenus.CRUSHER.get(),
                id,
                inventory,
                blockEntity.inventory(),
                blockEntity.data(),
                ModProcessingMachines.CRUSHER,
                blockEntity::stillValid);
    }
}
