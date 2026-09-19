package com.masson.cruciblecraft.content.mold;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * GT6 {@code ITileEntityCrucible.fillMoldAtSide}: pour the first molten
 * material that the mold will accept.
 */
public interface CruciblePour {
    boolean fillMoldAtSide(MoldHost mold, Direction crucibleSide, Direction moldSide);

    /**
     * Direct crucible BE, or the large-crucible controller behind a
     * middle-layer dummy casing (GT6 {@code ONLY_CRUCIBLE} parts).
     */
    @Nullable
    static CruciblePour at(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CruciblePour pour) {
            return pour;
        }
        return LargeCrucibleBlockEntity.pourHostAtWall(level, pos);
    }
}
