package com.masson.cruciblecraft.content.blockentity;

import java.util.LinkedHashMap;
import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore;
import com.masson.cruciblecraft.material.MaterialCatalog;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * GT6 {@code onEntityCollidedWithBlock} / {@code onWalkOver2}: a kill inside
 * a hot crucible yields catalog materials, never vanilla stand-ins.
 */
public final class CrucibleEntityMelts {
    private CrucibleEntityMelts() {}

    public static boolean tryMelt(
            CrucibleProcessCore process, Entity entity, float environmentCelsius) {
        if (!(entity instanceof LivingEntity living)
                || living.isAlive()
                || process.frozen()) {
            return false;
        }
        Map<String, Integer> additions = meltsOf(living.getType());
        if (additions.isEmpty()) {
            return false;
        }
        float temperature = meltTemperature(living.getType(), environmentCelsius);
        int room = process.maxUnits() - process.totalUnits();
        if (room <= 0) {
            return false;
        }
        Map<String, Integer> accepted = new LinkedHashMap<>();
        int remaining = room;
        for (var entry : additions.entrySet()) {
            if (!MaterialCatalog.contains(entry.getKey()) || remaining <= 0) {
                continue;
            }
            int units = Math.min(entry.getValue(), remaining);
            if (units > 0) {
                accepted.put(entry.getKey(), units);
                remaining -= units;
            }
        }
        if (accepted.isEmpty()) {
            return false;
        }
        process.applyAdditions(accepted, temperature);
        return true;
    }

    private static float meltTemperature(EntityType<?> type, float environmentCelsius) {
        if (type == EntityType.SNOW_GOLEM) {
            return -10.0F;
        }
        if (type == EntityType.IRON_GOLEM
                || type == EntityType.SKELETON
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.ZOMBIE) {
            return environmentCelsius;
        }
        if (type == EntityType.CREEPER || type == EntityType.ENDERMAN) {
            return 20.0F;
        }
        return 37.0F;
    }

    private static Map<String, Integer> meltsOf(EntityType<?> type) {
        int ingot = MaterialPrefixes.INGOT.units();
        if (type == EntityType.VILLAGER || type == EntityType.WITCH) {
            return Map.of("soylent_green", 2 * ingot);
        }
        if (type == EntityType.SNOW_GOLEM) {
            return Map.of("snow", 4 * ingot);
        }
        if (type == EntityType.IRON_GOLEM) {
            return Map.of("iron", 4 * ingot);
        }
        if (type == EntityType.WITHER_SKELETON) {
            return Map.of("bone", ingot, "coal", ingot);
        }
        if (type == EntityType.SKELETON) {
            return Map.of("bone", ingot);
        }
        if (type == EntityType.ZOMBIE) {
            return Map.of("meat_rotten", ingot);
        }
        if (type == EntityType.MOOSHROOM
                || type == EntityType.COW
                || type == EntityType.HORSE) {
            return Map.of("meat_raw", 3 * ingot);
        }
        if (type == EntityType.PIG
                || type == EntityType.SHEEP
                || type == EntityType.WOLF
                || type == EntityType.SQUID) {
            return Map.of("meat_raw", 2 * ingot);
        }
        if (type == EntityType.CHICKEN
                || type == EntityType.OCELOT
                || type == EntityType.CAT
                || type == EntityType.SPIDER
                || type == EntityType.SILVERFISH) {
            return Map.of("meat_raw", ingot);
        }
        if (type == EntityType.CREEPER) {
            return Map.of("gunpowder", ingot);
        }
        if (type == EntityType.ENDERMAN) {
            return Map.of("ender_pearl", ingot);
        }
        return Map.of();
    }
}
