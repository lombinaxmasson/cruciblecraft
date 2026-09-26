package com.masson.cruciblecraft.gametest.support;

import java.util.Optional;

import com.masson.cruciblecraft.machine.processing.Gt6SidedIo;
import com.masson.cruciblecraft.machine.processing.IoChannel;
import com.masson.cruciblecraft.machine.processing.MachineRelativeFace;

import net.minecraft.core.Direction;

/**
 * World directions for a GT6 processing profile. Left and right are the
 * player's left and right while facing the machine front.
 */
public final class GameTestMachinePlacement {
    private GameTestMachinePlacement() {}

    public static Direction world(Direction front, MachineRelativeFace relative) {
        return relative.toWorld(front);
    }

    public static Direction energy(String profileKey, Direction front) {
        return required(
                profileKey,
                "energy",
                Gt6SidedIo.policy(profileKey).energyChannel().firstInputWorld(front));
    }

    public static Direction itemInput(String profileKey, Direction front) {
        IoChannel channel = Gt6SidedIo.policy(profileKey).itemsChannel();
        return required(
                profileKey,
                "item input",
                channel.autoInputWorld(front).or(() -> channel.firstInputWorld(front)));
    }

    public static Direction itemOutput(String profileKey, Direction front) {
        IoChannel channel = Gt6SidedIo.policy(profileKey).itemsChannel();
        return required(
                profileKey,
                "item output",
                channel.autoOutputWorld(front).or(() -> channel.firstOutputWorld(front)));
    }

    public static Direction fluidInput(String profileKey, Direction front) {
        IoChannel channel = Gt6SidedIo.policy(profileKey).fluidsChannel();
        return required(
                profileKey,
                "fluid input",
                channel.autoInputWorld(front).or(() -> channel.firstInputWorld(front)));
    }

    public static Direction fluidOutput(String profileKey, Direction front) {
        IoChannel channel = Gt6SidedIo.policy(profileKey).fluidsChannel();
        return required(
                profileKey,
                "fluid output",
                channel.autoOutputWorld(front).or(() -> channel.firstOutputWorld(front)));
    }

    private static Direction required(
            String profileKey,
            String channel,
            Optional<Direction> direction) {
        if (direction.isPresent()) {
            return direction.orElse(Direction.DOWN);
        }
        throw new IllegalArgumentException(
                profileKey + " has no " + channel + " face");
    }
}
