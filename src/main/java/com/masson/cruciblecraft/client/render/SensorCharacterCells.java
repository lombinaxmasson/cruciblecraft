package com.masson.cruciblecraft.client.render;

import net.minecraft.core.Direction;

/**
 * GT6 sensor character-pass geometry and texture coordinates.
 *
 * <p>This is the relevant part of {@code gt6_w}'s
 * {@code MultiTileEntitySensor.setBlockBounds2} plus
 * {@code GT6QuadBuilder.corners}. The 64×64 character images are intentionally
 * tiled 4×4. GT6 samples the image using the pass bounds, rather than mapping
 * the whole image to every 2×2 pixel box.
 */
final class SensorCharacterCells {
    static final int COUNT = 6;
    private static final float PIXEL = 1.0F / 16.0F;
    /** GT6 {@code CS.PX_OFFSET}. */
    private static final float OFFSET = 0.005F;
    private static final int[] EMIT_ORDER = {1, 0, 3, 2};

    private SensorCharacterCells() {}

    /**
     * Outward-face corners in the same canonical order emitted by
     * {@code gt6_w.GT6QuadBuilder}.
     */
    static float[][] worldCorners(Direction facing, int index) {
        float[][] corners = corners(facing, bounds(facing, index));
        float[][] result = new float[EMIT_ORDER.length][3];
        for (int vertex = 0; vertex < EMIT_ORDER.length; vertex++) {
            float[] corner = corners[EMIT_ORDER[vertex]];
            result[vertex] = new float[] {corner[0], corner[1], corner[2]};
        }
        return result;
    }

    /**
     * Per-vertex UVs in block-texture texel coordinates (0–16), matching
     * {@code GT6QuadBuilder.corners}. The caller normalizes them only when
     * passing them to {@code TextureAtlasSprite.getU/getV}.
     */
    static float[][] textureCoordinates(Direction facing, int index) {
        float[][] corners = corners(facing, bounds(facing, index));
        float[][] result = new float[EMIT_ORDER.length][2];
        for (int vertex = 0; vertex < EMIT_ORDER.length; vertex++) {
            float[] corner = corners[EMIT_ORDER[vertex]];
            result[vertex] = new float[] {corner[3], corner[4]};
        }
        return result;
    }

    static float[] normal(Direction facing) {
        return new float[] {
            facing.getStepX(), facing.getStepY(), facing.getStepZ()
        };
    }

    /**
     * Exact GT6 pass bounds. The front face is offset by
     * {@code CS.PX_OFFSET}; the other five bounds are the original
     * 2-pixel boxes.
     */
    private static float[] bounds(Direction facing, int index) {
        int ascendingPixel = 2 + 2 * index;
        int descendingPixel = 12 - 2 * index;
        float ascending = ascendingPixel * PIXEL;
        float ascendingEnd = (ascendingPixel + 2) * PIXEL;
        float descending = descendingPixel * PIXEL;
        float descendingEnd = (descendingPixel + 2) * PIXEL;
        float displayMin = 12.0F * PIXEL;
        float displayMax = 14.0F * PIXEL;
        float outwardLow = 2.0F * PIXEL + OFFSET;
        float outwardHigh = 14.0F * PIXEL - OFFSET;
        return switch (facing) {
            case NORTH -> new float[] {
                descending, displayMin, outwardHigh,
                descendingEnd, displayMax, 1.0F
            };
            case SOUTH -> new float[] {
                ascending, displayMin, 0.0F,
                ascendingEnd, displayMax, outwardLow
            };
            case WEST -> new float[] {
                outwardHigh, displayMin, ascending,
                1.0F, displayMax, ascendingEnd
            };
            case EAST -> new float[] {
                0.0F, displayMin, descending,
                outwardLow, displayMax, descendingEnd
            };
            case DOWN -> new float[] {
                ascending, outwardHigh, displayMin,
                ascendingEnd, 1.0F, displayMax
            };
            case UP -> new float[] {
                ascending, 0.0F, ascending,
                ascendingEnd, outwardLow, ascendingEnd
            };
        };
    }

    /**
     * Port of {@code GT6QuadBuilder.corners}. Each entry is
     * {@code {x, y, z, uTexels, vTexels}}.
     */
    private static float[][] corners(Direction facing, float[] box) {
        float x0 = box[0];
        float y0 = box[1];
        float z0 = box[2];
        float x1 = box[3];
        float y1 = box[4];
        float z1 = box[5];
        float u0x = x0 * 16.0F;
        float u1x = x1 * 16.0F;
        float u0z = z0 * 16.0F;
        float u1z = z1 * 16.0F;
        float v0y = (1.0F - y0) * 16.0F;
        float v1y = (1.0F - y1) * 16.0F;
        return switch (facing) {
            case DOWN -> new float[][] {
                {x0, y0, z0, u0x, u0z},
                {x0, y0, z1, u0x, u1z},
                {x1, y0, z1, u1x, u1z},
                {x1, y0, z0, u1x, u0z}
            };
            case UP -> new float[][] {
                {x0, y1, z1, u0x, u1z},
                {x0, y1, z0, u0x, u0z},
                {x1, y1, z0, u1x, u0z},
                {x1, y1, z1, u1x, u1z}
            };
            case NORTH -> new float[][] {
                {x1, y0, z0, u0x, v0y},
                {x1, y1, z0, u0x, v1y},
                {x0, y1, z0, u1x, v1y},
                {x0, y0, z0, u1x, v0y}
            };
            case SOUTH -> new float[][] {
                {x0, y0, z1, u0x, v0y},
                {x0, y1, z1, u0x, v1y},
                {x1, y1, z1, u1x, v1y},
                {x1, y0, z1, u1x, v0y}
            };
            case WEST -> new float[][] {
                {x0, y0, z0, u0z, v0y},
                {x0, y1, z0, u0z, v1y},
                {x0, y1, z1, u1z, v1y},
                {x0, y0, z1, u1z, v0y}
            };
            case EAST -> new float[][] {
                {x1, y0, z1, u0z, v0y},
                {x1, y1, z1, u0z, v1y},
                {x1, y1, z0, u1z, v1y},
                {x1, y0, z0, u1z, v0y}
            };
        };
    }
}
