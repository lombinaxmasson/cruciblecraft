package com.masson.cruciblecraft.energy.transformer;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/** Validated bundled catalog of 9 GT6 plus 5 CC_EXTENSION electric transformers. */
public final class EnergyTransformerCatalog {
    private static final Map<ResourceLocation, EnergyTransformerProfile> PROFILES =
            loadBundled();

    public static List<EnergyTransformerProfile> profiles() {
        return List.copyOf(PROFILES.values());
    }

    public static EnergyTransformerProfile require(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        EnergyTransformerProfile profile =
                parsed == null ? null : PROFILES.get(parsed);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown energy transformer profile " + id);
        }
        return profile;
    }

    public static EnergyTransformerProfile require(ResourceLocation id) {
        EnergyTransformerProfile profile = PROFILES.get(id);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown energy transformer profile " + id);
        }
        return profile;
    }

    private static Map<ResourceLocation, EnergyTransformerProfile> loadBundled() {
        LinkedHashMap<ResourceLocation, EnergyTransformerProfile> result =
                new LinkedHashMap<>();
        Set<Integer> sourceIds = new HashSet<>();
        for (EnergyTransformerTierCatalog.Entry tier
                : EnergyTransformerTierCatalog.entries()) {
            EnergyTransformerKindCatalog.Kind kind =
                    EnergyTransformerKindCatalog.require(tier.kindId());
            EnergyTransformerProfile profile =
                    EnergyTransformerProfile.synthesize(kind, tier);
            if (result.putIfAbsent(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate transformer profile " + profile.id());
            }
            if (!sourceIds.add(profile.sourceId())) {
                throw new IllegalStateException(
                        "Duplicate transformer source id " + profile.sourceId());
            }
            if (profile.energyType() != EnergyType.ELECTRIC) {
                throw new IllegalStateException(
                        "Transformer is not EU " + profile.id());
            }
        }
        if (result.size() != EnergyTransformerTierCatalog.EXPECTED_SIZE) {
            throw new IllegalStateException(
                    "Energy transformer profile count drifted: " + result.size());
        }
        EnergyTransformerProfile first =
                result.get(ResourceLocation.parse(
                        "cruciblecraft:electric_transformer_ulv_lv"));
        EnergyTransformerProfile last =
                result.get(ResourceLocation.parse(
                        "cruciblecraft:electric_transformer_uv_puv1"));
        if (first == null
                || first.sourceId() != 10040
                || first.inputSize() != 32L
                || first.outputSize() != 8L
                || last == null
                || last.sourceId() != 10048
                || last.inputSize() != 2_097_152L
                || last.outputSize() != 524_288L
                || !sourceIds.contains(10040)
                || sourceIds.contains(10064)) {
            throw new IllegalStateException(
                    "Energy transformer anchors drifted");
        }
        return Map.copyOf(result);
    }

    private EnergyTransformerCatalog() {}
}
