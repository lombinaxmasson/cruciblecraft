package com.masson.cruciblecraft.recipe.gt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Merges historical Java compact policies with schema-validated datapack
 * manifests. After the runtime-manifest cutover the historical map is empty
 * and every live group must arrive as a datapack policy. Manifests must not
 * collide with historical keys or each other.
 */
public final class CompactPublicationPolicy {
    private CompactPublicationPolicy() {}

    public static Map<PublicationGroupKey,
            CompactRecipeFamilyProvider.MaterializationPolicy> merge(
            Map<PublicationGroupKey,
                    CompactRecipeFamilyProvider.MaterializationPolicy> historical,
            List<CompactPublicationPolicyEntry> manifests) {
        Objects.requireNonNull(historical, "historical");
        Objects.requireNonNull(manifests, "manifests");
        LinkedHashMap<PublicationGroupKey,
                CompactRecipeFamilyProvider.MaterializationPolicy> merged =
                        new LinkedHashMap<>(historical);
        for (CompactPublicationPolicyEntry entry : manifests) {
            Objects.requireNonNull(entry, "manifest");
            CompactPublicationPolicyDefinition definition = entry.definition();
            PublicationGroupKey key = definition.key();
            if (historical.containsKey(key)) {
                throw new IllegalArgumentException(
                        "Publication policy manifest collides with historical Java policy for "
                                + key.targetMap() + "/" + key.publicationGroup());
            }
            if (merged.put(key, definition.toPolicy()) != null) {
                throw new IllegalArgumentException(
                        "Duplicate publication policy manifest for "
                                + key.targetMap() + "/" + key.publicationGroup());
            }
        }
        return Map.copyOf(merged);
    }

    public static String membershipRoot(List<CompactRecipeFamilySource> sources) {
        List<String> familyIds = sources.stream()
                .map(source -> source.definition().familyId())
                .sorted()
                .toList();
        List<String> stableIds = sources.stream()
                .flatMap(source -> source.definition().relations().stream())
                .map(relation -> relation.stableId().toString())
                .sorted()
                .toList();
        StringBuilder payload = new StringBuilder();
        for (String familyId : familyIds) {
            payload.append(familyId).append('\n');
        }
        for (String stableId : stableIds) {
            payload.append(stableId).append('\n');
        }
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256")
                    .digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }

    public static void validateLiveSources(
            CompactPublicationPolicyDefinition definition,
            List<CompactRecipeFamilySource> sources) {
        Objects.requireNonNull(definition, "definition");
        sources = sources == null ? List.of() : sources;
        int familyCount = definition.familyCount().orElseThrow();
        int relationCount = definition.relationCount().orElseThrow();
        if (sources.size() != familyCount) {
            throw new IllegalArgumentException(
                    "Publication policy family_count drifted for "
                            + definition.publicationGroup()
                            + ": live "
                            + sources.size()
                            + " != "
                            + familyCount);
        }
        int liveRelations = sources.stream()
                .mapToInt(source -> source.definition().relations().size())
                .sum();
        if (liveRelations != relationCount) {
            throw new IllegalArgumentException(
                    "Publication policy relation_count drifted for "
                            + definition.publicationGroup()
                            + ": live "
                            + liveRelations
                            + " != "
                            + relationCount);
        }
        String live = membershipRoot(sources);
        String expected = definition.membershipRootSha256().orElseThrow();
        if (!live.equals(expected)) {
            throw new IllegalArgumentException(
                    "Publication policy membership_root_sha256 drifted for "
                            + definition.publicationGroup());
        }
    }
}
