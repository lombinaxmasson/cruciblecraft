package com.masson.cruciblecraft.content.menu;

import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Concrete menu used by every configured T2/T3 processing machine. */
public final class ConfiguredProcessingMachineMenu extends ProcessingMachineMenu {
    public static final int DATA_COUNT = 6;

    public ConfiguredProcessingMachineMenu(
            int id,
            Inventory playerInventory,
            ProcessingMachineSpec spec) {
        this(ModMenus.forMachine(spec).get(), id, playerInventory, spec);
    }

    public ConfiguredProcessingMachineMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            ProcessingMachineSpec spec) {
        super(type, id, playerInventory, new ItemStackHandler(spec.items().slotCount()),
                new SimpleContainerData(DATA_COUNT), spec, player -> true);
    }

    public ConfiguredProcessingMachineMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            ConfiguredProcessingMachineBlockEntity machine) {
        super(type, id, playerInventory, machine.inventory(), machine.data(),
                machine.spec(), machine::stillValid);
    }
}
