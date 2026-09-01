package com.masson.cruciblecraft.worldgen;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Bounded overworld scatter of catalog GT stone blocks. Placement is the
 * surface-rock shape; the tag is {@code cruciblecraft:gt_stones}, not
 * {@code c:rocks}. Empty-input worldgen drops are the stone acquisition path.
 */
public class GtStoneScatterFeature extends Feature<SurfaceRockConfiguration> {
    public GtStoneScatterFeature() {
        super(SurfaceRockConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<SurfaceRockConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        SurfaceRockConfiguration config = context.config();
        List<Block> stones = level.registryAccess()
                .registryOrThrow(Registries.BLOCK)
                .getTag(config.rockTag())
                .map(holders -> holders.stream()
                        .map(holder -> holder.value())
                        .toList())
                .orElse(List.of());
        if (stones.isEmpty()) {
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
                BlockState stone = stones.get(random.nextInt(stones.size()))
                        .defaultBlockState();
                level.setBlock(cursor, stone, Block.UPDATE_ALL);
                placed = true;
            }
        }
        return placed;
    }
}
