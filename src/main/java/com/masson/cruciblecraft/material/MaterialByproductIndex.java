package com.masson.cruciblecraft.material;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.material.def.MaterialDefinition;

/** Immutable runtime query snapshot of ordered, resolved GT6 byproducts. */
public final class MaterialByproductIndex {
    private static volatile Snapshot snapshot = new Snapshot(0L, Map.of());

    private MaterialByproductIndex() {}

    static void publish(Collection<MaterialDefinition> materials, long revision) {
        LinkedHashMap<String, List<String>> bySource = new LinkedHashMap<>();
        materials.stream()
                .sorted(java.util.Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> bySource.put(
                        material.id(),
                        material.gt6Metadata()
                                .map(metadata -> metadata.byproducts().stream()
                                        .map(reference -> reference.material())
                                        .toList())
                                .orElse(List.of())));
        snapshot = new Snapshot(revision, bySource);
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static List<String> byproducts(String materialId) {
        return snapshot.bySource().getOrDefault(materialId, List.of());
    }

    public record Snapshot(long revision, Map<String, List<String>> bySource) {
        public Snapshot {
            LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>();
            bySource.forEach((key, value) -> copy.put(key, List.copyOf(value)));
            bySource = java.util.Collections.unmodifiableMap(copy);
        }
    }
}
