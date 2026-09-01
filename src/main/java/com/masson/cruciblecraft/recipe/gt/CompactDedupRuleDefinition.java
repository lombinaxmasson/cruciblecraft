package com.masson.cruciblecraft.recipe.gt;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * Datapack compact dedup/supersede rule. Replaces Txx-specific Java drop
 * paths. Unknown phase, match mode, overlapping selectors, duplicate rule
 * ids, or undeclared owners fail closed.
 */
public record CompactDedupRuleDefinition(
        ResourceLocation ruleId,
        String owner,
        String phase,
        ResourceLocation targetMap,
        String matchMode,
        boolean requireOutputMatch,
        Selector winnerSelector,
        Selector victimSelector) {

    public static final String PHASE_PRE_SNAPSHOT = "pre_snapshot";
    public static final String PHASE_POST_ENUMERATION = "post_enumeration";
    public static final String MODE_LOGICAL =
            "logical_input_and_output_identity";
    public static final String MODE_SIGNATURE = "input_and_output_signature";

    public static final MapCodec<CompactDedupRuleDefinition> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ResourceLocation.CODEC.fieldOf("rule_id")
                            .forGetter(CompactDedupRuleDefinition::ruleId),
                    Codec.STRING.fieldOf("owner")
                            .forGetter(CompactDedupRuleDefinition::owner),
                    Codec.STRING.fieldOf("phase")
                            .forGetter(CompactDedupRuleDefinition::phase),
                    ResourceLocation.CODEC.fieldOf("target_map")
                            .forGetter(CompactDedupRuleDefinition::targetMap),
                    Codec.STRING.fieldOf("match_mode")
                            .forGetter(CompactDedupRuleDefinition::matchMode),
                    Codec.BOOL.optionalFieldOf("require_output_match", true)
                            .forGetter(CompactDedupRuleDefinition::requireOutputMatch),
                    Selector.CODEC.fieldOf("winner_selector")
                            .forGetter(CompactDedupRuleDefinition::winnerSelector),
                    Selector.CODEC.fieldOf("victim_selector")
                            .forGetter(CompactDedupRuleDefinition::victimSelector)
            ).apply(instance, CompactDedupRuleDefinition::new));
    public static final Codec<CompactDedupRuleDefinition> CODEC = MAP_CODEC.codec();

    public CompactDedupRuleDefinition {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(targetMap, "targetMap");
        Objects.requireNonNull(matchMode, "matchMode");
        Objects.requireNonNull(winnerSelector, "winnerSelector");
        Objects.requireNonNull(victimSelector, "victimSelector");
        if (!isDeclaredOwner(owner)) {
            throw new IllegalArgumentException(
                    "Undeclared compact dedup owner on " + ruleId + ": " + owner);
        }
        if (!PHASE_PRE_SNAPSHOT.equals(phase)
                && !PHASE_POST_ENUMERATION.equals(phase)) {
            throw new IllegalArgumentException(
                    "Unknown compact dedup phase on " + ruleId + ": " + phase);
        }
        if (!MODE_LOGICAL.equals(matchMode)
                && !MODE_SIGNATURE.equals(matchMode)) {
            throw new IllegalArgumentException(
                    "Unknown compact dedup match_mode on " + ruleId + ": "
                            + matchMode);
        }
        Set<String> overlap = new HashSet<>(winnerSelector.values());
        overlap.retainAll(victimSelector.values());
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException(
                    "Compact dedup selector overlap on " + ruleId + ": "
                            + overlap);
        }
    }

    public static boolean isDeclaredOwner(String owner) {
        if (owner == null || owner.isBlank()) {
            return false;
        }
        if (owner.length() >= 2
                && (owner.charAt(0) == 'T' || owner.charAt(0) == 't')
                && Character.isDigit(owner.charAt(1))) {
            return false;
        }
        return owner.matches(
                "[a-z][a-z0-9_]*(?:-[a-z0-9_]+)*/[a-z][a-z0-9_]*(?:-[a-z0-9_]+)*");
    }

    public record Selector(
            String kind,
            List<ResourceLocation> publicationGroups,
            List<String> prefixes) {
        public static final String KIND_GROUP = "publication_group";
        public static final String KIND_PREFIX = "recipe_id_prefix";

        public static final Codec<Selector> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("kind")
                                .forGetter(Selector::kind),
                        ResourceLocation.CODEC.listOf()
                                .optionalFieldOf("publication_groups", List.of())
                                .forGetter(Selector::publicationGroups),
                        Codec.STRING.listOf()
                                .optionalFieldOf("prefixes", List.of())
                                .forGetter(Selector::prefixes)
                ).apply(instance, Selector::new));

        public Selector {
            Objects.requireNonNull(kind, "kind");
            publicationGroups = List.copyOf(Objects.requireNonNull(
                    publicationGroups, "publicationGroups"));
            prefixes = List.copyOf(Objects.requireNonNull(prefixes, "prefixes"));
            if (KIND_GROUP.equals(kind)) {
                if (publicationGroups.isEmpty()) {
                    throw new IllegalArgumentException(
                            "publication_group selector requires publication_groups");
                }
                if (!prefixes.isEmpty()) {
                    throw new IllegalArgumentException(
                            "publication_group selector must not set prefixes");
                }
            } else if (KIND_PREFIX.equals(kind)) {
                if (prefixes.isEmpty()) {
                    throw new IllegalArgumentException(
                            "recipe_id_prefix selector requires prefixes");
                }
                if (!publicationGroups.isEmpty()) {
                    throw new IllegalArgumentException(
                            "recipe_id_prefix selector must not set publication_groups");
                }
            } else {
                throw new IllegalArgumentException(
                        "Unknown compact dedup selector kind " + kind);
            }
        }

        Set<String> values() {
            if (KIND_GROUP.equals(kind)) {
                Set<String> values = new HashSet<>();
                for (ResourceLocation group : publicationGroups) {
                    values.add(group.toString());
                }
                return values;
            }
            return new HashSet<>(prefixes);
        }
    }
}
