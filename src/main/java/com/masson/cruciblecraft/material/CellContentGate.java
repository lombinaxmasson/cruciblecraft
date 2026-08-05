package com.masson.cruciblecraft.material;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/** Closed T10 projection of fluid identities accepted by each generic cell. */
public final class CellContentGate {
    private static final String RESOURCE =
            "/data/cruciblecraft/t10_cell_content_gate.json";
    private static final Map<ResourceLocation, Kind> ENTRIES = load();

    private CellContentGate() {}

    public static boolean accepts(Kind kind, Fluid fluid) {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
        return id != null && accepts(kind, id);
    }

    public static boolean accepts(Kind kind, ResourceLocation fluidId) {
        return ENTRIES.get(fluidId) == kind;
    }

    public static Map<ResourceLocation, Kind> entries() {
        return ENTRIES;
    }

    private static Map<ResourceLocation, Kind> load() {
        JsonObject root;
        try (var stream =
                CellContentGate.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing committed T10 cell content gate: " + RESOURCE);
            }
            root = JsonParser.parseReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to load committed T10 cell content gate",
                    exception);
        }
        if (!root.has("schema_version")
                || root.get("schema_version").getAsInt() != 1
                || !root.has("fluids")
                || !root.get("fluids").isJsonArray()) {
            throw new IllegalStateException(
                    "Unsupported T10 cell content gate schema");
        }
        LinkedHashMap<ResourceLocation, Kind> entries = new LinkedHashMap<>();
        for (JsonElement value : root.getAsJsonArray("fluids")) {
            JsonObject row = value.getAsJsonObject();
            if (!row.has("id") || !row.has("kind") || !row.has("material")) {
                throw new IllegalStateException(
                        "T10 cell content gate row is incomplete");
            }
            ResourceLocation id = ResourceLocation.tryParse(
                    row.get("id").getAsString());
            if (id == null) {
                throw new IllegalStateException(
                        "Invalid T10 cell fluid id: "
                                + row.get("id").getAsString());
            }
            Kind kind;
            try {
                kind = Kind.valueOf(
                        row.get("kind").getAsString()
                                .toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException(
                        "Invalid T10 cell kind for " + id,
                        exception);
            }
            String material = row.get("material").getAsString();
            if (!material.matches("[a-z0-9_]+")) {
                throw new IllegalStateException(
                        "Invalid T10 cell material id: " + material);
            }
            Kind previous = entries.putIfAbsent(id, kind);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate T10 cell fluid id: " + id);
            }
        }
        Set<ResourceLocation> flowing = entries.keySet().stream()
                .filter(id -> id.getPath().startsWith("flowing_"))
                .collect(java.util.stream.Collectors.toSet());
        if (!flowing.isEmpty()) {
            throw new IllegalStateException(
                    "T10 cell gate must use source fluid ids only: " + flowing);
        }
        return Map.copyOf(entries);
    }

    public enum Kind {
        FLUID,
        GAS
    }
}
