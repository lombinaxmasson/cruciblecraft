package com.masson.cruciblecraft.recipe.gt;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
 * Catalog-only codec/router capacity for the 61/151 Electrolyzer fixture.
 * Results here must not drive production policy or census.
 */
class ElectrolyzerCatalogFixtureTest {
    private static RegistryAccess registries;
    private static List<JsonObject> catalogFamilies;
    private static List<CompactRecipeFamilySource> sources;

    @BeforeAll
    static void bootstrapMinecraft() throws IOException {
        MinecraftTestBootstrap.bootstrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
        Path root = CompactGTRecipeFamilyGeneratedSupport.electrolyzerCatalogFixtureRoot();
        catalogFamilies = CompactGTRecipeFamilyGeneratedSupport.loadGeneratedFamilies(root);
        sources = catalogFamilies.stream()
                .map(document -> CompactGTRecipeFamilyGeneratedSupport
                        .sourceFromGenerated(root, document, registries))
                .toList();
    }

    @Test
    void catalogFixtureHoldsSixtyOneFamiliesAndOneHundredFiftyOneRelations() {
        assertEquals(61, catalogFamilies.size());
        int relations = sources.stream()
                .mapToInt(source -> source.definition().relations().size())
                .sum();
        assertEquals(151, relations);
        for (CompactRecipeFamilySource source : sources) {
            assertEquals(ModRecipeMaps.ELECTROLYZER.id(), source.definition().targetMap());
            ResourceLocation group = source.definition().resolvedPublicationGroup();
            assertTrue(
                    group.equals(CompactPublicationGroups.ELECTROLYZER_SINGLETON)
                            || group.equals(CompactPublicationGroups.ELECTROLYZER_MULTI)
                            || "t40_electrolyzer_combinatorial".equals(group.getPath()),
                    () -> "unexpected catalog group " + group);
        }
    }
}
