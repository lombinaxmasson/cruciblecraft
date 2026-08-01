package com.masson.cruciblecraft.client.screen;

import com.masson.cruciblecraft.content.menu.CrusherMenu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Stable crusher screen wrapper over the shared machine renderer. */
public final class CrusherScreen extends ProcessingMachineScreen<CrusherMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");

    public CrusherScreen(
            CrusherMenu menu,
            Inventory inventory,
            Component title) {
        super(menu, inventory, title);
    }

    @Override protected ResourceLocation texture() {
        return TEXTURE;
    }
}
