package com.masson.cruciblecraft.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModBlocks.OreBlockKey;

/**
 * Host-pair view derived from the material ore block registration map. Unknown
 * paths are never inferred from naming conventions.
 */
public final class OreHostVariantCatalog {
    private static volatile Map<String, Pair> registeredIndex;

    private OreHostVariantCatalog() {}

    public static Optional<String> adaptPath(String selectedPath, Host host) {
        Pair pair = registeredIndex().get(selectedPath);
        if (pair == null) {
            return Optional.empty();
        }
        return Optional.of(host == Host.DEEPSLATE ? pair.deepslate() : pair.stone());
    }

    static Optional<String> adaptPath(
            String selectedPath, Host host, Map<OreBlockKey, String> registeredBlocks) {
        Pair pair = build(registeredBlocks).get(selectedPath);
        if (pair == null) {
            return Optional.empty();
        }
        return Optional.of(host == Host.DEEPSLATE ? pair.deepslate() : pair.stone());
    }

    public static Set<String> registeredPaths() {
        return registeredIndex().keySet();
    }

    static Set<String> registeredPaths(Map<OreBlockKey, String> registeredBlocks) {
        return build(registeredBlocks).keySet();
    }

    private static Map<String, Pair> registeredIndex() {
        Map<String, Pair> snapshot = registeredIndex;
        if (snapshot != null) {
            return snapshot;
        }
        Map<OreBlockKey, String> registeredBlocks = ModBlocks.oreBlockPaths();
        if (registeredBlocks.isEmpty()) {
            return Map.of();
        }
        synchronized (OreHostVariantCatalog.class) {
            if (registeredIndex == null) {
                registeredIndex = build(registeredBlocks);
            }
            return registeredIndex;
        }
    }

    private static Map<String, Pair> build(Map<OreBlockKey, String> registeredBlocks) {
        Map<String, MutablePair> byMaterial = new LinkedHashMap<>();
        registeredBlocks.forEach((key, path) -> {
            MutablePair pair = byMaterial.computeIfAbsent(
                    key.materialId(), ignored -> new MutablePair());
            if (key.host() == Host.DEEPSLATE) {
                pair.deepslate = path;
            } else {
                pair.stone = path;
            }
        });
        Map<String, Pair> result = new LinkedHashMap<>();
        byMaterial.forEach((material, mutable) -> {
            if (mutable.stone == null || mutable.deepslate == null) {
                throw new IllegalStateException(
                        "Incomplete registered ore host pair for material " + material);
            }
            Pair pair = new Pair(mutable.stone, mutable.deepslate);
            result.put(pair.stone(), pair);
            result.put(pair.deepslate(), pair);
        });
        return Map.copyOf(result);
    }

    public enum Host {
        STONE,
        DEEPSLATE
    }

    private record Pair(String stone, String deepslate) {}

    private static final class MutablePair {
        private String stone;
        private String deepslate;
    }
}
