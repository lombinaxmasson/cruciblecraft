package com.masson.cruciblecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialForm;

class AnvilRecipeTest {
    @Test
    void acceptsUnitConservingFormChanges() {
        assertDoesNotThrow(() ->
                AnvilRecipeRules.validate(MaterialForm.INGOT, MaterialForm.PLATE, 1, 4));
        assertDoesNotThrow(() ->
                AnvilRecipeRules.validate(MaterialForm.PLATE, MaterialForm.ROD, 2, 5));
        assertDoesNotThrow(() ->
                AnvilRecipeRules.validate(MaterialForm.ROD, MaterialForm.BOLT, 4, 3));
    }

    @Test
    void rejectsRecipesThatCreateOrDestroyMaterial() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AnvilRecipeRules.validate(
                        MaterialForm.INGOT,
                        MaterialForm.ROD,
                        1,
                        4));
    }

    @Test
    void rejectsNonPositiveCountsAndHits() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AnvilRecipeRules.validate(MaterialForm.INGOT, MaterialForm.PLATE, 0, 4));
        assertThrows(
                IllegalArgumentException.class,
                () -> AnvilRecipeRules.validate(MaterialForm.INGOT, MaterialForm.PLATE, 1, 0));
    }

    @Test
    void normalizesBlankMaterialFilterToGenericRecipe() {
        assertTrue(AnvilRecipeRules.normalizeMaterial(Optional.of(" ")).isEmpty());
    }

    @Test
    void acceptsConservativeTwoInputAndSecondaryOutputRecipes() {
        assertDoesNotThrow(() -> AnvilRecipeRules.validate(
                MaterialForm.INGOT,
                1,
                Optional.of(MaterialForm.INGOT),
                1,
                MaterialForm.PLATE,
                1,
                6,
                Optional.of(MaterialForm.ROD),
                1,
                0.25,
                80_000L));
    }

    @Test
    void secondaryChanceHasExactBoundaryBehavior() {
        assertFalse(AnvilRecipeRules.secondarySucceeds(0.0, 0.0));
        assertTrue(AnvilRecipeRules.secondarySucceeds(1.0, 0.999999));
        assertTrue(AnvilRecipeRules.secondarySucceeds(0.25, 0.249999));
        assertFalse(AnvilRecipeRules.secondarySucceeds(0.25, 0.25));
        assertThrows(
                IllegalArgumentException.class,
                () -> AnvilRecipeRules.secondarySucceeds(0.5, 1.0));
    }

    @Test
    void twoInputConsumptionUsesTheMatchedSlotOrderExactly() {
        assertEquals(
                new AnvilRecipeRules.RemainingCounts(3, 5),
                AnvilRecipeRules.consume(5, 8, 0, 2, 1, 3));
        assertEquals(
                new AnvilRecipeRules.RemainingCounts(2, 6),
                AnvilRecipeRules.consume(5, 8, 1, 2, 0, 3));
        assertThrows(
                IllegalArgumentException.class,
                () -> AnvilRecipeRules.consume(1, 1, 0, 2, 1, 1));
    }
}
