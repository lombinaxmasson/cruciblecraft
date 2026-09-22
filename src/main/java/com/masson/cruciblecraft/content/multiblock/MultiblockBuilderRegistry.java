package com.masson.cruciblecraft.content.multiblock;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Explicit runtime registry for builder-wand structure adapters.
 *
 * <p>JSON geometry and Java-defined structures have separate adapters, but
 * share one resolution and execution entry point.</p>
 */
public final class MultiblockBuilderRegistry {
    private static final List<MultiblockBuilderAdapter> ADAPTERS = List.of(
            new JsonMultiblockBuilderAdapter(),
            new SpecialMultiblockBuilderAdapter());

    private MultiblockBuilderRegistry() {}

    public static Optional<MultiblockBuilderTarget> resolve(
            Level level,
            BlockPos clicked) {
        for (MultiblockBuilderAdapter adapter : ADAPTERS) {
            Optional<MultiblockBuilderTarget> target =
                    adapter.resolve(level, clicked);
            if (target.isPresent()) {
                return target;
            }
        }
        return Optional.empty();
    }

    public static List<MultiblockBuilderAdapter> adapters() {
        return ADAPTERS;
    }
}
