package com.masson.cruciblecraft.content.multiblock;

import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;

import net.minecraft.resources.ResourceLocation;

/** Runtime contract used by generic ports without knowing a machine kind. */
public interface MultiblockControllerBinding
        extends MultiblockBuilderRecheckable {
    ResourceLocation structureId();

    boolean structureValid();

    /**
     * Builder-wand hook. Controllers that keep a positive formed cache may
     * override this to force an immediate validation; invalid controllers
     * already recheck on their next server tick.
     */
    default void requestBuilderRecheck() {}

    /** Shared processing host; null for conversion/storage controllers. */
    ProcessingMachineBlockEntity processingHost();

    /** Capability host bridged by ports. Processing hosts return
     * themselves; conversion/storage controllers implement it directly. */
    default MultiblockPortHost portHost() {
        return (MultiblockPortHost) processingHost();
    }
}
