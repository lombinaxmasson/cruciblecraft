package com.masson.cruciblecraft.energy.converter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityGeneratorLiquid} {@code WD.fire}/{@code WD.burn}
 * front-face and neighbourhood flame.
 */
public final class BurningBoxWorldEffects {
    public static final int FLAME_RANGE = 2;

    private BurningBoxWorldEffects() {}

    public static boolean isFlaming(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
    }

    public static boolean tryAutoIgnite(Level level, BlockPos front) {
        return level.random.nextInt(200) == 0 && isFlaming(level, front);
    }

    public static void trySpreadFlame(
            Level level, BlockPos origin, Integer efficiencyBps) {
        int efficiency = efficiencyBps == null || efficiencyBps < 1
                ? 1
                : efficiencyBps;
        if (level.random.nextInt(efficiency) != 0) {
            return;
        }
        int span = 2 * FLAME_RANGE + 1;
        BlockPos target = origin.offset(
                -FLAME_RANGE + level.random.nextInt(span),
                -1 + level.random.nextInt(2 + FLAME_RANGE),
                -FLAME_RANGE + level.random.nextInt(span));
        tryPlaceFire(level, target);
    }

    public static void burnFront(Level level, BlockPos front) {
        BlockState state = level.getBlockState(front);
        if (state.isAir() || state.liquid()) {
            return;
        }
        if (state.isFlammable(level, front, Direction.UP)
                || state.ignitedByLava()) {
            tryPlaceFire(level, front.above());
        }
    }

    public static boolean tryPlaceFire(Level level, BlockPos pos) {
        BlockState current = level.getBlockState(pos);
        if (!current.isAir() && !current.canBeReplaced()) {
            return false;
        }
        BlockState fire = Blocks.FIRE.defaultBlockState();
        if (!fire.canSurvive(level, pos)) {
            return false;
        }
        return level.setBlock(pos, fire, 3);
    }
}
