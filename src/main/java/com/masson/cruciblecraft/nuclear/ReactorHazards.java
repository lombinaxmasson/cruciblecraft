package com.masson.cruciblecraft.nuclear;

import com.masson.cruciblecraft.content.item.HazmatArmorItem;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** GT6 reactor contact / area / fail-burst radiation and heat. */
public final class ReactorHazards {
    public static final float HEAT_DAMAGE = 5.0F;
    public static final int CONTACT_RADIATION_LEVEL = 3;
    public static final int CONTACT_RADIATION_AMOUNT = 1;
    public static final int AREA_RANGE = 200;
    public static final int FAIL_RANGE = 500;

    private ReactorHazards() {}

    public static int neutronCalc(int neutronSum) {
        return neutronSum / 256;
    }

    public static int bindInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    public static int areaStrength(int neutronSum, int horizontalDistance) {
        return bindInt((long) neutronCalc(neutronSum) - horizontalDistance);
    }

    public static int failStrength(int neutronSum, int horizontalDistance) {
        return bindInt((long) neutronCalc(neutronSum) * 2L - horizontalDistance);
    }

    public static int radioactivityLevel(int strength) {
        if (strength <= 0) {
            return 0;
        }
        return (int) ReactorRodPhysics.divUp(strength, 10L);
    }

    public static void tickContactAndArea(
            Level level, BlockPos pos, boolean running, int neutronSum, long gameTime) {
        if (level.isClientSide) {
            return;
        }
        if (running) {
            AABB box = new AABB(pos);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
                applyHeatDamage(entity, HEAT_DAMAGE);
                applyRadioactivity(entity, CONTACT_RADIATION_LEVEL, CONTACT_RADIATION_AMOUNT);
            }
        }
        if (gameTime % 20L == 10L) {
            applyPulse(level, pos, neutronSum, AREA_RANGE, false);
        }
    }

    public static void applyFailBurst(Level level, BlockPos pos, int neutronSum) {
        if (level.isClientSide) {
            return;
        }
        playExplodeSound(level, pos);
        applyPulse(level, pos, neutronSum, FAIL_RANGE, true);
    }

    public static void playExplodeSound(Level level, BlockPos pos) {
        level.playSound(
                null,
                pos,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
    }

    public static int applyPulse(
            Level level, BlockPos pos, int neutronSum, int range, boolean failBurst) {
        AABB box = new AABB(pos).inflate(range, 256.0, range);
        int applied = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            int dx = (int) Math.floor(entity.getX()) - pos.getX();
            int dz = (int) Math.floor(entity.getZ()) - pos.getZ();
            int distance = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
            int strength = failBurst
                    ? failStrength(neutronSum, distance)
                    : areaStrength(neutronSum, distance);
            if (applyRadioactivity(entity, radioactivityLevel(strength), strength)) {
                applied++;
            }
        }
        return applied;
    }

    public static boolean applyRadioactivity(LivingEntity entity, int level, int amount) {
        if (level <= 0 || amount <= 0) {
            return false;
        }
        if (entity.getType().is(EntityTypeTags.UNDEAD)
                || entity.getType().is(EntityTypeTags.ARTHROPOD)) {
            return false;
        }
        if (entity instanceof Player player && player.isCreative()) {
            return false;
        }
        if (HazmatArmorItem.isWearingFull(entity, HazmatArmorItem.Kind.RADIATION)) {
            return false;
        }
        int duration = Math.max(20, amount * 20);
        int amplifier = Math.max(0, level - 1);
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier));
        entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, amplifier));
        entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, amplifier));
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration, amplifier));
        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, 0));
        return true;
    }

    public static boolean applyHeatDamage(LivingEntity entity, float amount) {
        if (amount <= 0.0F || entity.level().isClientSide) {
            return false;
        }
        if (entity.getType() == EntityType.BLAZE
                || entity.fireImmune()
                || entity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return false;
        }
        if (entity instanceof Player player && player.isCreative()) {
            return false;
        }
        if (HazmatArmorItem.isWearingFull(entity, HazmatArmorItem.Kind.HEAT)) {
            return false;
        }
        return entity.hurt(entity.damageSources().onFire(), amount);
    }
}
