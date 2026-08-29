package com.masson.cruciblecraft.recipe.gt;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Datapack publication-group policy. Undeclared (target_map, publication_group)
 * pairs fail closed. T37/T38 family JSON may still omit publication_group;
 * {@link CompactGTRecipeFamilyDefinition#historicalPublicationGroup()} remains
 * the decode fallback and does not own materialization policy.
 */
public record CompactPublicationPolicyDefinition(
        ResourceLocation targetMap,
        ResourceLocation publicationGroup,
        String policyType,
        int cacheCeiling,
        List<ResourceLocation> eagerStableIds,
        String routingSchemaVersion,
        Optional<String> membershipRootSha256,
        Optional<Integer> familyCount,
        Optional<Integer> relationCount) {

    public static final String ROUTING_SCHEMA_VERSION = "t39-shard-v1";

    public static final MapCodec<CompactPublicationPolicyDefinition> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("target_map")
                            .forGetter(CompactPublicationPolicyDefinition::targetMap),
                    ResourceLocation.CODEC.fieldOf("publication_group")
                            .forGetter(CompactPublicationPolicyDefinition::publicationGroup),
                    Codec.STRING.fieldOf("policy_type")
                            .forGetter(CompactPublicationPolicyDefinition::policyType),
                    Codec.INT.optionalFieldOf("cache_ceiling", 0)
                            .forGetter(CompactPublicationPolicyDefinition::cacheCeiling),
                    ResourceLocation.CODEC.listOf()
                            .optionalFieldOf("eager_stable_ids", List.of())
                            .forGetter(CompactPublicationPolicyDefinition::eagerStableIds),
                    Codec.STRING.fieldOf("routing_schema_version")
                            .forGetter(CompactPublicationPolicyDefinition::routingSchemaVersion),
                    Codec.STRING.fieldOf("membership_root_sha256")
                            .forGetter(definition -> definition.membershipRootSha256().orElseThrow()),
                    Codec.INT.fieldOf("family_count")
                            .forGetter(definition -> definition.familyCount().orElseThrow()),
                    Codec.INT.fieldOf("relation_count")
                            .forGetter(definition -> definition.relationCount().orElseThrow())
            ).apply(instance, (targetMap, publicationGroup, policyType, cacheCeiling,
                    eagerStableIds, routingSchemaVersion, membershipRoot, familyCount,
                    relationCount) -> new CompactPublicationPolicyDefinition(
                            targetMap,
                            publicationGroup,
                            policyType,
                            cacheCeiling,
                            eagerStableIds,
                            routingSchemaVersion,
                            Optional.of(membershipRoot),
                            Optional.of(familyCount),
                            Optional.of(relationCount))));
    public static final Codec<CompactPublicationPolicyDefinition> CODEC =
            MAP_CODEC.codec();

    public CompactPublicationPolicyDefinition {
        Objects.requireNonNull(targetMap, "targetMap");
        Objects.requireNonNull(publicationGroup, "publicationGroup");
        Objects.requireNonNull(policyType, "policyType");
        eagerStableIds = List.copyOf(Objects.requireNonNull(
                eagerStableIds, "eagerStableIds"));
        Objects.requireNonNull(routingSchemaVersion, "routingSchemaVersion");
        membershipRootSha256 = Objects.requireNonNull(
                membershipRootSha256, "membershipRootSha256");
        familyCount = Objects.requireNonNull(familyCount, "familyCount");
        relationCount = Objects.requireNonNull(relationCount, "relationCount");
        if (!ROUTING_SCHEMA_VERSION.equals(routingSchemaVersion)) {
            throw new IllegalArgumentException(
                    "Compact publication policy routing_schema_version must be "
                            + ROUTING_SCHEMA_VERSION);
        }
        if (membershipRootSha256.isEmpty()
                || membershipRootSha256.get().length() != 64) {
            throw new IllegalArgumentException(
                    "Compact publication policy membership_root_sha256 is required");
        }
        if (familyCount.isEmpty() || relationCount.isEmpty()) {
            throw new IllegalArgumentException(
                    "Compact publication policy family_count and relation_count are required");
        }
        if (cacheCeiling < 0) {
            throw new IllegalArgumentException(
                    "Compact publication policy cache_ceiling must not be negative");
        }
    }

    public PublicationGroupKey key() {
        return new PublicationGroupKey(targetMap, publicationGroup);
    }

    public CompactRecipeFamilyProvider.MaterializationPolicy toPolicy() {
        Set<ResourceLocation> eager = new HashSet<>(eagerStableIds);
        return switch (policyType) {
            case "immediate" -> {
                if (cacheCeiling != 0) {
                    throw new IllegalArgumentException(
                            "Immediate compact publication policy uses cache ceiling 0");
                }
                yield CompactRecipeFamilyProvider.MaterializationPolicy.immediate();
            }
            case "on_demand" -> CompactRecipeFamilyProvider.MaterializationPolicy
                    .onDemand(cacheCeiling);
            case "hybrid" -> CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                    cacheCeiling,
                    (index, relation) -> eager.contains(relation.stableId()));
            default -> throw new IllegalArgumentException(
                    "Unknown compact publication policy_type " + policyType);
        };
    }
}
