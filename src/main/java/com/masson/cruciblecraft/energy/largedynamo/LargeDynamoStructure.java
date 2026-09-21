package com.masson.cruciblecraft.energy.largedynamo;

import com.masson.cruciblecraft.content.multiblock.CoilHosts;
import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * GT6 3x3x4 dynamo: two facing-axis 3x3 wall caps, 18 copper coils in the
 * 3x3x2 interior, far-cap center {@code ONLY_ENERGY_OUT}.
 */
public final class LargeDynamoStructure {
    public static final int COILS = 18;
    public static final int WALLS = 17;

    private LargeDynamoStructure() {}

    public static boolean interior(
            BlockPos controller, Direction facing, BlockPos pos) {
        SteamTurbineStructure.Cell cell = SteamTurbineStructure.cell(controller, facing);
        return switch (facing.getAxis()) {
            case X -> pos.getX() != cell.minX() && pos.getX() != cell.maxX();
            case Y -> pos.getY() != cell.minY() && pos.getY() != cell.maxY();
            case Z -> pos.getZ() != cell.minZ() && pos.getZ() != cell.maxZ();
        };
    }

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
        Block coil = CoilHosts.block(CoilHosts.COPPER);
        BlockPos energyOut = SteamTurbineStructure.energyOut(controller, facing);
        SteamTurbineStructure.Cell cell = SteamTurbineStructure.cell(controller, facing);
        int coils = 0;
        int walls = 0;
        for (int x = cell.minX(); x <= cell.maxX(); x++) {
            for (int y = cell.minY(); y <= cell.maxY(); y++) {
                for (int z = cell.minZ(); z <= cell.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(controller)) {
                        continue;
                    }
                    Block found = level.getBlockState(pos).getBlock();
                    if (interior(controller, facing, pos)) {
                        if (found != coil) {
                            return false;
                        }
                        coils++;
                    } else {
                        if (found != wall) {
                            return false;
                        }
                        walls++;
                    }
                }
            }
        }
        return coils == COILS && walls == WALLS && energyOut != null;
    }
}
