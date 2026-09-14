package com.masson.cruciblecraft.recipe.gt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.resources.ResourceLocation;

/**
 * Fail-closed transport reassembly for compact families that exceed one
 * RecipeHolder's wire ceilings. Semantic {@code family_id} is preserved;
 * physical holder ids may use a {@code _fragment_NNNN} suffix.
 */
public final class CompactTransportFragments {
    private static final Pattern FRAGMENT_NAME =
            Pattern.compile("^(.*)_fragment_(\\d{4})$");

    private CompactTransportFragments() {}

    public static String semanticDigest(
            String familyId,
            ResourceLocation targetMap,
            String sourceRevision,
            Optional<ResourceLocation> publicationGroup,
            List<CompactGTRecipeFamilyDefinition.Relation> relations) {
        Objects.requireNonNull(familyId, "familyId");
        Objects.requireNonNull(targetMap, "targetMap");
        Objects.requireNonNull(sourceRevision, "sourceRevision");
        Objects.requireNonNull(publicationGroup, "publicationGroup");
        Objects.requireNonNull(relations, "relations");
        StringBuilder payload = new StringBuilder();
        payload.append(familyId).append('\n');
        payload.append(targetMap).append('\n');
        payload.append(sourceRevision).append('\n');
        payload.append(publicationGroup.map(ResourceLocation::toString).orElse(""))
                .append('\n');
        List<CompactGTRecipeFamilyDefinition.Relation> ordered =
                new ArrayList<>(relations);
        ordered.sort(Comparator
                .comparingInt(CompactGTRecipeFamilyDefinition.Relation::shadowOrder)
                .thenComparing(relation -> relation.stableId().toString()));
        for (CompactGTRecipeFamilyDefinition.Relation relation : ordered) {
            payload.append(relation.stableId())
                    .append('\t')
                    .append(relation.shadowOrder())
                    .append('\n');
        }
        return sha256Hex(payload.toString());
    }

