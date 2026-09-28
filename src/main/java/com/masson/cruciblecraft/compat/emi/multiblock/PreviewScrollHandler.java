package com.masson.cruciblecraft.compat.emi.multiblock;

import com.masson.cruciblecraft.CrucibleCraft;

import dev.emi.emi.screen.RecipeScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * EMI keeps the scroll wheel for its own page scrolling and never hands it
 * to a recipe widget. While the pointer is over a projection, zoom instead
 * and stop the page from changing.
 */
@EventBusSubscriber(modid = CrucibleCraft.MODID, value = Dist.CLIENT)
public final class PreviewScrollHandler {
    private PreviewScrollHandler() {}

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof RecipeScreen)) {
            return;
        }
        int mouseX = (int) event.getMouseX();
        int mouseY = (int) event.getMouseY();
        MultiblockProjectionWidget projection =
                MultiblockProjectionWidget.underPointer(mouseX, mouseY);
        if (projection != null) {
            projection.zoomBy(event.getScrollDeltaY() > 0 ? 1.15f : 1f / 1.15f);
            event.setCanceled(true);
        }
    }
}
