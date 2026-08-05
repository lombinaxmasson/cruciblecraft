package com.masson.cruciblecraft.recipe.gt;

/** Pure event-policy decisions, separated from NeoForge event plumbing. */
public final class GTRecipeReloadDecision {
    private GTRecipeReloadDecision() {}

    public static boolean onServerTags(boolean serverDataLoad, boolean serverAvailable) {
        return serverDataLoad && serverAvailable;
    }

    public static boolean onServerStarted(Object manager, Object managerReloadedFromTags) {
        return manager != managerReloadedFromTags;
    }

    public static boolean onClientRecipesUpdated(
            Object manager,
            boolean integratedServerAvailable) {
        return manager != null && !integratedServerAvailable;
    }
}
