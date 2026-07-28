package com.masson.cruciblecraft.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry-independent catalog of every CrucibleCraft ore host pair. Keeping
 * paths here makes pair completeness and host selection unit-testable.
 */
public final class OreHostVariantCatalog {
    private static final Map<String, Pair> BY_PATH = build();

    private OreHostVariantCatalog() {}

    public static Optional<String> adaptPath(String selectedPath, Host host) {
        Pair pair = BY_PATH.get(selectedPath);
        if (pair == null) {
            return Optional.empty();
        }
        return Optional.of(host == Host.DEEPSLATE ? pair.deepslate() : pair.stone());
    }

    public static Set<String> registeredPaths() {
        return BY_PATH.keySet();
    }

    private static Map<String, Pair> build() {
        Map<String, Pair> result = new LinkedHashMap<>();
        for (String material : new String[] {"copper", "tin", "iron", "gold", "zinc", "lead", "nickel"}) {
            Pair pair = new Pair(material + "_ore", "deepslate_" + material + "_ore");
            result.put(pair.stone(), pair);
            result.put(pair.deepslate(), pair);
        }
        return Map.copyOf(result);
    }

    public enum Host {
        STONE,
        DEEPSLATE
    }

    private record Pair(String stone, String deepslate) {}
}
