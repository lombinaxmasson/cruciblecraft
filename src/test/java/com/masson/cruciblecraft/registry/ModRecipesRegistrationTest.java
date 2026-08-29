package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ModRecipesRegistrationTest {
    @Test
    void registersOnlyActiveRecipeTypesAndSerializers() {
        Set<ResourceLocation> expected = Set.of(
                id("gt_recipe"),
                id("material_rule"),
                id("compact_gt_recipe_family"),
                id("compact_publication_policy"));

        assertEquals(expected, ModRecipes.RECIPE_TYPES.getEntries().stream()
                .map(holder -> holder.getId())
                .collect(Collectors.toSet()));
        assertEquals(expected, ModRecipes.RECIPE_SERIALIZERS.getEntries().stream()
                .map(holder -> holder.getId())
                .collect(Collectors.toSet()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
