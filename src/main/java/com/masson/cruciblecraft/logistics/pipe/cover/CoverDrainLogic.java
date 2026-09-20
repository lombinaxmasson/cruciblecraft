package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * GT6 {@code CoverDrain}: rain on top/horizontal faces, plus source water
 * and lava in front of the cover. XP/sewage walk-over stays out until those
 * fluids exist as live CC fluids.
 */
public final class CoverDrainLogic {
    private CoverDrainLogic() {}

    public static boolean isDrain(PipeCover cover) {
        return cover != null
                && "cover_drain".equals(cover.definitionId().getPath());
    }

    public static void tick(
            Level level,
            BlockPos pos,
            Direction side,
            boolean stopped,
            BiFunction<FluidStack, IFluidHandler.FluidAction, Integer> fill) {
        if (level == null
                || level.isClientSide
                || pos == null
                || side == null
                || stopped
                || fill == null) {
            return;
        }
        long time = level.getGameTime();
        if (time % 100 == 10 && side != Direction.DOWN) {
            collectRain(level, pos, side, fill);
        }
        if (time % 20 == 5) {
            collectBlock(level, pos, side, fill);
        }
    }

    private static void collectRain(
            Level level,
            BlockPos pos,
            Direction side,
            BiFunction<FluidStack, IFluidHandler.FluidAction, Integer> fill) {
        BlockPos front = pos.relative(side);
        if (!level.isRainingAt(front)
                && !(side == Direction.UP && level.isRainingAt(pos.above()))) {
            return;
        }
        Biome biome = level.getBiome(front).value();
        if (biome.getBaseTemperature() < 0.2F || !biome.hasPrecipitation()) {
            return;
        }
        BlockState inFront = level.getBlockState(front);
        if (!inFront.getFluidState().isEmpty() || inFront.isSolidRender(level, front)) {
            return;
        }
        int amount = Math.max(1, Math.round(biome.getModifiedClimateSettings().downfall() * 10_000));
        if (level.isThundering()) {
            amount *= 2;
        }
        fill.apply(
                new FluidStack(Fluids.WATER, amount),
                IFluidHandler.FluidAction.EXECUTE);
    }

    private static void collectBlock(
            Level level,
            BlockPos pos,
            Direction side,
            BiFunction<FluidStack, IFluidHandler.FluidAction, Integer> fill) {
        BlockPos front = pos.relative(side);
        var fluid = level.getFluidState(front);
        if (fluid.isEmpty() || !fluid.isSource()) {
            return;
        }
        boolean water = fluid.is(Fluids.WATER);
        boolean lava = fluid.is(Fluids.LAVA);
        if (!water && !lava) {
            return;
        }
        if (side == Direction.UP && lava) {
            return;
        }
        if (side == Direction.DOWN && water) {
            return;
        }
        FluidStack stack = new FluidStack(water ? Fluids.WATER : Fluids.LAVA, 1000);
        if (fill.apply(stack, IFluidHandler.FluidAction.SIMULATE) < 1000) {
            return;
        }
        if (fill.apply(stack, IFluidHandler.FluidAction.EXECUTE) < 1000) {
            return;
        }
        if (water && isInfiniteWater(level, front)) {
            return;
        }
        level.setBlock(front, Blocks.AIR.defaultBlockState(), LiquidBlock.UPDATE_ALL);
    }

    private static boolean isInfiniteWater(Level level, BlockPos pos) {
        int sources = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (level.getFluidState(pos.relative(side)).is(Fluids.WATER)
                    && level.getFluidState(pos.relative(side)).isSource()) {
                sources++;
            }
        }
        return sources >= 2;
    }
}
