package com.masson.cruciblecraft.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 {@code MultiTileEntityRock} positional AABB: seed {@code x ^ y ^ z}
 * then {@code PX_P[4+r]} / {@code PX_N[4+r]} / {@code PX_P[1+r]}.
 *
 * <p>Shape is derived from coordinates, not a 0–1023 blockstate. Encoding
 * it as a property on every catalog {@code RockBlock} exploded the
 * blockstate table at registry and OOMed {@code runClient}. The client
 * mesh consumes the same {@link RandomSource} sequence after
 * {@code Block#getSeed} so outline and quads stay aligned.
 */
public final class PebbleShape {
    public static final int MAX_PACKED = 1023;
    /**
     * 8×3×8 cube used for the item form
     * ({@code min=4}, {@code max=12}, {@code height=3}).
     */
    public static final int ITEM_PACKED = 2 << 8;

    private PebbleShape() {}

    public static long seed(BlockPos pos) {
        return pos.getX() ^ pos.getY() ^ pos.getZ();
    }

    public static int pack(RandomSource random) {
        return random.nextInt(4)
                | (random.nextInt(4) << 2)
                | (random.nextInt(4) << 4)
                | (random.nextInt(4) << 6)
                | (random.nextInt(4) << 8);
    }

    public static int pack(BlockPos pos) {
        RandomSource random = RandomSource.create();
        random.setSeed(seed(pos));
        return pack(random);
    }

    public static Pixels pixels(int packed) {
        int clamped = packed & MAX_PACKED;
        int minX = 4 + (clamped & 3);
        int minZ = 4 + ((clamped >> 2) & 3);
        int maxX = 12 - ((clamped >> 4) & 3);
        int maxZ = 12 - ((clamped >> 6) & 3);
        int maxY = 1 + ((clamped >> 8) & 3);
        return new Pixels(minX, minZ, maxX, maxY, maxZ);
    }

    public static VoxelShape shape(BlockPos pos) {
        return pixels(pack(pos)).voxel();
    }

    public record Pixels(int minX, int minZ, int maxX, int maxY, int maxZ) {
        public VoxelShape voxel() {
            return Block.box(minX, 0.0, minZ, maxX, maxY, maxZ);
        }
    }
}
