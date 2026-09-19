package com.masson.cruciblecraft.content.block;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.material.MaterialCatalog;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntitySmeltery} identities share {@code CrucibleBlockEntity}.
 * Unique in-place smelting crucibles stay distinct BlockItems.
 */
public final class SmelteryHosts {
    public static final ResourceLocation STEEL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "foundry/smelting_crucible_steel");

    /** GT6 {@code NBT_ACIDPROOF} T on visible smelteries. */
    private static final Set<String> ACID_PROOF = Set.of(
            "stainless_steel",
            "netherite",
            "thaumium",
            "chromium",
            "iridium",
            "tungsten",
            "void_metal",
            "adamantium");

    private SmelteryHosts() {}

    public static boolean isSmeltery(MteInPlaceSpec spec) {
        return spec != null && spec.gt6Class().contains("MultiTileEntitySmeltery");
    }

    public static boolean isSmeltery(Block block) {
        return block instanceof MteInPlaceBlock inplace && isSmeltery(inplace.spec());
    }

    public static String materialId(MteInPlaceSpec spec) {
        return resolveMaterialId(token(spec.registryPath()));
    }

    public static Optional<String> bakedMaterial(BlockState state) {
        if (state.getBlock() instanceof MteInPlaceBlock inplace
                && isSmeltery(inplace.spec())) {
            return Optional.of(materialId(inplace.spec()));
        }
        return Optional.empty();
    }

    public static boolean acidProof(String materialId) {
        return materialId != null && ACID_PROOF.contains(materialId);
    }

    static String token(String registryPath) {
        if (registryPath.startsWith("foundry/smelting_crucible_")) {
            return registryPath.substring("foundry/smelting_crucible_".length());
        }
        if (registryPath.endsWith("/smelting_crucible")) {
            return registryPath.substring(
                    0, registryPath.length() - "/smelting_crucible".length());
        }
        return registryPath.substring(registryPath.lastIndexOf('/') + 1);
    }

    static String resolveMaterialId(String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        if (MaterialCatalog.contains(normalized)) {
            return normalized;
        }
        String elemental = normalized + "_elemental";
        return MaterialCatalog.contains(elemental) ? elemental : normalized;
    }
}
