package com.masson.cruciblecraft.client.screen;

import com.masson.cruciblecraft.content.menu.ProcessingMachineMenu;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineDisplayData;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.network.CompactFamilyRequestPayload;
import com.masson.cruciblecraft.recipe.gt.CompactFamilyOnDemand;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** Shared spec-driven machine background and progress rendering. */
public abstract class ProcessingMachineScreen<M extends ProcessingMachineMenu>
        extends AbstractContainerScreen<M> {
    protected ProcessingMachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        inventoryLabelY = -10_000;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hasSingleplayerServer()) {
            return;
        }
        var targetMap = menu.machineSpec().requireRecipeMap().id();
        if (CompactFamilyOnDemand.beginRequest(targetMap)) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new CompactFamilyRequestPayload(targetMap));
        }
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
            graphics.blit(
                    texture(),
                    leftPos + progress.x(),
                    topPos + progress.y(),
                    Gt6BasicMachineGui.PROGRESS_U,
                    Gt6BasicMachineGui.PROGRESS_V,
                    scaled,
                    progress.height());
        }
        for (ProcessingMachineSpec.TankPosition tank : menu.machineSpec().ui().tanks()) {
            FluidStack fluid = menu.tankFluid(tank.tank());
            int amount = fluid.getAmount();
            int capacity = menu.tankCapacity(tank.tank());
            int innerHeight = Math.max(0, tank.height() - 2);
            int filled = capacity <= 0
                    ? 0
                    : (int) Math.min(
                            innerHeight,
                            (long) amount * innerHeight / capacity);
            int x = leftPos + tank.x();
            int y = topPos + tank.y();
            if (filled > 0) {
                int tint = fluid.isEmpty()
                        ? 0xFF3F76E4
                        : IClientFluidTypeExtensions.of(fluid.getFluid())
                                .getTintColor(fluid);
                graphics.fill(
                        x + 1,
                        y + tank.height() - 1 - filled,
                        x + tank.width() - 1,
                        y + tank.height() - 1,
                        tint);
            }
        }
    }

    @Override protected void renderLabels(
            GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(
                font,
                ProcessingMachineDisplayData.statusComponent(
                        menu.status(), menu.statusArgument()),
                8,
                74,
                0x404040,
                false);
    }

    @Override public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (ProcessingMachineSpec.TankPosition tank : menu.machineSpec().ui().tanks()) {
            if (isHovering(
                    tank.x(), tank.y(), tank.width(), tank.height(), mouseX, mouseY)) {
                FluidStack fluid = menu.tankFluid(tank.tank());
                graphics.renderTooltip(
                        font,
                        fluid.isEmpty()
                                ? Component.translatable(
                                        "screen.cruciblecraft.processing.tank_empty",
                                        menu.tankCapacity(tank.tank()))
                                : Component.translatable(
                                        "screen.cruciblecraft.processing.tank_named",
                                        fluid.getHoverName(),
                                        fluid.getAmount(),
                                        menu.tankCapacity(tank.tank())),
                        mouseX,
                        mouseY);
                break;
            }
        }
    }
}
