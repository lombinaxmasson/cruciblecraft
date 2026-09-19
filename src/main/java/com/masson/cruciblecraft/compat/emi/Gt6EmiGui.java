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
 * Shared GT6 NEI chrome for EMI: machine-GUI crop, progress overlay from
 * {@code u=176}, slot frames from the texture, and Costs/Usage/Time lines.
 */
final class Gt6EmiGui {
    private Gt6EmiGui() {}

    static ResourceLocation texture(String path) {
        return MachineGuiTextures.forPath(path);
    }

    static ResourceLocation neiChrome() {
        return texture("nei");
    }

    static void addPanel(WidgetHolder widgets, ResourceLocation texture) {
        widgets.addTexture(
                texture,
                0,
                0,
                ProcessingEmiLayout.PANEL_WIDTH,
                ProcessingEmiLayout.PANEL_HEIGHT,
                0,
                0,
                ProcessingEmiLayout.PANEL_WIDTH,
                ProcessingEmiLayout.PANEL_HEIGHT,
                ProcessingEmiLayout.TEXTURE_SIZE,
                ProcessingEmiLayout.TEXTURE_SIZE);
        widgets.addTexture(
                neiChrome(),
                0,
                ProcessingEmiLayout.PANEL_HEIGHT,
                ProcessingEmiLayout.PANEL_WIDTH,
                ProcessingEmiLayout.NEI_HEIGHT - ProcessingEmiLayout.PANEL_HEIGHT,
                0,
                ProcessingEmiLayout.PANEL_HEIGHT,
                ProcessingEmiLayout.PANEL_WIDTH,
                ProcessingEmiLayout.NEI_HEIGHT - ProcessingEmiLayout.PANEL_HEIGHT,
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
                progress.x(),
                progress.y(),
                progress.width(),
                progress.height(),
                Gt6BasicMachineGui.PROGRESS_U,
                Gt6BasicMachineGui.PROGRESS_V,
                Math.max(1, durationTicks) * 50,
                true,
                false,
                false);
    }

    static SlotWidget slot(WidgetHolder widgets, EmiIngredient stack, int x, int y) {
        return widgets.addSlot(stack, x, y).drawBack(false);
    }

    static SlotWidget output(
            WidgetHolder widgets, EmiStack stack, EmiRecipe recipe, int x, int y) {
        return slot(widgets, stack, x, y).recipeContext(recipe);
    }

    static SlotWidget catalyst(WidgetHolder widgets, EmiIngredient stack, int x, int y) {
        return slot(widgets, stack, x, y).catalyst(true);
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
