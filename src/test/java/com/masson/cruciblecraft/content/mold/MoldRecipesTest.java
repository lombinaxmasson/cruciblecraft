package com.masson.cruciblecraft.content.mold;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;

import net.minecraft.core.Direction;

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
    void unknownSingleCellIsNuggetUnits() {
        int shape = 1 << 12;
        assertEquals(Optional.of(MaterialPrefixes.NUGGET), MoldRecipes.recipe(shape));
        assertEquals(
                MaterialPrefixes.NUGGET.units(),
                MoldRecipes.requiredUnits(shape));
    }

    @Test
    void emptyMaskCannotFill() {
        assertTrue(MoldRecipes.recipe(0).isEmpty());
        assertEquals(0, MoldRecipes.requiredUnits(0));
    }

    @Test
    void blankCellsShowTopOnlyUntilANeighbourIsChiseled() {
        assertTrue(MoldRecipes.cellPresent(0, 0));
        assertTrue(MoldRecipes.cellFaceVisible(0, 0, Direction.UP));
        assertFalse(MoldRecipes.cellFaceVisible(0, 0, Direction.EAST));
        assertFalse(MoldRecipes.cellPresent(1, 0));
        assertFalse(MoldRecipes.cellFaceVisible(1, 0, Direction.UP));
        assertTrue(MoldRecipes.cellFaceVisible(1, 5, Direction.WEST));
    }

    @Test
    void gt6ClayFiringMasksKeepPrefixAndUnits() {
        assertFiring(0b0_01110_01110_01110_01110_01110, MaterialPrefixes.INGOT, 144);
        assertFiring(0b0_11111_11111_11111_11111_11111, MaterialPrefixes.PLATE, 144);
        assertFiring(0b0_00000_00000_11111_00000_00000, MaterialPrefixes.ROD, 72);
        assertFiring(0b0_00000_00000_00100_00100_00000, MaterialPrefixes.BOLT, 18);
        assertFiring(0b0_10101_01110_11011_01110_10101, MaterialPrefixes.GEAR, 576);
        assertFiring(0b0_01010_11111_01010_11111_01010, MaterialPrefixes.SMALL_GEAR, 144);
        assertFiring(0b0_00000_01110_01010_01110_00000, MaterialPrefixes.RING, 36);
        assertFiring(0b0_10000_01000_00100_00010_00001, MaterialPrefixes.LONG_ROD, 144);
        assertFiring(0b0_01100_11110_11110_01100_00000, prefix("billet"), 144);
        assertFiring(0b0_11000_11000_00000_00000_00000, prefix("chunk"), 144);
        assertFiring(0b0_00000_01110_01110_01110_00000, prefix("tiny_plate"), 16);
        assertFiring(0b0_11101_11101_11101_00001_11100, prefix("small_casing"), 72);
        assertFiring(0b0_00100_01110_01110_01110_01110, prefix("tool_head_raw_sword"), 288);
        assertFiring(0b0_00000_01110_10001_00000_00000, prefix("tool_head_raw_pickaxe"), 432);
        assertFiring(0b0_01110_01110_01110_01010_00000, prefix("tool_head_raw_spade"), 144);
        assertFiring(0b0_00100_01110_01110_01110_00000, prefix("tool_head_raw_shovel"), 144);
        assertFiring(0b0_00100_01110_01100_01110_00000, prefix("tool_head_raw_universal_spade"), 128);
        assertFiring(0b0_00000_01110_01110_01000_00000, prefix("tool_head_raw_axe"), 432);
        assertFiring(0b0_00000_11111_11111_10001_00000, prefix("tool_head_raw_axe_double"), 720);
        assertFiring(0b0_00000_11111_11111_00000_00000, prefix("tool_head_raw_saw"), 288);
        assertFiring(0b0_01110_01110_01010_01110_01110, prefix("tool_head_hammer"), 864);
        assertFiring(0b0_01110_01110_01110_00100_00100, prefix("tool_head_file"), 216);
        assertFiring(0b0_00000_00100_00100_00100_00100, prefix("tool_head_screwdriver"), 144);
        assertFiring(0b0_01110_00100_00100_00100_00100, prefix("tool_head_raw_chisel"), 216);
        assertFiring(0b0_00000_00100_00100_01110_00000, prefix("tool_head_raw_arrow"), 18);
        assertFiring(0b0_00000_00110_01110_00000_00000, prefix("tool_head_raw_hoe"), 288);
        assertFiring(0b0_00000_01111_11111_00000_00000, prefix("tool_head_raw_sense"), 432);
        assertFiring(0b0_11111_11111_11111_11111_00100, prefix("tool_head_raw_plow"), 576);
        assertFiring(0b0_00000_00100_11111_01110_01010, prefix("tool_head_builderwand"), 144);
        assertFiring(0b0_00000_00000_00100_00000_00000, MaterialPrefixes.NUGGET, 16);
    }

    private static void assertFiring(int mask, MaterialPrefix prefix, int units) {
        assertEquals(Optional.of(prefix), MoldRecipes.recipe(mask), prefix.id());
        assertEquals(units, MoldRecipes.requiredUnits(mask), prefix.id());
        assertEquals(units, prefix.units(), prefix.id());
    }

    private static MaterialPrefix prefix(String path) {
        return new MaterialPrefix("cruciblecraft:" + path);
    }
}
