package com.masson.cruciblecraft.energy.steam;

import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.energy.cable.GT6VoltageTiers;

import net.minecraft.world.entity.LivingEntity;

/**
 * GT6 {@code MultiTileEntityTurbineSteam} overlays, {@code onWalkOver2},
 * and converter {@code mExplosionPrevention} then {@code overcharge}.
 */
public final class SteamTurbinePresentation {
    public static final int OVERLOAD_EXPLOSION_THRESHOLD = 100;

    private SteamTurbinePresentation() {}

    public static boolean single(MteInPlaceSpec spec) {
        return spec != null
                && spec.kind() == MteInPlaceKind.STEAM_TURBINE
                && SteamTurbineCatalog.find(spec.id())
                        .map(profile -> !profile.large())
                        .orElse(false);
    }

    public static boolean large(MteInPlaceSpec spec) {
        return spec != null
                && spec.kind() == MteInPlaceKind.STEAM_TURBINE
                && SteamTurbineCatalog.find(spec.id())
                        .map(SteamTurbineCatalog.Profile::large)
                        .orElse(false);
    }

    public static int yawDelta(boolean counterclockwise, boolean fast) {
        return (counterclockwise ? -5 : 5) * (fast ? 2 : 1);
    }

    public static void spinWalker(
            LivingEntity entity, boolean counterclockwise, boolean fast) {
        float delta = yawDelta(counterclockwise, fast);
        entity.setYRot(entity.getYRot() + delta);
        entity.setYHeadRot(entity.getYHeadRot() + delta);
    }

    /** GT6 {@code overcharge(aSize, RU)}: RU is in {@code ALL_EXPLODING}. */
    public static float overchargeExplosionStrength(long size) {
        return GT6VoltageTiers.tierMax(size);
    }
}
