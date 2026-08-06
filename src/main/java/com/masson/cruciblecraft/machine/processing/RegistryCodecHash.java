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
    private static final ThreadLocal<MessageDigest> SHA_256 =
            ThreadLocal.withInitial(RegistryCodecHash::newSha256);

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
            MessageDigest digest = SHA_256.get();
            digest.reset();
            return Optional.of(java.util.HexFormat.of().formatHex(
                    digest.digest(canonical(encoded.get()).getBytes(StandardCharsets.UTF_8))));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
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
