package com.masson.cruciblecraft.content.item;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/** Sidecar SOURCE_BACKED block textures. Sealed catalogs stay vanilla. */
public final class BlockArtIndex {
    private static final String RESOURCE =
            "/data/cruciblecraft/block_art_index.json";
    private static final Gson GSON = new Gson();
    private static final Map<String, Entry> ENTRIES = load();

    private BlockArtIndex() {}

    public static ResourceLocation texture(String registryPath) {
        Entry entry = ENTRIES.get(registryPath);
        return entry == null ? null : entry.texture;
    }

    public static ResourceLocation sideTexture(String registryPath) {
        Entry entry = ENTRIES.get(registryPath);
        return entry == null ? null : entry.side;
    }

    public static boolean dyeTint(String registryPath) {
        Entry entry = ENTRIES.get(registryPath);
        return entry != null && entry.dyeTint;
    }

    public static int dyeColor(int meta) {
        return DYES[meta & 15];
    }

    private static final int[] DYES = {
        0x202020, 0xFF0000, 0x00FF00, 0x604000,
        0x0000FF, 0x800080, 0x00FFFF, 0xC0C0C0,
        0x808080, 0xFFC0C0, 0x80FF80, 0xFFFF00,
        0x8080FF, 0xFF00FF, 0xFF8000, 0xFFFFFF
    };

    private static Map<String, Entry> load() {
        try (var stream = BlockArtIndex.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                return Map.of();
            }
            JsonObject document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    JsonObject.class);
            if (document == null || !document.has("textures")) {
                return Map.of();
            }
            Map<String, Entry> entries = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> row :
                    document.getAsJsonObject("textures").entrySet()) {
                JsonObject body = row.getValue().getAsJsonObject();
                String texture = body.get("texture").getAsString();
                boolean dye = body.has("tint")
                        && !body.get("tint").isJsonNull()
                        && "dye".equals(body.get("tint").getAsString());
                ResourceLocation side = null;
                if (body.has("side") && !body.get("side").isJsonNull()) {
                    side = ResourceLocation.parse(body.get("side").getAsString());
                }
                entries.put(
                        row.getKey(),
                        new Entry(ResourceLocation.parse(texture), side, dye));
            }
            return Collections.unmodifiableMap(entries);
        } catch (IOException failure) {
            throw new IllegalStateException(
                    "Failed to read block art index", failure);
        }
    }

    private record Entry(
            ResourceLocation texture, ResourceLocation side, boolean dyeTint) {}
}
