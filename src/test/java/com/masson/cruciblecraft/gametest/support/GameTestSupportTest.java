package com.masson.cruciblecraft.gametest.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.machine.processing.MachineRelativeFace;

import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

class GameTestSupportTest {
    @Test
    void failureTextStopsAtNineHundredCharacters() {
        String message = "x".repeat(1200);
        String truncated = GameTestFailures.truncate(message);
        assertEquals(GameTestFailures.LIMIT, truncated.length());
        assertTrue(truncated.endsWith("..."));
        assertEquals("short", GameTestFailures.truncate("short"));
        assertEquals("", GameTestFailures.truncate(null));
    }

    @Test
    void electrolyzerAndCentrifugeTakePowerFromBelow() {
        // GT6 Loader_MultiTileEntities: electrolyzer and centrifuge energy
        // masks are SIDE_BOTTOM. Top and bottom do not depend on the front.
        assertEquals(
                Direction.DOWN,
                GameTestMachinePlacement.energy("electrolyzer", Direction.NORTH));
        assertEquals(
                Direction.DOWN,
                GameTestMachinePlacement.energy("centrifuge", Direction.EAST));
        assertEquals(
                Direction.UP,
                GameTestMachinePlacement.itemInput("electrolyzer", Direction.NORTH));
    }

    @Test
    void leftIsThePlayersLeftWhileFacingTheFront() {
        assertEquals(
                Direction.EAST,
                GameTestMachinePlacement.world(Direction.NORTH, MachineRelativeFace.LEFT));
        assertEquals(
                Direction.WEST,
                GameTestMachinePlacement.world(Direction.NORTH, MachineRelativeFace.RIGHT));
    }

    @Test
    void unknownProfileNamesTheMissingMachine() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> GameTestMachinePlacement.energy("not_a_gt6_machine", Direction.NORTH));
        assertTrue(failure.getMessage().contains("not_a_gt6_machine"));
    }
}
