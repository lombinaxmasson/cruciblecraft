package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;

/** GT6 {@code MultiTileEntityMoldCoinage}: anvil-stamp coins, not smeltery. */
public final class CoinageMoldHosts {
    public static final String REGISTRY_PATH = "misc_tool/coinage_mold";

    private CoinageMoldHosts() {}

    public static boolean isCoinage(MteInPlaceSpec spec) {
        return spec != null
                && spec.kind() == MteInPlaceKind.MISC_TOOL
                && (REGISTRY_PATH.equals(spec.registryPath())
                        || spec.gt6Class().contains("MultiTileEntityMoldCoinage"));
    }
}
