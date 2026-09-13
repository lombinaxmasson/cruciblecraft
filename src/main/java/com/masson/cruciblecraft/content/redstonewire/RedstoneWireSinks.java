package com.masson.cruciblecraft.content.redstonewire;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;

/**
 * GT6 {@code CS.REDSTONE_SINKS} plus power-rail registrations from
 * {@code BlockBaseRail}. Wires must not treat these blocks as vanilla
 * redstone sources.
 */
public final class RedstoneWireSinks {
    private RedstoneWireSinks() {}

    public static boolean isSink(Block block) {
        return block == Blocks.TNT
                || block == Blocks.POWERED_RAIL
                || block == Blocks.ACTIVATOR_RAIL
                || block == Blocks.NOTE_BLOCK
                || block == Blocks.PISTON
                || block == Blocks.STICKY_PISTON
                || block == Blocks.DISPENSER
                || block == Blocks.DROPPER
                || block == Blocks.REDSTONE_LAMP
                || block instanceof DoorBlock
                || block instanceof TrapDoorBlock;
    }
}
