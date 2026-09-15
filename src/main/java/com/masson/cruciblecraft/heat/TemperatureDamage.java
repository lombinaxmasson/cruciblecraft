package com.masson.cruciblecraft.heat;

import com.masson.cruciblecraft.nuclear.ReactorHazards;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * GT6 {@code UT.Entities.applyTemperatureDamage} with CC Celsius storage.
 * Thresholds stay in Kelvin: heat above 320 K, frost below 260 K.
 */
public final class TemperatureDamage {
    public static final float KELVIN_OFFSET = 273.15F;

    private TemperatureDamage() {}

    public static long kelvin(float celsius) {
        if (!Float.isFinite(celsius)) {
            return 0L;
        }
        return Math.max(0L, Math.round(celsius + KELVIN_OFFSET));
    }

    public static boolean apply(Entity entity, float celsiusCelsius) {
        return apply(entity, celsiusCelsius, 1.0F, Float.MAX_VALUE);
    }

    public static boolean apply(
            Entity entity, float celsius, float multiplier, float cap) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        long kelvin = kelvin(celsius);
        if (kelvin > 320L) {
            float damage = (multiplier * (kelvin - 300L)) / 50.0F;
            return hurtHeat(living, capped(damage, cap));
        }
        if (kelvin < 260L) {
            float damage = (multiplier * (270L - kelvin)) / 25.0F;
            return hurtFrost(living, capped(damage, cap));
        }
        return false;
    }

    private static float capped(float damage, float cap) {
        if (!Float.isFinite(cap) || cap == Float.MAX_VALUE) {
            return Math.max(0.0F, damage);
        }
        return Math.max(1.0F, Math.min(cap, damage));
    }

    private static boolean hurtHeat(LivingEntity entity, float damage) {
        return ReactorHazards.applyHeatDamage(entity, damage);
    }

    private static boolean hurtFrost(LivingEntity entity, float damage) {
        if (damage <= 0.0F || entity.level().isClientSide) {
            return false;
        }
        if (entity instanceof Player player && player.isCreative()) {
            return false;
        }
        return entity.hurt(entity.damageSources().freeze(), damage);
    }
}
