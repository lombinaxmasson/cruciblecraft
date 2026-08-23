package com.masson.cruciblecraft.worldgen;

import java.util.List;

import com.masson.cruciblecraft.content.block.RockBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.tags.BlockTags;

/**
 * GT6 {@code WorldgenRocks} surface scatter: at each surface position, one
 * rock per {@code rarity} rolls, placed on top of dirt/sand contact blocks.
 * The rock block is drawn deterministically from the {@code c:rocks} block
 * tag with the chunk-scoped feature random. Purely positional — it never
 * consults the underground vein map, so the future vein-indicator feature
 * stays a separate card (4.5 planning doc section P4 seam).
 */
public class SurfaceRockFeature extends Feature<SurfaceRockConfiguration> {
    public SurfaceRockFeature() {
        super(SurfaceRockConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<SurfaceRockConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        SurfaceRockConfiguration config = context.config();
        List<Block> rocks = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BLOCK)
                .getTag(config.rockTag())
                .map(holders -> holders.stream()
                        .map(holder -> holder.value())
                        .filter(block -> block instanceof RockBlock)
                        .toList())
                .orElse(List.of());
        if (rocks.isEmpty()) {
            return false;
        }
        BlockPos origin = context.origin();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                if (random.nextInt(config.rarity()) != 0) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int y = level.getHeight(
                        Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                cursor.set(x, y, z);
                BlockState contact = level.getBlockState(cursor);
                if (!contact.is(BlockTags.DIRT) && !contact.is(BlockTags.SAND)) {
                    continue;
                }
                cursor.set(x, y + 1, z);
                if (!level.getBlockState(cursor).isAir()) {
                    continue;
                }
                BlockState rock = rocks.get(random.nextInt(rocks.size()))
                        .defaultBlockState();
                level.setBlock(cursor, rock, Block.UPDATE_ALL);
                placed = true;
            }
        }
        return placed;
    }
}
