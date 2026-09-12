package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class GtTreeLeavesBlock extends LeavesBlock {
    private final GtTreeSpecies species;

    public GtTreeLeavesBlock(GtTreeSpecies species, Properties properties) {
        super(properties);
        this.species = species;
    }

    public GtTreeSpecies species() {
        return species;
    }

    public static int rainbowColor(BlockPos pos) {
        if (pos == null) {
            return 0xFFFFFF;
        }
        float hue = Math.floorMod(pos.getX() * 3 + pos.getY() * 5 + pos.getZ() * 7, 360) / 360.0F;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.65F, 1.0F);
        return rgb & 0xFFFFFF;
    }

    public static int itemColor(BlockState state, int tintIndex) {
        if (tintIndex != 0) {
            return 0xFFFFFF;
        }
        return 0xFF66CC;
    }
}
