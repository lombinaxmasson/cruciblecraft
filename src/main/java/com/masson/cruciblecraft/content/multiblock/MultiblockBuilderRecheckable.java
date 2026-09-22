package com.masson.cruciblecraft.content.multiblock;

/** Optional immediate validation hook used after a builder placement. */
public interface MultiblockBuilderRecheckable {
    void requestBuilderRecheck();
}
