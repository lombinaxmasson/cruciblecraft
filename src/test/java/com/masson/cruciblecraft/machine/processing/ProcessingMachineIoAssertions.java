package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.Direction;

public final class ProcessingMachineIoAssertions {
    private ProcessingMachineIoAssertions() {}

    public static void assertMatchesProfile(ProcessingMachineSpec spec) {
        ProcessingMachineSpec.SidedIoPolicy expected =
                Gt6SidedIo.policy(spec);
        Direction front = Direction.NORTH;
        Direction[] sides = {
                null,
                Direction.DOWN,
                Direction.UP,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.WEST,
                Direction.EAST
        };
        for (Direction side : sides) {
            String label = spec.id() + " " + side;
            assertEquals(
                    expected.items().resolve(front, side),
                    spec.sidedIo().items().resolve(front, side),
                    label + " items");
            assertEquals(
                    expected.fluids().resolve(front, side),
                    spec.sidedIo().fluids().resolve(front, side),
                    label + " fluids");
            assertEquals(
                    expected.energy().resolve(front, side),
                    spec.sidedIo().energy().resolve(front, side),
                    label + " energy");
        }
    }
}
