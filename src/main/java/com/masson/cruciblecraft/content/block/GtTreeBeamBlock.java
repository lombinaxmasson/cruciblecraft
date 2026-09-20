package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;

import net.minecraft.world.level.block.RotatedPillarBlock;

/**
 * GT-tree debarked log. Vanilla woods reuse {@code stripped_*} blocks as
 * {@code beamWood}; these nine have no vanilla stripped form.
 */
public final class GtTreeBeamBlock extends RotatedPillarBlock {
    private final GtTreeSpecies species;

    public GtTreeBeamBlock(GtTreeSpecies species, Properties properties) {
        super(properties);
        this.species = species;
    }

    public GtTreeSpecies species() {
        return species;
    }
}
