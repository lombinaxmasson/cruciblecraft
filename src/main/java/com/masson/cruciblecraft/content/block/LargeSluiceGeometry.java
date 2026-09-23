package com.masson.cruciblecraft.content.block;

import java.util.List;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** GT6 17107 Sluice geometry and its two adjacent RU source positions. */
public final class LargeSluiceGeometry {
    private static final Offset WEST_ENERGY = new Offset(-1, 1, 5);
    private static final Offset EAST_ENERGY = new Offset(1, 1, 5);

    private LargeSluiceGeometry() {}

    /**
     * GT6 {@code tD}: idle designs are 0..3 and active designs are 4..7.
     * The source uses the four horizontal side ids in north/south/west/east
     * order, with the active state offset by four.
     */
    public static int partDesign(Direction facing, boolean active) {
        int idle = switch (facing) {
            case NORTH -> 0;
            case SOUTH -> 1;
            case WEST -> 2;
            case EAST -> 3;
            default -> 0;
        };
        return active ? idle + 4 : idle;
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
