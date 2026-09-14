package com.masson.cruciblecraft.energy.longdistance;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

final class CatalogJson {
    static final Gson GSON = new Gson();
    static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";

    static <T> T readBundled(Class<?> owner, String resource, Class<T> type) {
        try (InputStream stream = owner.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled catalog " + resource);
            }
            try (InputStreamReader reader = new InputStreamReader(
                    stream, StandardCharsets.UTF_8)) {
                T document = GSON.fromJson(reader, type);
                if (document == null) {
                    throw new IllegalStateException("Invalid catalog " + resource);
                }
                return document;
            }
        } catch (IOException | JsonIOException | JsonSyntaxException exception) {
            throw new IllegalStateException(
                    "Could not load catalog " + resource, exception);
        }
    }

    static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    static void requireRevision(String revision, String name) {
        if (!SOURCE_REVISION.equals(revision)) {
            throw new IllegalStateException(
                    name + " source revision drifted: " + revision);
        }
    }

    private CatalogJson() {}
}
