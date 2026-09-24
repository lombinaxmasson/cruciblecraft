package com.masson.cruciblecraft.content.block;

import java.util.List;
import java.util.function.Function;

import com.masson.cruciblecraft.content.mte.MteInPlaceKind;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Voxel geometry matching GT6 fluid-attachment render passes. */
public final class FluidAttachmentShapes {
    private FluidAttachmentShapes() {}

    public static VoxelShape shape(
            MteInPlaceKind kind,
            Direction facing) {
        List<Box> boxes = switch (kind) {
            case FAUCET -> List.of(
                    new Box(6, 1, 0, 10, 2, 4),
                    new Box(5, 2, 0, 11, 6, 4),
                    new Box(10, 2, 0, 11, 6, 4));
            case TAP -> List.of(
                    new Box(6, 6, 2, 10, 7, 4),
                    new Box(7, 4, 0, 9, 6, 16),
                    new Box(7, 3, 4, 9, 6, 6));
            case FUNNEL -> List.of(
                    new Box(5, 9, 0, 11, 10, 6),
                    new Box(6, 8, 0, 10, 9, 4),
                    new Box(7, 7, 0, 9, 9, 2));
            case NOZZLE -> List.of(
                    new Box(6, 3, 1, 10, 7, 2),
                    new Box(7, 4, 0, 9, 6, 6));
            case CAP_NOZZLE -> List.of(
                    new Box(6, 3, 1, 10, 7, 6),
                    new Box(7, 4, 0, 9, 6, 2));
            default -> throw new IllegalArgumentException(
                    "Not a fluid attachment kind: " + kind);
        };
        VoxelShape result = Shapes.empty();
        for (Box box : boxes) {
            result = Shapes.or(result, Block.box(
                    box.x0(), box.y0(), box.z0(),
                    box.x1(), box.y1(), box.z1()));
        }
        if (facing == Direction.NORTH) {
            return result;
        }
        return rotated(boxes, facing);
    }

    private static VoxelShape rotated(List<Box> boxes, Direction facing) {
        Function<Point, Point> transform = switch (facing) {
            case SOUTH -> point -> new Point(
                    16.0 - point.x(), point.y(), 16.0 - point.z());
            case EAST -> point -> new Point(
                    16.0 - point.z(), point.y(), point.x());
            case WEST -> point -> new Point(
                    point.z(), point.y(), 16.0 - point.x());
            case UP -> point -> new Point(
                    point.x(), point.z(), 16.0 - point.y());
            case DOWN -> point -> new Point(
                    point.x(), 16.0 - point.z(), point.y());
            case NORTH -> Function.identity();
        };
        VoxelShape result = Shapes.empty();
        for (Box box : boxes) {
            Point[] corners = {
                new Point(box.x0(), box.y0(), box.z0()),
                new Point(box.x0(), box.y0(), box.z1()),
                new Point(box.x0(), box.y1(), box.z0()),
                new Point(box.x0(), box.y1(), box.z1()),
                new Point(box.x1(), box.y0(), box.z0()),
                new Point(box.x1(), box.y0(), box.z1()),
                new Point(box.x1(), box.y1(), box.z0()),
                new Point(box.x1(), box.y1(), box.z1()),
            };
            double x0 = Double.MAX_VALUE;
            double y0 = Double.MAX_VALUE;
            double z0 = Double.MAX_VALUE;
            double x1 = -Double.MAX_VALUE;
            double y1 = -Double.MAX_VALUE;
            double z1 = -Double.MAX_VALUE;
            for (Point corner : corners) {
                Point point = transform.apply(corner);
                x0 = Math.min(x0, point.x());
                y0 = Math.min(y0, point.y());
                z0 = Math.min(z0, point.z());
                x1 = Math.max(x1, point.x());
                y1 = Math.max(y1, point.y());
                z1 = Math.max(z1, point.z());
            }
            result = Shapes.or(result, Block.box(x0, y0, z0, x1, y1, z1));
        }
        return result;
    }

    private record Box(
            double x0,
            double y0,
            double z0,
            double x1,
            double y1,
            double z1) {}

    private record Point(double x, double y, double z) {}
}
