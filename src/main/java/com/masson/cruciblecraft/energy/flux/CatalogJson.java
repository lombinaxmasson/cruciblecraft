package com.masson.cruciblecraft.energy.flux;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/** Bundled-JSON load helper for the flux converter catalog. */
final class CatalogJson {
    static final Gson GSON = new Gson();
    static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";

    static <T> T readBundled(Class<?> owner, String resource, Class<T> type) {
        try (InputStream stream = owner.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled catalog " + resource);
            }
            return read(stream, type, resource);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load catalog " + resource, exception);
        }
    }

    static <T> T read(InputStream stream, Class<T> type, String name) {
        return read(
                new InputStreamReader(stream, StandardCharsets.UTF_8),
                type,
                name);
    }

    static <T> T read(Reader reader, Class<T> type, String name) {
        try {
            T document = GSON.fromJson(reader, type);
            if (document == null) {
                throw new IllegalStateException("Invalid catalog " + name);
            }
            return document;
        } catch (JsonIOException | JsonSyntaxException exception) {
            throw new IllegalStateException(
                    "Could not parse catalog " + name, exception);
        }
    }

    static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    static String requireRevision(String revision, String name) {
        if (!SOURCE_REVISION.equals(revision)) {
            throw new IllegalStateException(
                    name + " source revision drifted: " + revision);
        }
        return revision;
    }

    static List<String> list(List<String> values) {
        return values == null ? List.of() : values;
    }

    private CatalogJson() {}
}
