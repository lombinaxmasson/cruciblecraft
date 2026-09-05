package com.masson.cruciblecraft.logistics.core;

/** SOURCE_BACKED 5x5x5 Logistics Core geometry from GT6 checkStructure2. */
public final class LogisticsCoreGeometry {
    public static final int HALF = 2;
    public static final int SIZE = HALF * 2 + 1;
    public static final int MAX_STORAGE_CPU = 108;
    public static final int SYNC_INTERVAL = 20;
    public static final long ENERGY_INPUT_MIN = 256L;
    public static final long ENERGY_INPUT_MAX = 1024L;
    public static final int DUMP_MAX_MOVE = 64;

    public enum CellKind {
        INNER,
        VENT,
        WALL
    }

    private LogisticsCoreGeometry() {}

    public static CellKind cell(int i, int j, int k) {
        int radius = i * i + j * j + k * k;
        if (radius < 4) {
            return CellKind.INNER;
        }
        if (radius > 6) {
            return CellKind.WALL;
        }
        return CellKind.VENT;
    }

    public static int chebyshev(int dx, int dy, int dz) {
        return Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
    }

    public static long operateThreshold(int logic, int conversion) {
        return 128L + (long) logic * 64L * (long) conversion;
    }

    public static long capacity(int logic, int conversion) {
        return 128L + (long) logic * 256L * (long) conversion;
    }

    public static long idleDrain(
            int logic, int control, int storage, int conversion) {
        return 20L + logic + control + storage + conversion;
    }
}
