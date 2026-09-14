package com.masson.cruciblecraft.machine.processing;

import java.util.Objects;

import com.masson.cruciblecraft.energy.converter.EnergyConverterKindCatalog;
import com.masson.cruciblecraft.energy.converter.EnergyConverterTierCatalog;

/**
 * Shared GT6 machine texture folder resolution for datagen and tint.
 *
 * <p>Variant ids map through {@link MachineTierCatalog#textureProfile(String)}
 * then delivery aliases. Presence is the on-disk / classpath folder, not a
 * hardcoded whitelist.
 */
public final class MachineTextureProfiles {
    private static final String ASSET_PREFIX =
            "/assets/cruciblecraft/textures/block/machine/";

    private MachineTextureProfiles() {}

    public static String textureId(String blockPath) {
        Objects.requireNonNull(blockPath, "blockPath");
        var converter = EnergyConverterTierCatalog.findByPath(blockPath);
        if (converter != null) {
            return EnergyConverterKindCatalog.require(converter.kindId())
                    .textureProfile();
        }
        if (blockPath.startsWith("quantum_energizer")) {
            return "quantum_energizer";
        }
        if (blockPath.startsWith("long_distance_transformer")) {
            return "long_distance_transformer";
        }
        if (blockPath.startsWith("bedrock_drill")) {
            return "bedrock_drill";
        }
        String catalogProfile = MachineTierCatalog.textureProfile(blockPath);
        if (!catalogProfile.equals(blockPath)) {
            return catalogProfile;
        }
        String aliased = MachineDeliveryCatalog.textureAlias(blockPath);
        if (!aliased.equals(blockPath)) {
            return aliased;
        }
        return MachineDeliveryCatalog.findByPath(blockPath)
                .map(MachineDeliveryCatalog.Host::textureProfile)
                .orElse(catalogProfile);
    }

    public static String shapedMachineModel(String textureId) {
        return MachineDeliveryCatalog.isShapedModel(textureId) ? textureId : null;
    }

    public static boolean hasMachineTextures(String textureId) {
        Objects.requireNonNull(textureId, "textureId");
        if (MachineDeliveryCatalog.isShapedModel(textureId)) {
            return true;
        }
        return resourceExists(textureId, "colored/front.png")
                || resourceExists(textureId, "colored/bottom.png")
                || resourceExists(textureId, "colored/top.png")
                || resourceExists(textureId, "colored/sides.png");
    }

    private static boolean resourceExists(String textureId, String relative) {
        return MachineTextureProfiles.class.getResource(
                ASSET_PREFIX + textureId + "/" + relative) != null;
    }
}
