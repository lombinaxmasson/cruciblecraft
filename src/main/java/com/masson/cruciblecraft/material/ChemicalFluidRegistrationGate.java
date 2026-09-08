package com.masson.cruciblecraft.material;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

/**
 * Loads the committed chemical non-molten fluid registry projection.
 *
 * <p>The source ledger owns selection and physical-state policy. Runtime code
 * only validates and registers the closed projection; it never guesses fluid
 * state from a material name or from {@code molten_fluid}.
 */
public final class ChemicalFluidRegistrationGate {
    private static final List<String> RESOURCES = List.of(
            "/data/cruciblecraft/chemical_fluid_gate.json",
            "/data/cruciblecraft/container_fluid_gate.json",
            "/data/cruciblecraft/hydrocarbon_fluid_gate.json");

    private ChemicalFluidRegistrationGate() {}

    public static List<Entry> load(Iterable<MaterialDefinition> definitions) {
        LinkedHashMap<String, MaterialDefinition> definitionsById = new LinkedHashMap<>();
        definitions.forEach(definition -> {
            MaterialDefinition previous =
                    definitionsById.putIfAbsent(definition.id(), definition);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate material while loading chemical fluid gate: "
                                + definition.id());
            }
        });
        LinkedHashSet<String> reservedFluidIds = new LinkedHashSet<>(
                List.of("creosote", "flowing_creosote", "steam", "flowing_steam"));
        definitionsById.values().stream()
                .filter(MaterialDefinition::moltenFluid)
                .forEach(material -> {
                    reservedFluidIds.add("molten_" + material.id());
                    reservedFluidIds.add("flowing_molten_" + material.id());
                });
        for (HotFluidRegistrationGate.Entry hot : HotFluidRegistrationGate.load()) {
            reservedFluidIds.add(hot.id());
            reservedFluidIds.add("flowing_" + hot.id());
        }

        LinkedHashMap<String, Entry> byId = new LinkedHashMap<>();
        for (String resource : RESOURCES) {
            JsonObject root = loadRoot(resource);
            for (JsonElement value : root.getAsJsonArray("fluids")) {
                Entry entry = decode(value.getAsJsonObject());
                if (reservedFluidIds.contains(entry.id())
                        || reservedFluidIds.contains("flowing_" + entry.id())) {
                    throw new IllegalStateException(
                            "Chemical fluid id collides with an existing or projected "
                                    + "fluid registration: " + entry.id());
                }
                MaterialDefinition material = definitionsById.get(entry.materialId());
                if (material == null) {
                    throw new IllegalStateException(
                            "Chemical fluid gate references unknown material: "
                                    + entry.materialId());
                }
                if (!entry.color().equals(material.color())) {
                    throw new IllegalStateException(
                            "Chemical fluid color drifted from material "
                                    + entry.materialId() + ": " + entry.color()
                                    + " != " + material.color());
                }
                Entry previous = byId.putIfAbsent(entry.id(), entry);
                if (previous != null) {
                    throw new IllegalStateException(
                            "Duplicate chemical fluid id: " + entry.id());
                }
            }
        }
        return List.copyOf(byId.values());
    }

    private static JsonObject loadRoot(String resource) {
        JsonObject root;
        try (var stream =
                ChemicalFluidRegistrationGate.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing committed chemical fluid gate: " + resource);
            }
            root = JsonParser.parseReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to load committed chemical fluid gate " + resource,
                    exception);
        }
        if (!root.has("schema_version")
                || root.get("schema_version").getAsInt() != 1
                || !root.has("fluids")
                || !root.get("fluids").isJsonArray()) {
            throw new IllegalStateException(
                    "Unsupported chemical fluid gate schema: " + resource);
        }
        return root;
    }

    private static Entry decode(JsonObject value) {
        requireFields(
                value,
                "id",
                "material",
                "state",
                "temperature_kelvin",
                "density",
                "viscosity",
                "color",
                "world_placeable",
                "source");
        String id = value.get("id").getAsString();
        String material = value.get("material").getAsString();
        State state;
        try {
            state = State.valueOf(
                    value.get("state").getAsString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Unsupported chemical fluid state for " + id, exception);
        }
        JsonObject source = value.getAsJsonObject("source");
        requireFields(source, "repository", "revision", "path", "reason");
        return new Entry(
                id,
                material,
                state,
                value.get("temperature_kelvin").getAsInt(),
                value.get("density").getAsInt(),
                value.get("viscosity").getAsInt(),
                value.get("color").getAsString(),
                value.get("world_placeable").getAsBoolean(),
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
                        "Chemical fluid gate entry is missing " + field);
            }
        }
    }

    public enum State {
        LIQUID,
        GAS
    }

    public record Entry(
            String id,
            String materialId,
            State state,
            int temperatureKelvin,
            int density,
            int viscosity,
            String color,
            boolean worldPlaceable,
            Source source) {
        public Entry {
            if (id == null || !id.matches("[a-z0-9_]+")
                    || materialId == null
                    || !materialId.matches("[a-z0-9_]+")) {
                throw new IllegalArgumentException(
                        "Invalid chemical fluid identity: " + id + "/" + materialId);
            }
            if (state == null || source == null
                    || temperatureKelvin < 0
                    || density == 0
                    || viscosity <= 0
                    || !MaterialColors.isValid(color)
                    || worldPlaceable) {
                throw new IllegalArgumentException(
                        "Invalid non-placeable chemical fluid policy for " + id);
            }
        }
    }

    public record Source(
            String repository,
            String revision,
            String path,
            String reason) {
        public Source {
            Map<String, String> values = Map.of(
                    "repository", repository,
                    "revision", revision,
                    "path", path,
                    "reason", reason);
            values.forEach((field, value) -> {
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException(
                            "Chemical fluid source is missing " + field);
                }
            });
        }
    }
}
