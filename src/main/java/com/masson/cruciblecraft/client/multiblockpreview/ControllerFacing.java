package com.masson.cruciblecraft.client.multiblockpreview;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * The facing a controller validates its structure with. Controllers use one
 * of two direction properties; vertical values fall back to north, matching
 * the crucible hosts.
 */
public final class ControllerFacing {
    private static final DirectionProperty[] PROPERTIES = {
            com.masson.cruciblecraft.content.block.ProcessingMachineBlock.FACING,
            com.masson.cruciblecraft.content.block.MteInPlaceBlock.FACING,
            com.masson.cruciblecraft.content.block.CokeOvenBlock.FACING
    };

    private ControllerFacing() {}

    public static Direction of(BlockState state) {
        for (DirectionProperty property : PROPERTIES) {
            if (state.hasProperty(property)) {
                Direction facing = state.getValue(property);
                return facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
            }
        }
        return Direction.NORTH;
    }
}
