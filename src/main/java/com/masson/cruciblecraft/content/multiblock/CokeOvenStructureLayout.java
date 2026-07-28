package com.masson.cruciblecraft.content.multiblock;

import java.util.ArrayList;
import java.util.List;

public final class CokeOvenStructureLayout {
    private CokeOvenStructureLayout() {}

    public static Offset center(int facingX, int facingZ) {
        validateHorizontal(facingX, facingZ);
        return new Offset(-facingX, 0, -facingZ);
    }

    public static Offset heatSource(int facingX, int facingZ) {
        Offset center = center(facingX, facingZ);
        return new Offset(center.x(), -2, center.z());
    }

    public static List<Offset> firebrickOffsets(int facingX, int facingZ) {
        Offset center = center(facingX, facingZ);
        List<Offset> result = new ArrayList<>(25);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    Offset target = new Offset(center.x() + x, y, center.z() + z);
                    if (!target.equals(Offset.ZERO)) {
                        result.add(target);
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static void validateHorizontal(int facingX, int facingZ) {
        if (Math.abs(facingX) + Math.abs(facingZ) != 1) {
            throw new IllegalArgumentException("Facing must be a horizontal unit vector");
        }
    }

    public record Offset(int x, int y, int z) {
        public static final Offset ZERO = new Offset(0, 0, 0);
    }
}
