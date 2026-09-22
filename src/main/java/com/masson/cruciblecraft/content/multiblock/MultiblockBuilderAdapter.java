package com.masson.cruciblecraft.content.multiblock;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * Runtime adapter for one family of multiblock definitions.
 *
 * <p>The adapter is deliberately separate from the validator. Validators
 * answer whether a structure is formed; builder adapters expose the same
 * geometry as a bounded, inventory-backed repair plan.</p>
 */
public interface MultiblockBuilderAdapter {
    ResourceLocation id();

    Optional<MultiblockBuilderTarget> resolve(
            Level level,
            BlockPos clicked);

    MultiblockBuildPlan plan(
            Level level,
            MultiblockBuilderTarget target);

    /**
     * Gives a controller a chance to perform an immediate server-side
     * structure refresh. Periodic ticking remains the fallback for older
     * controllers.
     */
    default void recheck(
            Level level,
            MultiblockBuilderTarget target) {
        if (level != null && !level.isClientSide) {
            level.updateNeighborsAt(
                    target.controller(),
                    level.getBlockState(target.controller()).getBlock());
        }
    }
}
