package com.masson.cruciblecraft.client.screen;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.menu.HopperMenu;
import com.masson.cruciblecraft.logistics.hopper.HopperMenuLayout;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class HopperScreen extends AbstractContainerScreen<HopperMenu> {
    private static final ResourceLocation GENERIC =
            ResourceLocation.withDefaultNamespace(
                    "textures/gui/container/generic_54.png");

    public HopperScreen(
            HopperMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = HopperMenuLayout.IMAGE_WIDTH;
        this.imageHeight = menu.imageHeight();
        this.titleLabelY = HopperMenuLayout.titleLabelY(menu.machineSlots());
        this.inventoryLabelY = menu.playerInventoryLabelY();
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int slots = menu.machineSlots();
        if (HopperMenuLayout.hasChestBackground(slots)) {
            graphics.blit(
                    chestBackground(slots),
                    leftPos,
                    topPos,
                    0,
                    0,
                    imageWidth,
                    imageHeight);
            return;
        }
        graphics.blit(GENERIC, leftPos, topPos, 0, 0, imageWidth, 17);
        graphics.fill(
                leftPos + 7,
                topPos + 17,
                leftPos + imageWidth - 7,
                topPos + HopperMenuLayout.PLAYER_INVENTORY_Y - 1,
                0xFFC6C6C6);
        for (int index = 0; index < slots; index++) {
            Slot slot = menu.slots.get(index);
            graphics.blit(
                    GENERIC,
                    leftPos + slot.x - 1,
                    topPos + slot.y - 1,
                    7,
                    17,
                    HopperMenuLayout.SLOT,
                    HopperMenuLayout.SLOT);
        }
        graphics.blit(
                GENERIC,
                leftPos,
                topPos + HopperMenuLayout.PLAYER_INVENTORY_Y - 1,
                0,
                126,
                imageWidth,
                imageHeight - (HopperMenuLayout.PLAYER_INVENTORY_Y - 1));
    }

    @Override
    public void render(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static ResourceLocation chestBackground(int slots) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "textures/gui/gt6_import/chests/" + slots + ".png");
    }
}
