package com.masson.cruciblecraft.content.block;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * GT6 {@code BlockBaseFluid} bathing / breathing lists from
 * {@code Loader_Blocks}. Immersive Engineering flammable / sticky / slippery
 * potion IDs are not registered here; blindness, poison, nausea, regeneration,
 * resistance, drown, and fire-refresh still apply.
 */
public final class SpringLiquidContact {
    public static final SpringLiquidContact OIL = new SpringLiquidContact(
            List.of(effect(MobEffects.BLINDNESS, 60, 1)),
            List.of(
                    effect(MobEffects.POISON, 300, 0),
                    effect(MobEffects.CONFUSION, 120, 0)),
            true,
            true);
    public static final SpringLiquidContact GAS = new SpringLiquidContact(
            List.of(),
            List.of(
                    effect(MobEffects.POISON, 300, 0),
                    effect(MobEffects.CONFUSION, 120, 0)),
            true,
            false);
    public static final SpringLiquidContact GEOTHERMAL = new SpringLiquidContact(
            List.of(
                    effect(MobEffects.REGENERATION, 100, 0),
                    effect(MobEffects.DAMAGE_RESISTANCE, 2400, 2)),
            List.of(),
            false,
            false);

    private static final int FLAMMABLE_FIRE_TICKS = 300;

    private final List<MobEffectInstance> bathing;
    private final List<MobEffectInstance> breathing;
    private final boolean drown;
    private final boolean flammable;

    private SpringLiquidContact(
            List<MobEffectInstance> bathing,
            List<MobEffectInstance> breathing,
            boolean drown,
            boolean flammable) {
        this.bathing = bathing;
        this.breathing = breathing;
        this.drown = drown;
        this.flammable = flammable;
    }

    void apply(Block block, Level level, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        for (MobEffectInstance effect : bathing) {
            living.addEffect(copy(effect));
        }
        if (flammable && living.isOnFire()) {
            living.setRemainingFireTicks(
                    Math.max(living.getRemainingFireTicks(), FLAMMABLE_FIRE_TICKS));
        }
        BlockPos eyes = BlockPos.containing(
                entity.getX(), entity.getEyeY(), entity.getZ());
        if (!level.getBlockState(eyes).is(block)) {
            return;
        }
        for (MobEffectInstance effect : breathing) {
            living.addEffect(copy(effect));
        }
        if (drown && level.getGameTime() % 20L == 0L) {
            living.hurt(level.damageSources().drown(), 2.0F);
        }
    }

    private static MobEffectInstance effect(
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> id,
            int duration,
            int amplifier) {
        return new MobEffectInstance(id, duration, amplifier, false, true);
    }

    private static MobEffectInstance copy(MobEffectInstance source) {
        return new MobEffectInstance(source);
    }
}
