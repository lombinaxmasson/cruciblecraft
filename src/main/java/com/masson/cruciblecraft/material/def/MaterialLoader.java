package com.masson.cruciblecraft.material.def;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup loader for immutable material structure and initial tuning.
 *
 * <p>A future datapack-reloadable tuning layer can reuse {@link MaterialTuning}
 * after bootstrap, but must never mutate ids, resolved prefixes, item mappings,
 * fluids, or composition in the frozen structural snapshot.
 */
public final class MaterialLoader {
    private static final String RESOURCE_ROOT = "/data/cruciblecraft/materials/";
    private static final String T11_RESOURCE_ROOT =
            "/data/cruciblecraft/t11_materials/";
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialLoader.class);

    private MaterialLoader() {}

    public static Map<String, MaterialDefinition> load(Path configDirectory) {
        return load(configDirectory, List.of());
    }

    public static Map<String, MaterialDefinition> load(
            Path configDirectory,
            Collection<MaterialDefinition> startupAdditions) {
        return load(configDirectory, startupAdditions, List.of());
    }

    public static Map<String, MaterialDefinition> load(
            Path configDirectory,
            Collection<MaterialDefinition> startupAdditions,
            Collection<MaterialTuning> startupTunings) {
        LinkedHashMap<String, MaterialDefinition> materials = new LinkedHashMap<>();
        loadBundled(materials);
        for (MaterialDefinition definition : startupAdditions) {
            if (materials.putIfAbsent(definition.id(), definition) != null) {
                LOGGER.warn(
                        "Ignoring startup material {} because that id already exists",
                        definition.id());
            }
        }
        loadConfig(configDirectory, materials);
        for (MaterialTuning tuning : startupTunings) {
            applyTuning(materials, tuning, "startup integration");
        }
        removeInvalidReferences(materials);
        return Collections.unmodifiableMap(new LinkedHashMap<>(materials));
    }

    private static void loadBundled(Map<String, MaterialDefinition> output) {
        for (String root : List.of(
                RESOURCE_ROOT, T11_RESOURCE_ROOT)) {
            try (Reader reader = resourceReader(root, "index.json")) {
                JsonArray index = JsonParser.parseReader(
                        reader).getAsJsonArray();
                for (JsonElement entry : index) {
                    String fileName = entry.getAsString();
                    try (Reader materialReader = resourceReader(
                            root, fileName)) {
                        put(output, decode(JsonParser.parseReader(
                                materialReader)), "bundled/" + root
                                        + fileName);
                    }
                }
            } catch (IOException exception) {
                throw new IllegalStateException(
                        "Failed to load bundled material definitions from "
                                + root,
                        exception);
            }
        }
    }

    private static void loadConfig(Path directory, Map<String, MaterialDefinition> output) {
        try {
            Files.createDirectories(directory);
            List<Path> files = new ArrayList<>();
            try (var paths = Files.list(directory)) {
                paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(files::add);
            }
            for (Path file : files) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (!parsed.isJsonObject()) {
                        throw new IllegalArgumentException("Material tuning must be a JSON object");
                    }
                    JsonObject json = parsed.getAsJsonObject();
                    String id = json.has("id") ? json.get("id").getAsString() : "";
                    if (!output.containsKey(id) && isStructuralDefinition(json)) {
                        put(output, decode(json), "startup config " + file);
                        LOGGER.info("Loaded startup material definition {} from {}", id, file);
                        continue;
                    }
                    warnUnsupportedConfigFields(json, file);
                    applyTuning(output, MaterialTuning.fromJson(json), "config file " + file);
                } catch (RuntimeException exception) {
                    LOGGER.warn("Ignoring invalid material tuning {}", file, exception);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load materials from " + directory, exception);
        }
    }

    private static void applyTuning(
            Map<String, MaterialDefinition> materials,
            MaterialTuning tuning,
            String source) {
        MaterialDefinition base = materials.get(tuning.id());
        if (base == null) {
            LOGGER.warn(
                    "Ignoring {} override from {} because material ids can only come from bundled or mod-bus definitions",
                    tuning.id(),
                    source);
            return;
        }
        try {
            materials.put(tuning.id(), tuning.apply(base));
            LOGGER.info("Applied material tuning {} from {}", tuning.id(), source);
        } catch (RuntimeException exception) {
            LOGGER.warn("Ignoring invalid material tuning {} from {}", tuning.id(), source, exception);
        }
    }

    private static void warnUnsupportedConfigFields(JsonObject json, Path file) {
        Set<String> allowedRoot = Set.of("id", "tier", "color", "thermal");
        for (String key : json.keySet()) {
            if (!allowedRoot.contains(key)) {
                LOGGER.warn(
                        "Ignoring structural material field '{}' in {}; registry shape is not configurable",
                        key,
                        file);
            }
        }
        if (json.has("thermal") && json.get("thermal").isJsonObject()) {
            Set<String> allowedThermal = Set.of("melting_point", "boiling_point", "density");
            for (String key : json.getAsJsonObject("thermal").keySet()) {
                if (!allowedThermal.contains(key)) {
                    LOGGER.warn("Ignoring unsupported thermal field '{}' in {}", key, file);
                }
            }
        }
    }

    private static boolean isStructuralDefinition(JsonObject json) {
        return json.has("thermal")
                && (json.has("forms")
                        || json.has("generation_flags")
                        || json.has("include_prefixes"));
    }

    private static Reader resourceReader(
            String root, String fileName) throws IOException {
        var stream = MaterialLoader.class.getResourceAsStream(
                root + fileName);
        if (stream == null) {
            throw new IOException(
                    "Missing material resource " + root + fileName);
        }
        return new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static MaterialDefinition decode(JsonElement json) {
        return MaterialDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(message -> new IllegalArgumentException("Invalid material JSON: " + message));
    }

    private static void put(
            Map<String, MaterialDefinition> output,
            MaterialDefinition definition,
            String source) {
        if (output.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalArgumentException("Duplicate bundled material " + definition.id() + " in " + source);
        }
    }

    private static void removeInvalidReferences(Map<String, MaterialDefinition> materials) {
        boolean changed;
        do {
            Set<String> invalid = new LinkedHashSet<>();
            for (MaterialDefinition definition : materials.values()) {
                for (String component : definition.composition().keySet()) {
                    if (component.equals(definition.id()) || !materials.containsKey(component)) {
                        LOGGER.warn(
                                "Skipping material {} because component {} is unavailable",
                                definition.id(),
                                component);
                        invalid.add(definition.id());
                        break;
                    }
                }
            }
            if (invalid.isEmpty()) {
                Set<String> cyclic = findCyclicMaterials(materials);
                for (String material : cyclic) {
                    LOGGER.warn(
                            "Skipping material {} because its composition is cyclic",
                            material);
                }
                invalid.addAll(cyclic);
            }
            invalid.forEach(materials::remove);
            changed = !invalid.isEmpty();
        } while (changed);
    }

    private static Set<String> findCyclicMaterials(
            Map<String, MaterialDefinition> materials) {
        Map<String, Integer> states = new LinkedHashMap<>();
        List<String> path = new ArrayList<>();
        Set<String> cyclic = new LinkedHashSet<>();
        for (String material : materials.keySet()) {
            if (states.getOrDefault(material, 0) == 0) {
                collectCycles(material, materials, states, path, cyclic);
            }
        }
        return cyclic;
    }

    private static void collectCycles(
            String material,
            Map<String, MaterialDefinition> materials,
            Map<String, Integer> states,
            List<String> path,
            Set<String> cyclic) {
        states.put(material, 1);
        path.add(material);
        for (String component : materials.get(material).composition().keySet()) {
            if (!materials.containsKey(component)) {
                continue;
            }
            int state = states.getOrDefault(component, 0);
            if (state == 0) {
                collectCycles(component, materials, states, path, cyclic);
            } else if (state == 1) {
                int cycleStart = path.lastIndexOf(component);
                cyclic.addAll(path.subList(cycleStart, path.size()));
            }
        }
        path.removeLast();
        states.put(material, 2);
    }
}
