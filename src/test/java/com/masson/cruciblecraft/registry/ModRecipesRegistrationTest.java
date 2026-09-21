package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModRecipesRegistrationTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void registersOnlyActiveRecipeTypesAndSerializers() {
        Set<ResourceLocation> expected = Set.of(
                id("gt_recipe"),
                id("material_rule"),
                id("compact_gt_recipe_family"),
                id("compact_publication_policy"),
                id("compact_dedup_rule"),
                id("shaped_catalyst"),
                id("tool_head_assembly"));

        assertEquals(expected, ModRecipes.RECIPE_TYPES.getEntries().stream()
                .map(holder -> holder.getId())
                .collect(Collectors.toSet()));
        Set<ResourceLocation> expectedSerializers = new java.util.HashSet<>(expected);
        expectedSerializers.add(id("prefix_pack"));
        assertEquals(expectedSerializers, ModRecipes.RECIPE_SERIALIZERS.getEntries().stream()
                .map(holder -> holder.getId())
                .collect(Collectors.toSet()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
