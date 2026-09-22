package com.masson.cruciblecraft.api.tileentity;

import net.minecraft.core.Direction;

/**
 * CC's NeoForge-side equivalent of GT6's {@code ITileEntityProgress}.
 *
 * <p>NeoForge has no generic progress capability. The side is retained because
 * GT6 exposes progress through a side-aware contract, even when a particular
 * host ignores it.
 */
public interface ProgressHost {
    /** Progress currently completed; implementations must return a non-negative value. */
    long progressValue(Direction side);

    /** Progress required for the current job; implementations should return a positive value. */
    long progressMax(Direction side);
}
