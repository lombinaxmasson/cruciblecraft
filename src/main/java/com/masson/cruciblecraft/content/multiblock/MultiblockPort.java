package com.masson.cruciblecraft.content.multiblock;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Bindable typed endpoint discovered by the shared structure validator. */
public interface MultiblockPort {
    MultiblockStructureDefinition.PortType portType();

    void bind(BlockPos controller, ResourceLocation structureId);

    void unbind(BlockPos controller);

    Optional<BlockPos> controllerPosition();

    Optional<ResourceLocation> structureId();
}
