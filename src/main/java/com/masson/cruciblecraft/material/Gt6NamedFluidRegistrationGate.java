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
 * GT6 fluids that are registered under their own names and are not a molten
 * or chemical form of a CrucibleCraft material.
 */
public final class Gt6NamedFluidRegistrationGate {
    private static final String RESOURCE =
            "/data/cruciblecraft/gt6_named_fluid_gate.json";
    private static final List<Entry> ENTRIES = loadFromResource();

    private Gt6NamedFluidRegistrationGate() {}

    public static List<Entry> load() {
        return ENTRIES;
    }

    private static List<Entry> loadFromResource() {
        JsonObject root;
        try (var stream =
                Gt6NamedFluidRegistrationGate.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing committed named fluid gate: " + RESOURCE);
            }
            root = JsonParser.parseReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to load committed named fluid gate " + RESOURCE,
                    exception);
        }
        if (!root.has("schema_version")
                || root.get("schema_version").getAsInt() != 1
                || !root.has("fluids")
                || !root.get("fluids").isJsonArray()) {
            throw new IllegalStateException(
                    "Unsupported named fluid gate schema: " + RESOURCE);
        }
        LinkedHashMap<String, Entry> byId = new LinkedHashMap<>();
        for (JsonElement value : root.getAsJsonArray("fluids")) {
            Entry entry = decode(value.getAsJsonObject());
            Entry previous = byId.putIfAbsent(entry.id(), entry);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate named fluid id: " + entry.id());
            }
        }
        if (byId.size() != 4) {
            throw new IllegalStateException(
                    "Named fluid gate must contain exactly 4 identities, found "
                            + byId.size());
        }
        return List.copyOf(byId.values());
    }

    private static Entry decode(JsonObject value) {
        requireFields(
                value,
                "id",
                "gt6_fluid",
                "state",
                "temperature_kelvin",
                "density",
                "viscosity",
                "light_level",
                "english",
                "chinese",
                "source");
        State state;
        try {
            state = State.valueOf(
                    value.get("state").getAsString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Unsupported named fluid state for " + value.get("id"),
                    exception);
        }
        JsonObject source = value.getAsJsonObject("source");
        requireFields(source, "repository", "revision", "path", "reason");
        return new Entry(
                value.get("id").getAsString(),
                value.get("gt6_fluid").getAsString(),
                state,
                value.get("temperature_kelvin").getAsInt(),
                value.get("density").getAsInt(),
                value.get("viscosity").getAsInt(),
                value.get("light_level").getAsInt(),
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
                        "Named fluid gate entry is missing " + field);
            }
        }
    }

    public enum State {
        LIQUID
    }

    public record Entry(
            String id,
            String gt6Fluid,
            State state,
            int temperatureKelvin,
            int density,
            int viscosity,
            int lightLevel,
            String english,
            String chinese,
            Source source) {
        public Entry {
            if (id == null || !id.matches("[a-z0-9_]+")
                    || gt6Fluid == null || gt6Fluid.isBlank()
                    || state != State.LIQUID
                    || temperatureKelvin < 0
                    || density == 0
                    || viscosity <= 0
                    || lightLevel < 0
                    || lightLevel > 15
                    || english == null || english.isBlank()
                    || chinese == null || chinese.isBlank()
                    || source == null) {
                throw new IllegalArgumentException(
                        "Invalid named fluid policy for " + id);
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
                        "Named fluid source is incomplete");
            }
        }
    }
}
