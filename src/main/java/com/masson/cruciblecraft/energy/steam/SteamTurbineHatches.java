package com.masson.cruciblecraft.energy.steam;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * GT6 {@code MultiTileEntityLargeTurbineSteam.checkStructure2} hatch bits on
 * the 3x3x4 hull. The controller cell is not a wall hatch.
 *
 * <p>Modes: {@code ONLY_ENERGY_OUT} far-wall center, {@code ONLY_FLUID} /
 * {@code ONLY_FLUID_IN} on the frontal 3x3, {@code ONLY_FLUID_OUT} on other
 * bottom walls, {@code NOTHING} elsewhere.
 */
public final class SteamTurbineHatches {
    public static final int WALLS = 35;
    public static final int ENERGY_OUT = 1;
    public static final int FLUID = 3;
    public static final int FLUID_IN = 5;
    public static final int FLUID_OUT = 9;
    public static final int NOTHING = 17;

    private SteamTurbineHatches() {}

    public record Hatch(BlockPos pos, SteamTurbineHatchRole role) {
        public Hatch {
            pos = pos.immutable();
            if (role == null) {
                throw new IllegalArgumentException("Steam turbine hatch role");
            }
        }
    }

    public record HatchCounts(
            int energyOut,
            int fluid,
            int fluidIn,
            int fluidOut,
            int nothing) {
        public boolean matchesGt6Horizontal() {
            return energyOut == ENERGY_OUT
                    && fluid == FLUID
                    && fluidIn == FLUID_IN
                    && fluidOut == FLUID_OUT
                    && nothing == NOTHING
                    && energyOut + fluid + fluidIn + fluidOut + nothing == WALLS;
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
                    "Steam turbine wall count drifted: " + hatches.size());
        }
        return List.copyOf(hatches);
    }

    public static HatchCounts counts(BlockPos controller, Direction facing) {
        int energyOut = 0;
        int fluid = 0;
        int fluidIn = 0;
        int fluidOut = 0;
        int nothing = 0;
        for (Hatch hatch : hatches(controller, facing)) {
            switch (hatch.role()) {
                case ENERGY_OUT -> energyOut++;
                case FLUID -> fluid++;
                case FLUID_IN -> fluidIn++;
                case FLUID_OUT -> fluidOut++;
                case NOTHING -> nothing++;
            }
        }
        return new HatchCounts(energyOut, fluid, fluidIn, fluidOut, nothing);
    }

    public static SteamTurbineHatchRole role(
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

    private static SteamTurbineHatchRole role(
            BlockPos controller,
            Direction facing,
            SteamTurbineStructure.Cell cell,
            BlockPos energyOut,
            BlockPos pos) {
        if (pos.equals(energyOut)) {
            return SteamTurbineHatchRole.ENERGY_OUT;
        }
        boolean bottom = pos.getY() == cell.minY();
        if (SteamTurbineStructure.frontal(controller, facing, pos)) {
            return bottom
                    ? SteamTurbineHatchRole.FLUID
                    : SteamTurbineHatchRole.FLUID_IN;
        }
        return bottom
                ? SteamTurbineHatchRole.FLUID_OUT
                : SteamTurbineHatchRole.NOTHING;
    }
}
