package com.masson.cruciblecraft.benchmark.recipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRecipe;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.ExtruderRelation;
import com.masson.cruciblecraft.benchmark.recipe.RecipeFamilyProvider.LookupRequest;

/** Exact T14a compact relation source shared by all measured candidates. */
public final class ExtruderBenchmarkModel {
    public static final int BASE_RELATION_COUNT = 2782;
    private static final Pattern COMPACT_FINGERPRINT = Pattern.compile(
            "\"compact_fingerprint\"\\s*:\\s*\"([0-9a-f]{64})\"");
    private static final Pattern RELATION = Pattern.compile(
            "\\{\\s*\"duration\"\\s*:\\s*(\\d+)\\s*,"
                    + "\\s*\"eut\"\\s*:\\s*(\\d+)\\s*,"
                    + ".*?\"heat_mode\"\\s*:\\s*\"([^\"]+)\"\\s*,"
                    + "\\s*\"input\"\\s*:\\s*\\{"
                    + "\\s*\"count\"\\s*:\\s*(\\d+)\\s*,"
                    + "\\s*\"prefix\"\\s*:\\s*\"([^\"]+)\"\\s*\\}\\s*,"
                    + "\\s*\"material\"\\s*:\\s*\"([^\"]+)\"\\s*,"
                    + "\\s*\"output\"\\s*:\\s*\\{"
                    + "\\s*\"count\"\\s*:\\s*(\\d+)\\s*,"
                    + "\\s*\"prefix\"\\s*:\\s*\"([^\"]+)\"\\s*\\}\\s*,"
                    + ".*?\"shadow_order\"\\s*:\\s*(\\d+)\\s*,"
                    + ".*?\"stable_id\"\\s*:\\s*\"([^\"]+)\"\\s*,"
                    + "\\s*\"template_id\"\\s*:\\s*\"[^\"]+\"\\s*\\}",
            Pattern.DOTALL);

    private ExtruderBenchmarkModel() {}

    public static LoadedRelations load(
            Path compactPath,
            int count,
            int hotModulo) {
        if (count <= 0
                || count % BASE_RELATION_COUNT != 0
                || hotModulo <= 0) {
            throw new IllegalArgumentException("Relation geometry must be positive");
        }
        String json;
        try {
            json = Files.readString(compactPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot read T14a compact relations " + compactPath,
                    exception);
        }
        Matcher fingerprint = COMPACT_FINGERPRINT.matcher(json);
        if (!fingerprint.find()) {
            throw new IllegalArgumentException(
                    "T14a compact artifact has no compact_fingerprint");
        }
        List<ParsedRelation> parsed = new ArrayList<>(BASE_RELATION_COUNT);
        Matcher matcher = RELATION.matcher(json);
        while (matcher.find()) {
            int duration = Integer.parseInt(matcher.group(1));
            int eut = Integer.parseInt(matcher.group(2));
            String heatMode = matcher.group(3);
            int inputCount = Integer.parseInt(matcher.group(4));
            String inputPrefix = matcher.group(5);
            String material = matcher.group(6);
            int outputCount = Integer.parseInt(matcher.group(7));
            String outputPrefix = matcher.group(8);
            int shadowOrder = Integer.parseInt(matcher.group(9));
            String stableId = matcher.group(10);
            parsed.add(new ParsedRelation(
                    shadowOrder,
                    stableId,
                    material + "|" + inputPrefix + "|" + inputCount,
                    material + "|" + outputPrefix + "|" + outputCount
                            + "|" + heatMode,
                    duration,
                    eut));
        }
        parsed.sort(Comparator.comparingInt(ParsedRelation::shadowOrder));
        validateBase(parsed);

        int copies = count / BASE_RELATION_COUNT;
        List<ExtruderRelation> result = new ArrayList<>(count);
        for (int copy = 0; copy < copies; copy++) {
            for (ParsedRelation base : parsed) {
                int ordinal = result.size();
                String relationId = copy == 0
                        ? base.stableId()
                        : base.stableId() + "/benchmark_replica_" + copy;
                String replicaKey = copy == 0
                        ? "" : "|benchmark_replica_" + copy;
                result.add(new ExtruderRelation(
                        ordinal,
                        relationId,
                        base.inputKey() + replicaKey,
                        base.outputKey() + replicaKey,
                        base.durationTicks(),
                        base.eut(),
                        base.shadowOrder() % hotModulo == 0));
            }
        }
        return new LoadedRelations(
                List.copyOf(result),
                fingerprint.group(1),
                sha256(compactPath),
                copies);
    }

