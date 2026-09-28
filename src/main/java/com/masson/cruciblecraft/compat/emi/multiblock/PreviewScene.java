package com.masson.cruciblecraft.compat.emi.multiblock;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.joml.Vector3f;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;

import net.minecraft.core.BlockPos;

/**
 * The cells one preview frame draws, keyed by their local block position.
 * Local positions are the JSON offsets as written, so the controller sits at
 * the origin facing north like the validator's default.
 */
public final class PreviewScene {
    private final Map<BlockPos, MultiblockProjectionGrid.Cell> cells;
    private final BlockPos min;
    private final BlockPos max;

    private PreviewScene(
            Map<BlockPos, MultiblockProjectionGrid.Cell> cells,
            BlockPos min,
            BlockPos max) {
        this.cells = cells;
        this.min = min;
        this.max = max;
    }

    /** {@code layer == null} keeps every local Y. */
    public static PreviewScene of(MultiblockProjectionGrid grid, Integer layer) {
        Map<BlockPos, MultiblockProjectionGrid.Cell> cells = new LinkedHashMap<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (MultiblockProjectionGrid.Cell cell : grid.cells()) {
            Offset offset = cell.offset();
            if (layer != null && offset.y() != layer) {
                continue;
            }
            cells.put(position(offset), cell);
            minX = Math.min(minX, offset.x());
            minY = Math.min(minY, offset.y());
            minZ = Math.min(minZ, offset.z());
            maxX = Math.max(maxX, offset.x());
            maxY = Math.max(maxY, offset.y());
            maxZ = Math.max(maxZ, offset.z());
        }
        if (cells.isEmpty()) {
            return new PreviewScene(Map.of(), BlockPos.ZERO, BlockPos.ZERO);
        }
        return new PreviewScene(
                Collections.unmodifiableMap(cells),
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ));
    }

    public static BlockPos position(Offset offset) {
        return new BlockPos(offset.x(), offset.y(), offset.z());
    }

    public Map<BlockPos, MultiblockProjectionGrid.Cell> cells() {
        return cells;
    }

    public Optional<MultiblockProjectionGrid.Cell> at(BlockPos pos) {
        return Optional.ofNullable(cells.get(pos));
    }

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    public Vector3f center() {
        return new Vector3f(
                (min.getX() + max.getX() + 1) / 2f,
                (min.getY() + max.getY() + 1) / 2f,
                (min.getZ() + max.getZ() + 1) / 2f);
    }

    /** Half the bounding box diagonal; never smaller than one block. */
    public float radius() {
        float x = max.getX() - min.getX() + 1;
        float y = max.getY() - min.getY() + 1;
        float z = max.getZ() - min.getZ() + 1;
        return Math.max((float) Math.sqrt(x * x + y * y + z * z) / 2f, 0.87f);
    }
}
