package com.masson.cruciblecraft.content.block;

import java.util.List;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** GT6 Squeezer's two exterior RU source positions. */
public final class LargeSqueezerGeometry {
    private static final Offset WEST = new Offset(-2, 1, 2);
    private static final Offset EAST = new Offset(2, 1, 2);

    private LargeSqueezerGeometry() {}

    public static List<Neighbor> adjacentEnergySources(
            BlockPos controller, Direction facing) {
        return List.of(neighbor(controller, facing, WEST, Direction.WEST),
                neighbor(controller, facing, EAST, Direction.EAST));
    }

    private static Neighbor neighbor(BlockPos controller, Direction facing,
            Offset port, Direction localOut) {
        Offset rotated = port.rotate(facing);
        BlockPos portPos = controller.offset(
                rotated.x(), rotated.y(), rotated.z());
        Direction worldOut = rotateHorizontal(localOut, facing);
        return new Neighbor(portPos.relative(worldOut), worldOut.getOpposite());
    }

    private static Direction rotateHorizontal(Direction local, Direction facing) {
        Offset step = new Offset(local.getStepX(), 0, local.getStepZ())
                .rotate(facing);
        if (step.x() == 1) return Direction.EAST;
        if (step.x() == -1) return Direction.WEST;
        if (step.z() == 1) return Direction.SOUTH;
        if (step.z() == -1) return Direction.NORTH;
        throw new IllegalArgumentException("Horizontal rotation produced zero");
    }

    public record Neighbor(BlockPos position, Direction face) {}
}
