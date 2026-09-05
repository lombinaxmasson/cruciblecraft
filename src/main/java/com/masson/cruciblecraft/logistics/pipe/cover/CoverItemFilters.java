package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code ST.move} filter: an empty filter matches everything and ignores
 * invert; a present filter XOR invert.
 */
public final class CoverItemFilters {
    private CoverItemFilters() {}

    public static boolean matches(
            Optional<String> matchId,
            boolean invert,
            ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return matches(
                matchId,
                invert,
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static boolean matches(
            Optional<String> matchId,
            boolean invert,
            String actualId) {
        if (matchId == null || matchId.isEmpty()) {
            return true;
        }
        boolean match = matchId.orElseThrow().equals(actualId);
        return invert ? !match : match;
    }

    public static boolean inverted(PipeCoverConfig config) {
        return config != null && config.invert().orElse(0) != 0;
    }
}
