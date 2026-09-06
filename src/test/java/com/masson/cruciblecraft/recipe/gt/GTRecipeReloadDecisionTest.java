package com.masson.cruciblecraft.recipe.gt;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GTRecipeReloadDecisionTest {
    @Test
    void serverTagsRequireServerDataAndAvailableServer() {
        assertTrue(GTRecipeReloadDecision.onServerTags(true, true));
        assertFalse(GTRecipeReloadDecision.onServerTags(false, true));
        assertFalse(GTRecipeReloadDecision.onServerTags(true, false));
    }

    @Test
    void serverStartedSuppressesManagerAlreadyReloadedByTags() {
        Object manager = new Object();
        assertFalse(GTRecipeReloadDecision.onServerStarted(manager, manager));
        assertTrue(GTRecipeReloadDecision.onServerStarted(manager, new Object()));
        assertTrue(GTRecipeReloadDecision.onServerStarted(manager, null));
    }

    @Test
    void clientReloadsOnlyWithoutAnIntegratedServer() {
        assertTrue(GTRecipeReloadDecision.onClientRecipesUpdated(
                new Object(), false));
        assertFalse(GTRecipeReloadDecision.onClientRecipesUpdated(
                new Object(), true));
        assertFalse(GTRecipeReloadDecision.onClientRecipesUpdated(null, false));
    }

    @Test
    void fullReloadPreparationAndPublicationAreSerialized() throws Exception {
        assertTrue(Modifier.isSynchronized(
                GTRecipeMapLoader.class
                        .getDeclaredMethod(
                                "reload",
                                net.minecraft.world.item.crafting.RecipeManager.class)
                        .getModifiers()));
        assertTrue(Modifier.isSynchronized(
                GTRecipeMapLoader.class
                        .getDeclaredMethod(
                                "reload",
                                net.minecraft.world.item.crafting.RecipeManager.class,
                                ExtruderRecipeFamilyProvider.RuntimeSide.class,
                                GTRecipeReloadCoordinator.Cause.class)
                        .getModifiers()));
    }
}
