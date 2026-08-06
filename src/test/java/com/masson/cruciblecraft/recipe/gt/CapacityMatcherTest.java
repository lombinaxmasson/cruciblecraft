package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CapacityMatcherTest {
    @Test
    void resolvesOverlappingInputsWithoutGreedyOrderDependence() {
        long[][] allocation = CapacityMatcher.solve(
                new long[] {1, 1},
                new long[] {1, 1},
                new boolean[][] {
                    {true, true},
                    {true, false}
                }).orElseThrow();

        assertEquals(1L, allocation[0][0] + allocation[0][1]);
        assertEquals(1L, allocation[1][0] + allocation[1][1]);
        assertEquals(1L, allocation[0][0] + allocation[1][0]);
        assertEquals(1L, allocation[0][1] + allocation[1][1]);
        assertEquals(0L, allocation[1][1]);
        assertTrue(CapacityMatcher.canSatisfy(
                new long[] {1, 1},
                new long[] {1, 1},
                new boolean[][] {
                    {true, true},
                    {true, false}
                }));
    }

    @Test
    void rejectsMissingOrIncompatibleCapacity() {
        assertFalse(CapacityMatcher.canSatisfy(
                new long[] {2, 1},
                new long[] {1, 1},
                new boolean[][] {
                    {true, true},
                    {false, true}
                }));
        assertFalse(CapacityMatcher.canSatisfy(
                new long[] {1},
                new long[] {1},
                new boolean[][] {{false}}));
    }

    @Test
    void checksPresenceOnlyRequirementsAfterConsumption() {
        long[][] allocation = CapacityMatcher.solve(
                        new long[] {1, 0},
                        new long[] {1, 1},
                        new boolean[][] {
                            {true, false},
                            {false, true}
                        },
                        new boolean[] {false, true})
                .orElseThrow();

        assertEquals(1L, allocation[0][0]);
        assertEquals(0L, allocation[1][0] + allocation[1][1]);
    }

    @Test
    void rejectsPresenceWhenItsOnlySupplyWasConsumed() {
        assertTrue(CapacityMatcher.solve(
                        new long[] {1, 0},
                        new long[] {1},
                        new boolean[][] {
                            {true},
                            {true}
                        },
                        new boolean[] {false, true})
                .isEmpty());
    }

    @Test
    void choosesAnAllocationThatLeavesAValidPresenceWitness() {
        long[][] allocation = CapacityMatcher.solve(
                        new long[] {1, 0},
                        new long[] {1, 1},
                        new boolean[][] {
                            {true, true},
                            {true, false}
                        },
                        new boolean[] {false, true})
                .orElseThrow();

        assertEquals(0L, allocation[0][0]);
        assertEquals(1L, allocation[0][1]);
    }

    @Test
    void acceptsAllPresenceOnlySubproblemWhenWitnessExists() {
        assertTrue(CapacityMatcher.solve(
                        new long[] {0},
                        new long[] {1},
                        new boolean[][] {{true}},
                        new boolean[] {true})
                .isPresent());
    }

    @Test
    void presenceOnlyRequirementsCanShareUnconsumedSupply() {
        assertTrue(CapacityMatcher.solve(
                        new long[] {1, 0, 0},
                        new long[] {2},
                        new boolean[][] {
                            {true},
                            {true},
                            {true}
                        },
                        new boolean[] {false, true, true})
                .isPresent());
    }

    @Test
    void acceptsPresenceReservationsAtTwelveSupplyBoundary() {
        boolean[][] compatible = new boolean[1][12];
        java.util.Arrays.fill(compatible[0], true);

        assertTrue(CapacityMatcher.solve(
                        new long[] {0},
                        new long[12],
                        compatible,
                        new boolean[] {true})
                .isEmpty());

        long[] supplies = new long[12];
        supplies[11] = 1L;
        assertTrue(CapacityMatcher.solve(
                        new long[] {0},
                        supplies,
                        compatible,
                        new boolean[] {true})
                .isPresent());
    }

    @Test
    void rejectsPresenceReservationsAboveTwelveSupplies() {
        boolean[][] compatible = new boolean[1][13];
        java.util.Arrays.fill(compatible[0], true);

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> CapacityMatcher.solve(
                        new long[] {0},
                        new long[13],
                        compatible,
                        new boolean[] {true}));

        assertTrue(failure.getMessage().contains("actual=13"));
        assertTrue(failure.getMessage().contains("cap=12"));
        assertTrue(failure.getMessage().contains("replace"));
    }

    @Test
    void keepsThirteenSupplyConsumingPathAvailable() {
        boolean[][] compatible = new boolean[1][13];
        java.util.Arrays.fill(compatible[0], true);
        long[] supplies = new long[13];
        supplies[12] = 1L;

        assertTrue(CapacityMatcher.solve(
                        new long[] {1},
                        supplies,
                        compatible)
                .isPresent());
    }

    @Test
    void denseConsumingPathCoversTwelveThroughSixtyFourSupplies() {
        for (int size : new int[] {12, 16, 32, 64}) {
            long[] demands = new long[size];
            long[] supplies = new long[size];
            java.util.Arrays.fill(demands, 1L);
            java.util.Arrays.fill(supplies, 1L);
            boolean[][] compatible = new boolean[size][size];
            for (boolean[] row : compatible) {
                java.util.Arrays.fill(row, true);
            }

            long[][] allocation = CapacityMatcher.solve(
                    demands, supplies, compatible).orElseThrow();
            for (int requirement = 0; requirement < size; requirement++) {
                assertEquals(
                        1L,
                        java.util.Arrays.stream(allocation[requirement]).sum());
            }
            for (int supply = 0; supply < size; supply++) {
                long allocated = 0L;
                for (long[] row : allocation) {
                    allocated += row[supply];
                }
                assertEquals(1L, allocated);
            }
        }
    }

    @Test
    void validatesMatrixShapeAndNegativeValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CapacityMatcher.canSatisfy(
                        new long[] {1},
                        new long[] {1},
                        new boolean[0][0]));
        assertThrows(
                IllegalArgumentException.class,
                () -> CapacityMatcher.canSatisfy(
                        new long[] {-1},
                        new long[] {1},
                        new boolean[][] {{true}}));
        assertThrows(
                IllegalArgumentException.class,
                () -> CapacityMatcher.canSatisfy(
                        new long[] {0},
                        new long[] {1},
                        new boolean[][] {{true}}));
        assertThrows(
                IllegalArgumentException.class,
                () -> CapacityMatcher.canSatisfy(
                        new long[] {Long.MAX_VALUE, 1},
                        new long[] {Long.MAX_VALUE, 1},
                        new boolean[][] {
                            {true, true},
                            {true, true}
                        }));
    }
}
