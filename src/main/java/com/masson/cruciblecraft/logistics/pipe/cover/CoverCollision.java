package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.Map;

import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 {@code BOXES_COVERS}: 2-pixel slabs on each occupied face. Paper plates
 * skip collision ({@code mHasCollide = false}).
 */
public final class CoverCollision {
    private static final VoxelShape[] SLABS = {
        box(0, 0, 0, 16, 2, 16),
        box(0, 14, 0, 16, 16, 16),
        box(0, 0, 0, 16, 16, 2),
        box(0, 0, 14, 16, 16, 16),
        box(0, 0, 0, 2, 16, 16),
        box(14, 0, 0, 16, 16, 16)
    };

    private CoverCollision() {}

    public static VoxelShape union(VoxelShape base, BlockEntity blockEntity) {
        return union(base, coversOn(blockEntity));
    }

    public static Map<Direction, PipeCover> coversOn(BlockEntity blockEntity) {
        if (blockEntity instanceof MachineCoverHost host) {
            return host.covers().snapshot();
        }
        if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            return pipe.coverSnapshot();
        }
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            return pipe.coverSnapshot();
        }
        if (blockEntity instanceof RedstoneWireBlockEntity wire) {
            return wire.covers().snapshot();
        }
        if (blockEntity instanceof CableBlockEntity cable) {
            return cable.covers().snapshot();
        }
        return Map.of();
    }

    public static VoxelShape union(
            VoxelShape base, Map<Direction, PipeCover> covers) {
        if (covers == null || covers.isEmpty()) {
            return base;
        }
        VoxelShape shape = base;
        for (Map.Entry<Direction, PipeCover> entry : covers.entrySet()) {
            if (!collides(entry.getValue())) {
                continue;
            }
            shape = Shapes.or(shape, SLABS[entry.getKey().ordinal()]);
        }
        return shape;
    }

    public static boolean collides(PipeCover cover) {
        return cover != null && !CoverTextureCycle.paperPlate(cover);
    }

    private static VoxelShape box(
            int x0, int y0, int z0, int x1, int y1, int z1) {
        return net.minecraft.world.level.block.Block.box(x0, y0, z0, x1, y1, z1);
    }
}
