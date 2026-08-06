package com.masson.cruciblecraft.content.multiblock;

import com.masson.cruciblecraft.content.blockentity.ProcessingMachineBlockEntity;

/** Runtime contract used by generic ports without knowing a machine kind. */
public interface MultiblockControllerBinding {
    MultiblockControllerSpec controllerSpec();

    boolean structureValid();

    ProcessingMachineBlockEntity processingHost();
}
