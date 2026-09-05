package com.masson.cruciblecraft.energy.battery;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/** Validated bundled catalog of 37 census storage batteries. */
public final class EnergyBatteryCatalog {
    private static final Map<ResourceLocation, EnergyBatteryProfile> PROFILES =
            loadBundled();

    public static List<EnergyBatteryProfile> profiles() {
        return List.copyOf(PROFILES.values());
    }

    public static EnergyBatteryProfile require(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        EnergyBatteryProfile profile =
                parsed == null ? null : PROFILES.get(parsed);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown energy battery profile " + id);
        }
        return profile;
    }

    public static EnergyBatteryProfile require(ResourceLocation id) {
        EnergyBatteryProfile profile = PROFILES.get(id);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown energy battery profile " + id);
        }
        return profile;
    }

    private static Map<ResourceLocation, EnergyBatteryProfile> loadBundled() {
        LinkedHashMap<ResourceLocation, EnergyBatteryProfile> result =
                new LinkedHashMap<>();
        Set<Integer> sourceIds = new HashSet<>();
        int luCount = 0;
        for (EnergyBatteryTierCatalog.Entry tier
                : EnergyBatteryTierCatalog.entries()) {
            EnergyBatteryKindCatalog.Kind kind =
                    EnergyBatteryKindCatalog.require(tier.kindId());
            EnergyBatteryProfile profile =
                    EnergyBatteryProfile.synthesize(kind, tier);
            if (result.putIfAbsent(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate battery profile " + profile.id());
            }
            if (!sourceIds.add(profile.sourceId())) {
                throw new IllegalStateException(
                        "Duplicate battery source id " + profile.sourceId());
            }
            if (profile.energyType() == EnergyType.LU) {
                luCount++;
            }
        }
        if (result.size() != EnergyBatteryTierCatalog.EXPECTED_SIZE) {
            throw new IllegalStateException(
                    "Energy battery profile count drifted: " + result.size());
        }
        EnergyBatteryProfile lead =
                result.get(ResourceLocation.parse(
                        "cruciblecraft:lead_acid_battery_ulv"));
        EnergyBatteryProfile crystal =
                result.get(ResourceLocation.parse(
                        "cruciblecraft:red_energium_crystal_iv"));
        if (lead == null
                || lead.sourceId() != 14000
                || lead.energyType() != EnergyType.ELECTRIC
                || lead.capacity() != 16_000L
                || crystal == null
                || crystal.sourceId() != 14505
                || crystal.energyType() != EnergyType.LU
                || crystal.capacity() != 3_276_800_000L
                || luCount != 12) {
            throw new IllegalStateException(
                    "Energy battery anchors drifted");
        }
        return Map.copyOf(result);
    }

    private EnergyBatteryCatalog() {}
}
