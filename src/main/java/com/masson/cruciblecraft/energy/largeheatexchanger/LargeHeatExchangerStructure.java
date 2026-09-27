package com.masson.cruciblecraft.energy.largeheatexchanger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.masson.cruciblecraft.registry.ModBlocks;

/**
 * GT6 {@code checkStructure2}: 3x3x2 world-axis, controller at bottom center.
 * The bottom ring is eight 18024 walls bound as
 * {@code ONLY_ITEM_FLUID_ENERGY_IN}. The top center is 18024
 * {@code NOTHING}. The eight 18101 transmitters are {@code NOTHING}; HU is
 * inserted into the block above each of them.
 */
public final class LargeHeatExchangerStructure {
    public static final Vec3i TOP_CENTER = new Vec3i(0, 1, 0);
    private static final Vec3i[] BOTTOM_WALLS = {
        new Vec3i(-1, 0, -1), new Vec3i(0, 0, -1), new Vec3i(1, 0, -1),
        new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0),
        new Vec3i(-1, 0, 1), new Vec3i(0, 0, 1), new Vec3i(1, 0, 1)
    };
    private static final Vec3i[] TRANSMITTERS = {
        new Vec3i(-1, 1, -1), new Vec3i(0, 1, -1), new Vec3i(1, 1, -1),
        new Vec3i(-1, 1, 0), new Vec3i(1, 1, 0),
        new Vec3i(-1, 1, 1), new Vec3i(0, 1, 1), new Vec3i(1, 1, 1)
    };

    private LargeHeatExchangerStructure() {}

    public static Vec3i[] bottomWalls() {
        return BOTTOM_WALLS;
    }

    public static Vec3i[] transmitters() {
        return TRANSMITTERS;
    }

    public static boolean check(Level level, BlockPos controller) {
        LargeHeatExchangerProfile profile = LargeHeatExchangerCatalog.profile();
        Block wall = mte(profile.wallId());
        Block transmitter = mte(profile.transmitterId());
        if (wall == null || transmitter == null) {
            return false;
        }
        for (Vec3i offset : BOTTOM_WALLS) {
            if (!level.getBlockState(controller.offset(offset)).is(wall)) {
                return false;
            }
        }
        if (!level.getBlockState(controller.offset(TOP_CENTER)).is(wall)) {
            return false;
        }
        for (Vec3i offset : TRANSMITTERS) {
            if (!level.getBlockState(controller.offset(offset)).is(transmitter)) {
                return false;
            }
        }
        return true;
    }

    private static Block mte(ResourceLocation id) {
        var holder = ModBlocks.mteInPlaceBlocksById().get(id);
        return holder == null ? null : holder.get();
    }
}
