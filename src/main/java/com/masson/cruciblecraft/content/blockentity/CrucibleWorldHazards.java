package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** GT6 smeltery/crucible gas, fire and meltdown world effects. */
public final class CrucibleWorldHazards {
    public static final int SMALL_GAS_RANGE = 3;
    public static final int LARGE_GAS_RANGE = 5;
    public static final long TOO_HOT_TO_PICK_UP_KELVIN = 1300L;
    /** GT6 {@code mTemperature + 100 > getTemperatureMax}. */
    public static final float NEAR_MELTDOWN_SLACK = 100.0F;
    public static final int NEAR_MELTDOWN_LIGHT = 15;

    private CrucibleWorldHazards() {}

    public static boolean tooHotToPickUp(float celsius) {
        return TemperatureDamage.kelvin(celsius) >= TOO_HOT_TO_PICK_UP_KELVIN;
    }

    public static boolean nearMeltdown(float temperature, float maxTemperature) {
        return temperature + NEAR_MELTDOWN_SLACK > maxTemperature;
    }

    /**
     * GT6 smeltery / large-crucible shell: {@code R*2+50}, {@code G*2+50},
     * {@code B/2+50}, then the texture is drawn glowing.
     */
    public static int meltDownTint(int argb) {
        int red = Math.min(255, ((argb >> 16) & 0xFF) * 2 + 50);
        int green = Math.min(255, ((argb >> 8) & 0xFF) * 2 + 50);
        int blue = Math.min(255, ((argb) & 0xFF) / 2 + 50);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public static void refreshAppearance(Level level, BlockPos pos) {
        if (level == null) {
            return;
        }
        level.getLightEngine().checkBlock(pos);
        if (level.isClientSide) {
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
        }
    }

    public static void refreshLargeCrucibleAppearance(Level level, BlockPos controller) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    refreshAppearance(level, controller.offset(dx, dy, dz));
                }
            }
        }
    }

    public static BlockState meltdownLavaState() {
        return ModBlocks.MELTDOWN_LAVA.get().fullState();
    }

    /**
     * GT6 mold, basin, faucet, smelting crucible and large crucible meltdowns
     * all place vanilla flowing lava (metadata 1), which runs downhill and
     * disappears. A full finite lava source fountains upward.
     */
    public static boolean moldMeltsDown(
            float temperature, double boilingCelsius, float moldMaxCelsius) {
        return temperature > boilingCelsius || temperature > moldMaxCelsius;
    }

    public static BlockState moldMeltdownState() {
        return Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 1);
    }

    public static void placeMoldMeltdown(Level level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.LAVA_EXTINGUISH,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        level.setBlock(pos, moldMeltdownState(), Block.UPDATE_ALL);
    }

    public static void boilHazards(
            Level level, BlockPos pos, float celsius, int range, int flameAttempts) {
        damageNearby(level, pos, celsius, range, range == LARGE_GAS_RANGE ? 4.0F : 2.0F);
        igniteAround(level, pos, range, flameAttempts);
    }

    public static void damageNearby(
            Level level, BlockPos pos, float celsius, int range, float multiplier) {
        AABB box = new AABB(pos).inflate(range).expandTowards(0.0, range, 0.0);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box)) {
            TemperatureDamage.apply(living, celsius, multiplier, Float.MAX_VALUE);
        }
    }

    public static void igniteAround(Level level, BlockPos pos, int range, int attempts) {
        if (attempts <= 0) {
            return;
        }
        var random = level.random;
        int span = 2 * range + 1;
        for (int i = 0; i < attempts; i++) {
            BlockPos fireAt = pos.offset(
                    random.nextInt(span) - range,
                    random.nextInt(range + 2) - 1,
                    random.nextInt(span) - range);
            if (level.getBlockState(fireAt).isAir()
                    && Blocks.FIRE.defaultBlockState().canSurvive(level, fireAt)
                    && random.nextInt(3) != 0) {
                level.setBlock(fireAt, Blocks.FIRE.defaultBlockState(), 3);
            }
        }
    }
}
