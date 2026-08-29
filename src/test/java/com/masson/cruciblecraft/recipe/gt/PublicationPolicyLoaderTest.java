package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicationPolicyLoaderTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final ResourceLocation SMELTER =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "smelter");
    private static final ResourceLocation T43_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "t43_smelter_stone");
    private static final ResourceLocation T37_GROUP =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "t37_assembler");
    private static final ResourceLocation ASSEMBLER =
            ResourceLocation.fromNamespaceAndPath("cruciblecraft", "assembler");
    private static final ResourceLocation T41_PLANKS =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "t41_assembler_planks");

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

    @Test
    void emptyHistoricalAllowsT37DatapackPolicy() {
        CompactPublicationPolicyDefinition manifest =
                new CompactPublicationPolicyDefinition(
                        ASSEMBLER,
                        T37_GROUP,
                        "hybrid",
                        8,
                        List.of(),
                        CompactPublicationPolicyDefinition.ROUTING_SCHEMA_VERSION,
                        java.util.Optional.of("0".repeat(64)),
                        java.util.Optional.of(50),
                        java.util.Optional.of(50));
        Map<PublicationGroupKey, CompactRecipeFamilyProvider.MaterializationPolicy>
                merged = CompactPublicationPolicy.merge(
                        Map.of(),
                        List.of(new CompactPublicationPolicyEntry(manifest)));
        CompactRecipeFamilyProvider.MaterializationPolicy policy = merged.get(
                new PublicationGroupKey(ASSEMBLER, T37_GROUP));
        assertEquals(
                CompactRecipeFamilyProvider.MaterializationStrategy.HYBRID,
                policy.strategy());
        assertEquals(8, policy.cacheCeiling());
    }

    @Test
    void unknownDedupMatchModeFailsClosed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new CompactDedupRuleDefinition(
                        ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft", "bad_mode"),
                        "T37",
                        CompactDedupRuleDefinition.PHASE_PRE_SNAPSHOT,
                        ASSEMBLER,
                        "by_family_id",
                        true,
                        new CompactDedupRuleDefinition.Selector(
                                CompactDedupRuleDefinition.Selector.KIND_GROUP,
                                List.of(T37_GROUP),
                                List.of()),
                        new CompactDedupRuleDefinition.Selector(
                                CompactDedupRuleDefinition.Selector.KIND_GROUP,
                                List.of(T41_PLANKS),
                                List.of())));
    }

    @Test
    void membershipRootMismatchFailsClosed() {
        CompactPublicationPolicyDefinition definition =
                new CompactPublicationPolicyDefinition(
                        SMELTER,
                        T43_GROUP,
                        "on_demand",
                        24,
                        List.of(),
                        CompactPublicationPolicyDefinition.ROUTING_SCHEMA_VERSION,
                        java.util.Optional.of("0".repeat(64)),
                        java.util.Optional.of(1),
                        java.util.Optional.of(0));
        CompactRecipeFamilySource source = new CompactRecipeFamilySource(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "t43/smelter/gt_recipe_smelter_2438"),
                new CompactGTRecipeFamilyDefinition(
                        "gt.recipe.smelter#2438",
                        SMELTER,
                        "3703e40308c8c030763fd6297dea8b210d2a77b1",
                        List.of(),
                        T43_GROUP));
        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> CompactPublicationPolicy.validateLiveSources(
                        definition, List.of(source)));
        assertTrue(thrown.getMessage().contains("membership_root_sha256"));
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
