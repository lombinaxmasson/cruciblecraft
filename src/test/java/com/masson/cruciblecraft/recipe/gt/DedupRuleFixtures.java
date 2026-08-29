package com.masson.cruciblecraft.recipe.gt;

import java.util.List;

import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.resources.ResourceLocation;

final class DedupRuleFixtures {
    private DedupRuleFixtures() {}

    static CompactDedupRuleDefinition t37T41PreSnapshot() {
        return new CompactDedupRuleDefinition(
                id("t37_t41_assembler_pre_snapshot"),
                "T37",
                CompactDedupRuleDefinition.PHASE_PRE_SNAPSHOT,
                ModRecipeMaps.ASSEMBLER.id(),
                CompactDedupRuleDefinition.MODE_LOGICAL,
                true,
                groupSelector(CompactGTRecipeFamilyDefinition
                        .T37_ASSEMBLER_PUBLICATION_GROUP),
                groupSelector(
                        CompactGTRecipeFamilyDefinition
                                .T41_ASSEMBLER_PLANKS_PUBLICATION_GROUP,
                        CompactGTRecipeFamilyDefinition
                                .T41_ASSEMBLER_FIREPROOF_PUBLICATION_GROUP,
                        CompactGTRecipeFamilyDefinition
                                .T41_ASSEMBLER_PLANKS2_PUBLICATION_GROUP));
    }

    static CompactDedupRuleDefinition t37T41PostEnumeration() {
        return new CompactDedupRuleDefinition(
                id("t37_t41_assembler_post_enumeration"),
                "T37",
                CompactDedupRuleDefinition.PHASE_POST_ENUMERATION,
                ModRecipeMaps.ASSEMBLER.id(),
                CompactDedupRuleDefinition.MODE_LOGICAL,
                true,
                prefixSelector("t37/"),
                prefixSelector("t41/", "t41_player_path_support/"));
    }

    static CompactDedupRuleDefinition t39T5PostEnumeration() {
        return new CompactDedupRuleDefinition(
                id("t39_t5_centrifuge_post_enumeration"),
                "T39",
                CompactDedupRuleDefinition.PHASE_POST_ENUMERATION,
                ModRecipeMaps.CENTRIFUGE.id(),
                CompactDedupRuleDefinition.MODE_SIGNATURE,
                true,
                prefixSelector("t39/", "t39_player_path_support/"),
                prefixSelector("t5/"));
    }

    static CompactDedupRuleDefinition t40T5PostEnumeration() {
        return new CompactDedupRuleDefinition(
                id("t40_t5_electrolyzer_post_enumeration"),
                "T40",
                CompactDedupRuleDefinition.PHASE_POST_ENUMERATION,
                ModRecipeMaps.ELECTROLYZER.id(),
                CompactDedupRuleDefinition.MODE_SIGNATURE,
                true,
                prefixSelector("t40/", "t40_player_path_support/"),
                prefixSelector("t5/"));
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
