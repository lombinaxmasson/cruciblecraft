package com.masson.cruciblecraft.material;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonWriter;

/**
 * Versioned semantic projection of {@code material_registration_gate.json}.
 * Canonical bytes must match {@code tools/semantic_projection.py}.
 */
public final class SemanticProjection {
    public static final int VERSION = 1;
    public static final String KIND_GATE = "material_registration_gate";
    static final List<String> SCHEMA_V1_OVERLAY_SECTIONS = List.of(
            "t38_source_backed_acquisition_forms",
            "t38_required_forms",
            "t39_required_forms",
            "t40_required_forms");

    private SemanticProjection() {}

    public static List<String> overlaySectionNames(JsonObject gate) {
        int schemaVersion = gate.get("schema_version").getAsInt();
        if (schemaVersion == 1) {
            return SCHEMA_V1_OVERLAY_SECTIONS;
        }
        if (!gate.has("java_overlay_sections")
                || !gate.get("java_overlay_sections").isJsonArray()) {
            throw new IllegalStateException(
                    "gate schema v2 is missing java_overlay_sections");
        }
        Set<String> sections = new TreeSet<>();
        gate.getAsJsonArray("java_overlay_sections").forEach(element ->
                sections.add(element.getAsString()));
        if (sections.isEmpty()) {
            throw new IllegalStateException(
                    "gate schema v2 is missing java_overlay_sections");
        }
        return List.copyOf(sections);
    }

    public static JsonObject gateProjection(JsonObject gate) {
        List<String> sections = overlaySectionNames(gate);
        JsonObject payloads = new JsonObject();
        for (String section : sections) {
            payloads.add(section, sortedFormMap(gate.get(section)));
        }
        JsonObject projection = new JsonObject();
        projection.addProperty("kind", KIND_GATE);
        projection.add("materials", sortedFormMap(gate.get("materials")));
        projection.add("overlay_payloads", payloads);
        JsonArray overlaySections = new JsonArray();
        sections.forEach(overlaySections::add);
        projection.add("overlay_sections", overlaySections);
        projection.addProperty("semantic_projection_version", VERSION);
        return projection;
    }

    public static String canonicalJson(JsonElement element) {
        try {
            StringWriter buffer = new StringWriter();
            JsonWriter writer = new JsonWriter(buffer);
            writeCanonical(element, writer);
            writer.close();
            return buffer.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("canonical JSON failed", exception);
        }
    }

    public static String semanticRootSha256(JsonObject gate) {
        return sha256Hex(canonicalJson(gateProjection(gate))
                .getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] payload) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 missing", exception);
        }
    }

    public static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JsonObject sortedFormMap(JsonElement element) {
        JsonObject result = new JsonObject();
        if (element == null || !element.isJsonObject()) {
            return result;
        }
        Map<String, JsonArray> sorted = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            sorted.put(entry.getKey(), sortedForms(entry.getValue()));
        }
        sorted.forEach(result::add);
        return result;
    }

    private static JsonArray sortedForms(JsonElement element) {
        JsonArray result = new JsonArray();
        if (element == null || !element.isJsonArray()) {
            return result;
        }
        Set<String> forms = new TreeSet<>();
        element.getAsJsonArray().forEach(item -> forms.add(item.getAsString()));
        forms.forEach(result::add);
        return result;
    }

    private static void writeCanonical(JsonElement element, JsonWriter writer)
            throws IOException {
        if (element == null || element.isJsonNull()) {
            writer.nullValue();
            return;
        }
        if (element.isJsonObject()) {
            writer.beginObject();
            List<String> keys = new ArrayList<>(element.getAsJsonObject().keySet());
            Collections.sort(keys);
            for (String key : keys) {
                writer.name(key);
                writeCanonical(element.getAsJsonObject().get(key), writer);
            }
            writer.endObject();
            return;
        }
        if (element.isJsonArray()) {
            writer.beginArray();
            for (JsonElement child : element.getAsJsonArray()) {
                writeCanonical(child, writer);
            }
            writer.endArray();
            return;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            writer.value(primitive.getAsBoolean());
            return;
        }
        if (primitive.isNumber()) {
            writeNumber(primitive, writer);
            return;
        }
        writer.value(primitive.getAsString());
    }

    private static void writeNumber(JsonPrimitive primitive, JsonWriter writer)
            throws IOException {
        String raw = primitive.getAsString();
        if (raw.indexOf('.') < 0 && raw.indexOf('e') < 0 && raw.indexOf('E') < 0) {
            writer.value(Long.parseLong(raw));
            return;
        }
        writer.value(primitive.getAsDouble());
    }
}
