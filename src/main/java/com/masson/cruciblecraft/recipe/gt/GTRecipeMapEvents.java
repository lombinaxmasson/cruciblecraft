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
    private GTRecipeMapEvents() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        RecipeManager manager = event.getServer().getRecipeManager();
        // Compact families are aggregated with Extruder inside the loader.
        // Same manager/generation as TagsUpdated is suppressed by the coordinator.
        GTRecipeMapLoader.reload(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.SERVER_STARTED);
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent event) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (GTRecipeReloadDecision.onServerTags(
                event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD,
                server != null)) {
            RecipeManager manager = server.getRecipeManager();
            GTRecipeMapLoader.reload(
                    manager,
                    ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                    GTRecipeReloadCoordinator.Cause.TAGS_UPDATED);
        }
    }
}
