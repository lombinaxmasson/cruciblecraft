package com.masson.cruciblecraft.content.multiblock;

import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;

import net.minecraft.resources.ResourceLocation;

/** Runtime contract used by generic ports without knowing a machine kind. */
public interface MultiblockControllerBinding {
    ResourceLocation structureId();

    boolean structureValid();

    /** Shared processing host; null for conversion/storage controllers. */
    ProcessingMachineBlockEntity processingHost();

    /** Capability host bridged by ports. Processing hosts return
     * themselves; conversion/storage controllers implement it directly. */
    default MultiblockPortHost portHost() {
        return (MultiblockPortHost) processingHost();
    }
}
