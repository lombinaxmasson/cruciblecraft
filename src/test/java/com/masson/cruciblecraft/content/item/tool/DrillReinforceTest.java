package com.masson.cruciblecraft.content.item.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DrillReinforceTest {
    @Test
    void bricksAndConcreteMapOntoReinforcedIdentities() {
        assertEquals(
                Optional.of("andesite/reinforced_bricks"),
                DrillReinforce.reinforcedPath("andesite/bricks"));
        assertEquals(
                Optional.of("concrete_reinforced/reinforced_concrete"),
                DrillReinforce.reinforcedPath("concrete/concrete"));
        assertEquals(
                Optional.of("concrete_reinforced/smooth"),
                DrillReinforce.reinforcedPath("concrete/smooth"));
        assertTrue(DrillReinforce.reinforcedPath("andesite/reinforced_bricks")
                .isEmpty());
        assertTrue(DrillReinforce.reinforcedPath("andesite/small_bricks")
                .isEmpty());
        assertTrue(DrillReinforce.reinforcedPath(
                "concrete/concrete_down_slab/slab_down").isEmpty());
        assertTrue(DrillReinforce.reinforcedPath(
                "concrete_reinforced/reinforced_concrete").isEmpty());
    }
}
