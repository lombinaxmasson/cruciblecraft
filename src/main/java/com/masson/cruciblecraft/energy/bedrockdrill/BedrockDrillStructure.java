package com.masson.cruciblecraft.energy.bedrockdrill;

import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * GT6 17999 structure: 3x3 bedrock rocks at y-5, 3x3 heads at y-4, dense
 * titanium walls filling the 3x3x4 tower, controller at the top center.
 */
public final class BedrockDrillStructure {
    public static final ResourceLocation WALL_ID = ResourceLocation.fromNamespaceAndPath(
            "cruciblecraft", "multiblock/dense_titanium_wall");
    public static final long RU_PER_TICK = 1_024L;
    public static final int LUBE_PER_TICK = 100;
    public static final int TANK_CAPACITY = 16_000;

    private BedrockDrillStructure() {}

    public static boolean check(Level level, BlockPos controller) {
        if (!ModBlocks.hasRockBlock("bedrock")) {
            return false;
        }
        Block head = ModBlocks.BEDROCK_DRILL_HEAD.get();
        var wallHolder = ModBlocks.mteInPlaceBlocksById().get(WALL_ID);
        if (wallHolder == null) {
            return false;
        }
        Block wall = wallHolder.get();
        Block rock = ModBlocks.rockBlock("bedrock").get();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos ore = controller.offset(dx, -5, dz);
                if (level.getBlockState(ore).getBlock() != rock) {
                    return false;
                }
                if (level.getBlockState(controller.offset(dx, -4, dz)).getBlock()
                        != head) {
                    return false;
                }
                for (int dy = -3; dy <= 0; dy++) {
                    if (dy == 0 && dx == 0 && dz == 0) {
                        continue;
                    }
                    if (level.getBlockState(controller.offset(dx, dy, dz)).getBlock()
                            != wall) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
