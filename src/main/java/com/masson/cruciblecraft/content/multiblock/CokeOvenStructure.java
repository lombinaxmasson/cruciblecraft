package com.masson.cruciblecraft.content.multiblock;

import java.util.List;

import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

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
            BlockGetter level,
            BlockPos controller,
            Direction facing) {
        if (!level.getBlockState(controller).is(ModBlocks.COKE_OVEN.get())
                || !level.getBlockState(center(controller, facing)).isAir()) {
            return false;
        }
        for (BlockPos offset : firebrickOffsets(facing)) {
            if (!level.getBlockState(controller.offset(offset)).is(ModBlocks.FIREBRICK.get())) {
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
