package com.masson.cruciblecraft.energy.bedrockdrill;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.content.block.BedrockOreBlock;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.worldgen.BedrockOreVeins;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 17999 structure: 3x3 bedrock / bedrock-ore at y-5, 3x3 heads at y-4,
 * dense titanium walls filling the 3x3x4 tower, controller at the top center.
 */
public final class BedrockDrillStructure {
    public static final ResourceLocation WALL_ID = ResourceLocation.fromNamespaceAndPath(
            "cruciblecraft", "multiblock/dense_titanium_wall");
    public static final long RU_PER_OPERATION = 32_768L;
    /** @deprecated use {@link #RU_PER_OPERATION} */
    public static final long RU_PER_TICK = RU_PER_OPERATION;
    public static final int LUBE_PER_OPERATION = 100;
    /** @deprecated use {@link #LUBE_PER_OPERATION} */
    public static final int LUBE_PER_TICK = LUBE_PER_OPERATION;
    public static final int TANK_CAPACITY = 16_000;

    private BedrockDrillStructure() {}

    /**
     * GT6 assigns different capabilities to the layers of the tower. Keep
     * that projection beside the structure check so builders and tests do not
     * reduce the 3x4x3 machine to an undifferentiated block volume.
     */
    public enum PortKind {
        BEDROCK_FLOOR,
        DRILL_HEAD,
        FLUID_INPUT,
        ENERGY_INPUT,
        CONTROLLER,
        NONE
    }

    public static PortKind portKind(int dx, int dy, int dz) {
        if (dx < -1 || dx > 1 || dz < -1 || dz > 1) {
            return PortKind.NONE;
        }
        if (dy == -5) {
            return PortKind.BEDROCK_FLOOR;
        }
        if (dy == -4) {
            return PortKind.DRILL_HEAD;
        }
        if (dy == -3 || dy == -2) {
            return PortKind.FLUID_INPUT;
        }
        if (dy == -1) {
            return Math.abs(dx) + Math.abs(dz) == 1
                    ? PortKind.ENERGY_INPUT
                    : PortKind.FLUID_INPUT;
        }
        if (dy == 0) {
            return dx == 0 && dz == 0
                    ? PortKind.CONTROLLER
                    : PortKind.FLUID_INPUT;
        }
        return PortKind.NONE;
    }

    public static boolean check(Level level, BlockPos controller) {
        if (controller.getY() < level.getMinBuildHeight() + 5) {
            return false;
        }
        Block head = ModBlocks.BEDROCK_DRILL_HEAD.get();
        var wallHolder = ModBlocks.mteInPlaceBlocksById().get(WALL_ID);
        if (wallHolder == null) {
            return false;
        }
        Block wall = wallHolder.get();
        boolean floorOk = true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!isFloor(level.getBlockState(controller.offset(dx, -5, dz)))) {
                    floorOk = false;
                }
                if (level.getBlockState(controller.offset(dx, -4, dz)).getBlock()
                        != head) {
                    return false;
                }
                for (int dy = -3; dy <= 0; dy++) {
                    if (dy == 0 && dx == 0 && dz == 0) {
                        continue;
                    }
                    if (level.getBlockState(controller.offset(dx, dy, dz)).getBlock()
                            != wall) {
                        return false;
                    }
                }
            }
        }
        return floorOk;
    }

    public static List<String> probeMaterials(Level level, BlockPos controller) {
        List<String> materials = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos ore = controller.offset(dx, -5, dz);
                BlockState state = level.getBlockState(ore);
                if (!(state.getBlock() instanceof BedrockOreBlock bedrockOre)) {
                    continue;
                }
                BedrockOreBlock.materialAt(level, ore).ifPresent(material -> {
                    materials.add(material);
                    if (!bedrockOre.small()) {
                        materials.add(material);
                    }
                });
            }
        }
        return materials;
    }

    public static boolean isFloor(BlockState state) {
        return BedrockOreVeins.isBedrockFloor(state) || state.is(Blocks.BEDROCK);
    }
}
