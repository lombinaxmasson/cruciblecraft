package com.masson.cruciblecraft.fluid;

import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;

/** Bootstrap-free mapping from insertion outcomes to player-facing messages. */
public final class CrucibleInteractionMessages {
    private CrucibleInteractionMessages() {}

    public static String key(InsertResult result) {
        return switch (result) {
            case FULL -> "message.cruciblecraft.crucible_full";
            case TIER_TOO_LOW -> "message.cruciblecraft.crucible_tier_too_low";
            case INEXACT_DECOMPOSITION -> "message.cruciblecraft.inexact_material_amount";
            case INVALID_MATERIAL -> "message.cruciblecraft.invalid_material";
            case SUCCESS -> "message.cruciblecraft.material_inserted";
        };
    }
}
