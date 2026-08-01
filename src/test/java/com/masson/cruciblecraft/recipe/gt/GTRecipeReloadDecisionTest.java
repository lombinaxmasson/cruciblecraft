package com.masson.cruciblecraft.recipe.gt;

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
    void clientRequiresARecipeManager() {
        assertTrue(GTRecipeReloadDecision.onClientRecipesUpdated(new Object()));
        assertFalse(GTRecipeReloadDecision.onClientRecipesUpdated(null));
    }
}
