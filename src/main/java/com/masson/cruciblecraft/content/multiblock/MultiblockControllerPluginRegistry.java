package com.masson.cruciblecraft.content.multiblock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/**
 * Explicit whitelist of multiblock controller plugin ids. Registration is
 * code-side with a disk mirror (data/cruciblecraft/multiblock_plugins.json);
 * tests assert the two sets are bidirectionally equal. Unknown ids fail
 * closed via {@link #require}; duplicates are rejected at registration.
 */
public final class MultiblockControllerPluginRegistry {
    private static final Map<ResourceLocation, MultiblockControllerPlugin>
            PLUGINS = new LinkedHashMap<>();

    public static void register(MultiblockControllerPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        ResourceLocation id = Objects.requireNonNull(
                plugin.id(), "plugin id");
        if (PLUGINS.putIfAbsent(id, plugin) != null) {
            throw new IllegalStateException(
                    "Duplicate multiblock controller plugin id: " + id);
        }
    }

    /** Fail closed: an unknown plugin id is an error, never a silent no-op. */
    public static MultiblockControllerPlugin require(ResourceLocation id) {
        MultiblockControllerPlugin plugin = PLUGINS.get(id);
        if (plugin == null) {
            throw new IllegalStateException(
                    "Unknown multiblock controller plugin (fail closed): "
                            + id);
        }
        return plugin;
    }

    public static boolean known(ResourceLocation id) {
        return PLUGINS.containsKey(id);
    }

    /** Immutable whitelist snapshot. */
    public static Map<ResourceLocation, MultiblockControllerPlugin>
            whitelist() {
        return Map.copyOf(PLUGINS);
    }

    private MultiblockControllerPluginRegistry() {}
}
