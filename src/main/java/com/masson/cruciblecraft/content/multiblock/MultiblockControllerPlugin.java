package com.masson.cruciblecraft.content.multiblock;

import net.minecraft.resources.ResourceLocation;

/**
 * A whitelisted multiblock controller plugin. Structure data only describes
 * values and constraints; transaction, UI, environment and special machine
 * behavior belongs to explicitly whitelisted plugins consumed by controller
 * classes. Every registered plugin must be consumed by at least one real
 * controller — zero-consumer plugins are rejected by the registry tests.
 */
public interface MultiblockControllerPlugin {
    /** Stable, persistable, quarantinable plugin id. */
    ResourceLocation id();
}
