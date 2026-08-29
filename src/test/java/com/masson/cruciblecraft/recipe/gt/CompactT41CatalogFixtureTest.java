package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Catalog-only codec/router capacity for the 294-family Assembler fixture
 * (292 exact singletons plus two parameterized fail-closed stubs).
 * Results here must not drive production policy or census.
 */
class CompactT41CatalogFixtureTest {
    private static final int CATALOG_FAMILIES = 294;
    private static final int EXACT_RELATIONS = 292;
    private static final int PARAMETERIZED_STUBS = 2;

    private static RegistryAccess registries;
    private static List<JsonObject> catalogFamilies;
    private static List<CompactRecipeFamilySource> sources;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path root = CompactGTRecipeFamilyGeneratedSupport.t41CatalogFixtureRoot();
        Assumptions.assumeTrue(
                CompactGTRecipeFamilyGeneratedSupport.hasGeneratedFamilies(root),
                () -> "T41 catalog fixture is not available at " + root);
        catalogFamilies = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies(root);
        sources = catalogFamilies.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(root, document, registries))
                .toList();
    }

    @Test
    void catalogFixtureHoldsTwoHundredNinetyFourFamiliesAndFifteenThirtyOneRelations() {
        assertEquals(CATALOG_FAMILIES, catalogFamilies.size());
        int relations = sources.stream()
                .mapToInt(source -> source.definition().relations().size())
                .sum();
        long parameterized = sources.stream()
                .filter(source -> source.definition().parameterized().isPresent())
                .count();
        assertEquals(EXACT_RELATIONS, relations);
        assertEquals(PARAMETERIZED_STUBS, parameterized);
        for (CompactRecipeFamilySource source : sources) {
            assertEquals(ModRecipeMaps.ASSEMBLER.id(), source.definition().targetMap());
            ResourceLocation group = source.definition().resolvedPublicationGroup();
            assertTrue(
                    allowedCatalogGroup(source, group),
                    () -> "unexpected catalog group " + group);
        }
    }

    @Test
    void catalogFixtureRoutersStayInsideHardCeilingForProductionGroups() {
        List<CompactGTRecipeFamilyDefinition.Relation> planks = new ArrayList<>();
        List<CompactGTRecipeFamilyDefinition.Relation> fireproof = new ArrayList<>();
        List<CompactGTRecipeFamilyDefinition.Relation> planks2 = new ArrayList<>();
        for (CompactRecipeFamilySource source : sources) {
            ResourceLocation group = source.definition().resolvedPublicationGroup();
            if (group.equals(CompactGTRecipeFamilyDefinition
                    .T41_ASSEMBLER_PLANKS_PUBLICATION_GROUP)) {
                planks.addAll(source.definition().relations());
            } else if (group.equals(CompactGTRecipeFamilyDefinition
                    .T41_ASSEMBLER_FIREPROOF_PUBLICATION_GROUP)) {
                fireproof.addAll(source.definition().relations());
            } else if (group.equals(CompactGTRecipeFamilyDefinition
                    .T41_ASSEMBLER_PLANKS2_PUBLICATION_GROUP)) {
                planks2.addAll(source.definition().relations());
            }
        }
        assertRouterCapacity(
                CompactGTRecipeFamilyDefinition.T41_ASSEMBLER_PLANKS_PUBLICATION_GROUP,
                planks);
        assertRouterCapacity(
                CompactGTRecipeFamilyDefinition.T41_ASSEMBLER_FIREPROOF_PUBLICATION_GROUP,
                fireproof);
        assertRouterCapacity(
                CompactGTRecipeFamilyDefinition.T41_ASSEMBLER_PLANKS2_PUBLICATION_GROUP,
                planks2);
    }

    private static boolean allowedCatalogGroup(
            CompactRecipeFamilySource source,
            ResourceLocation group) {
        if (source.definition().parameterized().isPresent()) {
            return source.definition().relations().isEmpty();
        }
        return group.equals(CompactGTRecipeFamilyDefinition
                .T41_ASSEMBLER_PLANKS_PUBLICATION_GROUP)
                || group.equals(CompactGTRecipeFamilyDefinition
                        .T41_ASSEMBLER_FIREPROOF_PUBLICATION_GROUP)
                || group.equals(CompactGTRecipeFamilyDefinition
                        .T41_ASSEMBLER_PLANKS2_PUBLICATION_GROUP);
    }

    private static void assertRouterCapacity(
            ResourceLocation publicationGroup,
            List<CompactGTRecipeFamilyDefinition.Relation> relations) {
        if (relations.isEmpty()) {
            return;
        }
        CompactRecipeShardRouter router = new CompactRecipeShardRouter(
                ModRecipeMaps.ASSEMBLER.id(), publicationGroup, relations);
        assertTrue(router.shardCount() > 0, publicationGroup::toString);
        assertTrue(
                router.overflowCount() <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                () -> publicationGroup + " overflow exceeded the hard ceiling: "
                        + router.overflowCount());
    }
}
