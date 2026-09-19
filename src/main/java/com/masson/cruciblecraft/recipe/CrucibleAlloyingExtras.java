package com.masson.cruciblecraft.recipe;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * GT6 {@code MT.addAlloyingRecipe} extras that are not a material's first-level
 * {@code mComponents} / {@code alloySimple} composition.
 */
public final class CrucibleAlloyingExtras {
    private static final String RESOURCE =
            "/data/cruciblecraft/crucible_alloying_extras.json";

    private CrucibleAlloyingExtras() {}

    public record Recipe(String result, int output, Map<String, Integer> inputs) {
        public Recipe {
            if (result == null || result.isBlank() || output <= 0) {
                throw new IllegalArgumentException("Invalid crucible alloying extra");
            }
            inputs = Map.copyOf(new LinkedHashMap<>(inputs));
        }
    }

    public record Bundle(
            Map<String, Integer> compositionOutputs, List<Recipe> recipes) {
        public Bundle {
            compositionOutputs = Map.copyOf(new LinkedHashMap<>(compositionOutputs));
            recipes = List.copyOf(recipes);
        }
    }

    public static List<Recipe> load() {
        return loadBundle().recipes();
    }

    public static Bundle loadBundle() {
        try (Reader reader = resourceReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            LinkedHashMap<String, Integer> compositionOutputs = new LinkedHashMap<>();
            if (root.has("composition_outputs")
                    && root.get("composition_outputs").isJsonObject()) {
                for (var entry : root.getAsJsonObject("composition_outputs").entrySet()) {
                    int output = entry.getValue().getAsInt();
                    if (output > 0) {
                        compositionOutputs.put(entry.getKey(), output);
                    }
                }
            }
            List<Recipe> recipes = new ArrayList<>();
            for (JsonElement entry : root.getAsJsonArray("recipes")) {
                JsonObject json = entry.getAsJsonObject();
                LinkedHashMap<String, Integer> inputs = new LinkedHashMap<>();
                JsonObject inputJson = json.getAsJsonObject("inputs");
                for (var input : inputJson.entrySet()) {
                    int amount = input.getValue().getAsInt();
                    if (amount > 0) {
                        inputs.put(input.getKey(), amount);
                    }
                }
                if (inputs.size() < 2) {
                    continue;
                }
                recipes.add(new Recipe(
                        json.get("result").getAsString(),
                        json.get("output").getAsInt(),
                        inputs));
            }
            return new Bundle(compositionOutputs, recipes);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Failed to load crucible alloying extras", exception);
        }
    }

    private static Reader resourceReader() throws IOException {
        var stream = CrucibleAlloyingExtras.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IOException("Missing " + RESOURCE);
        }
        return new InputStreamReader(stream, StandardCharsets.UTF_8);
    }
}
