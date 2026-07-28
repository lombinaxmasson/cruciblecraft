package com.masson.cruciblecraft.material;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.recipe.AlloyIndex;

public final class MaterialCatalog {
    private static Map<String, MaterialDefinition> definitions = Map.of();
    private static Map<String, Integer> decompositionQuanta = Map.of();
    private static AlloyIndex alloyIndex = AlloyIndex.empty();
    private static final Map<String, MaterialDefinition> startupAdditions = new LinkedHashMap<>();

    private MaterialCatalog() {}

    public static synchronized void bootstrap(Path configDirectory) {
        if (!definitions.isEmpty()) {
            throw new IllegalStateException("Material catalog already bootstrapped");
        }
        definitions = MaterialLoader.load(configDirectory, startupAdditions.values());
        startupAdditions.clear();
        LinkedHashMap<String, Integer> quanta = new LinkedHashMap<>();
        for (MaterialDefinition definition : definitions.values()) {
            quanta.put(definition.id(), calculateQuantum(definition, new LinkedHashMap<>()));
        }
        decompositionQuanta = Map.copyOf(quanta);
        alloyIndex = new AlloyIndex(definitions.values());
    }

    /**
     * Adds a startup-only material before item registration. Existing ids are
     * deliberately not replaceable; removal and replacement are migration hazards.
     */
    public static synchronized boolean addStartupMaterial(MaterialDefinition definition) {
        if (!definitions.isEmpty()) {
            throw new IllegalStateException("Materials can only be added before bootstrap");
        }
        return startupAdditions.putIfAbsent(definition.id(), definition) == null;
    }

    public static Collection<MaterialDefinition> values() {
        requireBootstrapped();
        return definitions.values();
    }

    public static boolean isBootstrapped() {
        return !definitions.isEmpty();
    }

    /** O(1) membership check for persisted and integration-provided material ids. */
    public static boolean contains(String id) {
        requireBootstrapped();
        return definitions.containsKey(id);
    }

    public static MaterialDefinition require(String id) {
        requireBootstrapped();
        MaterialDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown material: " + id);
        }
        return definition;
    }

    public static Map<String, Integer> decompose(MaterialDefinition material, int units) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        decomposeInto(material, units, result, 0);
        return Map.copyOf(result);
    }

    /**
     * Smallest positive amount that can be recursively decomposed without fractions.
     * This is computed and validated once after the catalog becomes immutable.
     */
    public static int decompositionQuantum(String materialId) {
        requireBootstrapped();
        Integer quantum = decompositionQuanta.get(materialId);
        if (quantum == null) {
            throw new IllegalArgumentException("Unknown material: " + materialId);
        }
        return quantum;
    }

    public static int decompositionQuantum(MaterialDefinition material) {
        return decompositionQuantum(material.id());
    }

    public static AlloyIndex alloys() {
        requireBootstrapped();
        return alloyIndex;
    }

    private static void decomposeInto(
            MaterialDefinition material,
            int units,
            Map<String, Integer> output,
            int depth) {
        if (depth > definitions.size()) {
            throw new IllegalArgumentException("Cyclic composition involving " + material.id());
        }
        if (material.composition().isEmpty() || material.noDecompose()) {
            output.merge(material.id(), units, Integer::sum);
            return;
        }

        int ratioTotal = material.composition().values().stream().mapToInt(Integer::intValue).sum();
        for (var component : material.composition().entrySet()) {
            int numerator = units * component.getValue();
            if (numerator % ratioTotal != 0) {
                throw new IllegalArgumentException(
                        material.id() + " cannot decompose " + units + " units exactly");
            }
            decomposeInto(require(component.getKey()), numerator / ratioTotal, output, depth + 1);
        }
    }

    private static int calculateQuantum(
            MaterialDefinition material,
            Map<String, Boolean> visiting) {
        if (material.composition().isEmpty() || material.noDecompose()) {
            return 1;
        }
        if (visiting.put(material.id(), Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Cyclic composition involving " + material.id());
        }
        long ratioTotal = material.composition().values().stream()
                .mapToLong(Integer::longValue)
                .sum();
        long quantum = 1;
        for (var component : material.composition().entrySet()) {
            MaterialDefinition child = require(component.getKey());
            long childQuantum = calculateQuantum(child, visiting);
            long required = Math.multiplyExact(ratioTotal, childQuantum);
            required /= gcd(required, component.getValue());
            quantum = lcm(quantum, required);
            if (quantum > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Exact decomposition quantum is too large for " + material.id());
            }
        }
        visiting.remove(material.id());
        int result = (int) quantum;
        // Bootstrap validation protects all hot paths that rely on this exact quantum.
        decompose(material, result);
        return result;
    }

    private static long gcd(long left, long right) {
        left = Math.abs(left);
        right = Math.abs(right);
        while (right != 0) {
            long remainder = left % right;
            left = right;
            right = remainder;
        }
        return left;
    }

    private static long lcm(long left, long right) {
        return Math.multiplyExact(left / gcd(left, right), right);
    }

    private static void requireBootstrapped() {
        if (definitions.isEmpty()) {
            throw new IllegalStateException("Material catalog has not been bootstrapped");
        }
    }
}
