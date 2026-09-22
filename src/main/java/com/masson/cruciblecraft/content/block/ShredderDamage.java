package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** GT6 {@code DamageSourceShredder}: 5 damage while the formed shredder runs. */
public final class ShredderDamage {
    public static final ResourceKey<DamageType> SHREDDER =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID, "shredder"));
    public static final float AMOUNT = 5.0F;

    private ShredderDamage() {}

    public static boolean apply(Level level, LivingEntity entity) {
        return entity.hurt(
                level.damageSources().source(SHREDDER), AMOUNT);
    }
}
