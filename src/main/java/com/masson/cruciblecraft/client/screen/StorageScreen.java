package com.masson.cruciblecraft.client.screen;

import com.masson.cruciblecraft.content.menu.StorageMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace(
                    "textures/gui/container/generic_54.png");

    public StorageScreen(
            StorageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = StorageMenu.IMAGE_WIDTH;
        this.imageHeight = menu.imageHeight();
        this.inventoryLabelY = menu.playerInventoryLabelY();
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int rows = Math.max(1, (menu.machineSlots() + 8) / 9);
        graphics.blit(
                TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                17 + rows * StorageMenu.SLOT);
        graphics.blit(
                TEXTURE,
                leftPos,
                topPos + 17 + rows * StorageMenu.SLOT,
                0,
                126,
                imageWidth,
                96);
    }
}
