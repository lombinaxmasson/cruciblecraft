package com.masson.cruciblecraft.energy.lightningrod;

import com.masson.cruciblecraft.content.multiblock.CoilHosts;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * GT6 lightning rod: 3x3 tungsten / NbTi / tungsten / NbTi / tungsten,
 * then an unbounded 18104 pillar. Pillar length is not a formation failure.
 */
public final class LightningRodStructure {
    public static final int BASE_LAYERS = 5;

    private LightningRodStructure() {}

    public record Check(boolean formed, int size) {}

    public static Check check(Level level, BlockPos controller) {
        Block wall = CoilHosts.block(CoilHosts.TUNGSTEN_WALL);
        Block coil = CoilHosts.block(CoilHosts.NIOBIUM_TITANIUM);
        Block rod = CoilHosts.block(CoilHosts.LIGHTNING_ROD_PART);
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                if (!is(level, controller.offset(i, 0, j), wall)
                        && !(i == 0 && j == 0)) {
                    return new Check(false, 0);
                }
                if (!is(level, controller.offset(i, 1, j), coil)
                        || !is(level, controller.offset(i, 2, j), wall)
                        || !is(level, controller.offset(i, 3, j), coil)
                        || !is(level, controller.offset(i, 4, j), wall)) {
                    return new Check(false, 0);
                }
            }
        }
        int size = 0;
        while (is(level, controller.offset(0, BASE_LAYERS + size, 0), rod)) {
            size++;
        }
        return new Check(true, size);
    }

    public static BlockPos tip(BlockPos controller, int size) {
        return controller.offset(0, BASE_LAYERS - 1 + size, 0);
    }

    private static boolean is(Level level, BlockPos pos, Block expected) {
        return level.getBlockState(pos).getBlock() == expected;
    }
}
