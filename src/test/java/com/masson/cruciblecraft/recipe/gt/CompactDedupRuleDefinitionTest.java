package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.resources.ResourceLocation;

class CompactDedupRuleDefinitionTest {
    static {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void acceptsSemanticSlugOwnerAndRejectsMilestone() {
        String milestone = "T" + "50";
        assertTrue(CompactDedupRuleDefinition.isDeclaredOwner(
                "smelter/ordinary-closure"));
        assertTrue(CompactDedupRuleDefinition.isDeclaredOwner("centrifuge/compact"));
        assertFalse(CompactDedupRuleDefinition.isDeclaredOwner(milestone + "-extra"));
        CompactDedupRuleDefinition.Selector winner =
                new CompactDedupRuleDefinition.Selector(
                        CompactDedupRuleDefinition.Selector.KIND_PREFIX,
                        List.of(),
                        List.of("smelter/ordinary_closure/"));
        CompactDedupRuleDefinition.Selector victim =
                new CompactDedupRuleDefinition.Selector(
                        CompactDedupRuleDefinition.Selector.KIND_PREFIX,
                        List.of(),
                        List.of("player_path_support/centrifuge/"));
        CompactDedupRuleDefinition rule = new CompactDedupRuleDefinition(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft",
                        "smelter_ordinary_closure_centrifuge_support_post_enumeration"),
                "smelter/ordinary-closure",
                CompactDedupRuleDefinition.PHASE_POST_ENUMERATION,
                ModRecipeMaps.SMELTER.id(),
                CompactDedupRuleDefinition.MODE_LOGICAL,
                false,
                winner,
                victim);
        assertTrue(CompactDedupRuleDefinition.isDeclaredOwner(rule.owner()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new CompactDedupRuleDefinition(
                        ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft", milestone.toLowerCase() + "_forbidden"),
                        "not a slug",
                        CompactDedupRuleDefinition.PHASE_POST_ENUMERATION,
                        ModRecipeMaps.SMELTER.id(),
                        CompactDedupRuleDefinition.MODE_LOGICAL,
                        false,
                        winner,
                        victim));
    }
}
