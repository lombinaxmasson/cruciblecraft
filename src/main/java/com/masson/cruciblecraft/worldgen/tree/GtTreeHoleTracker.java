package com.masson.cruciblecraft.worldgen.tree;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;

/**
 * World-scoped rubber resin-hole list. GT6 uses a global
 * {@code sListResinHoles}; CC keys it to the server level so GameTests and
 * concurrent worlds do not share the 256-block nearby check.
 */
public final class GtTreeHoleTracker {
    private static final Map<Object, Set<Long>> HOLES = new WeakHashMap<>();
    private static final int NEARBY_XZ = 256;

    private GtTreeHoleTracker() {}

    public static void add(LevelAccessor level, BlockPos pos) {
        holes(level).add(BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ()));
    }

    public static void remove(LevelAccessor level, BlockPos pos) {
        holes(level).remove(BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ()));
    }

    public static boolean nearby(LevelAccessor level, int x, int z) {
        for (long packed : holes(level)) {
            int holeX = BlockPos.getX(packed);
            int holeZ = BlockPos.getZ(packed);
            if (Math.abs(holeX - x) < NEARBY_XZ && Math.abs(holeZ - z) < NEARBY_XZ) {
                return true;
            }
        }
        return false;
    }

    public static void clear(LevelAccessor level) {
        HOLES.remove(key(level));
    }

    private static Set<Long> holes(LevelAccessor level) {
        Object key = key(level);
        return HOLES.computeIfAbsent(key, ignored -> new HashSet<>());
    }

    private static Object key(LevelAccessor level) {
        if (level instanceof WorldGenLevel worldGen) {
            return worldGen.getLevel();
        }
        return level;
    }
}
