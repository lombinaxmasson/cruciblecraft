package com.masson.cruciblecraft.recipe.gt;

import java.util.List;

import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.resources.ResourceLocation;

final class DedupRuleFixtures {
    private DedupRuleFixtures() {}

    static CompactDedupRuleDefinition assemblerCompactWoodPreSnapshot() {
        return new CompactDedupRuleDefinition(
                id("assembler/compact_wood_pre_snapshot"),
                "assembler/compact",
                CompactDedupRuleDefinition.PHASE_PRE_SNAPSHOT,
                ModRecipeMaps.ASSEMBLER.id(),
                CompactDedupRuleDefinition.MODE_LOGICAL,
                true,
                groupSelector(CompactPublicationGroups.ASSEMBLER_COMPACT),
                groupSelector(
                        CompactPublicationGroups.ASSEMBLER_PLANKS,
                        CompactPublicationGroups.ASSEMBLER_FIREPROOF,
                        CompactPublicationGroups.ASSEMBLER_PLANKS2));
    }

    static CompactDedupRuleDefinition assemblerCompactWoodPostEnumeration() {
        return new CompactDedupRuleDefinition(
                id("assembler/compact_wood_post_enumeration"),
                "assembler/compact",
                CompactDedupRuleDefinition.PHASE_POST_ENUMERATION,
                ModRecipeMaps.ASSEMBLER.id(),
                CompactDedupRuleDefinition.MODE_LOGICAL,
                true,
                prefixSelector("assembler/compact/"),
                prefixSelector("assembler/wood/", "player_path_support/assembler_wood/"));
    }

    static CompactDedupRuleDefinition centrifugeChemicalPostEnumeration() {
        return new CompactDedupRuleDefinition(
                id("centrifuge/chemical_post_enumeration"),
                "centrifuge/compact",
                CompactDedupRuleDefinition.PHASE_POST_ENUMERATION,
                ModRecipeMaps.CENTRIFUGE.id(),
                CompactDedupRuleDefinition.MODE_SIGNATURE,
                true,
                prefixSelector("centrifuge/compact/", "player_path_support/centrifuge/"),
                prefixSelector("chemical/"));
    }

    static CompactDedupRuleDefinition electrolyzerChemicalPostEnumeration() {
        return new CompactDedupRuleDefinition(
                id("electrolyzer/chemical_post_enumeration"),
                "electrolyzer/compact",
                CompactDedupRuleDefinition.PHASE_POST_ENUMERATION,
                ModRecipeMaps.ELECTROLYZER.id(),
                CompactDedupRuleDefinition.MODE_SIGNATURE,
                true,
                prefixSelector("electrolyzer/compact/", "player_path_support/electrolyzer/"),
                prefixSelector("chemical/"));
    }

    private static CompactDedupRuleDefinition.Selector groupSelector(
            ResourceLocation... groups) {
        return new CompactDedupRuleDefinition.Selector(
                CompactDedupRuleDefinition.Selector.KIND_GROUP,
                List.of(groups),
                List.of());
    }

    private static CompactDedupRuleDefinition.Selector prefixSelector(
            String... prefixes) {
        return new CompactDedupRuleDefinition.Selector(
                CompactDedupRuleDefinition.Selector.KIND_PREFIX,
                List.of(),
                List.of(prefixes));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
