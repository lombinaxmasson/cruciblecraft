package com.masson.cruciblecraft.census;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Classpath snapshot of the Python expected runtime registry gate. The full
 * {@code tools} runtime-registry artifact stays tooling-only; this
 * slim fixture is verification metadata with sorted category ids only.
 */
public final class RecipeCensusRuntimeRegistryGateFixture {
    public static final String RESOURCE = "/census/runtime_registry_gate.json";

    private final int schemaVersion;
    private final int compatibleSchemaVersion;
    private final String namespace;
    private final int totalExpectedIds;
    private final Map<String, Integer> countsByCategory;
    private final Map<String, List<String>> categories;

    private RecipeCensusRuntimeRegistryGateFixture(
            int schemaVersion,
            int compatibleSchemaVersion,
            String namespace,
            int totalExpectedIds,
            Map<String, Integer> countsByCategory,
            Map<String, List<String>> categories) {
        this.schemaVersion = schemaVersion;
        this.compatibleSchemaVersion = compatibleSchemaVersion;
        this.namespace = namespace;
        this.totalExpectedIds = totalExpectedIds;
        this.countsByCategory = countsByCategory;
        this.categories = categories;
    }

    public static RecipeCensusRuntimeRegistryGateFixture load() {
        try (InputStream stream = RecipeCensusRuntimeRegistryGateFixture.class
                .getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing classpath fixture " + RESOURCE);
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return decode(root);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to read runtime registry gate fixture",
                    exception);
        }
    }

    static RecipeCensusRuntimeRegistryGateFixture decode(JsonObject root) {
        int schemaVersion = root.get("schema_version").getAsInt();
        int compatibleSchemaVersion =
                root.get("compatible_schema_version").getAsInt();
        String namespace = root.get("namespace").getAsString();
        int totalExpectedIds = root.get("total_expected_ids").getAsInt();
        Map<String, Integer> countsByCategory = new LinkedHashMap<>();
        JsonObject counts = root.getAsJsonObject("counts_by_category");
        for (Map.Entry<String, JsonElement> entry : counts.entrySet()) {
            countsByCategory.put(
                    entry.getKey(),
                    entry.getValue().getAsInt());
        }
        Map<String, List<String>> categories = new LinkedHashMap<>();
        JsonObject categoryObject = root.getAsJsonObject("categories");
        for (Map.Entry<String, JsonElement> entry : categoryObject.entrySet()) {
            List<String> ids = entry.getValue().getAsJsonArray().asList().stream()
                    .map(JsonElement::getAsString)
                    .toList();
            categories.put(entry.getKey(), List.copyOf(ids));
        }
        return new RecipeCensusRuntimeRegistryGateFixture(
                schemaVersion,
                compatibleSchemaVersion,
                namespace,
                totalExpectedIds,
                Map.copyOf(countsByCategory),
                Map.copyOf(categories));
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public int compatibleSchemaVersion() {
        return compatibleSchemaVersion;
    }

    public String namespace() {
        return namespace;
    }

    public int totalExpectedIds() {
        return totalExpectedIds;
    }

    public Map<String, Integer> countsByCategory() {
        return countsByCategory;
    }

    public Map<String, List<String>> categories() {
        return categories;
    }

    public List<String> expectedIds(String category) {
        return Objects.requireNonNull(
                categories.get(category),
                "unknown category " + category);
    }
}
