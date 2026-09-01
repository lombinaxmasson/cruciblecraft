package com.masson.cruciblecraft.material;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;

/** Strict runtime view of the authored hydrocarbon spring and gas-hazard policy. */
public final class HydrocarbonRuntimePolicy {
    private static final String RESOURCE =
            "/data/cruciblecraft/hydrocarbon_runtime_policy.json";
    private static final Snapshot SNAPSHOT = load();

    private HydrocarbonRuntimePolicy() {}

    public static Production production(ResourceLocation material) {
        Production production = SNAPSHOT.production().get(material);
        if (production == null) {
            throw new IllegalArgumentException(
                    "No hydrocarbon production policy for " + material);
        }
        return production;
    }

    public static boolean supportsProduction(ResourceLocation material) {
        return SNAPSHOT.production().containsKey(material);
    }

    public static ResourceLocation migrate(ResourceLocation material) {
        return SNAPSHOT.migrations().getOrDefault(material, material);
    }

    public static boolean isFlammable(ResourceLocation fluidId) {
        return SNAPSHOT.flammable().getOrDefault(fluidId, false);
    }

    public static Cloud cloud() {
        return SNAPSHOT.cloud();
    }

    private static Snapshot load() {
        JsonObject root;
        try (var stream =
                HydrocarbonRuntimePolicy.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing hydrocarbon runtime policy");
            }
            root = JsonParser.parseReader(new InputStreamReader(
                    stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException(
                    "Failed to load hydrocarbon runtime policy", exception);
        }
        if (integer(root, "schema_version") != 1
                || !"DESIGN_POLICY".equals(string(root, "status"))) {
            throw new IllegalStateException(
                    "Unsupported hydrocarbon runtime policy");
        }
        JsonObject depletion = object(root, "depletion");
        if (!"non_depleting".equals(string(depletion, "mode"))
                || bool(depletion, "rate_decay")
                || !"migration_and_diagnostics_only".equals(
                        string(depletion, "legacy_reserve_role"))) {
            throw new IllegalStateException(
                    "hydrocarbon initial production must remain non-depleting");
        }

        LinkedHashMap<ResourceLocation, ResourceLocation> migrations =
                new LinkedHashMap<>();
        object(root, "deposit_migrations").entrySet().forEach(entry ->
                migrations.put(
                        id(entry.getKey()),
                        id(entry.getValue().getAsString())));

        LinkedHashMap<ResourceLocation, Production> production =
                new LinkedHashMap<>();
        object(root, "production").entrySet().forEach(entry -> {
            ResourceLocation material = id(entry.getKey());
            JsonObject value = entry.getValue().getAsJsonObject();
            String state = string(value, "state");
            if (!state.equals("liquid") && !state.equals("gas")) {
                throw new IllegalStateException(
                        "Unsupported hydrocarbon production state " + state);
            }
            Production selected = new Production(
                    positive(value, "amount_mb"),
                    positive(value, "interval_ticks"),
                    positive(value, "accumulation_cap_mb"),
                    bool(value, "vent_overflow"),
                    state.equals("gas"));
            if (selected.accumulationCapMb() < selected.amountMb()
                    || selected.ventOverflow() != selected.gas()) {
                throw new IllegalStateException(
                        "Invalid hydrocarbon production policy for " + material);
            }
            production.put(material, selected);
        });

        JsonObject cloud = object(root, "gas_cloud");
        Cloud cloudPolicy = new Cloud(
                positive(cloud, "parcel_mb"),
                positive(cloud, "movement_interval_ticks"),
                positive(cloud, "lifetime_ticks"),
                positive(cloud, "burn_interval_ticks"),
                positive(cloud, "burn_consumption_mb"),
                positive(cloud, "maximum_emitted_mb"));
        if (cloudPolicy.parcelMb() > cloudPolicy.maximumEmittedMb()) {
            throw new IllegalStateException(
                    "hydrocarbon gas cloud parcel exceeds its bounded emission");
        }

        LinkedHashMap<ResourceLocation, Boolean> flammable =
                new LinkedHashMap<>();
        object(root, "hazards").entrySet().forEach(entry -> {
            JsonObject value = entry.getValue().getAsJsonObject();
            boolean tagged = value.getAsJsonArray("material_tags").asList()
                    .stream()
                    .anyMatch(tag -> "PROPERTIES.FLAMMABLE".equals(
                            tag.getAsString()));
            flammable.put(id(entry.getKey()), tagged);
        });
        if (production.isEmpty() || flammable.values().stream().noneMatch(
                Boolean::booleanValue)) {
            throw new IllegalStateException(
                    "hydrocarbon runtime policy has no production or live flammable gas");
        }
        object(root, "source_policy");
        return new Snapshot(
                Map.copyOf(production),
                Map.copyOf(migrations),
                Map.copyOf(flammable),
                cloudPolicy);
    }

    private static JsonObject object(JsonObject value, String field) {
        if (!value.has(field) || !value.get(field).isJsonObject()) {
            throw new IllegalStateException(
                    "hydrocarbon runtime policy is missing object " + field);
        }
        return value.getAsJsonObject(field);
    }

    private static String string(JsonObject value, String field) {
        if (!value.has(field) || !value.get(field).isJsonPrimitive()) {
            throw new IllegalStateException(
                    "hydrocarbon runtime policy is missing string " + field);
        }
        return value.get(field).getAsString();
    }

    private static boolean bool(JsonObject value, String field) {
        if (!value.has(field) || !value.get(field).isJsonPrimitive()) {
            throw new IllegalStateException(
                    "hydrocarbon runtime policy is missing boolean " + field);
        }
        return value.get(field).getAsBoolean();
    }

    private static int integer(JsonObject value, String field) {
        if (!value.has(field) || !value.get(field).isJsonPrimitive()) {
            throw new IllegalStateException(
                    "hydrocarbon runtime policy is missing integer " + field);
        }
        return value.get(field).getAsInt();
    }

    private static int positive(JsonObject value, String field) {
        int result = integer(value, field);
        if (result <= 0) {
            throw new IllegalStateException(
                    "hydrocarbon runtime policy " + field + " must be positive");
        }
        return result;
    }

    private static ResourceLocation id(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalStateException(
                    "Invalid hydrocarbon resource location " + value);
        }
        return id;
    }

    public record Production(
            int amountMb,
            int intervalTicks,
            int accumulationCapMb,
            boolean ventOverflow,
            boolean gas) {}

    public record Cloud(
            int parcelMb,
            int movementIntervalTicks,
            int lifetimeTicks,
            int burnIntervalTicks,
            int burnConsumptionMb,
            int maximumEmittedMb) {}

    private record Snapshot(
            Map<ResourceLocation, Production> production,
            Map<ResourceLocation, ResourceLocation> migrations,
            Map<ResourceLocation, Boolean> flammable,
            Cloud cloud) {}
}
