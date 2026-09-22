package com.masson.cruciblecraft.worldgen;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Positional shape for GT6 {@code MultiTileEntityStick}.
 *
 * <p>GT6 stores a random 14×2 pixel rectangle in the stick tile entity. The
 * block has no mutable gameplay state here, so the same coordinate seed keeps
 * the server outline and client mesh identical.
 */
public final class StickShape {
    public static final int MAX_PACKED = 117;
    public static final int ITEM_PACKED = pack(0, 1, 1);

    private StickShape() {}

    public static long seed(BlockPos pos) {
        return pos.getX() ^ pos.getY() ^ pos.getZ();
    }

    public static int pack(BlockPos pos) {
        Random random = new Random(seed(pos));
        return pack(random.nextInt(1_000_000), random);
    }

    public static int pack(RandomSource random) {
        return pack(random.nextInt(1_000_000), random);
    }

    private static int pack(int roll, Random random) {
        boolean alongZ = roll >= 500_000;
        int longMin;
        int shortMin;
        if (alongZ) {
            longMin = random.nextInt(3);
            shortMin = random.nextInt(15);
        } else {
            shortMin = random.nextInt(15);
            longMin = random.nextInt(3);
        }
        return pack(alongZ ? 1 : 0, longMin, shortMin);
    }

    private static int pack(int roll, RandomSource random) {
        boolean alongZ = roll >= 500_000;
        int longMin;
        int shortMin;
        if (alongZ) {
            longMin = random.nextInt(3);
            shortMin = random.nextInt(15);
        } else {
            shortMin = random.nextInt(15);
            longMin = random.nextInt(3);
        }
        return pack(alongZ ? 1 : 0, longMin, shortMin);
    }

    public static int pack(int axis, int longMin, int shortMin) {
        return (axis & 1) | ((longMin & 3) << 1) | ((shortMin & 15) << 3);
    }

    public static Pixels pixels(int packed) {
        int value = Math.max(0, Math.min(packed, MAX_PACKED));
        int axis = value & 1;
        int longMin = (value >> 1) & 3;
        int shortMin = (value >> 3) & 15;
        if (axis == 0) {
            return new Pixels(shortMin, longMin, shortMin + 2, longMin + 14, 2);
        }
        return new Pixels(longMin, shortMin, longMin + 14, shortMin + 2, 2);
    }

    public static VoxelShape shape(BlockPos pos) {
        return pixels(pack(pos)).voxel();
    }

    public record Pixels(int minX, int minZ, int maxX, int maxZ, int maxY) {
        public VoxelShape voxel() {
            return Block.box(minX, 0.0, minZ, maxX, maxY, maxZ);
        }
    }
}
