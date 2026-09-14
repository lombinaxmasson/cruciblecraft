package com.masson.cruciblecraft.energy.longdistance;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Same-voltage long-distance conductor. Not an EU cable and not a transformer. */
public final class LongDistanceWireBlock extends Block {
    private final LongDistanceWireProfile profile;

    public LongDistanceWireBlock(
            LongDistanceWireProfile profile, BlockBehaviour.Properties properties) {
        super(properties);
        this.profile = profile;
    }

    public LongDistanceWireProfile profile() {
        return profile;
    }

    public long voltage() {
        return profile.voltage();
    }
}
