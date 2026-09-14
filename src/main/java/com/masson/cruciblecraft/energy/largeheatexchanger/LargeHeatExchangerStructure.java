package com.masson.cruciblecraft.energy.largeheatexchanger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.masson.cruciblecraft.registry.ModBlocks;

/**
 * GT6 {@code checkStructure2}: 3x3x2 world-axis, controller at bottom center.
 * Bottom ring is eight dense tungsten walls; top is eight heat transmitters
 * around a center wall.
 */
public final class LargeHeatExchangerStructure {
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
        if (!level.getBlockState(controller.above()).is(wall)) {
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
