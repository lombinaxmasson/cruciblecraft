package com.masson.cruciblecraft.material;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.masson.cruciblecraft.CrucibleCraft;

/**
 * zh_cn name table for the v1 critical path (T26d).
 *
 * <p>Loaded once from the bundled {@code data/cruciblecraft/material_zh_cn.json}
 * and shared by the datagen language provider and the runtime generated
 * material pack. Materials (or prefix/pipe/conductor forms) absent from the
 * table are explicitly {@code post_1_0}: no Chinese key is ever emitted for
 * them, the en_us name is shown, and the T26 localization ledger records them
 * as post_1_0. English-copy fake translations are forbidden by policy.
 */
public final class MaterialZhNames {
    public static final String RESOURCE = "data/cruciblecraft/material_zh_cn.json";

    private static volatile Map<String, String> materials = Map.of();
    private static volatile Map<String, String> prefixes = Map.of();
    private static volatile Map<String, String> pipes = Map.of();
    private static volatile Map<String, String> conductors = Map.of();
    private static volatile boolean loaded;

    private MaterialZhNames() {}

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        synchronized (MaterialZhNames.class) {
            if (loaded) {
                return;
            }
            try (InputStream input = MaterialZhNames.class.getClassLoader()
                    .getResourceAsStream(RESOURCE)) {
                if (input != null) {
                    JsonObject root = JsonParser.parseReader(
                                    new InputStreamReader(
                                            input, StandardCharsets.UTF_8))
                            .getAsJsonObject();
                    materials = mapOf(root.getAsJsonObject("materials"));
                    prefixes = mapOf(root.getAsJsonObject("prefixes"));
                    pipes = mapOf(root.getAsJsonObject("pipes"));
                    conductors = mapOf(root.getAsJsonObject("conductors"));
                }
            } catch (Exception failure) {
                CrucibleCraft.LOGGER.warn(
                        "Could not load the zh_cn material name table",
                        failure);
            }
            loaded = true;
        }
    }

    private static Map<String, String> mapOf(JsonObject object) {
        if (object == null) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, String> result =
                new java.util.LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return Map.copyOf(result);
    }

    public static Optional<String> material(String id) {
        ensureLoaded();
        return Optional.ofNullable(materials.get(id));
    }

    public static Optional<String> prefix(String serializedName) {
        ensureLoaded();
        return Optional.ofNullable(prefixes.get(serializedName));
    }

    public static Optional<String> pipe(String serializedName) {
        ensureLoaded();
        return Optional.ofNullable(pipes.get(serializedName));
    }

    public static Optional<String> conductor(String serializedName) {
        ensureLoaded();
        return Optional.ofNullable(conductors.get(serializedName));
    }
}
