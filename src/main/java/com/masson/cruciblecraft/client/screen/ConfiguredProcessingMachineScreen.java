package com.masson.cruciblecraft.client.screen;

import com.masson.cruciblecraft.content.menu.ConfiguredProcessingMachineMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GT6-textured shared screen for configured processing machines. */
public final class ConfiguredProcessingMachineScreen
        extends ProcessingMachineScreen<ConfiguredProcessingMachineMenu> {
    public ConfiguredProcessingMachineScreen(
            ConfiguredProcessingMachineMenu menu,
            Inventory inventory,
            Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected ResourceLocation texture() {
        return MachineGuiTextures.forMachine(menu.machineSpec().id());
    }
}
