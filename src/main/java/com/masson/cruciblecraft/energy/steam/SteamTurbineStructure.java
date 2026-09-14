package com.masson.cruciblecraft.energy.steam;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.masson.cruciblecraft.registry.ModBlocks;

/** GT6 3x3x4 large steam turbine: 35 walls, controller centered on the facing 3x3. */
public final class SteamTurbineStructure {
    private SteamTurbineStructure() {}

    public static boolean check(
            Level level,
            BlockPos controller,
            Direction facing,
            ResourceLocation wallId) {
        if (wallId == null) {
            return false;
        }
        var holder = ModBlocks.mteInPlaceBlocksById().get(wallId);
        if (holder == null) {
            return false;
        }
        Block wall = holder.get();
        int minX = controller.getX() - (facing == Direction.WEST
                ? 0 : facing == Direction.EAST ? 3 : 1);
        int minY = controller.getY() - (facing == Direction.DOWN
                ? 0 : facing == Direction.UP ? 3 : 1);
        int minZ = controller.getZ() - (facing == Direction.NORTH
                ? 0 : facing == Direction.SOUTH ? 3 : 1);
        int maxX = controller.getX() + (facing == Direction.EAST
                ? 0 : facing == Direction.WEST ? 3 : 1);
        int maxY = controller.getY() + (facing == Direction.UP
                ? 0 : facing == Direction.DOWN ? 3 : 1);
        int maxZ = controller.getZ() + (facing == Direction.SOUTH
                ? 0 : facing == Direction.NORTH ? 3 : 1);
        int walls = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(controller)) {
                        continue;
                    }
                    if (!level.getBlockState(pos).is(wall)) {
                        return false;
                    }
                    walls++;
                }
            }
        }
        return walls == 35;
    }
}
