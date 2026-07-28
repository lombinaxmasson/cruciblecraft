package com.masson.cruciblecraft.worldgen;

/** Pure deterministic helpers kept independent of Minecraft for unit tests. */
public final class LargeVeinLayout {
    private static final int ANCHOR_PHASE = 0x13579BDF;
    private static final int CHANCE_PHASE = 0x2468ACE0;
    private static final int VEIN_PHASE = 0x51ED270B;

    private LargeVeinLayout() {}

    public static Anchor anchor(long worldSeed, int regionX, int regionZ, int regionSize, int salt) {
        long value = hash(worldSeed, regionX, salt ^ ANCHOR_PHASE, regionZ);
        int x = regionX * regionSize + Math.floorMod((int) value, regionSize);
        int z = regionZ * regionSize + Math.floorMod((int) (value >>> 32), regionSize);
        return new Anchor(x, z);
    }

    public static double generationRoll(long worldSeed, int regionX, int regionZ, int salt) {
        return unit(hash(worldSeed, regionX, salt ^ CHANCE_PHASE, regionZ));
    }

    public static long veinSeed(long worldSeed, Anchor anchor, int salt) {
        return hash(worldSeed, anchor.x(), salt ^ VEIN_PHASE, anchor.z());
    }

    public static int centerY(long veinSeed, int minY, int maxY) {
        return minY + Math.floorMod((int) veinSeed, maxY - minY + 1);
    }

    public static Center center(
            long worldSeed,
            int regionX,
            int regionZ,
            int regionSize,
            int salt,
            int minY,
            int maxY) {
        Anchor anchor = anchor(worldSeed, regionX, regionZ, regionSize, salt);
        int y = centerY(veinSeed(worldSeed, anchor, salt), minY, maxY);
        return new Center(anchor.x() * 16 + 8, y, anchor.z() * 16 + 8);
    }

    public static String role(double vertical, double radial, long cell) {
        if (radial > 0.68 || (Math.abs(vertical) < 0.45 && unit(mix(cell)) < 0.10)) {
            return "spread";
        }
        if (Math.abs(vertical) < 0.22 && unit(cell) < 0.72) {
            return "between";
        }
        return vertical >= 0.0 ? "top" : "bottom";
    }

    static double unit(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    static long hash(long seed, int x, int salt, int z) {
        return mix(seed ^ ((long) x * 0x9E3779B97F4A7C15L)
                ^ ((long) z * 0xC2B2AE3D27D4EB4FL) ^ salt);
    }

    static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    public record Anchor(int x, int z) {}

    public record Center(int x, int y, int z) {}
}
