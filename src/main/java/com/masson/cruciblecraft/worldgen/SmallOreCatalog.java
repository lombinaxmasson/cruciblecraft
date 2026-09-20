package com.masson.cruciblecraft.worldgen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * No-mod GT6 {@code WorldgenOresSmall} rows that run on {@code GEN_GT} /
 * {@code GEN_NETHER}, plus {@code WorldgenColtan}.
 */
public final class SmallOreCatalog {
    private static final Document DOCUMENT = load();
    public static final List<Entry> ENTRIES = DOCUMENT.entries();
    public static final Coltan COLTAN = DOCUMENT.coltan();
    public static final int OVERWORLD_COUNT = DOCUMENT.overworldCount();
    public static final int NETHER_COUNT = DOCUMENT.netherCount();
    public static final int UNIQUE_NAME_COUNT = DOCUMENT.uniqueNameCount();

    private SmallOreCatalog() {}

    public static List<Entry> forNether(boolean nether) {
        return ENTRIES.stream()
                .filter(entry -> nether ? entry.nether() : entry.overworld())
                .toList();
    }

    private static Document load() {
        try (InputStream in = SmallOreCatalog.class.getResourceAsStream(
                "/data/cruciblecraft/worldgen_catalog/small_ores.json")) {
            if (in == null) {
                throw new IllegalStateException("missing small_ores.json");
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonArray array = root.getAsJsonArray("entries");
            List<Entry> entries = new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                entries.add(readEntry(array.get(i).getAsJsonObject()));
            }
            if (entries.isEmpty()) {
                throw new IllegalStateException("empty small ore catalog");
            }
            return new Document(
                    List.copyOf(entries),
                    readColtan(root.getAsJsonObject("coltan")),
                    root.get("overworld_count").getAsInt(),
                    root.get("nether_count").getAsInt(),
                    root.get("unique_name_count").getAsInt());
        } catch (IOException failure) {
            throw new IllegalStateException("small_ores.json", failure);
        }
    }

    private static Entry readEntry(JsonObject row) {
        return new Entry(
                row.get("gt6_name").getAsString(),
                row.get("material").getAsString(),
                row.get("min_y").getAsInt(),
                row.get("max_y").getAsInt(),
                row.get("amount").getAsInt(),
                row.get("overworld").getAsBoolean(),
                row.get("nether").getAsBoolean());
    }

    private static Coltan readColtan(JsonObject row) {
        JsonArray materials = row.getAsJsonArray("materials");
        List<String> ids = new ArrayList<>(materials.size());
        for (int i = 0; i < materials.size(); i++) {
            ids.add(materials.get(i).getAsString());
        }
        return new Coltan(
                row.get("gt6_name").getAsString(),
                row.get("min_y").getAsInt(),
                row.get("max_y").getAsInt(),
                row.get("amount").getAsInt(),
                row.get("range").getAsInt(),
                row.get("seed_offset").getAsInt(),
                row.get("gaussian_scale").getAsDouble(),
                row.get("large_ore_range_squared").getAsInt(),
                List.copyOf(ids));
    }

    private record Document(
            List<Entry> entries,
            Coltan coltan,
            int overworldCount,
            int netherCount,
            int uniqueNameCount) {}

    public record Entry(
            String gt6Name,
            String materialId,
            int minY,
            int maxY,
            int amount,
            boolean overworld,
            boolean nether) {
        public Entry {
            if (amount < 1) {
                throw new IllegalArgumentException(gt6Name);
            }
            if (maxY <= minY) {
                throw new IllegalArgumentException(gt6Name);
            }
        }

        public int ySpan() {
            return Math.max(1, maxY - minY);
        }
    }

    public record Coltan(
            String gt6Name,
            int minY,
            int maxY,
            int amount,
            int range,
            int seedOffset,
            double gaussianScale,
            int largeOreRangeSquared,
            List<String> materials) {
        public Coltan {
            if (materials == null || materials.size() != 3) {
                throw new IllegalArgumentException(gt6Name);
            }
        }

        public int ySpan() {
            return Math.max(1, maxY - minY);
        }

        public String pickMaterial(int roll) {
            return switch (roll) {
                case 0 -> materials.get(1);
                case 1 -> materials.get(2);
                default -> materials.get(0);
            };
        }
    }
}
