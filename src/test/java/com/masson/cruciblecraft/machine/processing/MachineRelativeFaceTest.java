package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

class MachineRelativeFaceTest {
    @Test
    void northFacingMatchesGt6PlayerLeft() {
        Direction front = Direction.NORTH;
        assertEquals(Direction.DOWN, MachineRelativeFace.BOTTOM.toWorld(front));
        assertEquals(Direction.UP, MachineRelativeFace.TOP.toWorld(front));
        assertEquals(Direction.EAST, MachineRelativeFace.LEFT.toWorld(front));
        assertEquals(Direction.NORTH, MachineRelativeFace.FRONT.toWorld(front));
        assertEquals(Direction.WEST, MachineRelativeFace.RIGHT.toWorld(front));
        assertEquals(Direction.SOUTH, MachineRelativeFace.BACK.toWorld(front));
        assertEquals(
                MachineRelativeFace.LEFT,
                MachineRelativeFace.fromWorld(front, Direction.EAST));
        assertEquals(
                MachineRelativeFace.RIGHT,
                MachineRelativeFace.fromWorld(front, Direction.WEST));
    }

    @Test
    void eastFacingMapsBackToWest() {
        Direction front = Direction.EAST;
        assertEquals(Direction.WEST, MachineRelativeFace.BACK.toWorld(front));
        assertEquals(Direction.SOUTH, MachineRelativeFace.LEFT.toWorld(front));
        assertEquals(Direction.NORTH, MachineRelativeFace.RIGHT.toWorld(front));
        assertEquals(Direction.DOWN, MachineRelativeFace.BOTTOM.toWorld(front));
    }
}
