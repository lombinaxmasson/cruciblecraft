package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.logistics.core.LogisticsCorePart;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreTagged;

import net.minecraft.world.level.block.Block;

/** Simple hull / CPU cube for the Logistics Core. */
public final class LogisticsCorePartBlock extends Block
        implements LogisticsCoreTagged {
    private final LogisticsCorePart part;

    public LogisticsCorePartBlock(
            LogisticsCorePart part, Properties properties) {
        super(properties);
        this.part = part;
    }

    @Override
    public LogisticsCorePart corePart() {
        return part;
    }
}
