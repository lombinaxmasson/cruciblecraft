package com.masson.cruciblecraft.content.multiblock;

import java.util.List;

import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;

public final class CokeOvenStructure {
    private CokeOvenStructure() {}

    public static BlockPos center(BlockPos controller, Direction facing) {
        var offset = CokeOvenStructureLayout.center(
                facing.getStepX(),
                facing.getStepZ());
        return controller.offset(offset.x(), offset.y(), offset.z());
    }

    public static BlockPos heatSource(BlockPos controller, Direction facing) {
        var offset = CokeOvenStructureLayout.heatSource(
                facing.getStepX(),
                facing.getStepZ());
        return controller.offset(offset.x(), offset.y(), offset.z());
    }

    public static boolean isValid(
            LevelReader level,
            BlockPos controller,
            Direction facing) {
        BlockPos center = center(controller, facing);
        if (!level.hasChunkAt(controller)
                || !level.hasChunkAt(center)
                || !level.getBlockState(controller).is(ModBlocks.COKE_OVEN.get())
                || !level.getBlockState(center).isAir()) {
            return false;
        }
        for (BlockPos offset : firebrickOffsets(facing)) {
            BlockPos firebrick = controller.offset(offset);
            if (!level.hasChunkAt(firebrick)
                    || !level.getBlockState(firebrick).is(ModBlocks.FIREBRICK.get())) {
                return false;
            }
        }
        return true;
    }

    public static List<BlockPos> firebrickOffsets(Direction facing) {
        return CokeOvenStructureLayout.firebrickOffsets(
                        facing.getStepX(),
                        facing.getStepZ())
                .stream()
                .map(offset -> new BlockPos(offset.x(), offset.y(), offset.z()))
                .toList();
    }
}
