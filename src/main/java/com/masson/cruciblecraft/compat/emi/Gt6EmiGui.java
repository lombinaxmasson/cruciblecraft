package com.masson.cruciblecraft.compat.emi;

import com.masson.cruciblecraft.client.screen.MachineGuiTextures;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Shared GT6 NEI chrome for EMI.
 *
 * <p>GT6 {@code NEI_RecipeMap.drawBackground} blits {@code NEI.png} 176×166 at
 * {@code (-5,-16)} and the machine GUI 176×79 from {@code v=3} at
 * {@code (-5,-8)}. Items sit at GUI {@code (x-5,y-11)}. Translating by
 * {@code (+5,+16)} puts chrome at EMI {@code (0,0)}. EMI's 18×18 slot widget
 * insets the item by 1px, so machine slots land at {@code (guiX-1, guiY+4)}.
 */
final class Gt6EmiGui {
    static final int MACHINE_PANEL_Y = 8;
    static final int MACHINE_PANEL_V = 3;
    static final int MACHINE_PANEL_HEIGHT = 79;
    static final int NEI_CHROME_HEIGHT = 166;
    private static final int SLOT_INSET = 1;

    private Gt6EmiGui() {}

    static ResourceLocation texture(String path) {
        return MachineGuiTextures.forPath(path);
    }

    static ResourceLocation neiChrome() {
        return texture("nei");
    }

    static int slotX(int guiX) {
        return guiX - SLOT_INSET;
    }

    static int slotY(int guiY) {
        return guiY + 4;
    }

    static int tankX(int guiX) {
        return guiX;
    }

    static int tankY(int guiY) {
        return guiY + MACHINE_PANEL_Y - MACHINE_PANEL_V;
    }

    static int progressX(int guiX) {
        return guiX;
    }

    static int progressY(int guiY) {
        return guiY + 5;
    }

    static void addPanel(WidgetHolder widgets, ResourceLocation texture) {
        widgets.addTexture(
                neiChrome(),
                0,
                0,
                ProcessingEmiLayout.PANEL_WIDTH,
                NEI_CHROME_HEIGHT,
                0,
                0,
                ProcessingEmiLayout.PANEL_WIDTH,
                NEI_CHROME_HEIGHT,
                ProcessingEmiLayout.TEXTURE_SIZE,
                ProcessingEmiLayout.TEXTURE_SIZE);
        widgets.addTexture(
                texture,
                0,
                MACHINE_PANEL_Y,
                ProcessingEmiLayout.PANEL_WIDTH,
                MACHINE_PANEL_HEIGHT,
                0,
                MACHINE_PANEL_V,
                ProcessingEmiLayout.PANEL_WIDTH,
                MACHINE_PANEL_HEIGHT,
                ProcessingEmiLayout.TEXTURE_SIZE,
                ProcessingEmiLayout.TEXTURE_SIZE);
    }

    static void addProgress(
            WidgetHolder widgets,
            ResourceLocation texture,
            ProcessingEmiLayout.Rect progress,
            int durationTicks) {
        widgets.addAnimatedTexture(
                texture,
                progressX(progress.x()),
                progressY(progress.y()),
                progress.width(),
                progress.height(),
                Gt6BasicMachineGui.PROGRESS_U,
                Gt6BasicMachineGui.PROGRESS_V,
                Math.max(1, durationTicks) * 50,
                true,
                false,
                false);
    }

    static SlotWidget slot(WidgetHolder widgets, EmiIngredient stack, int guiX, int guiY) {
        return widgets.addSlot(stack, slotX(guiX), slotY(guiY)).drawBack(false);
    }

    static SlotWidget emptySlot(WidgetHolder widgets, int guiX, int guiY) {
        return widgets.addSlot(slotX(guiX), slotY(guiY)).drawBack(false);
    }

    static SlotWidget output(
            WidgetHolder widgets, EmiStack stack, EmiRecipe recipe, int guiX, int guiY) {
        return slot(widgets, stack, guiX, guiY).recipeContext(recipe);
    }

    static SlotWidget catalyst(WidgetHolder widgets, EmiIngredient stack, int guiX, int guiY) {
        return slot(widgets, stack, guiX, guiY).catalyst(true);
    }

    static SlotWidget workstation(WidgetHolder widgets, EmiIngredient stack) {
        return widgets.addSlot(
                        stack,
                        ProcessingEmiLayout.WORKSTATION.x(),
                        ProcessingEmiLayout.WORKSTATION.y())
                .drawBack(false)
                .catalyst(true);
    }

    static SlotWidget tank(
            WidgetHolder widgets,
            EmiIngredient stack,
            int guiX,
            int guiY,
            int width,
            int height,
            int capacity) {
        return widgets.addTank(
                stack,
                tankX(guiX),
                tankY(guiY),
                width,
                height,
                capacity)
                .drawBack(false);
    }

    static void addStats(
            WidgetHolder widgets,
            ProcessingEmiRecipeData data,
            int costsY,
            int usageY,
            int timeY) {
        long rate = Math.abs(data.eut());
        long total = rate * (long) data.durationTicks();
        if (data.eut() != 0L) {
            widgets.addText(
                    Component.translatable(
                            data.eut() < 0L
                                    ? "emi.cruciblecraft.processing.gain"
                                    : "emi.cruciblecraft.processing.costs",
                            Long.toString(total),
                            data.energyName()),
                    8,
                    costsY,
                    0xFF000000,
                    false);
        }
        if (data.eut() != 0L && data.eut() != 1L) {
            widgets.addText(
                    Component.translatable(
                            data.eut() < 0L
                                    ? "emi.cruciblecraft.processing.output"
                                    : "emi.cruciblecraft.processing.usage",
                            Long.toString(rate),
                            data.energyUnit()),
                    8,
                    usageY,
                    0xFF000000,
                    false);
        }
        widgets.addText(timeComponent(data.durationTicks()), 8, timeY, 0xFF000000, false);
        if (data.specialValue() != 0L) {
            widgets.addText(
                    Component.translatable(
                            "emi.cruciblecraft.processing.special",
                            Long.toString(data.specialValue())),
                    8,
                    timeY + 10,
                    0xFF000000,
                    false);
        }
    }

    static Component timeComponent(int durationTicks) {
        if (durationTicks < 1_200) {
            return Component.translatable(
                    "emi.cruciblecraft.processing.time_ticks", durationTicks);
        }
        if (durationTicks < 36_000) {
            return Component.translatable(
                    "emi.cruciblecraft.processing.time_secs", durationTicks / 20);
        }
        return Component.translatable(
                "emi.cruciblecraft.processing.time_mins", durationTicks / 1_200);
    }
}
