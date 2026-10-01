package com.masson.cruciblecraft.recipe.gt;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.gt.CompactFamilyOnDemand;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = CrucibleCraft.MODID, value = Dist.CLIENT)
public final class GTRecipeMapClientEvents {
    private static boolean prefetchQueued;

    private GTRecipeMapClientEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void recipesUpdated(RecipesUpdatedEvent event) {
        long started = System.nanoTime();
        CompactFamilyOnDemand.clear();
        var manager = event.getRecipeManager();
        PipeCraftingGridGuard.dropTokenRecipes(manager);
        long dropped = System.nanoTime();
        Minecraft minecraft = Minecraft.getInstance();
        boolean integratedServer = minecraft.hasSingleplayerServer()
                || minecraft.getConnection() != null
                        && minecraft.getConnection().getConnection().isMemoryConnection()
                || ServerLifecycleHooks.getCurrentServer() != null;
        boolean reload = GTRecipeReloadDecision.onClientRecipesUpdated(
                manager,
                integratedServer);
        if (!integratedServer) {
            prefetchQueued = true;
        }
        if (reload) {
            // Dedicated clients rebuild Extruder and compact family snapshots
            // from the synced recipe manager. Integrated clients skip this
            // path and reuse the server epoch.
            GTRecipeMapLoader.reload(
                    manager,
                    ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                    GTRecipeReloadCoordinator.Cause.CLIENT_RECIPES_UPDATED);
        }
        CrucibleCraft.LOGGER.info(
                "Client recipes updated: dropTokens={}ms clientReload={}ms ranReload={}",
                elapsedMillis(started, dropped),
                elapsedMillis(dropped, System.nanoTime()),
                reload);
    }

    @SubscribeEvent
    public static void prefetchOnDemandFamilies(ClientTickEvent.Post event) {
        if (!prefetchQueued) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        prefetchQueued = false;
        CompactFamilyOnDemand.requestAll(ModRecipeMaps.ALL);
    }

    private static long elapsedMillis(long started, long ended) {
        return (ended - started) / 1_000_000L;
    }
}
