package com.masson.cruciblecraft.recipe.gt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GTRecipeReloadEpochTest {
    @BeforeEach
    void resetCoordinator() {
        GTRecipeReloadCoordinator.resetForTest();
        RecipeLoadLog.resetForTest();
    }

    @Test
    void tagsThenServerStartedPublishOnceForTheSameManager() {
        Object manager = new Object();
        var tags = GTRecipeReloadCoordinator.identify(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.TAGS_UPDATED);
        assertTrue(GTRecipeReloadCoordinator.decide(tags).publish());
        GTRecipeReloadCoordinator.markPublished(tags);

        var started = GTRecipeReloadCoordinator.identify(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.SERVER_STARTED);
        GTRecipeReloadCoordinator.Decision second =
                GTRecipeReloadCoordinator.decide(started);
        assertFalse(second.publish());
        assertEquals("same_generation", second.reason());
        assertEquals(1, GTRecipeReloadCoordinator.publicationCount());
        assertEquals(2, GTRecipeReloadCoordinator.requestCount());
        assertEquals(1, second.suppressedCount());
    }

    @Test
    void duplicateTagsOnTheSameManagerAreSuppressed() {
        Object manager = new Object();
        var first = GTRecipeReloadCoordinator.identify(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.TAGS_UPDATED);
        assertTrue(GTRecipeReloadCoordinator.decide(first).publish());
        GTRecipeReloadCoordinator.markPublished(first);

        var duplicate = GTRecipeReloadCoordinator.identify(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.TAGS_UPDATED);
        GTRecipeReloadCoordinator.Decision second =
                GTRecipeReloadCoordinator.decide(duplicate);
        assertFalse(second.publish());
        assertEquals("same_generation", second.reason());
        assertEquals(1, GTRecipeReloadCoordinator.publicationCount());
    }

    @Test
    void staleGenerationCannotOverwriteANewerPublication() {
        Object manager = new Object();
        var current = GTRecipeReloadCoordinator.identify(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.TAGS_UPDATED);
        assertTrue(GTRecipeReloadCoordinator.decide(current).publish());
        GTRecipeReloadCoordinator.markPublished(current);

        var stale = new GTRecipeReloadCoordinator.RequestIdentity(
                manager,
                current.dataGeneration() - 1,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.SERVER_STARTED);
        GTRecipeReloadCoordinator.Decision decision =
                GTRecipeReloadCoordinator.decide(stale);
        assertFalse(decision.publish());
        assertEquals("stale_generation", decision.reason());
        assertEquals(1, GTRecipeReloadCoordinator.publicationCount());
    }

    @Test
    void aDifferentManagerPublishesANewGeneration() {
        var first = GTRecipeReloadCoordinator.identify(
                new Object(),
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.TAGS_UPDATED);
        assertTrue(GTRecipeReloadCoordinator.decide(first).publish());
        GTRecipeReloadCoordinator.markPublished(first);

        var second = GTRecipeReloadCoordinator.identify(
                new Object(),
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.SERVER_STARTED);
        assertTrue(second.dataGeneration() > first.dataGeneration());
        assertTrue(GTRecipeReloadCoordinator.decide(second).publish());
    }

    @Test
    void dedicatedClientSideIsIndependentOfServerPublication() {
        Object manager = new Object();
        var server = GTRecipeReloadCoordinator.identify(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.SERVER,
                GTRecipeReloadCoordinator.Cause.TAGS_UPDATED);
        assertTrue(GTRecipeReloadCoordinator.decide(server).publish());
        GTRecipeReloadCoordinator.markPublished(server);

        var client = GTRecipeReloadCoordinator.identify(
                manager,
                ExtruderRecipeFamilyProvider.RuntimeSide.DEDICATED_CLIENT,
                GTRecipeReloadCoordinator.Cause.CLIENT_RECIPES_UPDATED);
        assertTrue(GTRecipeReloadCoordinator.decide(client).publish());
    }
}
