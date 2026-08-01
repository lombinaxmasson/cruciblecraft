package com.masson.cruciblecraft.client.screen;

import com.masson.cruciblecraft.content.menu.ProcessingMachineMenu;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Shared spec-driven machine background and progress rendering. */
public abstract class ProcessingMachineScreen<M extends ProcessingMachineMenu>
        extends AbstractContainerScreen<M> {
    protected ProcessingMachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected abstract ResourceLocation texture();

    @Override protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY) {
        graphics.blit(texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        ProcessingMachineSpec.ProgressBar progress = menu.machineSpec().ui().progress();
        int scaled = menu.scaledProgress(progress.width());
        if (scaled > 0) {
            graphics.fill(
                    leftPos + progress.x(),
                    topPos + progress.y(),
                    leftPos + progress.x() + scaled,
                    topPos + progress.y() + progress.height(),
                    0xFFC88634);
        }
    }

    @Override protected void renderLabels(
            GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.cruciblecraft.processing.status." + menu.status()),
                8,
                70,
                0x404040,
                false);
        if (menu.tankCapacity() > 0) {
            graphics.drawString(
                    font,
                    Component.translatable(
                            "screen.cruciblecraft.processing.tank",
                            menu.tankAmount(),
                            menu.tankCapacity()),
                    80,
                    70,
                    0x404040,
                    false);
        }
    }

    @Override public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
