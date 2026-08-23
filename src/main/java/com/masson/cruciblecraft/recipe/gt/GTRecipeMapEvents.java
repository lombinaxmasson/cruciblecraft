package com.masson.cruciblecraft.recipe.gt;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class GTRecipeMapEvents {
    private static volatile RecipeManager lastTagsLoadedManager;

    private GTRecipeMapEvents() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        RecipeManager manager = event.getServer().getRecipeManager();
        if (GTRecipeReloadDecision.onServerStarted(manager, lastTagsLoadedManager)) {
            // Compact families are aggregated with Extruder inside the loader.
            GTRecipeMapLoader.reload(manager);
        }
        lastTagsLoadedManager = null;
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (GTRecipeReloadDecision.onServerTags(
                event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD,
                server != null)) {
            RecipeManager manager = server.getRecipeManager();
            GTRecipeMapLoader.reload(manager);
            lastTagsLoadedManager = manager;
        }
    }
}
