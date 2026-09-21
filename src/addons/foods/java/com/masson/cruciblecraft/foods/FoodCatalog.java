package com.masson.cruciblecraft.foods;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class FoodCatalog {
    private static final Gson GSON = new Gson();
    private static final List<FoodIdentity> ITEMS = load();

    private FoodCatalog() {}

    public static List<FoodIdentity> items() {
        return ITEMS;
    }

    private static List<FoodIdentity> load() {
        try (var stream = FoodCatalog.class.getResourceAsStream(
                "/data/cruciblecraft_foods/food_catalog.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing food catalog");
            }
            JsonObject root = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    JsonObject.class);
            List<FoodIdentity> identities = new ArrayList<>();
            JsonArray array = root.getAsJsonArray("items");
            for (JsonElement element : array) {
                JsonObject json = element.getAsJsonObject();
                List<String> traits = new ArrayList<>();
                json.getAsJsonArray("traits").forEach(trait ->
                        traits.add(trait.getAsString()));
                identities.add(new FoodIdentity(
                        json.get("path").getAsString(),
                        json.get("il").getAsString(),
                        json.get("english").getAsString(),
                        json.get("chinese").getAsString(),
                        json.get("nutrition").getAsInt(),
                        json.get("saturation").getAsFloat(),
                        json.get("always_edible").getAsBoolean(),
                        List.copyOf(traits),
                        json.has("reuse_canonical") && !json.get("reuse_canonical").isJsonNull()
                                ? json.get("reuse_canonical").getAsString()
                                : null));
            }
            return List.copyOf(identities);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load food catalog", exception);
        }
    }
}
