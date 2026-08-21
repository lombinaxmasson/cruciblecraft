package com.masson.cruciblecraft.client.screen;

import com.masson.cruciblecraft.content.menu.HopperMenu;
import com.masson.cruciblecraft.logistics.hopper.HopperMenuLayout;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class HopperScreen extends AbstractContainerScreen<HopperMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace(
                    "textures/gui/container/generic_54.png");

    public HopperScreen(
            HopperMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = HopperMenuLayout.IMAGE_WIDTH;
        this.imageHeight = menu.imageHeight();
        this.inventoryLabelY = menu.playerInventoryLabelY();
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int rows = HopperMenuLayout.rows(menu.machineSlots());
        graphics.blit(
                TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                HopperMenuLayout.MACHINE_Y + rows * HopperMenuLayout.SLOT);
        int playerY = HopperMenuLayout.playerInventoryY(menu.machineSlots());
        graphics.blit(
                TEXTURE,
                leftPos,
                topPos + playerY - 1,
                0,
                125,
                imageWidth,
                imageHeight - (playerY - 1));
    }

    @Override
    public void render(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
