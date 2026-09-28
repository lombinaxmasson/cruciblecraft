package com.masson.cruciblecraft.client.screen;

import com.masson.cruciblecraft.content.menu.CrusherMenu;
import com.masson.cruciblecraft.gui.MachineGuiTextures;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CrusherScreen extends ProcessingMachineScreen<CrusherMenu> {
    public CrusherScreen(
            CrusherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected ResourceLocation texture() {
        return MachineGuiTextures.forPath("bronze_crusher");
    }
}
