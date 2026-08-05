package com.masson.cruciblecraft.energy.cable;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Applies the pinned GT6 wattage-tier contact damage formula. */
public final class ElectricalShock {
    public static final ResourceKey<DamageType> ELECTRICITY =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID, "electricity"));

    private ElectricalShock() {}

    public static boolean apply(Level level, Entity entity, long wattage) {
        float damage = GT6VoltageTiers.contactDamage(wattage);
        return damage > 0.0F
                && entity.hurt(
                        level.damageSources().source(ELECTRICITY),
                        damage);
    }
}
