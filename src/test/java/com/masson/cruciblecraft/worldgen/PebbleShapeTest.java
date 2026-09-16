package com.masson.cruciblecraft.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import org.junit.jupiter.api.Test;

/** GT6 {@code WD.random(x^y^z)} pebble packing via {@link RandomSource}. */
class PebbleShapeTest {
    @Test
    void packMatchesRandomSourceXorSeed() {
        BlockPos pos = new BlockPos(12, 64, -7);
        RandomSource random = RandomSource.create();
        random.setSeed(pos.getX() ^ pos.getY() ^ pos.getZ());
        int expected = random.nextInt(4)
                | (random.nextInt(4) << 2)
                | (random.nextInt(4) << 4)
                | (random.nextInt(4) << 6)
                | (random.nextInt(4) << 8);
        assertEquals(expected, PebbleShape.pack(pos));
        assertEquals(PebbleShape.seed(pos), pos.getX() ^ pos.getY() ^ pos.getZ());
        PebbleShape.Pixels pixels = PebbleShape.pixels(expected);
        assertEquals(4 + (expected & 3), pixels.minX());
        assertEquals(12 - ((expected >> 4) & 3), pixels.maxX());
        assertEquals(1 + ((expected >> 8) & 3), pixels.maxY());
    }

    @Test
    void itemPackedIsCenteredEightByThree() {
        PebbleShape.Pixels pixels = PebbleShape.pixels(PebbleShape.ITEM_PACKED);
        assertEquals(4, pixels.minX());
        assertEquals(4, pixels.minZ());
        assertEquals(12, pixels.maxX());
        assertEquals(12, pixels.maxZ());
        assertEquals(3, pixels.maxY());
    }

    @Test
    void nearbyPositionsUsuallyDiffer() {
        assertNotEquals(
                PebbleShape.pack(new BlockPos(0, 64, 0)),
                PebbleShape.pack(new BlockPos(1, 64, 0)));
    }
}
