package com.masson.cruciblecraft.energy.largegasturbine;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * GT6 {@code MultiTileEntityLargeTurbine.checkStructure2} hatch bits on the
 * 3x3x4 hull. The controller cell is not a wall hatch.
 *
 * <p>Modes: {@code ONLY_ENERGY_OUT} far-wall center, {@code ONLY_ITEM_FLUID} /
 * {@code ONLY_ITEM_FLUID_IN} on the frontal 3x3, {@code ONLY_ITEM_FLUID_OUT} on
 * other bottom walls, {@code NOTHING} elsewhere.
 */
public final class LargeTurbineHatches {
    public static final int WALLS = 35;
    public static final int ENERGY_OUT = 1;
    public static final int ITEM_FLUID = 3;
    public static final int ITEM_FLUID_IN = 5;
    public static final int ITEM_FLUID_OUT = 9;
    public static final int NOTHING = 17;

    private LargeTurbineHatches() {}

    public record Hatch(BlockPos pos, LargeTurbineHatchRole role) {
        public Hatch {
            pos = pos.immutable();
            if (role == null) {
                throw new IllegalArgumentException("Large turbine hatch role");
            }
        }
    }

    public record HatchCounts(
            int energyOut,
            int itemFluid,
            int itemFluidIn,
            int itemFluidOut,
            int nothing) {
        public boolean matchesGt6Horizontal() {
            return energyOut == ENERGY_OUT
                    && itemFluid == ITEM_FLUID
                    && itemFluidIn == ITEM_FLUID_IN
                    && itemFluidOut == ITEM_FLUID_OUT
                    && nothing == NOTHING
                    && energyOut + itemFluid + itemFluidIn + itemFluidOut + nothing
                            == WALLS;
        }
    }

    public static List<Hatch> hatches(BlockPos controller, Direction facing) {
        SteamTurbineStructure.Cell cell = SteamTurbineStructure.cell(controller, facing);
        BlockPos energyOut = SteamTurbineStructure.energyOut(controller, facing);
        List<Hatch> hatches = new ArrayList<>(WALLS);
        for (int x = cell.minX(); x <= cell.maxX(); x++) {
            for (int y = cell.minY(); y <= cell.maxY(); y++) {
                for (int z = cell.minZ(); z <= cell.maxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(controller)) {
                        continue;
                    }
                    hatches.add(new Hatch(
                            pos, role(controller, facing, cell, energyOut, pos)));
                }
            }
        }
        if (hatches.size() != WALLS) {
            throw new IllegalStateException(
                    "Large turbine wall count drifted: " + hatches.size());
        }
        return List.copyOf(hatches);
    }

    public static HatchCounts counts(BlockPos controller, Direction facing) {
        int energyOut = 0;
        int itemFluid = 0;
        int itemFluidIn = 0;
        int itemFluidOut = 0;
        int nothing = 0;
        for (Hatch hatch : hatches(controller, facing)) {
            switch (hatch.role()) {
                case ENERGY_OUT -> energyOut++;
                case ITEM_FLUID -> itemFluid++;
                case ITEM_FLUID_IN -> itemFluidIn++;
                case ITEM_FLUID_OUT -> itemFluidOut++;
                case NOTHING -> nothing++;
            }
        }
        return new HatchCounts(
                energyOut, itemFluid, itemFluidIn, itemFluidOut, nothing);
    }

    public static LargeTurbineHatchRole role(
            BlockPos controller,
            Direction facing,
            BlockPos pos) {
        return role(
                controller,
                facing,
                SteamTurbineStructure.cell(controller, facing),
                SteamTurbineStructure.energyOut(controller, facing),
                pos);
    }

    private static LargeTurbineHatchRole role(
            BlockPos controller,
            Direction facing,
            SteamTurbineStructure.Cell cell,
            BlockPos energyOut,
            BlockPos pos) {
        if (pos.equals(energyOut)) {
            return LargeTurbineHatchRole.ENERGY_OUT;
        }
        boolean bottom = pos.getY() == cell.minY();
        if (SteamTurbineStructure.frontal(controller, facing, pos)) {
            return bottom
                    ? LargeTurbineHatchRole.ITEM_FLUID
                    : LargeTurbineHatchRole.ITEM_FLUID_IN;
        }
        return bottom
                ? LargeTurbineHatchRole.ITEM_FLUID_OUT
                : LargeTurbineHatchRole.NOTHING;
    }
}
