package com.masson.cruciblecraft.logistics.pipe;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Level-local generation used to invalidate GTM-style route caches.
 *
 * <p>A topology or cover mutation bumps one coarse level generation in O(1).
 * This deliberately invalidates more item routes than a component-local
 * generation, but avoids an O(n) component walk on every placed or removed
 * pipe and never retains block positions. Fluid movement remains local.
 */
public final class PipeTopology {
    private static final Map<Level, Long> VERSIONS = new WeakHashMap<>();

    private PipeTopology() {}

    public static synchronized long version(
            Level level, BlockPos ignoredPosition) {
        return VERSIONS.getOrDefault(level, 0L);
    }

    public static synchronized long invalidate(
            Level level, BlockPos ignoredPosition) {
        long next = Math.addExact(
                VERSIONS.getOrDefault(level, 0L), 1L);
        VERSIONS.put(level, next);
        return next;
    }
}
