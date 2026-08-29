package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicationPolicyLoaderTest {
    private static final ResourceLocation SMELTER =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "smelter");
    private static final ResourceLocation T43_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "t43_smelter_stone");
    private static final ResourceLocation T37_GROUP =
            CompactGTRecipeFamilyDefinition.T37_ASSEMBLER_PUBLICATION_GROUP;
    private static final ResourceLocation ASSEMBLER =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "assembler");

    @Test
    void undeclaredGroupFailsClosed() {
        CompactGTRecipeFamilyDefinition definition =
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.smelter#2438",
                        SMELTER,
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(),
                        T43_GROUP);
        CompactRecipeFamilySource source = new CompactRecipeFamilySource(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "t43/smelter/gt_recipe_smelter_2438"),
                definition);
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactRecipeFamilyProvider.prepareByPublicationGroup(
                        List.of(source),
                        Map.of(SMELTER, new RecipeMap(SMELTER)),
                        1L,
                        CompactRecipeFamilyProvider.RuntimeSide.SERVER,
                        Map.of()));
        assertTrue(thrown.getMessage().contains("Missing compact materialization policy"));
        assertTrue(thrown.getMessage().contains("t43_smelter_stone"));
    }

    @Test
    void historicalCollisionFailsClosed() {
        CompactPublicationPolicyDefinition manifest =
                new CompactPublicationPolicyDefinition(
                        ASSEMBLER,
                        T37_GROUP,
                        "on_demand",
                        8,
                        List.of(),
                        CompactPublicationPolicyDefinition.ROUTING_SCHEMA_VERSION,
                java.util.Optional.of("0".repeat(64)),
                java.util.Optional.of(0),
                java.util.Optional.of(0));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactPublicationPolicy.merge(
                        Map.of(
                                new PublicationGroupKey(ASSEMBLER, T37_GROUP),
                                CompactRecipeFamilyProvider.MaterializationPolicy
                                        .onDemand(8)),
                        List.of(new CompactPublicationPolicyEntry(manifest))));
        assertTrue(thrown.getMessage().contains("historical Java policy"));
    }

    @Test
    void duplicateManifestFailsClosed() {
        CompactPublicationPolicyDefinition manifest =
                onDemandManifest();
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactPublicationPolicy.merge(
                        Map.of(),
                        List.of(
                                new CompactPublicationPolicyEntry(manifest),
                                new CompactPublicationPolicyEntry(manifest))));
        assertTrue(thrown.getMessage().contains("Duplicate publication policy"));
    }

    @Test
    void declaredManifestBecomesThePolicy() {
        Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy>
                merged = CompactPublicationPolicy.merge(
                        Map.of(),
                        List.of(new CompactPublicationPolicyEntry(onDemandManifest())));
        CompactRecipeFamilyProvider.MaterializationPolicy policy = merged.get(
                new PublicationGroupKey(SMELTER, T43_GROUP));
        assertEquals(
                CompactRecipeFamilyProvider.MaterializationStrategy.ON_DEMAND,
                policy.strategy());
        assertEquals(24, policy.cacheCeiling());
    }

    private static CompactPublicationPolicyDefinition onDemandManifest() {
        return new CompactPublicationPolicyDefinition(
                SMELTER,
                T43_GROUP,
                "on_demand",
                24,
                List.of(),
                CompactPublicationPolicyDefinition.ROUTING_SCHEMA_VERSION,
                java.util.Optional.of("0".repeat(64)),
                java.util.Optional.of(407),
                java.util.Optional.of(407));
    }
}
