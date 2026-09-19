package com.masson.cruciblecraft.content.block;

import java.util.Locale;
import java.util.Optional;

import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.material.MaterialCatalog;

/**
 * GT6 foundry identities: {@code MultiTileEntitySmeltery} / {@code Mold} /
 * {@code Basin} / {@code Crossing}. Material tokens follow the catalog suffix,
 * not the last underscore (so {@code stainless_steel} stays intact).
 */
public final class FoundryHosts {
    private FoundryHosts() {}

    public static boolean isFoundry(MteInPlaceSpec spec) {
        return spec != null && spec.kind() == MteInPlaceKind.CRUCIBLE_FOUNDRY;
    }

    public static boolean isMold(MteInPlaceSpec spec) {
        return isFoundry(spec) && spec.gt6Class().contains("MultiTileEntityMold");
    }

    public static boolean isBasin(MteInPlaceSpec spec) {
        return isFoundry(spec) && spec.gt6Class().contains("Basin");
    }

    public static boolean isCrossing(MteInPlaceSpec spec) {
        return isFoundry(spec) && spec.gt6Class().contains("Crossing");
    }

    public static boolean isCasting(MteInPlaceSpec spec) {
        return isMold(spec) || isBasin(spec);
    }

    public static String materialId(MteInPlaceSpec spec) {
        return resolveMaterialId(token(spec.registryPath()));
    }

    public static String token(String registryPath) {
        if (registryPath == null || registryPath.isEmpty()) {
            return "";
        }
        if (registryPath.startsWith("foundry/smelting_crucible_")) {
            return registryPath.substring("foundry/smelting_crucible_".length());
        }
        if (registryPath.endsWith("/smelting_crucible")) {
            return registryPath.substring(
                    0, registryPath.length() - "/smelting_crucible".length());
        }
        if (registryPath.startsWith("foundry/crucible_crossing_")) {
            return registryPath.substring("foundry/crucible_crossing_".length());
        }
        if (registryPath.startsWith("foundry/mold_")) {
            return registryPath.substring("foundry/mold_".length());
        }
        if (registryPath.startsWith("foundry/basin_")) {
            return registryPath.substring("foundry/basin_".length());
        }
        int slash = registryPath.lastIndexOf('/');
        return slash < 0 ? registryPath : registryPath.substring(slash + 1);
    }

    static String resolveMaterialId(String token) {
        String normalized = token == null ? "" : token.toLowerCase(Locale.ROOT);
        if (MaterialCatalog.contains(normalized)) {
            return normalized;
        }
        String elemental = normalized + "_elemental";
        return MaterialCatalog.contains(elemental) ? elemental : normalized;
    }

    public static Optional<String> kindTemplate(String registryPath) {
        if (registryPath == null) {
            return Optional.empty();
        }
        if (isSmeltingCrucible(registryPath)) {
            return Optional.of("坩埚（%s）");
        }
        if (registryPath.startsWith("foundry/crucible_crossing_")) {
            return Optional.of("坩埚交叉（%s）");
        }
        if (registryPath.startsWith("foundry/mold_")) {
            return Optional.of("模具（%s）");
        }
        if (registryPath.startsWith("foundry/basin_")) {
            return Optional.of("盆（%s）");
        }
        return Optional.empty();
    }

    public static boolean isSmeltingCrucible(String registryPath) {
        return registryPath != null
                && (registryPath.startsWith("foundry/smelting_crucible_")
                        || registryPath.endsWith("/smelting_crucible"));
    }

    public static String englishName(String sourceName) {
        if (sourceName != null && sourceName.startsWith("Smelting Crucible (")) {
            return "Crucible (" + sourceName.substring("Smelting Crucible (".length());
        }
        return sourceName;
    }
}
