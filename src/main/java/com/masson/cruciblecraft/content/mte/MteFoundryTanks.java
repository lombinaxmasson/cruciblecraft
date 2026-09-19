package com.masson.cruciblecraft.content.mte;

import com.masson.cruciblecraft.fluid.MoltenTransferMath;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore;

/**
 * GT6 foundry amounts in CC units: smeltery {@code 16*U}, mold one ingot,
 * basin {@code OP.blockSolid = 9*U}, crossing has no melt buffer. These are
 * not dummy {@code IFluidHandler} tanks.
 */
public final class MteFoundryTanks {
    public static final int SMELTERY_MB =
            CrucibleProcessCore.SINGLE_BLOCK_MAX_INGOTS
                    * MoltenTransferMath.MILLIBUCKETS_PER_INGOT;
    public static final int MOLD_MB = MoltenTransferMath.MILLIBUCKETS_PER_INGOT;
    public static final int BASIN_MB = 9 * MoltenTransferMath.MILLIBUCKETS_PER_INGOT;
    public static final int CROSSING_MB = 0;

    private MteFoundryTanks() {}

    public static int capacityMb(MteInPlaceSpec spec) {
        if (spec == null || spec.kind() != MteInPlaceKind.CRUCIBLE_FOUNDRY) {
            return 0;
        }
        return capacityMb(spec.gt6Class());
    }

    public static int capacityMb(String gt6Class) {
        if (gt6Class == null) {
            return 0;
        }
        if (gt6Class.contains("Smeltery")) {
            return SMELTERY_MB;
        }
        // Overlay class strings for basins are "MultiTileEntityBasin / Molds".
        if (gt6Class.contains("Basin")) {
            return BASIN_MB;
        }
        if (gt6Class.contains("Crossing")) {
            return CROSSING_MB;
        }
        if (gt6Class.contains("Mold")) {
            return MOLD_MB;
        }
        return 0;
    }
}
