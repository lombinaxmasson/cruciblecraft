package com.masson.cruciblecraft.content.block;

import java.util.List;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * GT6 {@code MultiTileEntityCrusher} walk XZ and adjacent RU source
 * positions. JSON is north-authored; energy holes sit at local {@code x=±2},
 * {@code y=1}, {@code z=2}.
 */
public final class LargeCrusherGeometry {
    private static final Offset BASIN_CENTER = new Offset(0, 0, 2);
    private static final Offset WEST_ENERGY = new Offset(-2, 1, 2);
    private static final Offset EAST_ENERGY = new Offset(2, 1, 2);

    private LargeCrusherGeometry() {}

    public static boolean insideWheelWalk(
            BlockPos controller, Direction facing, Vec3 position) {
        Offset center = BASIN_CENTER.rotate(facing);
        double cx = controller.getX() + center.x();
        double cz = controller.getZ() + center.z();
        return position.x >= cx - 1.0
                && position.z >= cz - 1.0
                && position.x <= cx + 2.0
                && position.z <= cz + 2.0;
    }

    /**
     * GT6 {@code tD}: Z-facing idle/running is 0/1, X-facing is 2/3.
     */
    public static int wheelDesign(Direction facing, boolean running) {
        boolean axisZ = facing.getAxis() == Direction.Axis.Z;
        if (axisZ) {
            return running ? 1 : 0;
        }
        return running ? 3 : 2;
    }

    public static List<Neighbor> adjacentEnergySources(
            BlockPos controller, Direction facing) {
        return List.of(
                neighbor(controller, facing, WEST_ENERGY, Direction.WEST),
                neighbor(controller, facing, EAST_ENERGY, Direction.EAST));
    }

    private static Neighbor neighbor(
            BlockPos controller,
            Direction facing,
            Offset port,
            Direction localOut) {
        Offset rotated = port.rotate(facing);
        BlockPos portPos = controller.offset(
                rotated.x(), rotated.y(), rotated.z());
        Direction worldOut = rotateHorizontal(localOut, facing);
        return new Neighbor(portPos.relative(worldOut), worldOut.getOpposite());
    }

    private static Direction rotateHorizontal(
            Direction local, Direction facing) {
        Offset step = new Offset(local.getStepX(), 0, local.getStepZ())
                .rotate(facing);
        if (step.x() == 1) {
            return Direction.EAST;
        }
        if (step.x() == -1) {
            return Direction.WEST;
        }
        if (step.z() == 1) {
            return Direction.SOUTH;
        }
        if (step.z() == -1) {
            return Direction.NORTH;
        }
        throw new IllegalArgumentException("Horizontal rotation produced zero");
    }

    public record Neighbor(BlockPos position, Direction face) {}
}