    public static ResourceLocation canonicalSourceId(
            ResourceLocation fragmentId, int index) {
        Objects.requireNonNull(fragmentId, "fragmentId");
        String path = fragmentId.getPath();
        int slash = path.lastIndexOf('/');
        String directory = slash >= 0 ? path.substring(0, slash + 1) : "";
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        Matcher matcher = FRAGMENT_NAME.matcher(name);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Compact transport fragment source id must end with _fragment_NNNN: "
                            + fragmentId);
        }
        int parsed = Integer.parseInt(matcher.group(2));
        if (parsed != index) {
            throw new IllegalArgumentException(
                    "Compact transport fragment index "
                            + index
                            + " does not match source id "
                            + fragmentId);
        }
        return ResourceLocation.fromNamespaceAndPath(
                fragmentId.getNamespace(), directory + matcher.group(1));
    }

    public static List<CompactRecipeFamilySource> reassemble(
            List<CompactRecipeFamilySource> sources) {
        Objects.requireNonNull(sources, "sources");
        TreeMap<SemanticKey, List<CompactRecipeFamilySource>> grouped =
                new TreeMap<>(Comparator
                        .comparing(SemanticKey::familyId)
                        .thenComparing(key -> key.targetMap().toString())
                        .thenComparing(SemanticKey::publicationGroup));
        for (CompactRecipeFamilySource source : sources) {
            Objects.requireNonNull(source, "source");
            CompactGTRecipeFamilyDefinition definition = source.definition();
            SemanticKey key = new SemanticKey(
                    definition.familyId(),
                    definition.targetMap(),
                    definition.publicationGroup().map(ResourceLocation::toString).orElse(""));
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(source);
        }
        List<CompactRecipeFamilySource> assembled = new ArrayList<>();
        for (List<CompactRecipeFamilySource> members : grouped.values()) {
            boolean anyFragment = members.stream().anyMatch(
                    source -> source.definition().transportFragment().isPresent());
            boolean allFragments = members.stream().allMatch(
                    source -> source.definition().transportFragment().isPresent());
            if (anyFragment && !allFragments) {
                throw new IllegalArgumentException(
                        "Compact family "
                                + members.getFirst().definition().familyId()
                                + " mixes transport fragments with unsplit holders");
            }
            if (!anyFragment) {
                assembled.addAll(members);
                continue;
            }
            assembled.add(mergeFragments(members));
        }
        assembled.sort(Comparator.comparing(source -> source.id().toString()));
        return List.copyOf(assembled);
    }

    private static CompactRecipeFamilySource mergeFragments(
            List<CompactRecipeFamilySource> members) {
        CompactGTRecipeFamilyDefinition first = members.getFirst().definition();
        CompactGTRecipeFamilyDefinition.TransportFragment header =
                first.transportFragment().orElseThrow();
        CompactRecipeFamilySource[] byIndex =
                new CompactRecipeFamilySource[header.count()];
        for (CompactRecipeFamilySource source : members) {
            CompactGTRecipeFamilyDefinition definition = source.definition();
            CompactGTRecipeFamilyDefinition.TransportFragment fragment =
                    definition.transportFragment().orElseThrow();
            if (!definition.familyId().equals(first.familyId())
                    || !definition.targetMap().equals(first.targetMap())
                    || !definition.sourceRevision().equals(first.sourceRevision())
                    || !definition.publicationGroup().equals(first.publicationGroup())
                    || !definition.parameterized().equals(first.parameterized())
                    || fragment.count() != header.count()
                    || fragment.totalRelations() != header.totalRelations()
                    || !fragment.semanticDigest().equals(header.semanticDigest())) {
                throw new IllegalArgumentException(
                        "Compact family " + first.familyId()
                                + " transport fragment headers drifted");
            }
            if (byIndex[fragment.index()] != null) {
                throw new IllegalArgumentException(
                        "Duplicate transport fragment index "
                                + fragment.index()
                                + " for "
                                + first.familyId());
            }
            canonicalSourceId(source.id(), fragment.index());
            byIndex[fragment.index()] = source;
        }
        List<CompactGTRecipeFamilyDefinition.Relation> relations = new ArrayList<>();
        for (int index = 0; index < byIndex.length; index++) {
            CompactRecipeFamilySource source = byIndex[index];
            if (source == null) {
                throw new IllegalArgumentException(
                        "Missing transport fragment index "
                                + index
                                + " for "
                                + first.familyId());
            }
            relations.addAll(source.authoredRelations());
        }
        if (relations.size() != header.totalRelations()) {
            throw new IllegalArgumentException(
                    "Compact family " + first.familyId()
                            + " reassembled relation count "
                            + relations.size()
                            + " != "
                            + header.totalRelations());
        }
        String digest = semanticDigest(
                first.familyId(),
                first.targetMap(),
                first.sourceRevision(),
                first.publicationGroup(),
                relations);
        if (!digest.equals(header.semanticDigest())) {
            throw new IllegalArgumentException(
                    "Compact family " + first.familyId()
                            + " semantic digest drifted");
        }
        Optional<CompactGTRecipeFamilyDefinition.ParameterizedSpec> parameterized =
                first.parameterized();
        if (parameterized.isPresent()
                && CompactAuthoredMatrix.TEMPLATE_COMPACT_MATRIX_V1.equals(
                        parameterized.get().template())) {
            parameterized = Optional.empty();
        }
        CompactGTRecipeFamilyDefinition assembled =
                new CompactGTRecipeFamilyDefinition(
                        first.familyId(),
                        first.targetMap(),
                        first.sourceRevision(),
                        List.copyOf(relations),
                        parameterized,
                        first.publicationGroup(),
                        Optional.empty(),
                        Optional.empty());
        return new CompactRecipeFamilySource(
                canonicalSourceId(byIndex[0].id(), 0),
                assembled);
    }

    private static String sha256Hex(String payload) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private record SemanticKey(
            String familyId,
            ResourceLocation targetMap,
            String publicationGroup) {}
}
