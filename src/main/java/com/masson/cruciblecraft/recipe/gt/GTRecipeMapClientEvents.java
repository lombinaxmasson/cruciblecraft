package com.masson.cruciblecraft.recipe.gt;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = CrucibleCraft.MODID, value = Dist.CLIENT)
public final class GTRecipeMapClientEvents {
    private GTRecipeMapClientEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void recipesUpdated(RecipesUpdatedEvent event) {
        var manager = event.getRecipeManager();
        Minecraft minecraft = Minecraft.getInstance();
        boolean integratedServer = minecraft.hasSingleplayerServer()
                || minecraft.getConnection() != null
                        && minecraft.getConnection().getConnection().isMemoryConnection()
                || ServerLifecycleHooks.getCurrentServer() != null;
        if (GTRecipeReloadDecision.onClientRecipesUpdated(
                manager,
                integratedServer)) {
            // Dedicated clients rebuild Extruder and compact family snapshots
            // from the synced recipe manager. Integrated clients skip this
            // path and reuse the server epoch.
            GTRecipeMapLoader.reload(
                    manager,
                    ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT);
        }
    }
}
