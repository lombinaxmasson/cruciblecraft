package com.masson.cruciblecraft.content.mold;

import net.minecraft.core.Direction;

/**
 * GT6 {@code ITileEntityCrucible.fillMoldAtSide}: pour the first molten
 * material that the mold will accept.
 */
public interface CruciblePour {
    boolean fillMoldAtSide(MoldHost mold, Direction crucibleSide, Direction moldSide);
}
