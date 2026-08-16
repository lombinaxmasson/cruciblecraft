package com.masson.cruciblecraft.client.screen;

import com.masson.cruciblecraft.content.menu.CokeOvenMenu;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CokeOvenScreen extends AbstractContainerScreen<CokeOvenMenu> {
    private static final ResourceLocation TEXTURE =
            MachineGuiTextures.forPath("coke_oven");

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

        ProcessingMachineSpec.ProgressBar progress = CokeOvenMenu.LAYOUT.progress();
        int scaled = menu.scaledProgress(progress.width());
        if (scaled > 0) {
            graphics.blit(
                    TEXTURE,
                    leftPos + progress.x(),
                    topPos + progress.y(),
                    Gt6BasicMachineGui.PROGRESS_U,
                    Gt6BasicMachineGui.PROGRESS_V,
                    scaled,
                    progress.height());
        }

        ProcessingMachineSpec.TankPosition tank = CokeOvenMenu.LAYOUT.tanks().getFirst();
        int fluidHeight = menu.scaledTank(Math.max(0, tank.height() - 2));
        if (fluidHeight > 0) {
            graphics.fill(
                    leftPos + tank.x() + 1,
                    topPos + tank.y() + tank.height() - 1 - fluidHeight,
                    leftPos + tank.x() + tank.width() - 1,
                    topPos + tank.y() + tank.height() - 1,
                    0xFF5A3219);
        }
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
        ProcessingMachineSpec.TankPosition tank = CokeOvenMenu.LAYOUT.tanks().getFirst();
        if (isHovering(
                tank.x(), tank.y(), tank.width(), tank.height(), mouseX, mouseY)) {
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
