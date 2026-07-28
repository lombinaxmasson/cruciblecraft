package com.masson.cruciblecraft.client.screen;

import com.masson.cruciblecraft.content.menu.CokeOvenMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CokeOvenScreen extends AbstractContainerScreen<CokeOvenMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final int TANK_X = 34;
    private static final int TANK_Y = 17;
    private static final int TANK_WIDTH = 10;
    private static final int TANK_HEIGHT = 52;

    public CokeOvenScreen(
            CokeOvenMenu menu,
            Inventory playerInventory,
            Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY) {
        graphics.blit(
                TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                imageHeight);

        int progress = menu.scaledProgress(24);
        if (progress > 0) {
            graphics.fill(
                    leftPos + 79,
                    topPos + 34,
                    leftPos + 79 + progress,
                    topPos + 40,
                    0xFFD36B19);
        }

        graphics.fill(
                leftPos + TANK_X - 1,
                topPos + TANK_Y - 1,
                leftPos + TANK_X + TANK_WIDTH + 1,
                topPos + TANK_Y + TANK_HEIGHT + 1,
                0xFF373737);
        int fluidHeight = menu.scaledTank(TANK_HEIGHT);
        graphics.fill(
                leftPos + TANK_X,
                topPos + TANK_Y + TANK_HEIGHT - fluidHeight,
                leftPos + TANK_X + TANK_WIDTH,
                topPos + TANK_Y + TANK_HEIGHT,
                0xFF5A3219);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component status = !menu.structureValid()
                ? Component.translatable("screen.cruciblecraft.coke_oven.invalid_structure")
                : !menu.heated()
                        ? Component.translatable("screen.cruciblecraft.coke_oven.no_heat")
                        : Component.empty();
        if (!status.getString().isEmpty()) {
            graphics.drawString(font, status, 79, 18, 0xA02020, false);
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + TANK_X
                && mouseX < leftPos + TANK_X + TANK_WIDTH
                && mouseY >= topPos + TANK_Y
                && mouseY < topPos + TANK_Y + TANK_HEIGHT) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "screen.cruciblecraft.coke_oven.creosote",
                            menu.creosoteAmount(),
                            menu.tankCapacity()),
                    mouseX,
                    mouseY);
        }
    }
}
