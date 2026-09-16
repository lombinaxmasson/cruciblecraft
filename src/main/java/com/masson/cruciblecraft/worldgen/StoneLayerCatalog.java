package com.masson.cruciblecraft.worldgen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

/**
 * No-mod GT6 {@code StoneLayer.LAYERS}, {@code StoneLayer.MAP}, and
 * {@code StoneLayer.DEEPSLATE} for cube replace, pebbles, and layer ores.
 */
public final class StoneLayerCatalog {
    public static final String DEEPSLATE = "deepslate";
    public static final int NO_DEEP_Y = 24;
    public static final int PROBABILITY = 128;

    public record Ore(
            String material,
            int chance,
            int minY,
            int maxY,
            boolean indicators,
            String vanillaBlock,
            List<String> biomes,
            boolean neverBiome) {}

    public record Layer(
            String material,
            String layerMaterial,
            boolean noDeep,
            List<Ore> ores) {}

    private static final Document DOCUMENT = load();
    public static final int UNIT = DOCUMENT.unit;
    private static final List<Layer> LAYERS = DOCUMENT.layers;
    public static final Layer DEEPSLATE_LAYER = DOCUMENT.deepslate;
    private static final Map<String, Map<String, List<Ore>>> BOUNDARIES =
            DOCUMENT.boundaries;
    private static final List<String> RANDOM_SMALL_GEMS = DOCUMENT.gems;

    private StoneLayerCatalog() {}

    public static List<Layer> layers() {
        return LAYERS;
    }

    public static List<String> randomSmallGems() {
        return RANDOM_SMALL_GEMS;
    }

    public static Layer layerAt(StoneLayerNoise noise, int x, int sampleY, int z) {
        return layerAt(noise, x, sampleY, z, 0);
    }

    public static Layer layerAt(
            StoneLayerNoise noise, int x, int sampleY, int z, int minBuildHeight) {
        Layer layer = LAYERS.get(noise.get(x, sampleY, z, LAYERS.size()));
        if (sampleY < noDeepCutoff(minBuildHeight) && layer.noDeep()) {
            return DEEPSLATE_LAYER;
        }
        return layer;
    }

    public static int noDeepCutoff(int minBuildHeight) {
        return minBuildHeight + NO_DEEP_Y;
    }

    public static String surfaceMaterial(Layer layer, int y) {
        return surfaceMaterial(layer, y, 0);
    }

    public static String surfaceMaterial(Layer layer, int y, int minBuildHeight) {
        if (y < noDeepCutoff(minBuildHeight) && layer.noDeep()) {
            return DEEPSLATE;
        }
        return layer.material();
    }

    public static String surfaceMaterial(StoneLayerNoise noise, int x, int y, int z) {
        return layerAt(noise, x, y, z).material();
    }

    public static List<Ore> boundaryOres(Layer top, Layer bottom) {
        Map<String, List<Ore>> byBottom = BOUNDARIES.get(top.layerMaterial());
        if (byBottom == null) {
            return List.of();
        }
        List<Ore> ores = byBottom.get(bottom.layerMaterial());
        return ores == null ? List.of() : ores;
    }

    public static boolean check(
            Ore ore, int y, RandomSource random, Holder<Biome> biome) {
        return check(ore, y, random, biome, 0);
    }

    public static boolean check(
            Ore ore,
            int y,
            RandomSource random,
            Holder<Biome> biome,
            int minBuildHeight) {
        int minY = ore.minY() == 0 ? minBuildHeight : ore.minY();
        return y >= minY
                && y <= ore.maxY()
                && random.nextInt(UNIT) < ore.chance()
                && biomeMatches(ore, biome);
    }

    public static boolean biomeMatches(Ore ore, Holder<Biome> biome) {
        if (ore.neverBiome()) {
            return false;
        }
        if (ore.biomes().isEmpty()) {
            return true;
        }
        for (String entry : ore.biomes()) {
            if (entry.startsWith("#")) {
                TagKey<Biome> tag = TagKey.create(
                        Registries.BIOME,
                        ResourceLocation.parse(entry.substring(1)));
                if (biome.is(tag)) {
                    return true;
                }
            } else if (biome.is(ResourceKey.create(
                    Registries.BIOME, ResourceLocation.parse(entry)))) {
                return true;
            }
        }
        return false;
    }

