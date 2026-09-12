package com.masson.cruciblecraft.worldgen.tree.prep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GtTreeGrowerTest {
    private static final int SAPLING_X = 0;
    private static final int SAPLING_Y = 16;
    private static final int SAPLING_Z = 0;

    @Test
    void everySpeciesGrowsOnAnOpenColumn() {
        for (GtTreeSpecies species : GtTreeSpecies.ALL) {
            MemoryWorld world = MemoryWorld.open();
            boolean grew = GtTreeGrower.grow(
                    species, world, SAPLING_X, SAPLING_Y, SAPLING_Z, new Random(1L));
            assertTrue(grew, species.id());
            assertTrue(world.logs() > 0, species.id());
            assertTrue(world.leaves() > 0, species.id());
        }
    }

    @Test
    void rubberGrowPlacesOneHoleWhenNoneAreNearby() {
        MemoryWorld world = MemoryWorld.open();
        world.nearbyRubberHole = false;
        assertTrue(GtTreeGrower.grow(
                GtTreeSpecies.RUBBER,
                world,
                SAPLING_X,
                SAPLING_Y,
                SAPLING_Z,
                new Random(1L)));
        assertEquals(1, world.holes());
        assertEquals(0, world.podzol());
    }

    @Test
    void mapleAndRainbowoodGrowPlaceNoHoles() {
        for (GtTreeSpecies species : new GtTreeSpecies[] {
            GtTreeSpecies.MAPLE, GtTreeSpecies.RAINBOWOOD
        }) {
            MemoryWorld world = MemoryWorld.open();
            assertTrue(GtTreeGrower.grow(
                    species, world, SAPLING_X, SAPLING_Y, SAPLING_Z, new Random(1L)));
            assertEquals(0, world.holes(), species.id());
        }
    }

    @Test
    void tooShortColumnFailsAndWritesNothing() {
        MemoryWorld world = MemoryWorld.open();
        world.blocked.add(key(SAPLING_X, SAPLING_Y + 3, SAPLING_Z));
        assertFalse(GtTreeGrower.grow(
                GtTreeSpecies.RUBBER,
                world,
                SAPLING_X,
                SAPLING_Y,
                SAPLING_Z,
                new Random(1L)));
        assertEquals(0, world.logs());
        assertEquals(0, world.leaves());
        assertEquals(0, world.holes());
    }

    @Test
    void blueSpruceConvertsDirtUnderTheCanopy() {
        MemoryWorld world = MemoryWorld.open();
        world.dirt.add(key(2, SAPLING_Y, 0));
        world.dirt.add(key(2, SAPLING_Y - 1, 0));
        assertTrue(GtTreeGrower.grow(
                GtTreeSpecies.BLUE_SPRUCE,
                world,
                SAPLING_X,
                SAPLING_Y,
                SAPLING_Z,
                new Random(1L)));
        assertTrue(world.podzol() > 0);
        assertTrue(world.cells.containsKey(key(2, SAPLING_Y, 0)));
        assertEquals(Cell.PODZOL, world.cells.get(key(2, SAPLING_Y, 0)));
    }

    @Test
    void holeModesMatchTheNineFeatureDenominator() {
        Map<GtTreeSpecies, GtTreeSpecies.TreeHoleMode> expected =
                new EnumMap<>(GtTreeSpecies.class);
        expected.put(GtTreeSpecies.RUBBER, GtTreeSpecies.TreeHoleMode.GROW_RUBBER);
        expected.put(GtTreeSpecies.MAPLE, GtTreeSpecies.TreeHoleMode.DRILL_MAPLE);
        expected.put(GtTreeSpecies.WILLOW, GtTreeSpecies.TreeHoleMode.NONE);
        expected.put(GtTreeSpecies.BLUE_MAHOE, GtTreeSpecies.TreeHoleMode.NONE);
        expected.put(GtTreeSpecies.HAZEL, GtTreeSpecies.TreeHoleMode.NONE);
        expected.put(GtTreeSpecies.CINNAMON, GtTreeSpecies.TreeHoleMode.NONE);
        expected.put(GtTreeSpecies.COCONUT, GtTreeSpecies.TreeHoleMode.NONE);
        expected.put(
                GtTreeSpecies.RAINBOWOOD, GtTreeSpecies.TreeHoleMode.DRILL_RAINBOWOOD);
        expected.put(GtTreeSpecies.BLUE_SPRUCE, GtTreeSpecies.TreeHoleMode.NONE);
        assertEquals(9, GtTreeSpecies.ALL.size());
        for (GtTreeSpecies species : GtTreeSpecies.ALL) {
            assertEquals(expected.get(species), species.holeMode(), species.id());
        }
        assertEquals(32762, GtTreeSpecies.RUBBER.holeSourceId());
        assertEquals(32761, GtTreeSpecies.MAPLE.holeSourceId());
        assertEquals(32760, GtTreeSpecies.RAINBOWOOD.holeSourceId());
        assertEquals("rubber_resin", GtTreeSpecies.RUBBER.holeItemId());
        assertEquals("FL.Sap_Maple", GtTreeSpecies.MAPLE.holeFluidToken());
        assertEquals("FL.Sap_Rainbow", GtTreeSpecies.RAINBOWOOD.holeFluidToken());
    }

    @Test
    void placementUsesOverworldBiomesAndRainbowRarePath() {
        Random neverRare = new Random() {
            @Override
            public int nextInt(int bound) {
                return bound == GtTreeSpecies.RAINBOWOOD_RARE_CHANCE ? 1 : super.nextInt(bound);
            }
        };
        Random alwaysRare = new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
        assertEquals(
                1,
                GtTreePlacement.canGenerate(
                        GtTreeSpecies.RUBBER, Set.of("minecraft:taiga"), neverRare));
        assertEquals(
                0,
                GtTreePlacement.canGenerate(
                        GtTreeSpecies.RUBBER, Set.of("minecraft:plains"), neverRare));
        assertEquals(
                1,
                GtTreePlacement.canGenerate(
                        GtTreeSpecies.COCONUT, Set.of("minecraft:beach"), neverRare));
        assertEquals(
                0,
                GtTreePlacement.canGenerate(
                        GtTreeSpecies.COCONUT, Set.of("minecraft:desert"), neverRare));
        assertEquals(
                0,
                GtTreePlacement.canGenerate(
                        GtTreeSpecies.RAINBOWOOD, Set.of("minecraft:plains"), neverRare));
        assertEquals(
                1,
                GtTreePlacement.canGenerate(
                        GtTreeSpecies.RAINBOWOOD, Set.of("minecraft:plains"), alwaysRare));
        assertTrue(GtTreePlacement.shouldPlaceRay(GtTreeSpecies.HAZEL, alwaysRare));
        assertNotEquals("minecraft:enchanted_forest", "minecraft:plains");
    }

    private static long key(int x, int y, int z) {
        return (((long) x & 0xFFFFF) << 40) | (((long) y & 0xFFFFF) << 20) | (z & 0xFFFFF);
    }

    private enum Cell {
        LOG,
        LEAVES,
        RUBBER_HOLE,
        PODZOL
    }

    private static final class MemoryWorld implements GtTreeWorld {
        private final Set<Long> blocked = new HashSet<>();
        private final Set<Long> dirt = new HashSet<>();
        private final Map<Long, Cell> cells = new HashMap<>();
        private boolean nearbyRubberHole;

        static MemoryWorld open() {
            return new MemoryWorld();
        }

        @Override
        public int height() {
            return 256;
        }

        @Override
        public boolean canPlaceTree(int x, int y, int z) {
            if (y < 0 || y >= height()) {
                return false;
            }
            long key = key(x, y, z);
            if (blocked.contains(key)) {
                return false;
            }
            Cell cell = cells.get(key);
            return cell == null || cell == Cell.LEAVES;
        }

        @Override
        public void setLog(int x, int y, int z) {
            cells.put(key(x, y, z), Cell.LOG);
        }

        @Override
        public void setLeaves(int x, int y, int z) {
            cells.put(key(x, y, z), Cell.LEAVES);
        }

        @Override
        public void setRubberResinHole(int x, int y, int z, HorizontalFacing facing) {
            cells.put(key(x, y, z), Cell.RUBBER_HOLE);
        }

        @Override
        public boolean hasNearbyRubberResinHole(int x, int z) {
            return nearbyRubberHole;
        }

        @Override
        public boolean isAir(int x, int y, int z) {
            return !cells.containsKey(key(x, y, z)) && !dirt.contains(key(x, y, z));
        }

        @Override
        public boolean isDirtOrGrass(int x, int y, int z) {
            return dirt.contains(key(x, y, z));
        }

        @Override
        public void setPodzol(int x, int y, int z) {
            cells.put(key(x, y, z), Cell.PODZOL);
        }

        int logs() {
            return count(Cell.LOG);
        }

        int leaves() {
            return count(Cell.LEAVES);
        }

        int holes() {
            return count(Cell.RUBBER_HOLE);
        }

        int podzol() {
            return count(Cell.PODZOL);
        }

        private int count(Cell wanted) {
            int total = 0;
            for (Cell cell : cells.values()) {
                if (cell == wanted) {
                    total++;
                }
            }
            return total;
        }
    }
}
