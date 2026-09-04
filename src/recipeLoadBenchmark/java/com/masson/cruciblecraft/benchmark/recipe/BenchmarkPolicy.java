package com.masson.cruciblecraft.benchmark.recipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Narrow, fail-closed reader for the committed T14b JSON policy. */
final class BenchmarkPolicy {
    private static final Pattern SCALE = Pattern.compile(
            "\\{\\s*\"id\"\\s*:\\s*\"([^\"]+)\"\\s*,"
                    + "\\s*\"relation_count\"\\s*:\\s*(\\d+)\\s*,"
                    + "\\s*\"purpose\"\\s*:\\s*\"([^\"]+)\"\\s*\\}");

    final Path path;
    final String sha256;
    final int schemaVersion;
    final String inputKind;
    final int prototypeRelationCount;
    final String compactPath;
    final String compactSha256;
    final String compactFingerprint;
    final List<Scale> scales;
    final int hotModulo;
    final int candidatesPerInput;
    final int reloadWarmup;
    final int reloadSamples;
    final int lookupWarmupOperations;
    final int lookupSamples;
    final int lookupOperationsPerSample;
    final int enumerationWarmup;
    final int enumerationSamples;
    final int retainedSamples;
    final String retainedHeap;
    final int retainedLookupWarmup;
    final int jfrSamples;

    private BenchmarkPolicy(Path path, String json) {
        this.path = path;
        this.sha256 = sha256(path);
        this.schemaVersion = intValue(json, "schema_version");
        this.inputKind = stringValue(json, "kind");
        this.prototypeRelationCount = intValue(
                json, "base_relation_count");
        this.compactPath = stringValue(json, "compact_path");
        this.compactSha256 = stringValue(json, "compact_sha256");
        this.compactFingerprint = stringValue(json, "compact_fingerprint");
        this.hotModulo = intValue(json, "hot_modulo");
        this.candidatesPerInput = intValue(
                json, "relation_candidates_per_input");
        this.reloadWarmup = intValue(
                json, "reload_warmup_iterations");
        this.reloadSamples = intValue(json, "reload_samples");
        this.lookupWarmupOperations = intValue(
                json, "lookup_warmup_operations");
        this.lookupSamples = intValue(json, "lookup_samples");
        this.lookupOperationsPerSample = intValue(
                json, "lookup_operations_per_sample");
        this.enumerationWarmup = intValue(
                json, "enumeration_warmup_iterations");
        this.enumerationSamples = intValue(
                json, "enumeration_samples");
        this.retainedSamples = intValue(
                json, "retained_child_samples");
        this.retainedHeap = stringValue(json, "retained_child_heap");
        this.retainedLookupWarmup = intValue(
                json, "retained_lookup_warmup_operations");
        this.jfrSamples = intValue(json, "jfr_allocation_samples");

        List<Scale> parsedScales = new ArrayList<>();
        Matcher matcher = SCALE.matcher(json);
        while (matcher.find()) {
            parsedScales.add(new Scale(
                    matcher.group(1),
                    Integer.parseInt(matcher.group(2)),
                    matcher.group(3)));
        }
        this.scales = List.copyOf(parsedScales);
        validate();
    }

    static BenchmarkPolicy load(Path path) throws IOException {
        return new BenchmarkPolicy(
                path.toAbsolutePath().normalize(),
                Files.readString(path, StandardCharsets.UTF_8));
    }

    private void validate() {
        if (schemaVersion != 2
                || !"t14a_compact_relations".equals(inputKind)
                || prototypeRelationCount != 2782
                || !compactSha256.matches("[0-9a-f]{64}")
                || !compactFingerprint.matches("[0-9a-f]{64}")
                || compactPath.isBlank()
                || hotModulo <= 0
                || candidatesPerInput <= 0
                || reloadWarmup < 0
                || reloadSamples < 3
                || lookupWarmupOperations < 0
                || lookupSamples < 3
                || lookupOperationsPerSample <= 0
                || enumerationWarmup < 0
                || enumerationSamples < 3
                || retainedSamples < 2
                || retainedLookupWarmup < 0
                || jfrSamples < 2
                || retainedHeap.isBlank()) {
            throw new IllegalArgumentException(
                    "T14c workload policy is incomplete or unsafe");
        }
        List<String> expectedIds = List.of("baseline", "1x", "5x", "20x");
        if (!scales.stream().map(Scale::id).toList().equals(expectedIds)) {
            throw new IllegalArgumentException(
                    "T14c scales must be baseline, 1x, 5x, 20x");
        }
        if (scales.get(0).relationCount() != prototypeRelationCount
                || scales.get(1).relationCount() != prototypeRelationCount
                || scales.get(2).relationCount() != prototypeRelationCount * 5
                || scales.get(3).relationCount() != prototypeRelationCount * 20) {
            throw new IllegalArgumentException(
                    "T14c scale relation counts drifted");
        }
    }

    private static int intValue(String json, String key) {
        Matcher matcher = Pattern.compile(
                "\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)")
                .matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Missing numeric T14b policy field " + key);
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static String stringValue(String json, String key) {
        Matcher matcher = Pattern.compile(
                "\"" + Pattern.quote(key)
                        + "\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException(
                    "Missing string T14b policy field " + key);
        }
        return matcher.group(1);
    }

    static String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (var input = Files.newInputStream(path)) {
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, count);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Cannot hash T14b input " + path, exception);
        }
    }

    record Scale(String id, int relationCount, String purpose) {}
}
