package com.masson.cruciblecraft.energy.vondagraagg;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.content.multiblock.CoilHosts;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * GT6 Von Da Graagg: cornerless 5x5x2 dense galvanized base (41 walls),
 * 5 copper coils, dense-steel cap wrap. Galvanized cells are
 * {@code ONLY_ENERGY_IN}.
 */
public final class VonDaGraaggStructure {
    public static final int COPPER_COILS = 5;
    public static final int GALVANIZED = 41;
    public static final net.minecraft.resources.ResourceLocation STRUCTURE_ID =
            CoilHosts.id("von_da_graagg");

    private VonDaGraaggStructure() {}

    public record Port(BlockPos pos, PortType type) {
        public Port {
            pos = pos.immutable();
        }
    }

    public record Check(boolean formed, List<Port> energyIn) {}

    public static Check check(Level level, BlockPos controller) {
        Block galvanized = CoilHosts.block(CoilHosts.DENSE_GALVANIZED);
        Block coil = CoilHosts.block(CoilHosts.COPPER);
        Block steel = CoilHosts.block(CoilHosts.DENSE_STEEL);
        List<Port> energyIn = new ArrayList<>();
        boolean ok = true;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i * j) >= 4) {
                    continue;
                }
                for (int y = 0; y <= 1; y++) {
                    BlockPos pos = controller.offset(i, y, j);
                    if (pos.equals(controller)) {
                        continue;
                    }
                    if (level.getBlockState(pos).getBlock() != galvanized) {
                        ok = false;
                    } else {
                        energyIn.add(new Port(pos, PortType.ENERGY_INPUT));
                    }
                }
            }
        }
        for (int y = 2; y <= 6; y++) {
            if (level.getBlockState(controller.offset(0, y, 0)).getBlock() != coil) {
                ok = false;
            }
        }
        if (level.getBlockState(controller.offset(0, 7, 0)).getBlock() != steel) {
            ok = false;
        }
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (i == 0 && j == 0) {
                    continue;
                }
                if (level.getBlockState(controller.offset(i, 6, j)).getBlock() != steel) {
                    ok = false;
                }
                if (i * j == 0) {
                    if (level.getBlockState(controller.offset(i, 5, j)).getBlock() != steel
                            || level.getBlockState(controller.offset(i, 7, j)).getBlock()
                                    != steel) {
                        ok = false;
                    }
                }
            }
        }
        return new Check(ok, List.copyOf(energyIn));
    }
}
