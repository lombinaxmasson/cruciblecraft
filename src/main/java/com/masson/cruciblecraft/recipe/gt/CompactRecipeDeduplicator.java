package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.gt.CompactDedupRuleDefinition.Selector;

import net.minecraft.resources.ResourceLocation;

/**
 * Interprets schema-validated compact dedup rules. Pre-snapshot rewrites
 * compact sources; post-enumeration drops published rows.
 */
public final class CompactRecipeDeduplicator {
    private CompactRecipeDeduplicator() {}

    public static void validate(List<CompactDedupRuleDefinition> rules) {
        Objects.requireNonNull(rules, "rules");
        Set<ResourceLocation> seen = new HashSet<>();
        Map<String, Set<String>> victimsByPhaseMap = new HashMap<>();
        for (CompactDedupRuleDefinition rule : rules) {
            Objects.requireNonNull(rule, "rule");
            if (!seen.add(rule.ruleId())) {
                throw new IllegalArgumentException(
                        "Duplicate compact dedup rule_id " + rule.ruleId());
            }
            String key = rule.phase() + "|" + rule.targetMap();
            Set<String> previous = victimsByPhaseMap.computeIfAbsent(
                    key, ignored -> new HashSet<>());
            Set<String> victim = rule.victimSelector().values();
            Set<String> overlap = new HashSet<>(previous);
            overlap.retainAll(victim);
            if (!overlap.isEmpty()) {
                throw new IllegalArgumentException(
                        "Compact dedup victim selector overlap on " + key
                                + ": " + overlap);
            }
            previous.addAll(victim);
        }
    }

    public static List<CompactRecipeFamilySource> applyPreSnapshot(
            List<CompactRecipeFamilySource> sources,
            List<CompactDedupRuleDefinition> rules) {
        Objects.requireNonNull(sources, "sources");
        List<CompactRecipeFamilySource> current = sources;
        for (CompactDedupRuleDefinition rule : sorted(rules)) {
            if (!CompactDedupRuleDefinition.PHASE_PRE_SNAPSHOT.equals(
                    rule.phase())) {
                continue;
            }
            current = applyPreSnapshotRule(current, rule);
        }
        return current;
    }

    public static void applyPostEnumeration(
            ResourceLocation targetMap,
            List<RecipeMap.Entry> complete,
            List<CompactDedupRuleDefinition> rules) {
        Objects.requireNonNull(targetMap, "targetMap");
        Objects.requireNonNull(complete, "complete");
        for (CompactDedupRuleDefinition rule : sorted(rules)) {
            if (!CompactDedupRuleDefinition.PHASE_POST_ENUMERATION.equals(
                    rule.phase())) {
                continue;
            }
            if (!rule.targetMap().equals(targetMap)) {
                continue;
            }
            applyPostEnumerationRule(complete, rule);
        }
    }

    private static List<CompactDedupRuleDefinition> sorted(
            List<CompactDedupRuleDefinition> rules) {
        return rules.stream()
                .sorted(Comparator.comparing(rule -> rule.ruleId().toString()))
                .toList();
    }

