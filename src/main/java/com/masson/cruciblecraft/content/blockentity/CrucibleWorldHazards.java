package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** GT6 smeltery/crucible gas, fire and meltdown world effects. */
public final class CrucibleWorldHazards {
    public static final int SMALL_GAS_RANGE = 3;
    public static final int LARGE_GAS_RANGE = 5;
    public static final long TOO_HOT_TO_PICK_UP_KELVIN = 1300L;

    private CrucibleWorldHazards() {}

    public static boolean tooHotToPickUp(float celsius) {
        return TemperatureDamage.kelvin(celsius) >= TOO_HOT_TO_PICK_UP_KELVIN;
    }

    public static net.minecraft.world.level.block.state.BlockState meltdownLavaState() {
        return ModBlocks.MELTDOWN_LAVA.get().fullState();
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
