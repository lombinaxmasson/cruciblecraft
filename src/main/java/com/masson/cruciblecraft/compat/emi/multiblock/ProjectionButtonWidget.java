package com.masson.cruciblecraft.compat.emi.multiblock;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Small labelled square button beside the projection. */
public final class ProjectionButtonWidget extends Widget {
    private final int x;
    private final int y;
    private final int size;
    private final String label;
    private final Component tooltip;
    private final Runnable action;

    public ProjectionButtonWidget(
            int x,
            int y,
            int size,
            String label,
            Component tooltip,
            Runnable action) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.label = label;
        this.tooltip = tooltip;
        this.action = action;
    }

    @Override
    public Bounds getBounds() {
        return new Bounds(x, y, size, size);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = getBounds().contains(mouseX, mouseY);
        graphics.fill(x, y, x + size, y + size, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1,
                hovered ? 0xFF7A7A7A : 0xFF5A5A5A);
        Font font = Minecraft.getInstance().font;
        int textX = x + (size - font.width(label) + 1) / 2;
        int textY = y + (size - font.lineHeight + 2) / 2;
        graphics.drawString(font, label, textX, textY, 0xFFFFFFFF, false);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        return List.of(ClientTooltipComponent.create(tooltip.getVisualOrderText()));
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        action.run();
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        return true;
    }
}