    /** Small deterministic source used only by the dependency-free contract. */
    public static List<ExtruderRelation> syntheticRelations(
            int count,
            int candidatesPerInput,
            int hotModulo) {
        if (count <= 0 || candidatesPerInput <= 0 || hotModulo <= 0) {
            throw new IllegalArgumentException("Relation geometry must be positive");
        }
        List<ExtruderRelation> result = new ArrayList<>(count);
        for (int ordinal = 0; ordinal < count; ordinal++) {
            String relationId = "cruciblecraft:contract/extruder/relation_"
                    + fixedWidth(ordinal);
            result.add(new ExtruderRelation(
                    ordinal,
                    relationId,
                    "form_group_" + fixedWidth(ordinal / candidatesPerInput),
                    "extruded_form_" + fixedWidth(ordinal),
                    40 + ordinal % 161,
                    16 + ordinal % 113,
                    ordinal % hotModulo == 0));
        }
        return List.copyOf(result);
    }

    public static ExtruderRecipe materialize(ExtruderRelation relation) {
        return new ExtruderRecipe(
                relation.relationId(),
                relation.relationId(),
                relation.inputKey(),
                relation.outputKey(),
                relation.durationTicks(),
                relation.eut());
    }

    public static LookupRequest request(ExtruderRelation relation) {
        return new LookupRequest(relation.inputKey(), relation.relationId());
    }

    public static long relationPayloadBytes(List<ExtruderRelation> relations) {
        long bytes = 0L;
        for (ExtruderRelation relation : relations) {
            bytes += encodedLength(relation.relationId())
                    + encodedLength(relation.inputKey())
                    + encodedLength(relation.outputKey())
                    + Integer.BYTES * 3L
                    + 1L;
        }
        return bytes;
    }

    public static long concretePayloadBytes(List<ExtruderRecipe> recipes) {
        long bytes = 0L;
        for (ExtruderRecipe recipe : recipes) {
            bytes += encodedLength(recipe.stableId())
                    + encodedLength(recipe.relationId())
                    + encodedLength(recipe.inputKey())
                    + encodedLength(recipe.outputKey())
                    + Integer.BYTES * 2L;
        }
        return bytes;
    }

    private static int encodedLength(String value) {
        return Integer.BYTES + value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static void validateBase(List<ParsedRelation> relations) {
        if (relations.size() != BASE_RELATION_COUNT) {
            throw new IllegalArgumentException(
                    "T14a compact parser expected 2782 relations, found "
                            + relations.size());
        }
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < relations.size(); index++) {
            ParsedRelation relation = relations.get(index);
            if (relation.shadowOrder() != index) {
                throw new IllegalArgumentException(
                        "T14a shadow_order is not contiguous at " + index);
            }
            if (!ids.add(relation.stableId())) {
                throw new IllegalArgumentException(
                        "Duplicate T14a stable id " + relation.stableId());
            }
        }
    }

    private static String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot hash " + path, exception);
        }
    }

    private static String fixedWidth(int value) {
        return String.format(java.util.Locale.ROOT, "%08d", value);
    }

    public record LoadedRelations(
            List<ExtruderRelation> relations,
            String compactFingerprint,
            String compactSha256,
            int distributionCopies) {}

    private record ParsedRelation(
            int shadowOrder,
            String stableId,
            String inputKey,
            String outputKey,
            int durationTicks,
            int eut) {}
}
