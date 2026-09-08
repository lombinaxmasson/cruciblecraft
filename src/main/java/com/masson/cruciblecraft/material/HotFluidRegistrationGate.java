package com.masson.cruciblecraft.material;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Closed projection of independent GT6 hot-fluid identities.
 *
 * <p>These fluids are not chemical or molten forms of their source material.
 * {@code materialFluid(source_material)} must keep returning the cold identity.
 */
public final class HotFluidRegistrationGate {
    private static final String RESOURCE =
            "/data/cruciblecraft/hot_fluid_gate.json";
    private static final List<Entry> ENTRIES = loadFromResource();

    private HotFluidRegistrationGate() {}

    public static List<Entry> load() {
        return ENTRIES;
    }

    private static List<Entry> loadFromResource() {
        JsonObject root;
        try (var stream =
                HotFluidRegistrationGate.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing committed hot fluid gate: " + RESOURCE);
            }
            root = JsonParser.parseReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to load committed hot fluid gate " + RESOURCE,
                    exception);
        }
        if (!root.has("schema_version")
                || root.get("schema_version").getAsInt() != 1
                || !root.has("fluids")
                || !root.get("fluids").isJsonArray()) {
            throw new IllegalStateException(
                    "Unsupported hot fluid gate schema: " + RESOURCE);
        }
        LinkedHashMap<String, Entry> byId = new LinkedHashMap<>();
        for (JsonElement value : root.getAsJsonArray("fluids")) {
            Entry entry = decode(value.getAsJsonObject());
            Entry previous = byId.putIfAbsent(entry.id(), entry);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate hot fluid id: " + entry.id());
            }
        }
        if (byId.size() != 8) {
            throw new IllegalStateException(
                    "Hot fluid gate must contain exactly 8 identities, found "
                            + byId.size());
        }
        return List.copyOf(byId.values());
    }

    private static Entry decode(JsonObject value) {
        requireFields(
                value,
                "id",
                "gt6_fluid",
                "source_material",
                "state",
                "temperature_kelvin",
                "density",
                "viscosity",
                "color",
                "world_placeable",
                "bucket",
                "cell_kind",
                "english",
                "chinese",
                "source");
        State state;
        try {
            state = State.valueOf(
                    value.get("state").getAsString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Unsupported hot fluid state for " + value.get("id"),
                    exception);
        }
        JsonObject source = value.getAsJsonObject("source");
        requireFields(source, "repository", "revision", "path", "reason");
        return new Entry(
                value.get("id").getAsString(),
                value.get("gt6_fluid").getAsString(),
                value.get("source_material").getAsString(),
                state,
                value.get("temperature_kelvin").getAsInt(),
                value.get("density").getAsInt(),
                value.get("viscosity").getAsInt(),
                value.get("color").getAsString(),
                value.get("world_placeable").getAsBoolean(),
                value.get("bucket").getAsBoolean(),
                value.get("cell_kind").getAsString(),
                value.get("english").getAsString(),
                value.get("chinese").getAsString(),
                new Source(
                        source.get("repository").getAsString(),
                        source.get("revision").getAsString(),
                        source.get("path").getAsString(),
                        source.get("reason").getAsString()));
    }

    private static void requireFields(JsonObject value, String... fields) {
        for (String field : fields) {
            if (!value.has(field) || value.get(field).isJsonNull()) {
                throw new IllegalStateException(
                        "Hot fluid gate entry is missing " + field);
            }
        }
    }

    public enum State {
        LIQUID,
        GAS
    }

    public record Entry(
            String id,
            String gt6Fluid,
            String sourceMaterialId,
            State state,
            int temperatureKelvin,
            int density,
            int viscosity,
            String color,
            boolean worldPlaceable,
            boolean bucket,
            String cellKind,
            String english,
            String chinese,
            Source source) {
        public Entry {
            if (id == null || !id.matches("[a-z0-9_]+")
                    || !id.startsWith("hot_")
                    || gt6Fluid == null
                    || gt6Fluid.isBlank()
                    || sourceMaterialId == null
                    || !sourceMaterialId.matches("[a-z0-9_]+")
                    || state == null
                    || source == null
                    || temperatureKelvin <= 0
                    || density == 0
                    || viscosity <= 0
                    || !MaterialColors.isValid(color)
                    || worldPlaceable
                    || bucket
                    || english == null
                    || english.isBlank()
                    || chinese == null
                    || chinese.isBlank()
                    || cellKind == null
                    || !(cellKind.equals("fluid") || cellKind.equals("gas"))) {
                throw new IllegalArgumentException(
                        "Invalid independent hot-fluid policy for " + id);
            }
            if (id.equals(sourceMaterialId)
                    || id.equals("molten_" + sourceMaterialId)
                    || id.equals("steam")) {
                throw new IllegalArgumentException(
                        "Hot fluid must not alias a cold identity: " + id);
            }
        }
    }

    public record Source(
            String repository,
            String revision,
            String path,
            String reason) {
        public Source {
            if (repository == null || repository.isBlank()
                    || revision == null || revision.isBlank()
                    || path == null || path.isBlank()
                    || reason == null || reason.isBlank()) {
                throw new IllegalArgumentException(
                        "Hot fluid source is incomplete");
            }
        }
    }
}