    public static String pickRandomGem(RandomSource random) {
        if (RANDOM_SMALL_GEMS.isEmpty()) {
            return "emerald";
        }
        return RANDOM_SMALL_GEMS.get(random.nextInt(RANDOM_SMALL_GEMS.size()));
    }

    private record Document(
            int unit,
            List<Layer> layers,
            Layer deepslate,
            Map<String, Map<String, List<Ore>>> boundaries,
            List<String> gems) {}

    private static Document load() {
        try (InputStream in = StoneLayerCatalog.class.getResourceAsStream(
                "/data/cruciblecraft/worldgen_catalog/stone_layer_rocks.json")) {
            if (in == null) {
                throw new IllegalStateException("missing stone_layer_rocks.json");
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            int unit = root.get("unit").getAsInt();
            JsonArray array = root.getAsJsonArray("layers");
            List<Layer> layers = new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                layers.add(readLayer(array.get(i).getAsJsonObject()));
            }
            if (layers.isEmpty()) {
                throw new IllegalStateException("empty stone layer catalog");
            }
            Layer deepslate = readLayer(root.getAsJsonObject("deepslate_layer"));
            Map<String, Map<String, List<Ore>>> boundaries = new HashMap<>();
            JsonArray boundArray = root.getAsJsonArray("boundaries");
            for (int i = 0; i < boundArray.size(); i++) {
                JsonObject row = boundArray.get(i).getAsJsonObject();
                if (row.get("top").isJsonNull() || row.get("bottom").isJsonNull()) {
                    continue;
                }
                String top = row.get("top").getAsString();
                String bottom = row.get("bottom").getAsString();
                List<Ore> ores = readOres(row.getAsJsonArray("ores"));
                putBoundary(boundaries, top, bottom, ores);
                if ("bothsides".equals(row.get("kind").getAsString())) {
                    putBoundary(boundaries, bottom, top, ores);
                }
            }
            List<String> gems = new ArrayList<>();
            JsonArray gemArray = root.getAsJsonArray("random_small_gems");
            for (int i = 0; i < gemArray.size(); i++) {
                gems.add(gemArray.get(i).getAsString());
            }
            return new Document(
                    unit,
                    Collections.unmodifiableList(layers),
                    deepslate,
                    boundaries,
                    Collections.unmodifiableList(gems));
        } catch (IOException error) {
            throw new IllegalStateException("stone layer catalog", error);
        }
    }

    private static void putBoundary(
            Map<String, Map<String, List<Ore>>> boundaries,
            String top,
            String bottom,
            List<Ore> ores) {
        boundaries.computeIfAbsent(top, ignored -> new HashMap<>())
                .put(bottom, ores);
    }

    private static Layer readLayer(JsonObject row) {
        return new Layer(
                row.get("material").getAsString(),
                row.has("layer_material")
                        ? row.get("layer_material").getAsString()
                        : row.get("material").getAsString(),
                row.has("no_deep") && row.get("no_deep").getAsBoolean(),
                readOres(row.getAsJsonArray("ores")));
    }

    private static List<Ore> readOres(JsonArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<Ore> ores = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JsonObject row = array.get(i).getAsJsonObject();
            List<String> biomes = new ArrayList<>();
            if (row.has("biomes")) {
                JsonArray biomeArray = row.getAsJsonArray("biomes");
                for (int j = 0; j < biomeArray.size(); j++) {
                    biomes.add(biomeArray.get(j).getAsString());
                }
            }
            JsonElement material = row.get("material");
            ores.add(new Ore(
                    material == null || material.isJsonNull()
                            ? null
                            : material.getAsString(),
                    row.get("chance").getAsInt(),
                    row.get("min_y").getAsInt(),
                    row.get("max_y").getAsInt(),
                    !row.has("indicators") || row.get("indicators").getAsBoolean(),
                    row.has("vanilla_block")
                            ? row.get("vanilla_block").getAsString()
                            : null,
                    List.copyOf(biomes),
                    row.has("never_biome") && row.get("never_biome").getAsBoolean()));
        }
        return List.copyOf(ores);
    }
}
