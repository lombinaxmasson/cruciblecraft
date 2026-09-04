package com.masson.cruciblecraft.material;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    /** Schema v1 fallback only. Schema v2 reads java_overlay_sections from the gate. */
    private static final List<String> SCHEMA_V1_OVERLAY_SECTIONS = List.of(
            "worldgen_acquisition_forms",
            "roaster_required_forms",
            "centrifuge_required_forms",
            "electrolyzer_required_forms",
            "bath_required_forms");

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
        int schemaVersion = root.get("schema_version").getAsInt();
        if ((schemaVersion != 1 && schemaVersion != 2)
                || !root.has("materials")
                || !root.get("materials").isJsonObject()) {
            throw new IllegalStateException("Unsupported material registration gate schema");
        }

        List<String> overlaySections = overlaySections(root, schemaVersion);
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
            Set<MaterialPrefix> allowed = new HashSet<>(definition.forms());
            allowed.addAll(sourceBackedOverlayForms(root, definition.id(), overlaySections));
            if (!allowed.containsAll(forms)) {
                throw new IllegalStateException(
                        "Material registration gate exceeds factual or source-backed "
                                + "forms for "
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

    private static List<String> overlaySections(JsonObject root, int schemaVersion) {
        if (schemaVersion == 1) {
            return SCHEMA_V1_OVERLAY_SECTIONS;
        }
        if (!root.has("java_overlay_sections")
                || !root.get("java_overlay_sections").isJsonArray()) {
            throw new IllegalStateException(
                    "Material registration gate schema v2 is missing java_overlay_sections");
        }
        List<String> sections = new ArrayList<>();
        root.getAsJsonArray("java_overlay_sections").forEach(element ->
                sections.add(element.getAsString()));
        if (sections.isEmpty()) {
            throw new IllegalStateException(
                    "Material registration gate schema v2 has empty java_overlay_sections");
        }
        return List.copyOf(sections);
    }

    private static Set<MaterialPrefix> sourceBackedOverlayForms(
            JsonObject root, String materialId, List<String> overlaySections) {
        Set<MaterialPrefix> overlay = new HashSet<>();
        for (String section : overlaySections) {
            if (!root.has(section) || !root.get(section).isJsonObject()) {
                continue;
            }
            JsonObject formsByMaterial = root.getAsJsonObject(section);
            if (!formsByMaterial.has(materialId)
                    || !formsByMaterial.get(materialId).isJsonArray()) {
                continue;
            }
            formsByMaterial.getAsJsonArray(materialId).forEach(element ->
                    overlay.add(MaterialPrefixCatalog.require(element.getAsString())));
        }
        return overlay;
    }
}
