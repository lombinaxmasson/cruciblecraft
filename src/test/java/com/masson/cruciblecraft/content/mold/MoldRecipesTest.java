package com.masson.cruciblecraft.content.mold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;

import org.junit.jupiter.api.Test;

class MoldRecipesTest {
    @Test
    void ingotMaskMapsToIngotPrefix() {
        assertEquals(
                Optional.of(MaterialPrefixes.INGOT),
                MoldRecipes.recipe(MoldShape.INGOT.mask()));
        assertEquals(
                MaterialPrefixes.INGOT.units(),
                MoldRecipes.requiredUnits(MoldShape.INGOT.mask()));
    }

    @Test
    void fourClockwiseRotationsReturnTheIngotMask() {
        int mask = MoldShape.INGOT.mask();
        int rotated = mask;
        for (int step = 0; step < 4; step++) {
            rotated = MoldRecipes.rotateClockwise(rotated);
            assertEquals(
                    Optional.of(MaterialPrefixes.INGOT),
                    MoldRecipes.recipe(rotated));
        }
        assertEquals(mask, rotated);
    }

    @Test
    void chiselBitUsesGt6InnerFiveByFive() {
        assertEquals(Optional.of(1), MoldRecipes.chiselBit(0.20, 0.20));
        assertTrue(MoldRecipes.chiselBit(0.05, 0.50).isEmpty());
    }

    @Test
    void unknownNonZeroMaskIsNuggetUnits() {
        int shape = MoldShape.INGOT.mask() | 1;
        assertEquals(Optional.of(MaterialPrefixes.NUGGET), MoldRecipes.recipe(shape));
        assertEquals(
                Integer.bitCount(shape) * MaterialPrefixes.NUGGET.units(),
                MoldRecipes.requiredUnits(shape));
    }

    @Test
    void emptyMaskCannotFill() {
        assertTrue(MoldRecipes.recipe(0).isEmpty());
        assertEquals(0, MoldRecipes.requiredUnits(0));
    }
}
