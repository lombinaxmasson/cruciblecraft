package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.worldgen.PebbleShape;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6-style small surface rock for one material's {@code rock} form
 * ({@code OP.rockGt}). Positional AABB matching {@code MultiTileEntityRock},
 * tinted per material on the client.
 */
public final class RockBlock extends Block {
    private final String materialId;

    public RockBlock(String materialId, Properties properties) {
        super(properties);
        this.materialId = materialId;
    }

    public String materialId() {
        return materialId;
    }

    @Override
    protected long getSeed(BlockState state, BlockPos pos) {
        return PebbleShape.seed(pos);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return PebbleShape.shape(pos);
    }
}
