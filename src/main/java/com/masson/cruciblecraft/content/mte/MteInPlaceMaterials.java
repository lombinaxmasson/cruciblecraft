package com.masson.cruciblecraft.content.mte;

import java.util.Locale;
import java.util.Optional;

import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialZhNames;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

/**
 * Material token for in-place storage ids such as {@code aluminium/chest},
 * {@code furniture/locker_aluminium}, and {@code safe/mechanical_lead_safe}.
 */
public final class MteInPlaceMaterials {
    private static final String[] FURNITURE_PREFIXES = {
        "advanced_crafting_table_",
        "compartment_drawer_",
        "mass_storage_",
        "bottlecrate_",
        "bookshelf_",
        "locker_",
    };

    private MteInPlaceMaterials() {}

    public static String id(MteInPlaceSpec spec) {
        String path = spec.registryPath();
        if (spec.kind() == MteInPlaceKind.FAUCET) {
            return faucetMaterial(path);
        }
        if (path.contains("stone_chest")) {
            return "stone";
        }
        if (path.contains("wooden_item_barrel_cheap")
                || "wooden_item_barrel_cheap".equals(path)) {
            return "wood";
        }
        if (path.endsWith("wooden_item_barrel")) {
            return "wood_treated";
        }
        return resolve(token(path)).orElseGet(() -> {
            MteInPlaceKind kind = spec.kind();
            if (kind == MteInPlaceKind.BARREL
                    || kind == MteInPlaceKind.BOOKSHELF
                    || kind == MteInPlaceKind.BOTTLE_CRATE) {
                return "wood";
            }
            return "steel";
        });
    }

    private static String faucetMaterial(String path) {
        String token;
        int slash = path.indexOf('/');
        if (slash > 0 && path.endsWith("/crucible_faucet")) {
            token = path.substring(0, slash);
        } else {
            String marker = "crucible_faucet_";
            int markerIndex = path.lastIndexOf(marker);
            token = markerIndex >= 0
                    ? path.substring(markerIndex + marker.length())
                    : "stone";
        }
        String resolved = resolve(token).orElse(token);
        if ("osmium".equals(resolved)) {
            return "osmium_elemental";
        }
        return MaterialCatalog.contains(resolved) ? resolved : "stone";
    }

    public static String token(String registryPath) {
        if (registryPath == null || registryPath.isEmpty()) {
            return "";
        }
        int slash = registryPath.indexOf('/');
        if (slash <= 0) {
            return registryPath;
        }
        String first = registryPath.substring(0, slash);
        String rest = registryPath.substring(slash + 1);
        if ("safe".equals(first)) {
            if (rest.startsWith("mechanical_") && rest.endsWith("_safe")) {
                return rest.substring("mechanical_".length(), rest.length() - "_safe".length());
            }
            if (rest.startsWith("key_locked_") && rest.endsWith("_safe")) {
                return rest.substring("key_locked_".length(), rest.length() - "_safe".length());
            }
            return rest;
        }
        if ("furniture".equals(first)) {
            for (String prefix : FURNITURE_PREFIXES) {
                if (rest.startsWith(prefix)) {
                    return rest.substring(prefix.length());
                }
            }
            int under = rest.lastIndexOf('_');
            if (under >= 0 && under < rest.length() - 1) {
                return rest.substring(under + 1);
            }
            return rest;
        }
        return first;
    }

    public static Optional<String> resolve(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        if (MaterialCatalog.contains(token)) {
            return Optional.of(token);
        }
        String elemental = token.endsWith("_elemental") ? token : token + "_elemental";
        if (MaterialCatalog.contains(elemental)) {
            return Optional.of(elemental);
        }
        String compact = token.replace("_", "").toLowerCase(Locale.ROOT);
        for (MaterialDefinition material : MaterialCatalog.values()) {
            if (material.id().replace("_", "").equalsIgnoreCase(compact)) {
                return Optional.of(material.id());
            }
        }
        for (MaterialDefinition material : MaterialCatalog.startupValues()) {
            if (material.id().replace("_", "").equalsIgnoreCase(compact)) {
                return Optional.of(material.id());
            }
        }
        return Optional.empty();
    }

    public static Optional<String> chineseMaterial(String registryPath) {
        String materialToken = token(registryPath);
        Optional<String> zh = MaterialZhNames.material(materialToken);
        if (zh.isPresent()) {
            return zh;
        }
        if (!materialToken.endsWith("_elemental")) {
            return MaterialZhNames.material(materialToken + "_elemental");
        }
        return Optional.empty();
    }
}
