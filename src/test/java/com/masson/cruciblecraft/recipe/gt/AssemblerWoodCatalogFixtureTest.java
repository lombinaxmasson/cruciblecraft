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
class AssemblerWoodCatalogFixtureTest {
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
        Path root = CompactGTRecipeFamilyGeneratedSupport.assemblerWoodCatalogFixtureRoot();
        Assumptions.assumeTrue(
                CompactGTRecipeFamilyGeneratedSupport.hasGeneratedFamilies(root),
                () -> "assembler-wood catalog fixture is not available at " + root);
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
            if (group.equals(CompactPublicationGroups.ASSEMBLER_PLANKS)) {
                planks.addAll(source.definition().relations());
            } else if (group.equals(CompactPublicationGroups.ASSEMBLER_FIREPROOF)) {
                fireproof.addAll(source.definition().relations());
            } else if (group.equals(CompactPublicationGroups.ASSEMBLER_PLANKS2)) {
                planks2.addAll(source.definition().relations());
            }
        }
        assertRouterCapacity(
                CompactPublicationGroups.ASSEMBLER_PLANKS,
                planks);
        assertRouterCapacity(
                CompactPublicationGroups.ASSEMBLER_FIREPROOF,
                fireproof);
        assertRouterCapacity(
                CompactPublicationGroups.ASSEMBLER_PLANKS2,
                planks2);
    }

    private static boolean allowedCatalogGroup(
            CompactRecipeFamilySource source,
            ResourceLocation group) {
        if (source.definition().parameterized().isPresent()) {
            return source.definition().relations().isEmpty();
        }
        return group.equals(CompactPublicationGroups.ASSEMBLER_PLANKS)
                || group.equals(CompactPublicationGroups.ASSEMBLER_FIREPROOF)
                || group.equals(CompactPublicationGroups.ASSEMBLER_PLANKS2);
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
