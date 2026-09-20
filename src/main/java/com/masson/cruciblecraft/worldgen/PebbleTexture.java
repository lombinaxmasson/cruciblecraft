package com.masson.cruciblecraft.worldgen;

import com.masson.cruciblecraft.content.block.GtStoneBlock;
import com.masson.cruciblecraft.content.block.StoneLayerRockOreBlock;
import com.masson.cruciblecraft.content.block.StoneLayerStoneBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityRock#getRenderPasses} contact copy: BlockStones
 * and stone cubes keep their own texture; sand/gravel/netherrack map to the
 * vanilla stand-ins; grass and dirt stay default stone.
 */
public final class PebbleTexture {
    private PebbleTexture() {}

    public static BlockState textureSource(BlockState below, BlockState pebble) {
        Block block = below.getBlock();
        if (block instanceof StoneLayerStoneBlock
                || block instanceof StoneLayerRockOreBlock
                || block instanceof GtStoneBlock) {
            return below;
        }
        if (block == Blocks.SAND
                || block == Blocks.RED_SAND
                || block == Blocks.SANDSTONE
                || block == Blocks.RED_SANDSTONE
                || block == Blocks.CUT_SANDSTONE
                || block == Blocks.CHISELED_SANDSTONE
                || block == Blocks.SMOOTH_SANDSTONE) {
            return Blocks.SANDSTONE.defaultBlockState();
        }
        if (block == Blocks.COBBLESTONE || block == Blocks.GRAVEL) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }
        if (block == Blocks.NETHERRACK
                || block == Blocks.NETHER_BRICKS
                || block == Blocks.SOUL_SAND) {
            return Blocks.NETHERRACK.defaultBlockState();
        }
        if (block == Blocks.END_STONE) {
            return Blocks.END_STONE.defaultBlockState();
        }
        if (block == Blocks.SNOW || block == Blocks.SNOW_BLOCK) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }
        if (block == Blocks.OBSIDIAN) {
            return Blocks.OBSIDIAN.defaultBlockState();
        }
        if (block == Blocks.STONE
                || block == Blocks.DEEPSLATE
                || block == Blocks.GRANITE
                || block == Blocks.DIORITE
                || block == Blocks.ANDESITE) {
            return below;
        }
        if (pebble.hasProperty(SurfaceRockAppearance.PROPERTY)) {
            return switch (pebble.getValue(SurfaceRockAppearance.PROPERTY)) {
                case SANDSTONE -> Blocks.SANDSTONE.defaultBlockState();
                case COBBLE -> Blocks.COBBLESTONE.defaultBlockState();
                case STONE -> Blocks.STONE.defaultBlockState();
            };
        }
        return Blocks.STONE.defaultBlockState();
    }
}
