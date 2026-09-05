package com.masson.cruciblecraft.logistics.core;

import net.minecraft.world.level.block.Block;

/** CPU / hull parts that fill the 5x5x5 Logistics Core. */
public enum LogisticsCorePart {
    CONTROLLER,
    WALL,
    VENT,
    VERSATILE,
    LOGIC,
    CONTROL,
    STORAGE,
    CONVERSION;

    public boolean innerAllowed() {
        return this == WALL
                || this == VERSATILE
                || this == LOGIC
                || this == CONTROL
                || this == STORAGE
                || this == CONVERSION;
    }

    public static LogisticsCorePart of(Block block) {
        if (block instanceof LogisticsCoreTagged tagged) {
            return tagged.corePart();
        }
        return null;
    }
}
