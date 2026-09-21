package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** GT6 {@code DamageSourceCrusher}: 5 damage while the formed crusher runs. */
public final class CrusherDamage {
    public static final ResourceKey<DamageType> CRUSHER =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID, "crusher"));
    public static final float AMOUNT = 5.0F;

    private CrusherDamage() {}

    public static boolean apply(Level level, LivingEntity entity) {
        return entity.hurt(
                level.damageSources().source(CRUSHER), AMOUNT);
    }
}
