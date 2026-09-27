package com.masson.cruciblecraft.content.block;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.machine.component.CrucibleInteriorGeometry;

import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Outline of the formed hull inside one structure block. The mesh is GT6's
 * half-block walls plus the floor at {@code y = 1.125}; a full-cube outline
 * sticks through that shell.
 */
public final class LargeCrucibleHullShape {
    private static final double WALL = 0.5;
    static final Box[] HULL = {
            new Box(-1.0, 0.0, -1.0, -WALL, 3.0, 2.0),
            new Box(-1.0, 0.0, -1.0, 2.0, 3.0, -WALL),
            new Box(2.0 - WALL, 0.0, -1.0, 2.0, 3.0, 2.0),
            new Box(-1.0, 0.0, 2.0 - WALL, 2.0, 3.0, 2.0),
            new Box(-1.0, 0.0, -1.0, 2.0, CrucibleInteriorGeometry.LARGE_BASE_Y, 2.0)
    };
    private static final VoxelShape[] CACHE = new VoxelShape[27];

    private LargeCrucibleHullShape() {}

    public static VoxelShape shape(int dx, int dy, int dz) {
        if (dx < -1 || dx > 1 || dy < 0 || dy > 2 || dz < -1 || dz > 1) {
            return Shapes.block();
        }
        int index = (dx + 1) + dy * 3 + (dz + 1) * 9;
        VoxelShape cached = CACHE[index];
        if (cached != null) {
            return cached;
        }
        List<Box> boxes = localBoxes(dx, dy, dz);
        VoxelShape shape = Shapes.empty();
        for (Box box : boxes) {
            shape = Shapes.or(shape, Shapes.box(box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1()));
        }
        if (shape.isEmpty()) {
            shape = Shapes.block();
        }
        CACHE[index] = shape;
        return shape;
    }

    static List<Box> localBoxes(int dx, int dy, int dz) {
        double blockX1 = dx + 1.0;
        double blockY1 = dy + 1.0;
        double blockZ1 = dz + 1.0;
        List<Box> boxes = new ArrayList<>();
        for (Box hull : HULL) {
            double x0 = Math.max(hull.x0(), dx);
            double y0 = Math.max(hull.y0(), dy);
            double z0 = Math.max(hull.z0(), dz);
            double x1 = Math.min(hull.x1(), blockX1);
            double y1 = Math.min(hull.y1(), blockY1);
            double z1 = Math.min(hull.z1(), blockZ1);
            if (x1 - x0 > 1.0e-4 && y1 - y0 > 1.0e-4 && z1 - z0 > 1.0e-4) {
                boxes.add(new Box(x0 - dx, y0 - dy, z0 - dz, x1 - dx, y1 - dy, z1 - dz));
            }
        }
        return boxes;
    }

    record Box(double x0, double y0, double z0, double x1, double y1, double z1) {}
}
