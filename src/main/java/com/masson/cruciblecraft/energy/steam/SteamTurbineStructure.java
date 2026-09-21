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

    public record Cell(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    public static Cell cell(BlockPos controller, Direction facing) {
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
        return new Cell(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** GT6 {@code getOffset*N(mFacing, 3)}: far-wall center, RU emit tile. */
    public static BlockPos energyOut(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite(), 3);
    }

    /**
     * Same coordinate along the facing axis as the controller: the frontal 3x3.
     */
    public static boolean frontal(
            BlockPos controller, Direction facing, BlockPos pos) {
        return switch (facing.getAxis()) {
            case X -> pos.getX() == controller.getX();
            case Y -> pos.getY() == controller.getY();
            case Z -> pos.getZ() == controller.getZ();
        };
    }

    /**
     * GT6 {@code worldObj.blockExists} on the AABB corners. A 3x3x4 hull
     * cannot span three chunks, so the two corners cover the volume.
     */
    public static boolean aabbLoaded(
            Level level, BlockPos controller, Direction facing) {
        Cell cell = cell(controller, facing);
        return level.hasChunkAt(new BlockPos(cell.minX(), cell.minY(), cell.minZ()))
                && level.hasChunkAt(new BlockPos(cell.maxX(), cell.maxY(), cell.maxZ()));
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
        Cell cell = cell(controller, facing);
        int walls = 0;
        for (int x = cell.minX(); x <= cell.maxX(); x++) {
            for (int y = cell.minY(); y <= cell.maxY(); y++) {
                for (int z = cell.minZ(); z <= cell.maxZ(); z++) {
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
