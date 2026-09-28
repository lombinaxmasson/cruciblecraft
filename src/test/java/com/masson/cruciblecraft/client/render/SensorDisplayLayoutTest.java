package com.masson.cruciblecraft.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.client.render.SensorDisplayLayout.Glyph;
import com.masson.cruciblecraft.content.sensor.SensorKind;
import com.masson.cruciblecraft.content.sensor.SensorMode;

import net.minecraft.core.Direction;

class SensorDisplayLayoutTest {
    private static final float EPS = 1.0e-4F;

    @Test
    void decimalReadoutFillsFiveDigitsAndTheUnitIcon() {
        Glyph[] cells = SensorDisplayLayout.glyphs(
                SensorKind.THERMOMETER, SensorMode.DISPLAY, false, 293L);
        assertIcons(cells, "0", "0", "2", "9", "3", "kelvin");
        assertEquals(SensorDisplayLayout.WHITE, cells[0].argb());
        assertEquals(0xFFFF0000, cells[5].argb());
    }

    @Test
    void hexadecimalReplacesTheLeadingDigitWithTheHexMarker() {
        Glyph[] cells = SensorDisplayLayout.glyphs(
                SensorKind.ELECTROMETER, SensorMode.DISPLAY, true, 0x1A2BL);
        assertIcons(cells, "hex", "0x1", "0xa", "0x2", "0xb", "eu");
    }

    @Test
    void comparisonModesKeepFourDigitsBesideTheSymbol() {
        Glyph[] cells = SensorDisplayLayout.glyphs(
                SensorKind.THERMOMETER, SensorMode.GREATER, false, 42L);
        assertIcons(cells, "greater", "0", "0", "4", "2", "kelvin");
    }

    @Test
    void fullModePaintsTheFixedReadoutRedAndLeavesTheUnitTinted() {
        Glyph[] cells = SensorDisplayLayout.glyphs(
                SensorKind.FLUIDOMETER, SensorMode.FULL, false, 0L);
        assertIcons(cells, "equal", "1", "0", "0", "percent", "liter");
        for (int index = 0; index < 5; index++) {
            assertEquals(SensorDisplayLayout.RED_192, cells[index].argb());
        }
        assertEquals(0xFF0000FF, cells[5].argb());
    }

    @Test
    void percentModeEndsOnAWhitePercentInsteadOfTheUnit() {
        Glyph[] cells = SensorDisplayLayout.glyphs(
                SensorKind.BUCKETOMETER, SensorMode.PERCENT, false, 100L);
        assertIcons(cells, "0", "0", "1", "0", "0", "percent");
        assertEquals(SensorDisplayLayout.WHITE, cells[5].argb());
    }

    @Test
    void itemMeterLeavesTheUnitWindowEmpty() {
        Glyph[] cells = SensorDisplayLayout.glyphs(
                SensorKind.ITEMOMETER, SensorMode.DISPLAY, false, 7L);
        assertIcons(cells, "0", "0", "0", "0", "7", null);
        assertNull(cells[5]);
    }

    @Test
    void wallWindowsMatchGt6CharacterBoxes() {
        assertFace(Direction.NORTH, 0, 0.75F, 0.75F, 0.870F, 0.875F, 0.875F, 0.870F);
        assertFace(Direction.NORTH, 5, 0.125F, 0.75F, 0.870F, 0.25F, 0.875F, 0.870F);
        assertFace(Direction.SOUTH, 0, 0.125F, 0.75F, 0.130F, 0.25F, 0.875F, 0.130F);
        assertFace(Direction.SOUTH, 5, 0.75F, 0.75F, 0.130F, 0.875F, 0.875F, 0.130F);
        assertFace(Direction.EAST, 0, 0.130F, 0.75F, 0.75F, 0.130F, 0.875F, 0.875F);
        assertFace(Direction.WEST, 0, 0.870F, 0.75F, 0.125F, 0.870F, 0.875F, 0.25F);
    }

    @Test
    void floorAndCeilingWindowsUseGt6PassBoxes() {
        assertFace(Direction.UP, 0, 0.125F, 0.130F, 0.125F, 0.25F, 0.130F, 0.25F);
        assertFace(Direction.DOWN, 0, 0.125F, 0.870F, 0.75F, 0.25F, 0.870F, 0.875F);
    }

    @Test
    void characterTexturesUseTheGt6BoundsUvInsteadOfTheWholeSprite() {
        assertUv(
                Direction.NORTH,
                0,
                new float[][] {
                    {12.0F, 2.0F},
                    {12.0F, 4.0F},
                    {14.0F, 4.0F},
                    {14.0F, 2.0F}
                });
        assertUv(
                Direction.NORTH,
                5,
                new float[][] {
                    {2.0F, 2.0F},
                    {2.0F, 4.0F},
                    {4.0F, 4.0F},
                    {4.0F, 2.0F}
                });
        assertUv(
                Direction.UP,
                0,
                new float[][] {
                    {2.0F, 2.0F},
                    {2.0F, 4.0F},
                    {4.0F, 4.0F},
                    {4.0F, 2.0F}
                });
    }

    private static void assertIcons(Glyph[] cells, String... icons) {
        assertEquals(icons.length, cells.length);
        for (int index = 0; index < icons.length; index++) {
            if (icons[index] == null) {
                assertNull(cells[index]);
            } else {
                assertEquals(icons[index], cells[index].icon());
            }
        }
    }

    private static void assertFace(
            Direction facing,
            int index,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ) {
        float[][] corners = SensorCharacterCells.worldCorners(facing, index);
        float ax = Float.POSITIVE_INFINITY;
        float ay = Float.POSITIVE_INFINITY;
        float az = Float.POSITIVE_INFINITY;
        float bx = Float.NEGATIVE_INFINITY;
        float by = Float.NEGATIVE_INFINITY;
        float bz = Float.NEGATIVE_INFINITY;
        for (float[] corner : corners) {
            ax = Math.min(ax, corner[0]);
            ay = Math.min(ay, corner[1]);
            az = Math.min(az, corner[2]);
            bx = Math.max(bx, corner[0]);
            by = Math.max(by, corner[1]);
            bz = Math.max(bz, corner[2]);
        }
        assertEquals(minX, ax, EPS);
        assertEquals(minY, ay, EPS);
        assertEquals(minZ, az, EPS);
        assertEquals(maxX, bx, EPS);
        assertEquals(maxY, by, EPS);
        assertEquals(maxZ, bz, EPS);
    }

    private static void assertUv(
            Direction facing,
            int index,
            float[][] expected) {
        float[][] actual = SensorCharacterCells.textureCoordinates(facing, index);
        assertEquals(expected.length, actual.length);
        for (int vertex = 0; vertex < expected.length; vertex++) {
            assertEquals(expected[vertex][0], actual[vertex][0], EPS);
            assertEquals(expected[vertex][1], actual[vertex][1], EPS);
        }
    }
}
