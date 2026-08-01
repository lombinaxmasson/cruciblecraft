package com.masson.cruciblecraft.recipe.gt;

import com.masson.cruciblecraft.CrucibleCraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

@EventBusSubscriber(modid = CrucibleCraft.MODID, value = Dist.CLIENT)
public final class GTRecipeMapClientEvents {
    private GTRecipeMapClientEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void recipesUpdated(RecipesUpdatedEvent event) {
        var manager = event.getRecipeManager();
        if (GTRecipeReloadDecision.onClientRecipesUpdated(manager)) {
            GTRecipeMapLoader.reload(manager);
        }
    }
}
