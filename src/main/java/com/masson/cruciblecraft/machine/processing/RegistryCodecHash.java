package com.masson.cruciblecraft.machine.processing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;

/** Deterministic SHA-256 over a complete registry-aware codec representation. */
public final class RegistryCodecHash {
    private RegistryCodecHash() {}

    public static <T> Optional<String> hash(
            Codec<T> codec,
            T value,
            HolderLookup.Provider registries) {
        try {
            Optional<JsonElement> encoded = codec.encodeStart(
                    RegistryOps.create(JsonOps.INSTANCE, registries), value).result();
            if (encoded.isEmpty()) {
                return Optional.empty();
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Optional.of(java.util.HexFormat.of().formatHex(
                    digest.digest(canonical(encoded.get()).getBytes(StandardCharsets.UTF_8))));
        } catch (RuntimeException | NoSuchAlgorithmException exception) {
            return Optional.empty();
        }
    }

    private static String canonical(JsonElement value) {
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            StringBuilder result = new StringBuilder("{");
            object.entrySet().stream()
                    .sorted(Comparator.comparing(java.util.Map.Entry::getKey))
                    .forEach(entry -> result
                            .append(entry.getKey().length()).append(':').append(entry.getKey())
                            .append('=').append(canonical(entry.getValue())).append(';'));
            return result.append('}').toString();
        }
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            StringBuilder result = new StringBuilder("[");
            for (JsonElement element : array) {
                result.append(canonical(element)).append(';');
            }
            return result.append(']').toString();
        }
        return value.toString();
    }
}
