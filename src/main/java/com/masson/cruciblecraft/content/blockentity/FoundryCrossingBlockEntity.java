package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.content.mold.MoldHost;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityCrossing}: no melt buffer. Routes
 * {@code fillMoldAtSide} to other horizontal crucibles.
 */
public final class FoundryCrossingBlockEntity extends BlockEntity implements CruciblePour {
    private static boolean routing;
    private static long generation;
    private long seenGeneration;

    public FoundryCrossingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_CROSSING.get(), pos, state);
    }

    @Override
    public boolean fillMoldAtSide(MoldHost mold, Direction crucibleSide, Direction moldSide) {
        if (level == null || mold == null) {
            return false;
        }
        if (routing) {
            if (seenGeneration == generation) {
                return false;
            }
            seenGeneration = generation;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (side == crucibleSide) {
                    continue;
                }
                CruciblePour neighbor = CruciblePour.at(level, worldPosition.relative(side));
                if (neighbor != null
                        && neighbor.fillMoldAtSide(mold, side.getOpposite(), moldSide)) {
                    return true;
                }
            }
            return false;
        }
        generation++;
        routing = true;
        try {
            return fillMoldAtSide(mold, crucibleSide, moldSide);
        } finally {
            routing = false;
        }
    }
}