    private static List<CompactRecipeFamilySource> applyPreSnapshotRule(
            List<CompactRecipeFamilySource> sources,
            CompactDedupRuleDefinition rule) {
        Map<String, String> winnerOutputs = new HashMap<>();
        for (CompactRecipeFamilySource source : sources) {
            if (!matchesSource(source, rule.winnerSelector())
                    || !source.definition().targetMap().equals(rule.targetMap())) {
                continue;
            }
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.authoredRelations()) {
                GTRecipe recipe = relation.materialize();
                winnerOutputs.put(inputIdentity(rule, recipe), outputIdentity(rule, recipe));
            }
        }
        if (winnerOutputs.isEmpty()) {
            return sources;
        }
        List<CompactRecipeFamilySource> kept = new ArrayList<>();
        for (CompactRecipeFamilySource source : sources) {
            if (matchesSource(source, rule.winnerSelector())
                    || !matchesSource(source, rule.victimSelector())
                    || !source.definition().targetMap().equals(rule.targetMap())) {
                kept.add(source);
                continue;
            }
            List<CompactGTRecipeFamilyDefinition.Relation> remaining =
                    new ArrayList<>();
            List<CompactGTRecipeFamilyDefinition.Relation> authored =
                    source.authoredRelations();
            for (CompactGTRecipeFamilyDefinition.Relation relation : authored) {
                GTRecipe recipe = relation.materialize();
                if (!matchesWinnerOutput(
                        rule,
                        winnerOutputs,
                        inputIdentity(rule, recipe),
                        outputIdentity(rule, recipe))) {
                    remaining.add(relation);
                }
            }
            if (remaining.isEmpty()) {
                continue;
            }
            if (remaining.size() == authored.size()) {
                kept.add(source);
                continue;
            }
            CompactGTRecipeFamilyDefinition definition = source.definition();
            kept.add(new CompactRecipeFamilySource(
                    source.id(),
                    new CompactGTRecipeFamilyDefinition(
                            definition.familyId(),
                            definition.targetMap(),
                            definition.sourceRevision(),
                            remaining,
                            definition.parameterized(),
                            definition.publicationGroup())));
        }
        return kept;
    }

    private static void applyPostEnumerationRule(
            List<RecipeMap.Entry> complete,
            CompactDedupRuleDefinition rule) {
        Map<String, String> winnerOutputs = new HashMap<>();
        for (RecipeMap.Entry entry : complete) {
            if (!matchesRecipeId(entry.id(), rule.winnerSelector())) {
                continue;
            }
            winnerOutputs.put(
                    inputIdentity(rule, entry.recipe()),
                    outputIdentity(rule, entry.recipe()));
        }
        if (winnerOutputs.isEmpty()) {
            return;
        }
        complete.removeIf(entry -> {
            if (!matchesRecipeId(entry.id(), rule.victimSelector())) {
                return false;
            }
            return matchesWinnerOutput(
                    rule,
                    winnerOutputs,
                    inputIdentity(rule, entry.recipe()),
                    outputIdentity(rule, entry.recipe()));
        });
    }

    private static boolean matchesWinnerOutput(
            CompactDedupRuleDefinition rule,
            Map<String, String> winnerOutputs,
            String input,
            String output) {
        String expected = winnerOutputs.get(input);
        if (expected == null) {
            return false;
        }
        return !rule.requireOutputMatch() || expected.equals(output);
    }

    private static boolean matchesSource(
            CompactRecipeFamilySource source,
            Selector selector) {
        if (Selector.KIND_GROUP.equals(selector.kind())) {
            ResourceLocation group = source.definition().resolvedPublicationGroup();
            return selector.publicationGroups().contains(group);
        }
        return matchesRecipeId(source.id(), selector);
    }

    private static boolean matchesRecipeId(
            ResourceLocation id,
            Selector selector) {
        if (!Selector.KIND_PREFIX.equals(selector.kind())) {
            return false;
        }
        if (!CrucibleCraft.MODID.equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        for (String prefix : selector.prefixes()) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String inputIdentity(
            CompactDedupRuleDefinition rule,
            GTRecipe recipe) {
        if (CompactDedupRuleDefinition.MODE_LOGICAL.equals(rule.matchMode())) {
            return GTRecipeMapLoader.logicalInputIdentity(recipe);
        }
        return GTRecipeMapLoader.inputSignature(recipe);
    }

    private static String outputIdentity(
            CompactDedupRuleDefinition rule,
            GTRecipe recipe) {
        if (CompactDedupRuleDefinition.MODE_LOGICAL.equals(rule.matchMode())) {
            return GTRecipeMapLoader.recipeOutputIdentity(recipe);
        }
        return GTRecipeMapLoader.outputSignature(recipe);
    }
}
