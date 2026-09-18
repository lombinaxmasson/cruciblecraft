package com.masson.cruciblecraft.content.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * GT6 mass-storage face click map. Origin is the top-left of the front
 * texture; coordinates are in 1/16 block pixels.
 */
public final class MassStorageClicks {
    private static final float PX = 1.0F / 16.0F;

    private MassStorageClicks() {}

    public static float[] facingUv(Direction face, BlockPos pos, Vec3 hit) {
        float x = (float) (hit.x - pos.getX());
        float y = (float) (hit.y - pos.getY());
        float z = (float) (hit.z - pos.getZ());
        return switch (face) {
            case DOWN -> clamp(x, 1.0F - z);
            case UP -> clamp(x, z);
            case NORTH -> clamp(1.0F - x, 1.0F - y);
            case SOUTH -> clamp(x, 1.0F - y);
            case WEST -> clamp(z, 1.0F - y);
            case EAST -> clamp(1.0F - z, 1.0F - y);
        };
    }

    public static boolean onFace(float[] uv) {
        return uv[0] >= PX && uv[0] <= 1.0F - PX
                && uv[1] >= PX && uv[1] <= 1.0F - PX;
    }

    /**
     * GT6 {@code MultiTileEntityDrawerQuad}: {@code (x>0.5?1:0)|(y>0.5?2:0)}
     * with Y from the bottom of the front face.
     */
    public static int drawerCompartment(Direction face, BlockPos pos, Vec3 hit) {
        float[] uv = facingUv(face, pos, hit);
        float x = uv[0];
        float y = 1.0F - uv[1];
        return (x > 0.5F ? 1 : 0) | (y > 0.5F ? 2 : 0);
    }

    /**
     * Positive = extract that many stored items. {@code -1} dumps matching
     * items from the player's inventory. {@code 0} is insert / no button.
     */
    public static int amount(float[] uv) {
        int extract = 0;
        if (uv[1] >= 6 * PX && uv[1] <= 8 * PX) {
            if (uv[0] >= PX && uv[0] <= 3 * PX) {
                extract = 8;
            }
            if (uv[0] >= 1.0F - 3 * PX && uv[0] <= 1.0F - PX) {
                extract = 64;
            }
        }
        if (uv[1] >= 9 * PX && uv[1] <= 11 * PX) {
            if (uv[0] >= PX && uv[0] <= 3 * PX) {
                extract = 4;
            }
            if (uv[0] >= 1.0F - 3 * PX && uv[0] <= 1.0F - PX) {
                extract = 32;
            }
        }
        if (uv[1] >= 12 * PX && uv[1] <= 14 * PX) {
            if (uv[0] >= PX && uv[0] <= 3 * PX) {
                extract = 1;
            }
            if (uv[0] >= 1.0F - 3 * PX && uv[0] <= 1.0F - PX) {
                extract = 16;
            }
        }
        if (extract == 0 && uv[1] >= 6 * PX
                && uv[0] >= 4 * PX && uv[0] <= 1.0F - 4 * PX) {
            return -1;
        }
        return extract;
    }

    private static float[] clamp(float x, float y) {
        return new float[] {
                Math.min(0.99F, Math.max(0.0F, x)),
                Math.min(0.99F, Math.max(0.0F, y))};
    }
}
