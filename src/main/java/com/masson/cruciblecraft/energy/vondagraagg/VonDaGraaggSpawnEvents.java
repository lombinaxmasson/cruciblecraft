package com.masson.cruciblecraft.energy.vondagraagg;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/** GT6 spawn inhibitor except mossy cobble. */
@EventBusSubscriber(modid = CrucibleCraft.MODID)
public final class VonDaGraaggSpawnEvents {
    private VonDaGraaggSpawnEvents() {}

    @SubscribeEvent
    public static void inhibitPosition(MobSpawnEvent.PositionCheck event) {
        if (event.getResult() == MobSpawnEvent.PositionCheck.Result.FAIL) {
            return;
        }
        LevelAccessor level = event.getLevel();
        BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        if (inhibited(level, pos)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void inhibitPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getResult() == MobSpawnEvent.SpawnPlacementCheck.Result.FAIL) {
            return;
        }
        if (inhibited(event.getLevel(), event.getPos())) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    private static boolean inhibited(LevelAccessor level, BlockPos pos) {
        for (VonDaGraaggBlockEntity graagg : VonDaGraaggBlockEntity.all()) {
            if (graagg.getLevel() == level && graagg.inhibits(graagg.getLevel(), pos)) {
                return true;
            }
        }
        return false;
    }
}
