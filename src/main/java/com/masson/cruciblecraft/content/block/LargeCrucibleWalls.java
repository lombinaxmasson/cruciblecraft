package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortHost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT6 wall parts: energy on the bottom layer, pour in the middle, items/fluids
 * on top. Walls keep {@code MteInPlaceBlockEntity} and forward to the controller.
 */
public final class LargeCrucibleWalls {
    private LargeCrucibleWalls() {}

    public static LargeCrucibleBlockEntity controllerAt(
            BlockGetter level, BlockPos wall) {
        if (level == null || !LargeCrucibleHosts.isWall(level.getBlockState(wall).getBlock())) {
            return null;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos candidate = wall.offset(-dx, -dy, -dz);
                    if (!(level.getBlockEntity(candidate)
                            instanceof LargeCrucibleBlockEntity crucible)
                            || !crucible.structureValid()
                            || crucible.pluginQuarantined()) {
                        continue;
                    }
                    if (!matchesController(level, wall, crucible)) {
                        continue;
                    }
                    return crucible;
                }
            }
        }
        return null;
    }

    public static boolean forwardsEnergy(MteInPlaceBlockEntity wall) {
        return layer(wall) == 0 && controller(wall) != null;
    }

    public static boolean forwardsItems(MteInPlaceBlockEntity wall) {
        return layer(wall) == 2 && controller(wall) != null;
    }

    public static IItemHandler items(MteInPlaceBlockEntity wall) {
        if (!forwardsItems(wall)) {
            return null;
        }
        MultiblockPortHost host = controller(wall);
        return host == null ? null : host.inventory();
    }

    public static IFluidHandler fluids(MteInPlaceBlockEntity wall) {
        if (!forwardsItems(wall)) {
            return null;
        }
        LargeCrucibleBlockEntity host = controller(wall);
        return host == null ? null : host.process().fluids();
    }

    public static long insertEnergy(
            MteInPlaceBlockEntity wall,
            com.masson.cruciblecraft.api.energy.EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        LargeCrucibleBlockEntity host = controller(wall);
        if (host == null || layer(wall) != 0) {
            return 0L;
        }
        return host.insert(type, size, amount, side, simulate);
    }

    private static boolean matchesController(
            BlockGetter level, BlockPos wall, LargeCrucibleBlockEntity crucible) {
        BlockEntity wallEntity = level.getBlockEntity(wall);
        if (!(wallEntity instanceof MteInPlaceBlockEntity inplace)
                || !LargeCrucibleHosts.isWall(inplace.spec())) {
            return false;
        }
        String wallMaterial = LargeCrucibleHosts.materialId(inplace.spec());
        return wallMaterial.equals(crucible.process().casing().materialId())
                && layer(crucible.getBlockPos(), wall) >= 0;
    }

    private static LargeCrucibleBlockEntity controller(MteInPlaceBlockEntity wall) {
        return controllerAt(wall.getLevel(), wall.getBlockPos());
    }

    private static int layer(MteInPlaceBlockEntity wall) {
        LargeCrucibleBlockEntity host = controller(wall);
        if (host == null) {
            return -1;
        }
        return layer(host.getBlockPos(), wall.getBlockPos());
    }

    private static int layer(BlockPos controller, BlockPos wall) {
        int dy = wall.getY() - controller.getY();
        return dy >= 0 && dy <= 2 ? dy : -1;
    }

    public static ItemStack insertItem(
            IItemHandler handler, int slot, ItemStack stack, boolean simulate) {
        return handler == null ? stack : handler.insertItem(slot, stack, simulate);
    }

    public static FluidStack emptyFluid() {
        return FluidStack.EMPTY;
    }
}
