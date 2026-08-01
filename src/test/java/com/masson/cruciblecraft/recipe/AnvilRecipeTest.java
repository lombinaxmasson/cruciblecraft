package com.masson.cruciblecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;

class AnvilRecipeTest {
    @Test
    void acceptsUnitConservingFormChanges() {
        assertDoesNotThrow(() ->
                AnvilRecipeRules.validate(MaterialPrefixes.INGOT, MaterialPrefixes.PLATE, 1, 4));
        assertDoesNotThrow(() ->
                AnvilRecipeRules.validate(MaterialPrefixes.PLATE, MaterialPrefixes.ROD, 2, 5));
        assertDoesNotThrow(() ->
                AnvilRecipeRules.validate(MaterialPrefixes.ROD, MaterialPrefixes.BOLT, 4, 3));
    }

    @Test
    void rejectsRecipesThatCreateOrDestroyMaterial() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AnvilRecipeRules.validate(
                        MaterialPrefixes.INGOT,
                        MaterialPrefixes.ROD,
                        1,
                        4));
    }

    @Test
    void rejectsNonPositiveCountsAndHits() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AnvilRecipeRules.validate(MaterialPrefixes.INGOT, MaterialPrefixes.PLATE, 0, 4));
        assertThrows(
                IllegalArgumentException.class,
                () -> AnvilRecipeRules.validate(MaterialPrefixes.INGOT, MaterialPrefixes.PLATE, 1, 0));
    }

    @Test
    void normalizesBlankMaterialFilterToGenericRecipe() {
        assertTrue(AnvilRecipeRules.normalizeMaterial(Optional.of(" ")).isEmpty());
    }

    @Test
    void acceptsConservativeTwoInputAndSecondaryOutputRecipes() {
        assertDoesNotThrow(() -> AnvilRecipeRules.validate(
                MaterialPrefixes.INGOT,
                1,
                Optional.of(MaterialPrefixes.INGOT),
                1,
                MaterialPrefixes.PLATE,
                1,
                6,
                Optional.of(MaterialPrefixes.ROD),
                1,
                0.25,
                80_000L));
    }

}
