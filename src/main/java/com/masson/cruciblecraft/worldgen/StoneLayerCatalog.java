package com.masson.cruciblecraft.worldgen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * No-mod GT6 {@code StoneLayer.LAYERS} surface materials for cube replace
 * and 32757 pebbles.
 */
public final class StoneLayerCatalog {
    public static final String DEEPSLATE = "deepslate";
    public static final int NO_DEEP_Y = 24;
    public static final int PROBABILITY = 128;

    public record Layer(String material, boolean noDeep) {}

    private static final List<Layer> LAYERS = load();

    private StoneLayerCatalog() {}

    public static List<Layer> layers() {
        return LAYERS;
    }

    public static String surfaceMaterial(Layer layer, int y) {
        if (y < NO_DEEP_Y && layer.noDeep()) {
            return DEEPSLATE;
        }
        return layer.material();
    }

    public static String surfaceMaterial(StoneLayerNoise noise, int x, int y, int z) {
        return surfaceMaterial(LAYERS.get(noise.get(x, y, z, LAYERS.size())), y);
    }

    private static List<Layer> load() {
        try (InputStream in = StoneLayerCatalog.class.getResourceAsStream(
                "/data/cruciblecraft/worldgen_catalog/stone_layer_rocks.json")) {
            if (in == null) {
                throw new IllegalStateException("missing stone_layer_rocks.json");
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonArray array = root.getAsJsonArray("layers");
            List<Layer> layers = new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                JsonObject row = array.get(i).getAsJsonObject();
                layers.add(new Layer(
                        row.get("material").getAsString(),
                        row.get("no_deep").getAsBoolean()));
            }
            if (layers.isEmpty()) {
                throw new IllegalStateException("empty stone layer catalog");
            }
            return Collections.unmodifiableList(layers);
        } catch (IOException error) {
            throw new IllegalStateException("stone layer catalog", error);
        }
    }
}
