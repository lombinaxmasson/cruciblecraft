package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 spike geometry: floor wall spikes (meta 0/8) or omni bars (meta 6/7/14/15).
 */
public final class GtBlockObjectSpikeBlock extends Block {
    private static final VoxelShape WALL = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(4, 1, 4, 5, 15, 5),
            Block.box(4, 1, 11, 5, 15, 12),
            Block.box(11, 1, 4, 12, 15, 5),
            Block.box(11, 1, 11, 12, 15, 12),
            Block.box(3, 1, 3, 6, 11, 6),
            Block.box(3, 1, 10, 6, 11, 13),
            Block.box(10, 1, 3, 13, 11, 6),
            Block.box(10, 1, 10, 13, 11, 13),
            Block.box(2, 1, 2, 7, 7, 7),
            Block.box(2, 1, 9, 7, 7, 14),
            Block.box(9, 1, 2, 14, 7, 7),
            Block.box(9, 1, 9, 14, 7, 14));
    private static final VoxelShape OMNI = Shapes.or(
            Block.box(4, 4, 4, 12, 12, 12),
            Block.box(0, 5, 5, 16, 6, 6),
            Block.box(0, 5, 10, 16, 6, 11),
            Block.box(0, 10, 5, 16, 11, 6),
            Block.box(0, 10, 10, 16, 11, 11),
            Block.box(5, 0, 5, 6, 16, 6),
            Block.box(5, 0, 10, 6, 16, 11),
            Block.box(10, 0, 5, 11, 16, 6),
            Block.box(10, 0, 10, 11, 16, 11),
            Block.box(5, 5, 0, 6, 6, 16),
            Block.box(5, 10, 0, 6, 11, 16),
            Block.box(10, 5, 0, 11, 6, 16),
            Block.box(10, 10, 0, 11, 11, 16));

    private final GtBlockObjectCatalog.Variant variant;

    public GtBlockObjectSpikeBlock(
            GtBlockObjectCatalog.Variant variant, Properties properties) {
        super(properties);
        this.variant = variant;
    }

    public GtBlockObjectCatalog.Variant variant() {
        return variant;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return variant.omniSpike() ? OMNI : WALL;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        hurt(level, entity);
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        hurt(level, entity);
        super.stepOn(level, pos, state, entity);
    }

    private static void hurt(Level level, Entity entity) {
        if (!entity.isSteppingCarefully() && entity instanceof LivingEntity living) {
            living.hurt(level.damageSources().cactus(), 1.0F);
        }
    }
}
