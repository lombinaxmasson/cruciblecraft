package com.masson.cruciblecraft.material.gen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** Versioned manifest cache for deterministic generated material packs. */
public final class GeneratedMaterialPackCache {
    public static final int VERSION = 3;
    private static final String MANIFEST = ".cruciblecraft-manifest.json";
    private static final Pattern DIGEST_AND_SIZE =
            Pattern.compile("([0-9a-f]{64}):(0|[1-9][0-9]*)");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GeneratedMaterialPackCache() {}

    public static boolean ensure(
            Path root, String structuralFingerprint, Map<String, String> files)
            throws IOException {
        return ensure(root, structuralFingerprint, () -> files);
    }

    public static boolean ensure(
            Path root,
            String structuralFingerprint,
            Supplier<Map<String, String>> files)
            throws IOException {
        Objects.requireNonNull(files, "files");
        if (fastValid(root, structuralFingerprint)) {
            return false;
        }
        return recreate(
                root,
                structuralFingerprint,
                Objects.requireNonNull(files.get(), "generated files"));
    }

    /** Strict diagnostic path which verifies the complete generated directory. */
    public static boolean ensureStrict(
            Path root, String structuralFingerprint, Map<String, String> files)
            throws IOException {
        if (valid(root, structuralFingerprint, files)) {
            return false;
        }
        return recreate(root, structuralFingerprint, files);
    }

    private static boolean recreate(
            Path root, String structuralFingerprint, Map<String, String> files)
            throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        List<ResolvedFile> resolvedFiles = files.entrySet().stream()
                .map(entry -> resolve(normalizedRoot, entry))
                .toList();
        LinkedHashMap<String, String> hashes = new LinkedHashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                hashes.put(
                        entry.getKey(),
                        digestAndSize(entry.getValue().getBytes(StandardCharsets.UTF_8))));
        Manifest expected = new Manifest(VERSION, structuralFingerprint, hashes);
        recreate(normalizedRoot);
        Set<Path> createdParents = new HashSet<>();
        createdParents.add(normalizedRoot);
        for (ResolvedFile file : resolvedFiles) {
            Path parent = file.path().getParent();
            if (createdParents.add(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(file.path(), file.content(), StandardCharsets.UTF_8);
        }
        Files.writeString(
                normalizedRoot.resolve(MANIFEST),
                GSON.toJson(expected),
                StandardCharsets.UTF_8);
        return true;
    }

    private static boolean fastValid(Path root, String structuralFingerprint) {
        Path manifestPath = root.resolve(MANIFEST);
        if (!Files.isRegularFile(manifestPath)) return false;
        try {
            Manifest actual = GSON.fromJson(Files.readString(manifestPath), Manifest.class);
            return actual != null
                    && actual.version == VERSION
                    && structuralFingerprint.equals(actual.fingerprint);
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    public static boolean valid(
            Path root, String structuralFingerprint, Map<String, String> files) {
        LinkedHashMap<String, String> hashes = new LinkedHashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
                hashes.put(
                        entry.getKey(),
                        digestAndSize(entry.getValue().getBytes(StandardCharsets.UTF_8))));
        return valid(root, new Manifest(VERSION, structuralFingerprint, hashes));
    }

    private static boolean valid(Path root, Manifest expected) {
        Path manifestPath = root.resolve(MANIFEST);
        if (!Files.isRegularFile(manifestPath)) return false;
        try {
            Manifest actual = GSON.fromJson(Files.readString(manifestPath), Manifest.class);
            if (actual == null
                    || actual.version != expected.version
                    || actual.files == null
                    || !manifestEntriesWellFormed(actual.files)
                    || !actual.fingerprint.equals(expected.fingerprint)
                    || !actual.files.equals(expected.files)) {
                return false;
            }
            try (var paths = Files.walk(root)) {
                var actualFiles = paths.filter(Files::isRegularFile)
                        .map(root::relativize)
                        .map(path -> path.toString().replace('\\', '/'))
                        .filter(path -> !path.equals(MANIFEST))
                        .collect(java.util.stream.Collectors.toSet());
                if (!actualFiles.equals(expected.files.keySet())) return false;
            }
            for (var entry : expected.files.entrySet()) {
                Path path = root.resolve(entry.getKey());
                EncodedDigest encoded = parseDigestAndSize(entry.getValue());
                if (!Files.isRegularFile(path)
                        || Files.size(path) != encoded.size()
                        || !sha256(path).equals(encoded.sha256())) {
                    return false;
                }
            }
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static String sha256(byte[] value) {
        return java.util.HexFormat.of().formatHex(newSha256().digest(value));
    }

    private static String sha256(Path path) throws IOException {
        MessageDigest digest = newSha256();
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8_192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String digestAndSize(byte[] value) {
        return sha256(value) + ":" + value.length;
    }

    private static EncodedDigest parseDigestAndSize(String value) {
        Matcher matcher = DIGEST_AND_SIZE.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid manifest digest and size: " + value);
        }
        try {
            return new EncodedDigest(matcher.group(1), Long.parseLong(matcher.group(2)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid manifest digest and size: " + value,
                    exception);
        }
    }

    private static boolean manifestEntriesWellFormed(Map<String, String> files) {
        try {
            files.values().forEach(GeneratedMaterialPackCache::parseDigestAndSize);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static ResolvedFile resolve(
            Path normalizedRoot,
            Map.Entry<String, String> entry) {
        String key = entry.getKey();
        try {
            Path relative = Path.of(key);
            Path resolved = normalizedRoot.resolve(relative).normalize();
            if (relative.isAbsolute() || !resolved.startsWith(normalizedRoot)) {
                throw escapedPath(key, normalizedRoot);
            }
            return new ResolvedFile(resolved, entry.getValue());
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalArgumentException
                    && exception.getMessage() != null
                    && exception.getMessage().startsWith("Generated pack key escapes root:")) {
                throw exception;
            }
            throw new IllegalArgumentException(
                    "Invalid generated pack key '" + key + "' for root '"
                            + normalizedRoot + "'",
                    exception);
        }
    }

    private static IllegalArgumentException escapedPath(String key, Path root) {
        return new IllegalArgumentException(
                "Generated pack key escapes root: key='" + key + "', root='" + root + "'");
    }

    private static void recreate(Path root) throws IOException {
        if (Files.exists(root)) {
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(root);
    }

    private record Manifest(int version, String fingerprint, Map<String, String> files) {
        private Manifest {
            files = Map.copyOf(files);
        }
    }

    private record ResolvedFile(Path path, String content) {}

    private record EncodedDigest(String sha256, long size) {}
}
