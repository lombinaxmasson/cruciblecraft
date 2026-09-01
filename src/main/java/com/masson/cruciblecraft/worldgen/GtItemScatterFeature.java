package com.masson.cruciblecraft.worldgen;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Bounded overworld scatter of catalog items. Placement matches the
 * surface-rock shape; the tag is {@code cruciblecraft:bath/mte_items}.
 * Empty-input worldgen drops are the item-acquisition path.
 */
public class GtItemScatterFeature extends Feature<ItemScatterConfiguration> {
    public GtItemScatterFeature() {
        super(ItemScatterConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<ItemScatterConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        ItemScatterConfiguration config = context.config();
        List<Item> items = level.registryAccess()
                .registryOrThrow(Registries.ITEM)
                .getTag(config.itemTag())
                .map(holders -> holders.stream()
                        .map(holder -> holder.value())
                        .toList())
                .orElse(List.of());
        if (items.isEmpty()) {
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
                Item chosen = items.get(random.nextInt(items.size()));
                ItemEntity entity = new ItemEntity(
                        level.getLevel(),
                        x + 0.5D,
                        y + 1.0D,
                        z + 0.5D,
                        new ItemStack(chosen));
                entity.setDefaultPickUpDelay();
                level.addFreshEntity(entity);
                placed = true;
            }
        }
        return placed;
    }
}
