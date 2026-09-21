package com.masson.cruciblecraft.content.multiblock;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * GT6 Matter Fabricator 17199: 5x5x5 hollow dense lead, 3x3x3 hollow osmium
 * coils with air center, 16 vents, 1 versatile, ≥4 control and ≥4 conversion
 * processors. Walls and coils are {@code ONLY_ITEM_FLUID_ENERGY}.
 */
public final class MatterFabricatorStructure {
    public static final int OSMIUM_COILS = 26;
    public static final int DENSE_LEAD = 97;
    public static final int VENTS = 16;
    public static final net.minecraft.resources.ResourceLocation STRUCTURE_ID =
            CoilHosts.id("large_matter_fabricator");

    private MatterFabricatorStructure() {}

    public record Port(BlockPos pos, PortType type) {
        public Port {
            pos = pos.immutable();
        }
    }

    public record Check(
            boolean formed,
            int coils,
            int lead,
            List<Port> ports) {}

    public static BlockPos origin(BlockPos controller, Direction facing) {
        return controller.relative(facing, 2).offset(-2, 0, -2);
    }

    public static Check check(Level level, BlockPos controller, Direction facing) {
        if (facing == null || facing.getAxis().isVertical()) {
            return new Check(false, 0, 0, List.of());
        }
        Block lead = CoilHosts.block(CoilHosts.DENSE_LEAD);
        Block coil = CoilHosts.block(CoilHosts.OSMIUM);
        Block vent = ModBlocks.VENTILATION_UNIT.get();
        Block versatile = ModBlocks.VERSATILE_PROCESSOR_UNIT.get();
        Block control = ModBlocks.CONTROL_PROCESSOR_UNIT.get();
        Block conversion = ModBlocks.CONVERSION_PROCESSOR_UNIT.get();
        BlockPos origin = origin(controller, facing);
        List<Port> ports = new ArrayList<>();
        int coils = 0;
        int leadCount = 0;
        boolean ok = true;
        for (int y = 0; y <= 4; y++) {
            for (int x = 0; x <= 4; x++) {
                for (int z = 0; z <= 4; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (pos.equals(controller)) {
                        continue;
                    }
                    boolean inner = x >= 1 && x <= 3 && z >= 1 && z <= 3 && y >= 1 && y <= 3;
                    boolean center = x == 2 && y == 2 && z == 2;
                    Block found = level.getBlockState(pos).getBlock();
                    if (center) {
                        if (!level.getBlockState(pos).isAir()) {
                            ok = false;
                        }
                        continue;
                    }
                    if (inner) {
                        if (found != coil) {
                            ok = false;
                        } else {
                            coils++;
                            ports.add(new Port(pos, PortType.ITEM_FLUID_ENERGY));
                        }
                    } else {
                        if (found != lead) {
                            ok = false;
                        } else {
                            leadCount++;
                            ports.add(new Port(pos, PortType.ITEM_FLUID_ENERGY));
                        }
                    }
                }
            }
        }
        int vents = 0;
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                if (x != 0 && x != 4 && z != 0 && z != 4) {
                    continue;
                }
                BlockPos pos = origin.offset(x, 5, z);
                if (level.getBlockState(pos).getBlock() == vent) {
                    vents++;
                } else {
                    ok = false;
                }
            }
        }
        if (level.getBlockState(origin.offset(2, 5, 2)).getBlock() != versatile) {
            ok = false;
        }
        int controlCount = 0;
        int conversionCount = 0;
        int[][] innerRing = {
                {1, 1}, {2, 1}, {3, 1},
                {1, 2}, {3, 2},
                {1, 3}, {2, 3}, {3, 3}
        };
        for (int[] cell : innerRing) {
            Block found = level.getBlockState(origin.offset(cell[0], 5, cell[1])).getBlock();
            if (found == control) {
                controlCount++;
            } else if (found == conversion) {
                conversionCount++;
            }
        }
        if (controlCount < 4 || conversionCount < 4 || vents != VENTS) {
            ok = false;
        }
        return new Check(
                ok && coils == OSMIUM_COILS && leadCount == DENSE_LEAD,
                coils,
                leadCount,
                List.copyOf(ports));
    }
}
