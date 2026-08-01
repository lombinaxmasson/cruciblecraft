package com.masson.cruciblecraft.material;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

/** Loads the committed build-time material form gate without recomputing it at runtime. */
public final class MaterialRegistrationGate {
    private static final String RESOURCE =
            "/data/cruciblecraft/material_registration_gate.json";

    private MaterialRegistrationGate() {}

    public static Map<String, List<MaterialPrefix>> load(
            Collection<MaterialDefinition> definitions) {
        LinkedHashMap<String, MaterialDefinition> definitionsById = new LinkedHashMap<>();
        definitions.forEach(definition ->
                definitionsById.put(definition.id(), definition));
        JsonObject root;
        try (var stream = MaterialRegistrationGate.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing committed material registration gate: " + RESOURCE);
            }
            root = JsonParser.parseReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to load committed material registration gate", exception);
        }
        if (root.get("schema_version").getAsInt() != 1
                || !root.has("materials")
                || !root.get("materials").isJsonObject()) {
            throw new IllegalStateException("Unsupported material registration gate schema");
        }

        LinkedHashMap<String, List<MaterialPrefix>> registered = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry
                : root.getAsJsonObject("materials").entrySet()) {
            MaterialDefinition definition = definitionsById.get(entry.getKey());
            if (definition == null) {
                throw new IllegalStateException(
                        "Material registration gate references unknown material: "
                                + entry.getKey());
            }
            List<MaterialPrefix> forms = entry.getValue().getAsJsonArray().asList().stream()
                    .map(JsonElement::getAsString)
                    .map(MaterialPrefixCatalog::require)
                    .toList();
            if (forms.stream().distinct().count() != forms.size()) {
                throw new IllegalStateException(
                        "Material registration gate contains duplicate forms for "
                                + definition.id());
            }
            if (!definition.forms().containsAll(forms)) {
                throw new IllegalStateException(
                        "Material registration gate exceeds factual forms for "
                                + definition.id());
            }
            if (definition.metadataOnly() && !forms.isEmpty()) {
                throw new IllegalStateException(
                        "Metadata-only material has registered forms: " + definition.id());
            }
            registered.put(definition.id(), List.copyOf(forms));
        }

        // Startup addon/KubeJS definitions are outside the GT6 artifact's domain.
        for (MaterialDefinition definition : definitions) {
            registered.putIfAbsent(definition.id(), definition.forms());
        }
        return java.util.Collections.unmodifiableMap(registered);
    }
}
