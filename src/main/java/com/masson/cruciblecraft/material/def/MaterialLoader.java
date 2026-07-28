package com.masson.cruciblecraft.material.def;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MaterialLoader {
    private static final String RESOURCE_ROOT = "/data/cruciblecraft/materials/";
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialLoader.class);

    private MaterialLoader() {}

    public static Map<String, MaterialDefinition> load(Path configDirectory) {
        return load(configDirectory, List.of());
    }

    public static Map<String, MaterialDefinition> load(
            Path configDirectory,
            Collection<MaterialDefinition> startupAdditions) {
        LinkedHashMap<String, MaterialDefinition> materials = new LinkedHashMap<>();
        loadBundled(materials);
        loadConfig(configDirectory, materials);
        for (MaterialDefinition definition : startupAdditions) {
            if (materials.putIfAbsent(definition.id(), definition) != null) {
                LOGGER.warn(
                        "Ignoring startup material {} because that id already exists",
                        definition.id());
            }
        }
        removeInvalidReferences(materials);
        return Map.copyOf(materials);
    }

    private static void loadBundled(Map<String, MaterialDefinition> output) {
        try (Reader reader = resourceReader("index.json")) {
            JsonArray index = JsonParser.parseReader(reader).getAsJsonArray();
            for (JsonElement entry : index) {
                String fileName = entry.getAsString();
                try (Reader materialReader = resourceReader(fileName)) {
                    put(output, decode(JsonParser.parseReader(materialReader)), "bundled/" + fileName);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load bundled material definitions", exception);
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
                    MaterialDefinition definition = decode(JsonParser.parseReader(reader));
                    if (output.containsKey(definition.id())) {
                        LOGGER.info("Config overrides material {}", definition.id());
                    }
                    output.put(definition.id(), definition);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load materials from " + directory, exception);
        }
    }

    private static Reader resourceReader(String fileName) throws IOException {
        var stream = MaterialLoader.class.getResourceAsStream(RESOURCE_ROOT + fileName);
        if (stream == null) {
            throw new IOException("Missing material resource " + RESOURCE_ROOT + fileName);
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
            invalid.forEach(materials::remove);
            changed = !invalid.isEmpty();
        } while (changed);
    }
}
