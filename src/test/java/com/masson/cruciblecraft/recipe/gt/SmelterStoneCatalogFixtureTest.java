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
 * Catalog fixture codec/router capacity for the 407-family Smelter lock.
 * Production equals catalog; results here must not invent extra families.
 */
class SmelterStoneCatalogFixtureTest {
    private static final int CATALOG_FAMILIES = 407;
    private static final int EXACT_RELATIONS = 407;

    private static RegistryAccess registries;
    private static List<JsonObject> catalogFamilies;
    private static List<CompactRecipeFamilySource> sources;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path root = CompactGTRecipeFamilyGeneratedSupport.smelterStoneCatalogFixtureRoot();
        Assumptions.assumeTrue(
                CompactGTRecipeFamilyGeneratedSupport.hasGeneratedFamilies(root),
                () -> "smelter-stone catalog fixture is not available at " + root);
        catalogFamilies = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies(root);
        sources = catalogFamilies.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(root, document, registries))
                .toList();
    }

    @Test
    void catalogFixtureHoldsFourHundredSevenExactSingletons() {
        assertEquals(CATALOG_FAMILIES, catalogFamilies.size());
        int relations = sources.stream()
                .mapToInt(source -> source.authoredRelations().size())
                .sum();
        long parameterized = sources.stream()
                .filter(source -> source.definition().parameterized().isPresent())
                .count();
        assertEquals(EXACT_RELATIONS, relations);
        assertEquals(0, parameterized);
        for (CompactRecipeFamilySource source : sources) {
            assertEquals(ModRecipeMaps.SMELTER.id(), source.definition().targetMap());
            assertEquals(
                    CompactPublicationGroups.SMELTER_STONE,
                    source.definition().resolvedPublicationGroup());
        }
    }

    @Test
    void catalogFixtureRouterStaysInsideHardCeiling() {
        List<CompactGTRecipeFamilyDefinition.Relation> relations = new ArrayList<>();
        for (CompactRecipeFamilySource source : sources) {
            relations.addAll(source.authoredRelations());
        }
        CompactRecipeShardRouter router = new CompactRecipeShardRouter(
                ModRecipeMaps.SMELTER.id(),
                CompactPublicationGroups.SMELTER_STONE,
                relations);
        assertTrue(router.shardCount() > 0);
        assertTrue(
                router.overflowCount() <= CompactRecipeShardRouter.HARD_SHARD_CEILING,
                () -> "overflow exceeded the hard ceiling: " + router.overflowCount());
    }
}
