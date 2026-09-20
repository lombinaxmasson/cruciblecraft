package com.masson.cruciblecraft.logistics.pipe.cover;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;

import net.minecraft.resources.ResourceLocation;

/** Strict bundled catalog for data-defined cover instances. */
public final class CoverDefinitionCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/cover_definitions.json";
    private static final String ITEM_NETWORK_RESOURCE =
            "/data/cruciblecraft/item_network_cover_definitions.json";
    private static final String FLUID_NETWORK_RESOURCE =
            "/data/cruciblecraft/fluid_network_cover_definitions.json";
    private static final String GENERIC_NETWORK_RESOURCE =
            "/data/cruciblecraft/generic_network_cover_definitions.json";
    private static final String DUMP_COVER_RESOURCE =
            "/data/cruciblecraft/logistics_dump_cover_definitions.json";
    private static final String DISPLAY_CPU_RESOURCE =
            "/data/cruciblecraft/logistics_display_cpu_cover_definitions.json";
    private static final String COMPONENT_TIER_RESOURCE =
            "/data/cruciblecraft/cover_component_tier_definitions.json";
    private static final String MACHINE_COVER_RESOURCE =
            "/data/cruciblecraft/machine_cover_definitions.json";
    private static final String PLATE_COVER_RESOURCE =
            "/data/cruciblecraft/cover_plate_definitions.json";
    private static final String DECORATIVE_COVER_RESOURCE =
            "/data/cruciblecraft/cover_decorative_definitions.json";
    private static final Set<String> DOCUMENT_FIELDS =
            Set.of("schemaVersion", "definitions");
    private static final Set<String> DEFINITION_FIELDS = Set.of(
            "id",
            "behavior",
            "medium",
            "values",
            "configurable");
    private static final Set<String> VALUE_FIELDS = Set.of(
            "rate",
            "pressureThreshold",
            "exactCount",
            "mode",
            "selector");
    private static final Set<String> REQUIRED_IDS = Set.of(
            "cruciblecraft:filter",
            "cruciblecraft:shutter",
            "cruciblecraft:pump",
            "cruciblecraft:conveyor",
            "cruciblecraft:retriever_item",
            "cruciblecraft:robot_arm",
            "cruciblecraft:pressure_valve",
            "cruciblecraft:selector_manual");
    private static final Set<String> REQUIRED_ITEM_NETWORK_IDS = Set.of(
            "cruciblecraft:logistics_item_storage",
            "cruciblecraft:logistics_item_import",
            "cruciblecraft:logistics_item_export");
    private static final Set<String> REQUIRED_FLUID_NETWORK_IDS = Set.of(
            "cruciblecraft:logistics_fluid_storage",
            "cruciblecraft:logistics_fluid_import",
            "cruciblecraft:logistics_fluid_export");
    private static final Set<String> REQUIRED_GENERIC_NETWORK_IDS = Set.of(
            "cruciblecraft:logistics_generic_storage",
            "cruciblecraft:logistics_generic_import",
            "cruciblecraft:logistics_generic_export");
    private static final Set<String> REQUIRED_DUMP_IDS = Set.of(
            "cruciblecraft:logistics_generic_dump");
    private static final Set<String> REQUIRED_PLATE_IDS = Set.of(
            "cruciblecraft:cover_plate");
    private static final Set<String> REQUIRED_DECORATIVE_IDS = Set.of(
            "cruciblecraft:cover_asphalt",
            "cruciblecraft:cover_wood",
            "cruciblecraft:cover_panel");
    private static final Set<String> REQUIRED_DISPLAY_CPU_IDS = Set.of(
            "cruciblecraft:logistics_display_cpu_logic",
            "cruciblecraft:logistics_display_cpu_control",
            "cruciblecraft:logistics_display_cpu_storage",
            "cruciblecraft:logistics_display_cpu_conversion");
    private static final Map<ResourceLocation, CoverDefinition> DEFINITIONS =
            loadBundled();

    public static List<CoverDefinition> definitions() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static Optional<CoverDefinition> find(ResourceLocation id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static CoverDefinition require(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        CoverDefinition definition =
                parsed == null ? null : DEFINITIONS.get(parsed);
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unknown cover definition " + id);
        }
        return definition;
    }

    public static CoverDefinition require(ResourceLocation id) {
        CoverDefinition definition = DEFINITIONS.get(id);
        if (definition == null) {
            throw new IllegalArgumentException(
                    "Unknown cover definition " + id);
        }
        return definition;
    }

    private static Map<ResourceLocation, CoverDefinition> loadBundled() {
        LinkedHashMap<ResourceLocation, CoverDefinition> definitions =
                new LinkedHashMap<>();
        loadInto(definitions, RESOURCE, "cover catalog");
        Set<String> bundled = definitions.keySet().stream()
                .map(ResourceLocation::toString)
                .collect(java.util.stream.Collectors.toSet());
        if (!bundled.containsAll(REQUIRED_IDS) || bundled.size() != 9) {
            throw new IllegalStateException(
                    "Base cover definitions drifted: " + bundled);
        }
        loadInto(definitions, ITEM_NETWORK_RESOURCE, "item network cover catalog");
        loadInto(definitions, FLUID_NETWORK_RESOURCE, "fluid network cover catalog");
        loadInto(definitions, GENERIC_NETWORK_RESOURCE, "generic network cover catalog");
        loadInto(definitions, DUMP_COVER_RESOURCE, "dump cover catalog");
        loadInto(
                definitions,
                DISPLAY_CPU_RESOURCE,
                "display cpu cover catalog");
        loadInto(
                definitions,
                COMPONENT_TIER_RESOURCE,
                "compact electric cover catalog");
        loadInto(
                definitions,
                MACHINE_COVER_RESOURCE,
                "machine cover catalog");
        loadInto(definitions, PLATE_COVER_RESOURCE, "plate cover catalog");
        loadInto(
                definitions,
                DECORATIVE_COVER_RESOURCE,
                "decorative cover catalog");
        Set<String> ids = definitions.keySet().stream()
                .map(ResourceLocation::toString)
                .collect(java.util.stream.Collectors.toSet());
        int expected = 27
                + CoverComponentTiers.definitionIds().size()
                + MachineCoverKinds.DEFINITION_COUNT;
        if (!ids.containsAll(REQUIRED_ITEM_NETWORK_IDS)
                || !ids.containsAll(REQUIRED_FLUID_NETWORK_IDS)
                || !ids.containsAll(REQUIRED_GENERIC_NETWORK_IDS)
                || !ids.containsAll(REQUIRED_DUMP_IDS)
                || !ids.containsAll(REQUIRED_DISPLAY_CPU_IDS)
                || !ids.containsAll(REQUIRED_PLATE_IDS)
                || !ids.containsAll(REQUIRED_DECORATIVE_IDS)
                || !ids.containsAll(CoverComponentTiers.definitionIds())
                || definitions.size() != expected) {
            throw new IllegalStateException(
                    "Required cover definitions are missing or the "
                            + "catalog is not exact: " + ids);
        }
        return Map.copyOf(definitions);
    }

    private static void loadInto(
            LinkedHashMap<ResourceLocation, CoverDefinition> definitions,
            String resource,
            String owner) {
        try (var stream = CoverDefinitionCatalog.class.getResourceAsStream(
                resource)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled cover definition catalog " + resource);
            }
            JsonElement parsed = JsonParser.parseReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalStateException(
                        owner + " must be an object");
            }
            JsonObject document = parsed.getAsJsonObject();
            exactFields(document, DOCUMENT_FIELDS, owner);
            if (requiredInt(document, "schemaVersion") != 1
                    || !document.get("definitions").isJsonArray()) {
                throw new IllegalStateException(
                        "Invalid " + owner + " header");
            }
            JsonArray rows = document.getAsJsonArray("definitions");
            for (JsonElement element : rows) {
                if (!element.isJsonObject()) {
                    throw new IllegalStateException(
                            "Cover definition row must be an object");
                }
                CoverDefinition definition = parse(element.getAsJsonObject());
                if (definitions.putIfAbsent(
                        definition.id(), definition) != null) {
                    throw new IllegalStateException(
                            "Duplicate cover definition " + definition.id());
                }
            }
        } catch (IOException
                | JsonIOException
                | JsonSyntaxException
                | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Could not load " + owner, exception);
        }
    }

    private static CoverDefinition parse(JsonObject row) {
        exactFields(row, DEFINITION_FIELDS, "cover definition");
        ResourceLocation id = requiredId(row, "id");
        ResourceLocation behavior = requiredId(row, "behavior");
        CoverDefinition.Medium medium = parseEnum(
                CoverDefinition.Medium.class,
                requiredString(row, "medium"),
                id + ".medium");
        if (!row.get("values").isJsonObject()
                || !row.get("configurable").isJsonArray()) {
            throw new IllegalStateException(
                    id + ": values/configurable have invalid types");
        }
        JsonObject values = row.getAsJsonObject("values");
        valueFields(values, id + ".values");
        CoverDefinition.Values parsedValues = new CoverDefinition.Values(
                requiredInt(values, "rate"),
                requiredInt(values, "pressureThreshold"),
                requiredInt(values, "exactCount"),
                parseEnum(
                        CoverDefinition.TransferMode.class,
                        requiredString(values, "mode"),
                        id + ".values.mode"),
                requiredInt(values, "selector"),
                optionalInt(values, "interval", 1));
        EnumSet<CoverDefinition.ConfigField> configurable =
                EnumSet.noneOf(CoverDefinition.ConfigField.class);
        List<String> duplicateCheck = new ArrayList<>();
        for (JsonElement field : row.getAsJsonArray("configurable")) {
            if (!field.isJsonPrimitive()
                    || !field.getAsJsonPrimitive().isString()) {
                throw new IllegalStateException(
                        id + ": configurable field is not a string");
            }
            String name = field.getAsString();
            duplicateCheck.add(name);
            configurable.add(parseEnum(
                    CoverDefinition.ConfigField.class,
                    name,
                    id + ".configurable"));
        }
        if (duplicateCheck.size() != configurable.size()) {
            throw new IllegalStateException(
                    id + ": duplicate configurable field");
        }
        return new CoverDefinition(
                id,
                behavior,
                medium,
                parsedValues,
                configurable);
    }

    private static ResourceLocation requiredId(
            JsonObject object, String field) {
        String value = requiredString(object, field);
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) {
            throw new IllegalStateException(
                    "Invalid resource id in " + field + ": " + value);
        }
        return parsed;
    }

    private static String requiredString(
            JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing string field " + field);
        }
        return value.getAsString();
    }

    private static int optionalInt(
            JsonObject object, String field, int fallback) {
        if (!object.has(field)) {
            return fallback;
        }
        return requiredInt(object, field);
    }

    private static int requiredInt(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null
                || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException("Missing integer field " + field);
        }
        try {
            return value.getAsInt();
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    "Invalid integer field " + field, exception);
        }
    }

    private static void exactFields(
            JsonObject object, Set<String> expected, String owner) {
        if (!object.keySet().equals(expected)) {
            throw new IllegalStateException(
                    owner + " fields drifted: " + object.keySet());
        }
    }

    private static void valueFields(JsonObject object, String owner) {
        if (!object.keySet().containsAll(VALUE_FIELDS)) {
            throw new IllegalStateException(
                    owner + " missing value fields: " + object.keySet());
        }
        for (String key : object.keySet()) {
            if (!VALUE_FIELDS.contains(key) && !"interval".equals(key)) {
                throw new IllegalStateException(
                        owner + " fields drifted: " + object.keySet());
            }
        }
    }

    private static <T extends Enum<T>> T parseEnum(
            Class<T> type, String value, String owner) {
        try {
            return Enum.valueOf(
                    type, value.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    owner + ": invalid value " + value, exception);
        }
    }

    private CoverDefinitionCatalog() {}
}
