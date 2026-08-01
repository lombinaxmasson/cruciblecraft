package com.masson.cruciblecraft.material.def;

import java.util.Optional;

import com.google.gson.JsonObject;

/**
 * Non-structural material overrides. Applying one cannot change registry ids,
 * generated forms, form item mappings, fluids, or alloy composition.
 */
public record MaterialTuning(
        String id,
        Optional<Integer> tier,
        Optional<String> color,
        Optional<Double> meltingPoint,
        Optional<Double> boilingPoint,
        Optional<Double> density) {

    public MaterialTuning {
        if (id == null || !id.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid material id: " + id);
        }
        tier = tier == null ? Optional.empty() : tier;
        color = color == null ? Optional.empty() : color;
        meltingPoint = meltingPoint == null ? Optional.empty() : meltingPoint;
        boilingPoint = boilingPoint == null ? Optional.empty() : boilingPoint;
        density = density == null ? Optional.empty() : density;
    }

    public static MaterialTuning fromJson(JsonObject json) {
        if (!json.has("id")) {
            throw new IllegalArgumentException("Material tuning must declare an id");
        }
        if (json.has("thermal") && !json.get("thermal").isJsonObject()) {
            throw new IllegalArgumentException("Material tuning thermal field must be an object");
        }
        JsonObject thermal = json.has("thermal")
                ? json.getAsJsonObject("thermal")
                : new JsonObject();
        return new MaterialTuning(
                json.get("id").getAsString(),
                optionalInt(json, "tier"),
                optionalString(json, "color"),
                optionalDouble(thermal, "melting_point"),
                optionalDouble(thermal, "boiling_point"),
                optionalDouble(thermal, "density"));
    }

    public MaterialDefinition apply(MaterialDefinition base) {
        if (!id.equals(base.id())) {
            throw new IllegalArgumentException(
                    "Cannot apply material tuning " + id + " to " + base.id());
        }
        ThermalProperties baseThermal = base.thermal();
        return base.withTuning(
                tier.orElse(base.tier()),
                color.orElse(base.color()),
                new ThermalProperties(
                        meltingPoint.orElse(baseThermal.meltingPoint()),
                        boilingPoint.orElse(baseThermal.boilingPoint()),
                        density.orElse(baseThermal.density())));
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return json.has(key) ? Optional.of(json.get(key).getAsInt()) : Optional.empty();
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) ? Optional.of(json.get(key).getAsString()) : Optional.empty();
    }

    private static Optional<Double> optionalDouble(JsonObject json, String key) {
        return json.has(key) ? Optional.of(json.get(key).getAsDouble()) : Optional.empty();
    }
}
