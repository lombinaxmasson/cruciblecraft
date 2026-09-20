package com.masson.cruciblecraft.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;
import org.junit.jupiter.api.Test;

class WoodDebarkTest {
    @Test
    void beamCokeAmountsFollowGt6BeamEntry() {
        assertEquals(1, WoodDebark.beamCharcoal(GtTreeSpecies.RUBBER));
        assertEquals(300, WoodDebark.beamCreosoteMb(GtTreeSpecies.RUBBER));
        assertEquals(1, WoodDebark.beamCharcoal(GtTreeSpecies.MAPLE));
        assertEquals(350, WoodDebark.beamCreosoteMb(GtTreeSpecies.MAPLE));
        assertEquals(2, WoodDebark.beamCharcoal(GtTreeSpecies.WILLOW));
        assertEquals(400, WoodDebark.beamCreosoteMb(GtTreeSpecies.WILLOW));
        assertEquals(1, WoodDebark.beamCharcoal(GtTreeSpecies.RAINBOWOOD));
        assertEquals(400, WoodDebark.beamCreosoteMb(GtTreeSpecies.RAINBOWOOD));
        assertEquals(1, WoodDebark.beamCharcoal(GtTreeSpecies.CINNAMON));
        assertEquals(200, WoodDebark.beamCreosoteMb(GtTreeSpecies.CINNAMON));
        assertEquals(1, WoodDebark.vanillaBeamCharcoal());
        assertEquals(200, WoodDebark.vanillaBeamCreosoteMb());
    }

    @Test
    void washerCountsMatchVanillaPairsPlusGtLogs() {
        assertEquals(21, WoodDebark.VANILLA_PAIRS.size());
        assertEquals(11, WoodDebark.VANILLA_BEAM_COKE_INPUTS.size());
        assertEquals(9, GtTreeSpecies.ALL.size());
    }
}
